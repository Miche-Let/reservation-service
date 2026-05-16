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
import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationCommandServiceImpl implements ReservationCommandService {

    private final ReservationRepository reservationRepository;
    private final ReservationCourseRepository reservationCourseRepository;
    private final TimeSlotPort timeSlotPort;
    private final WaitingPort waitingPort;
    private final OutboxEventPort outboxEventPort;
    private final PlatformTransactionManager txManager;

    private TransactionTemplate writeTx;
    private TransactionTemplate readTx;

    @PostConstruct
    void initTx() {
        writeTx = new TransactionTemplate(txManager);
        TransactionTemplate rt = new TransactionTemplate(txManager);
        rt.setReadOnly(true);
        readTx = rt;
    }

    // ── create ──────────────────────────────────────────────────────────────

    @Override
    public ReservationResult create(CreateReservationCommand command) {
        // 1단계: 대기열 토큰 검증 (Feign 호출, DB 커넥션 미점유)
        var tokenResult = waitingPort.verifyToken(command.waitingToken());
        if (!tokenResult.valid()) {
            throw new BusinessException(ReservationErrorCode.INVALID_WAITING_TOKEN);
        }

        // 1.5단계: 사전 중복 체크 — Feign 전에 빠르게 실패해 불필요한 슬롯 차감 방지
        // 쓰기 TX 내부에서 race window 방어를 위한 권위 있는 2차 체크를 다시 수행
        checkDuplicate(command.userId(), command.timeSlotId(), command.reservedDate());

        // 2단계: DB 쓰기 전 슬롯 용량 차감 (Feign 호출, DB 커넥션 미점유)
        // preId는 멱등성 키로만 사용; 실제 예약 엔티티는 도메인 팩토리에서 독립적으로 UUID 생성
        UUID preId = UUID.randomUUID();
        try {
            timeSlotPort.decrementStock(command.timeSlotId(), command.guestCount(), preId);
        } catch (ExternalCallFailedException e) {
            // 타임아웃: timeslot-service가 차감을 실제로 적용했을 수 있으므로 보상용 voided 이벤트 발행
            outboxEventPort.recordReservationCreationVoided(
                    preId, command.timeSlotId(), command.guestCount(), LocalDateTime.now());
            throw new BusinessException(ReservationErrorCode.TIMESLOT_SERVICE_UNAVAILABLE);
        }
        // BusinessException(예: SLOT_NOT_AVAILABLE)은 즉시 전파 — DB 미접촉이므로 보상 불필요

        // 3단계: DB 전용 트랜잭션 (SQL 실행 시간만 커넥션 점유)
        ReservationResult result;
        try {
            result = Objects.requireNonNull(
                    writeTx.execute(status -> persistConfirmedReservation(command, tokenResult.waitingId())));
        } catch (RuntimeException ex) {
            tryRestoreSlotQuietly(command.timeSlotId(), preId, "create-rollback", command.guestCount());
            throw ex;
        }

        // 4단계: 대기열 완료 처리 베스트-에포트 (Feign 호출, DB 커넥션 미점유)
        tryCompleteWaitingQuietly(tokenResult.waitingId());

        return result;
    }

    private ReservationResult persistConfirmedReservation(CreateReservationCommand command, UUID waitingId) {
        checkDuplicate(command.userId(), command.timeSlotId(), command.reservedDate());

        LocalDateTime noshowDeadline = LocalDateTime.of(command.reservedDate(), command.slotStartTime())
                .plusMinutes(30);
        Reservation reservation = Reservation.create(
                command.userId(), command.restaurantId(), command.timeSlotId(),
                command.reservedDate(), GuestCount.of(command.guestCount()), noshowDeadline);
        reservation.confirm();
        Reservation saved = reservationRepository.save(reservation);
        List<ReservationCourse> savedCourses = saveCourses(saved.getId(), command.courses());

        LocalDateTime now = LocalDateTime.now();
        outboxEventPort.recordReservationCreated(
                saved.getId(), saved.getUserId(), saved.getRestaurantId(),
                saved.getTimeSlotId(), saved.getReservedDate(), saved.getGuestCount().value(), now);
        outboxEventPort.recordWaitingCompleted(waitingId, saved.getId(), now);

        return toResult(saved, savedCourses);
    }

    // ── modify ──────────────────────────────────────────────────────────────

    @Override
    public ReservationResult modify(ModifyReservationCommand command) {
        // 1단계: 델타 계산을 위한 현재 슬롯 상태 스냅샷 로드 (짧은 읽기 전용 TX, 즉시 커넥션 반환)
        SlotSnapshot snapshot = Objects.requireNonNull(
                readTx.execute(status -> loadSlotSnapshot(command.reservationId(), command.userId(), command.userRole())));

        UUID originalTimeSlotId = snapshot.timeSlotId();
        LocalDate originalDate = snapshot.reservedDate();
        int originalGuestCount = snapshot.guestCount();

        UUID newTimeSlotId = command.timeSlotId() != null ? command.timeSlotId() : originalTimeSlotId;
        LocalDate newDate = command.reservedDate() != null ? command.reservedDate() : originalDate;
        int newGuestCount = command.guestCount() != null ? command.guestCount() : originalGuestCount;

        boolean slotChanged = !newTimeSlotId.equals(originalTimeSlotId) || !newDate.equals(originalDate);
        boolean guestCountChanged = newGuestCount != originalGuestCount;

        SlotChange slotChange = computeSlotChange(
                slotChanged, guestCountChanged,
                originalTimeSlotId, originalGuestCount, newTimeSlotId, newGuestCount);

        // Feign 전 사전 검증: timeSlotId 변경 시 slotStartTime 필수
        if (!newTimeSlotId.equals(originalTimeSlotId) && command.slotStartTime() == null) {
            throw new BusinessException(ReservationErrorCode.SLOT_START_TIME_REQUIRED);
        }

        // 2단계: 필요 시 신규 슬롯 용량 차감 (Feign 호출, DB 커넥션 미점유)
        if (slotChange.needsDeduct()) {
            try {
                timeSlotPort.decrementStock(
                        slotChange.deductTimeSlotId(), slotChange.deductCount(), command.reservationId());
            } catch (ExternalCallFailedException e) {
                log.error("[modify] timeslot-service 호출 실패 (reservationId={})", command.reservationId(), e);
                outboxEventPort.recordReservationModificationVoided(
                        command.reservationId(), slotChange.deductTimeSlotId(), slotChange.deductCount(),
                        LocalDateTime.now());
                throw new BusinessException(ReservationErrorCode.TIMESLOT_SERVICE_UNAVAILABLE);
            }
        }

        // 3단계: DB 전용 쓰기 트랜잭션 (SQL 실행 시간만 커넥션 점유)
        ReservationResult result;
        try {
            result = Objects.requireNonNull(writeTx.execute(status -> persistModification(command, snapshot)));
        } catch (ObjectOptimisticLockingFailureException e) {
            if (slotChange.needsDeduct()) {
                tryRestoreSlotQuietly(slotChange.deductTimeSlotId(), command.reservationId(),
                        "modify-rollback", slotChange.deductCount());
            }
            throw new BusinessException(ReservationErrorCode.CONCURRENT_UPDATE_CONFLICT);
        } catch (RuntimeException ex) {
            if (slotChange.needsDeduct()) {
                tryRestoreSlotQuietly(slotChange.deductTimeSlotId(), command.reservationId(),
                        "modify-rollback", slotChange.deductCount());
            }
            throw ex;
        }

        // 4단계: 필요 시 기존 슬롯 용량 복구 (Feign 호출, DB 커넥션 미점유, 베스트-에포트)
        if (slotChange.needsRestore()) {
            try {
                timeSlotPort.incrementStock(
                        slotChange.restoreTimeSlotId(), slotChange.restoreCount(),
                        "restore:" + slotChange.restoreTimeSlotId() + ":" + command.reservationId() + ":modify-slot");
            } catch (Exception e) {
                log.warn("[modify] 기존 슬롯 복구 Feign 실패 — outbox 이벤트로 비동기 복구 예정 (reservationId={}, timeSlotId={})",
                        command.reservationId(), slotChange.restoreTimeSlotId(), e);
                outboxEventPort.recordSlotReleased(
                        command.reservationId(), slotChange.restoreTimeSlotId(), slotChange.restoreCount(),
                        LocalDateTime.now());
            }
        }

        return result;
    }

    private SlotSnapshot loadSlotSnapshot(UUID reservationId, UUID userId, String userRole) {
        Reservation r = findAndVerifyOwnership(reservationId, userId, userRole);
        return new SlotSnapshot(r.getTimeSlotId(), r.getReservedDate(), r.getGuestCount().value(), r.getVersion());
    }

    private ReservationResult persistModification(ModifyReservationCommand command, SlotSnapshot snapshot) {
        Reservation reservation = findAndVerifyOwnership(command.reservationId(), command.userId(), command.userRole());

        // readTx 이후 타 TX가 수정했으면 스냅샷 기반 델타가 낡았을 수 있으므로 낙관적 충돌로 처리
        if (!Objects.equals(reservation.getVersion(), snapshot.version())) {
            throw new ObjectOptimisticLockingFailureException(Reservation.class, command.reservationId());
        }

        UUID originalTimeSlotId = reservation.getTimeSlotId();
        LocalDate originalDate = reservation.getReservedDate();
        UUID newTimeSlotId = command.timeSlotId() != null ? command.timeSlotId() : originalTimeSlotId;
        LocalDate newDate = command.reservedDate() != null ? command.reservedDate() : originalDate;
        int newGuestCount = command.guestCount() != null ? command.guestCount() : reservation.getGuestCount().value();

        if (!newTimeSlotId.equals(originalTimeSlotId) && command.slotStartTime() == null) {
            throw new BusinessException(ReservationErrorCode.SLOT_START_TIME_REQUIRED);
        }
        if (!newTimeSlotId.equals(originalTimeSlotId) || !newDate.equals(originalDate)) {
            checkDuplicate(reservation.getUserId(), newTimeSlotId, newDate);
        }

        LocalDateTime newNoshowDeadline = resolveNoshowDeadline(reservation, command.reservedDate(),
                command.slotStartTime(), newDate);

        reservation.modify(newTimeSlotId, newDate, GuestCount.of(newGuestCount), newNoshowDeadline);
        Reservation saved = reservationRepository.save(reservation);
        List<ReservationCourse> courses = updateCourses(saved.getId(), command.courses());

        return toResult(saved, courses);
    }

    private SlotChange computeSlotChange(boolean slotChanged, boolean guestCountChanged,
                                          UUID originalTimeSlotId, int originalGuestCount,
                                          UUID newTimeSlotId, int newGuestCount) {
        if (slotChanged) {
            return new SlotChange(true, newTimeSlotId, newGuestCount, true, originalTimeSlotId, originalGuestCount);
        }
        if (guestCountChanged) {
            int delta = newGuestCount - originalGuestCount;
            return delta > 0
                    ? new SlotChange(true, newTimeSlotId, delta, false, null, 0)
                    : new SlotChange(false, null, 0, true, originalTimeSlotId, -delta);
        }
        return new SlotChange(false, null, 0, false, null, 0);
    }

    // ── cancel ──────────────────────────────────────────────────────────────

    @Override
    public void cancel(CancelReservationCommand command) {
        // 1단계: DB 전용 트랜잭션
        SlotReturnInfo slotInfo = writeTx.execute(status -> cancelRecord(command));

        // 2단계: 슬롯 복구 베스트-에포트 (Feign 호출, DB 커넥션 미점유)
        // outbox reservation.cancelled 이벤트가 Feign 실패 시 비동기 복구 보장
        if (slotInfo != null) {
            tryRestoreSlotQuietly(slotInfo.timeSlotId(), slotInfo.reservationId(), "cancel", slotInfo.guestCount());
        }
    }

    private SlotReturnInfo cancelRecord(CancelReservationCommand command) {
        Reservation reservation = findAndVerifyOwnership(command.reservationId(), command.userId(), command.userRole());
        reservation.cancel();
        Reservation saved = reservationRepository.save(reservation);
        outboxEventPort.recordReservationCancelled(
                saved.getId(), saved.getUserId(), saved.getRestaurantId(),
                saved.getTimeSlotId(), saved.getReservedDate(),
                saved.getGuestCount().value(), saved.getStatus(), LocalDateTime.now());
        return new SlotReturnInfo(saved.getId(), saved.getTimeSlotId(), saved.getGuestCount().value());
    }

    // ── delete ──────────────────────────────────────────────────────────────

    @Override
    public void delete(DeleteReservationCommand command) {
        // 1단계: DB 전용 트랜잭션
        SlotReturnInfo slotInfo = writeTx.execute(status -> deleteRecord(command));

        // 2단계: 필요 시 슬롯 복구 베스트-에포트 (Feign 호출, DB 커넥션 미점유)
        if (slotInfo != null) {
            tryRestoreSlotQuietly(slotInfo.timeSlotId(), slotInfo.reservationId(), "delete", slotInfo.guestCount());
        }
    }

    private SlotReturnInfo deleteRecord(DeleteReservationCommand command) {
        Reservation reservation = findAndVerifyOwnership(command.reservationId(), command.userId(), command.userRole());
        reservationRepository.delete(reservation.getId(), command.userId());
        if (reservation.requiresSlotReturn()) {
            outboxEventPort.recordReservationDeleted(
                    reservation.getId(), reservation.getUserId(), reservation.getRestaurantId(),
                    reservation.getTimeSlotId(), reservation.getGuestCount().value(), LocalDateTime.now());
            return new SlotReturnInfo(reservation.getId(), reservation.getTimeSlotId(), reservation.getGuestCount().value());
        }
        return null;
    }

    // ── checkIn ─────────────────────────────────────────────────────────────

    @Override
    public ReservationStatusResult checkIn(CheckInCommand command) {
        return Objects.requireNonNull(writeTx.execute(status -> {
            Reservation reservation = reservationRepository.findById(command.reservationId())
                    .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));
            if (!reservation.getRestaurantId().equals(command.restaurantId())) {
                throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND);
            }
            reservation.complete(LocalDateTime.now());
            Reservation saved = reservationRepository.save(reservation);
            outboxEventPort.recordCheckInCompleted(
                    saved.getId(), saved.getRestaurantId(),
                    saved.getReservedDate(), command.checkedInBy(), saved.getCheckedInAt());
            return ReservationStatusResult.from(saved);
        }));
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private void tryRestoreSlotQuietly(UUID timeSlotId, UUID reservationId, String operation, int guestCount) {
        try {
            timeSlotPort.incrementStock(timeSlotId, guestCount,
                    "restore:" + timeSlotId + ":" + reservationId + ":" + operation);
        } catch (Exception e) {
            log.warn("[RestoreSlot] 슬롯 복구 실패 — timeSlotId={}, reservationId={}, op={}",
                    timeSlotId, reservationId, operation, e);
        }
    }

    private void tryCompleteWaitingQuietly(UUID waitingId) {
        if (waitingId == null) return;
        try {
            waitingPort.completeWaiting(waitingId, "complete-waiting:" + waitingId);
        } catch (Exception e) {
            log.warn("[CompleteWaiting] 대기열 완료 실패 — outbox 이벤트로 재처리 보장 (waitingId={})", waitingId, e);
        }
    }

    private void checkDuplicate(UUID userId, UUID timeSlotId, LocalDate reservedDate) {
        if (reservationRepository.existsByUserIdAndTimeSlotIdAndReservedDateAndStatusIn(
                userId, timeSlotId, reservedDate,
                List.of(ReservationStatus.WAITING, ReservationStatus.CONFIRMED))) {
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
     * 코스 목록 수정 규칙: - courses == null : 기존 코스 유지 (DB 재조회) - courses == [] : 전체 삭제 - courses != [] : 전체 교체 (삭제 후 새로 저장)
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
        return ReservationResult.of(reservation, courses.stream().map(ReservationCourseResult::from).toList());
    }

    // ── inner records ────────────────────────────────────────────────────────

    private record SlotSnapshot(UUID timeSlotId, LocalDate reservedDate, int guestCount, Long version) {}

    private record SlotReturnInfo(UUID reservationId, UUID timeSlotId, int guestCount) {}

    private record SlotChange(
            boolean needsDeduct, UUID deductTimeSlotId, int deductCount,
            boolean needsRestore, UUID restoreTimeSlotId, int restoreCount) {}
}
