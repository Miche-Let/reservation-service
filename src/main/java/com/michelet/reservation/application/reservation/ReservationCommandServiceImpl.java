package com.michelet.reservation.application.reservation;

import com.michelet.common.exception.BusinessException;
import com.michelet.reservation.application.event.ReservationCreatedAppEvent;
import com.michelet.reservation.application.exception.ExternalCallFailedException;
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
import com.michelet.reservation.application.port.TimeSlotPort;
import com.michelet.reservation.application.port.WaitingPort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

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

        // Phase 1: 슬롯 차감 — 실패 시 WAITING 유지 (트랜잭션 커밋)
        // 비즈니스 거부(슬롯 부족 등)는 BusinessException 전파 → 트랜잭션 롤백
        // TODO: Outbox 패턴 도입 후 WAITING 예약 자동 재처리 스케줄러 구현 필요
        try {
            timeSlotPort.decrementStock(saved.getTimeSlotId(), saved.getGuestCount().value(), saved.getId());
        } catch (ExternalCallFailedException e) {
            log.warn("[create] timeslot-service 호출 실패 — 예약 WAITING 유지 (reservationId={})", saved.getId(), e);
            return toResult(saved, savedCourses);
        }

        // Phase 2: 대기열 완료 — 슬롯 차감 성공 이후이므로 실패해도 예약 확정 진행
        // 대기열 항목은 TTL 만료 또는 운영자 정리로 처리됨
        // TODO: WaitingAdapter에도 ExternalCallFailedException 래핑 적용 필요
        try {
            waitingPort.completeWaiting(tokenResult.waitingId());
        } catch (RuntimeException e) {
            log.warn("[create] 대기열 완료 처리 실패 — 예약 확정 계속 진행 (waitingId={})", tokenResult.waitingId(), e);
        }

        saved.confirm();
        Reservation confirmed = reservationRepository.save(saved);

        // DB 커밋 완료 후 Kafka 발행 — AFTER_COMMIT 훅을 통해 처리됨
        eventPublisher.publishEvent(new ReservationCreatedAppEvent(
                confirmed.getId(),
                confirmed.getUserId(),
                confirmed.getRestaurantId(),
                confirmed.getTimeSlotId(),
                confirmed.getReservedDate(),
                confirmed.getGuestCount().value()
        ));

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
            // 신규 슬롯 차감 먼저 확인 — 실패 시 원래 슬롯 복구 없이 트랜잭션 롤백
            try {
                timeSlotPort.decrementStock(newTimeSlotId, newGuestCount, command.reservationId());
            } catch (ExternalCallFailedException e) {
                log.error("[modify] timeslot-service 호출 실패 — 예약 변경 롤백 (reservationId={})", command.reservationId(), e);
                throw new BusinessException(ReservationErrorCode.TIMESLOT_SERVICE_UNAVAILABLE);
            }
            // 신규 슬롯 차감 성공 확인 후 원래 슬롯 복구
            timeSlotPort.incrementStock(originalTimeSlotId, originalGuestCount);
        }

        return toResult(saved, courses);
    }

    @Override
    public void cancel(CancelReservationCommand command) {
        Reservation reservation = findAndVerifyOwnership(command.reservationId(), command.userId(), command.userRole());

        reservation.cancel();
        reservationRepository.save(reservation);

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
