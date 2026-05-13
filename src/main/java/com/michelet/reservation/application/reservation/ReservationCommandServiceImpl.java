package com.michelet.reservation.application.reservation;

import com.michelet.common.exception.BusinessException;
import com.michelet.reservation.application.exception.ExternalCallFailedException;
import com.michelet.reservation.application.port.OutboxEventPort;
import com.michelet.reservation.application.port.TimeSlotPort;
import com.michelet.reservation.application.port.WaitingPort;
import com.michelet.reservation.application.reservation.command.CancelReservationCommand;
import com.michelet.reservation.application.reservation.command.CheckInCommand;
import com.michelet.reservation.application.reservation.command.CreateReservationCommand;
import com.michelet.reservation.application.reservation.command.DeleteReservationCommand;
import com.michelet.reservation.application.reservation.command.ModifyReservationCommand;
import com.michelet.reservation.application.reservation.result.ReservationCourseResult;
import com.michelet.reservation.application.reservation.result.ReservationResult;
import com.michelet.reservation.application.reservation.result.ReservationStatusResult;
import com.michelet.reservation.domain.entity.Reservation;
import com.michelet.reservation.domain.entity.ReservationCourse;
import com.michelet.reservation.domain.enums.ReservationStatus;
import com.michelet.reservation.domain.exception.ReservationErrorCode;
import com.michelet.reservation.domain.repository.ReservationCourseRepository;
import com.michelet.reservation.domain.repository.ReservationRepository;
import com.michelet.reservation.domain.vo.GuestCount;
import com.michelet.reservation.domain.vo.Money;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReservationCommandServiceImpl implements ReservationCommandService {

    private final ReservationRepository reservationRepository;
    private final ReservationCourseRepository reservationCourseRepository;
    private final TimeSlotPort timeSlotPort;
    private final WaitingPort waitingPort;
    private final OutboxEventPort outboxEventPort;

    @Override
    public ReservationResult create(CreateReservationCommand command) {
        // waiting-service가 ACTIVE 상태인지 확인 — userId·restaurantId 바인딩 검증은
        // waiting-service API가 해당 필드를 응답에 포함할 때 추가 예정
        var tokenResult = waitingPort.verifyToken(command.waitingToken());
        if (!tokenResult.valid()) {
            throw new BusinessException(ReservationErrorCode.INVALID_WAITING_TOKEN);
        }

        checkDuplicate(command.userId(), command.timeSlotId(), command.reservedDate());

        LocalDateTime noshowDeadline = LocalDateTime.of(command.reservedDate(), command.slotStartTime()).plusMinutes(30);

        // WAITING 상태로 먼저 저장 — feign 호출 전 예약 레코드 확보
        Reservation reservation = Reservation.create(
                command.userId(),
                command.restaurantId(),
                command.timeSlotId(),
                command.reservedDate(),
                GuestCount.of(command.guestCount()),
                noshowDeadline
        );
        Reservation saved = reservationRepository.save(reservation);
        List<ReservationCourse> savedCourses = saveCourses(saved.getId(), command.courses());

        // 슬롯 차감 — 오버셀링 방지를 위해 동기 Feign 유지
        // 비즈니스 거부(슬롯 부족)는 BusinessException 전파 → 트랜잭션 롤백
        // 외부 장애 시 즉시 실패 처리 (트랜잭션 롤백 — WAITING 레코드 제거)
        try {
            timeSlotPort.decrementStock(saved.getTimeSlotId(), saved.getGuestCount().value(), saved.getId());
        } catch (ExternalCallFailedException e) {
            // 슬롯 차감 실패(타임아웃 포함) 시 즉시 실패 처리 — WAITING 상태를 고객에게 노출하지 않음
            // 고객은 즉시 재시도 가능하며, WAITING 레코드는 트랜잭션 롤백으로 함께 제거됨
            //
            // [주의] 타임아웃 케이스에서 timeslot-service가 실제로 차감을 완료했을 가능성이 있음.
            // 이 경우 예약 레코드도 없고 reservation.cancelled 이벤트도 발행되지 않으므로
            // timeslot-service 측에 고아 차감(orphaned deduction)이 발생할 수 있음.
            // → 향후 Saga 보상 트랜잭션 설계 시 timeslot-service가
            //    "reservationId 기준으로 일정 시간 내 예약 확정 이벤트 미수신 시 자동 복구"
            //    로직을 갖춰야 이 케이스가 완전히 해결됨.
            throw new BusinessException(ReservationErrorCode.TIMESLOT_SERVICE_UNAVAILABLE);
        }

        // Phase 2: 대기열 완료 — Feign 직접 호출 (빠른 경로)
        // 실패해도 예약 확정 계속 진행 (outbox 이벤트가 재처리 보장)
        // waiting-service는 Feign + Kafka 양쪽에서 수신할 수 있으므로 멱등 처리 필수
        try {
            waitingPort.completeWaiting(tokenResult.waitingId());
        } catch (RuntimeException e) {
            log.warn("[create] 대기열 완료 Feign 호출 실패 — outbox 이벤트로 재처리 보장 (waitingId={})", tokenResult.waitingId(), e);
        }

        saved.confirm();
        Reservation confirmed = reservationRepository.save(saved);

        // reservation.created + waiting.completed 를 동일 트랜잭션 내 outbox에 적재
        // → Feign 성공 여부와 무관하게 Kafka 발행 보장 (waiting-service 측 멱등 처리 전제)
        LocalDateTime now = LocalDateTime.now();
        outboxEventPort.recordReservationCreated(
                confirmed.getId(), confirmed.getUserId(), confirmed.getRestaurantId(),
                confirmed.getTimeSlotId(), confirmed.getReservedDate(),
                confirmed.getGuestCount().value(), now);
        outboxEventPort.recordWaitingCompleted(tokenResult.waitingId(), confirmed.getId(), now);

        return toResult(confirmed, savedCourses);
    }

    @Override
    public ReservationResult modify(ModifyReservationCommand command) {
        Reservation reservation = findAndVerifyOwnership(command.reservationId(), command.userId(), command.userRole());

        UUID originalTimeSlotId = reservation.getTimeSlotId();
        LocalDate originalDate = reservation.getReservedDate();
        int originalGuestCount = reservation.getGuestCount().value();

        UUID newTimeSlotId = command.timeSlotId() != null ? command.timeSlotId() : originalTimeSlotId;
        LocalDate newDate = command.reservedDate() != null ? command.reservedDate() : originalDate;
        Integer newGuestCount =
                command.guestCount() != null ? command.guestCount() : reservation.getGuestCount().value();

        LocalDateTime newNoshowDeadline = resolveNoshowDeadline(reservation, command.reservedDate(), command.slotStartTime(), newDate);

        boolean slotChanged = !newTimeSlotId.equals(originalTimeSlotId) || !newDate.equals(originalDate);
        boolean guestCountChanged = newGuestCount != originalGuestCount;

        if (slotChanged) {
            // timeSlotId가 바뀌면 새 슬롯의 시작 시각이 달라질 수 있으므로 slotStartTime 필수
            if (!newTimeSlotId.equals(originalTimeSlotId) && command.slotStartTime() == null) {
                throw new BusinessException(ReservationErrorCode.SLOT_START_TIME_REQUIRED);
            }
            checkDuplicate(reservation.getUserId(), newTimeSlotId, newDate);
        }

        reservation.modify(newTimeSlotId, newDate, GuestCount.of(newGuestCount), newNoshowDeadline);
        Reservation saved = reservationRepository.save(reservation);
        List<ReservationCourse> courses = updateCourses(saved.getId(), command.courses());

        if (slotChanged) {
            // 슬롯 변경: 새 슬롯에 새 인원 전체 차감 확인 후 원래 슬롯 복구
            try {
                timeSlotPort.decrementStock(newTimeSlotId, newGuestCount, command.reservationId());
            } catch (ExternalCallFailedException e) {
                log.error("[modify] timeslot-service 호출 실패 — 예약 변경 롤백 (reservationId={})", command.reservationId(), e);
                throw new BusinessException(ReservationErrorCode.TIMESLOT_SERVICE_UNAVAILABLE);
            }
            // [주의] decrementStock 성공 후 incrementStock이 실패하면 DB는 롤백되지만
            // 새 슬롯의 차감은 취소 불가 → 고아 차감(orphaned deduction) 발생.
            // create()의 타임아웃 케이스와 동일한 구조적 한계이며, Saga 보상 트랜잭션 도입 시 해결 예정.
            timeSlotPort.incrementStock(originalTimeSlotId, originalGuestCount);
        } else if (guestCountChanged) {
            // 동일 슬롯에서 인원만 변경: delta(newGuestCount - originalGuestCount)만 처리
            int delta = newGuestCount - originalGuestCount;
            if (delta > 0) {
                try {
                    timeSlotPort.decrementStock(newTimeSlotId, delta, command.reservationId());
                } catch (ExternalCallFailedException e) {
                    log.error("[modify] 인원 증가 차감 실패 — 예약 변경 롤백 (reservationId={})", command.reservationId(), e);
                    throw new BusinessException(ReservationErrorCode.TIMESLOT_SERVICE_UNAVAILABLE);
                }
            } else {
                timeSlotPort.incrementStock(originalTimeSlotId, -delta);
            }
        }

        return toResult(saved, courses);
    }

    @Override
    public void cancel(CancelReservationCommand command) {
        Reservation reservation = findAndVerifyOwnership(command.reservationId(), command.userId(), command.userRole());

        reservation.cancel();
        Reservation saved = reservationRepository.save(reservation);

        outboxEventPort.recordReservationCancelled(
                saved.getId(), saved.getUserId(), saved.getRestaurantId(),
                saved.getTimeSlotId(), saved.getReservedDate(),
                saved.getGuestCount().value(), saved.getStatus(), LocalDateTime.now());

        timeSlotPort.incrementStock(reservation.getTimeSlotId(), reservation.getGuestCount().value());
    }

    @Override
    public void delete(DeleteReservationCommand command) {
        Reservation reservation = findAndVerifyOwnership(command.reservationId(), command.userId(), command.userRole());
        reservationRepository.delete(reservation.getId(), command.userId());
        if (reservation.requiresSlotReturn()) {
            timeSlotPort.incrementStock(reservation.getTimeSlotId(), reservation.getGuestCount().value());
        }
    }

    @Override
    public ReservationStatusResult checkIn(CheckInCommand command) {
        Reservation reservation = reservationRepository.findById(command.reservationId())
                .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        if (!reservation.getRestaurantId().equals(command.restaurantId())) {
            throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND);
        }

        reservation.complete(LocalDateTime.now());
        Reservation saved = reservationRepository.save(reservation);

        outboxEventPort.recordCheckInCompleted(
                saved.getId(), saved.getRestaurantId(),
                saved.getReservedDate(), saved.getCheckedInAt());

        return ReservationStatusResult.from(saved);
    }

    private void checkDuplicate(UUID userId, UUID timeSlotId, LocalDate reservedDate) {
        if (reservationRepository.existsByUserIdAndTimeSlotIdAndReservedDateAndStatus(
                userId, timeSlotId, reservedDate, ReservationStatus.CONFIRMED)) {
            throw new BusinessException(ReservationErrorCode.DUPLICATE_RESERVATION);
        }
    }

    private Reservation findAndVerifyOwnership(UUID reservationId, UUID userId, String userRole) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));
        if ("MASTER".equalsIgnoreCase(userRole)) {
            return reservation;
        }
        // TODO: OWNER 역할 — restaurant-service RestaurantClient(Feign) 구현 후 소유 식당 예약만 허용
        // 현재 Feign 미구현 → fail-closed: OWNER도 userId 기반 검증 적용 (USER 동일 수준)
        if (!reservation.getUserId().equals(userId)) {
            throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND);
        }
        return reservation;
    }

    // noshowDeadline = slotStartTime + 30분 이므로, 역산: slotStartTime = noshowDeadline - 30분
    private LocalDateTime resolveNoshowDeadline(Reservation existing, LocalDate requestedDate,
                                                LocalTime requestedSlotStartTime, LocalDate effectiveDate) {
        if (requestedDate == null && requestedSlotStartTime == null) {
            return existing.getNoshowDeadline();
        }
        LocalTime slotStartTime = requestedSlotStartTime != null
                ? requestedSlotStartTime
                : existing.getNoshowDeadline().toLocalTime().minusMinutes(30);
        return LocalDateTime.of(effectiveDate, slotStartTime).plusMinutes(30);
    }

    private List<ReservationCourse> saveCourses(UUID reservationId, List<CreateReservationCommand.CourseItem> items) {
        return items.stream()
                .map(item -> reservationCourseRepository.save(
                        ReservationCourse.create(reservationId, item.courseId(), item.quantity(),
                                Money.of(item.unitPrice()))
                ))
                .toList();
    }

    /**
     * 코스 목록 수정 규칙:
     * - courses == null : 기존 코스 유지 (DB 재조회)
     * - courses == [] : 전체 삭제
     * - courses != [] : 전체 교체 (삭제 후 새로 저장)
     */
    private List<ReservationCourse> updateCourses(UUID reservationId, List<ModifyReservationCommand.CourseItem> items) {
        if (items == null) {
            return reservationCourseRepository.findAllByReservationId(reservationId);
        }
        reservationCourseRepository.deleteAllByReservationId(reservationId);
        return items.stream()
                .map(item -> reservationCourseRepository.save(
                        ReservationCourse.create(reservationId, item.courseId(), item.quantity(),
                                Money.of(item.unitPrice()))
                ))
                .toList();
    }

    private ReservationResult toResult(Reservation reservation, List<ReservationCourse> courses) {
        List<ReservationCourseResult> courseResults = courses.stream()
                .map(ReservationCourseResult::from)
                .toList();
        return ReservationResult.of(reservation, courseResults);
    }
}
