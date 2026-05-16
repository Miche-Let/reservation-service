package com.michelet.reservation.domain.entity;

import com.michelet.common.exception.BusinessException;
import com.michelet.reservation.domain.enums.ReservationStatus;
import com.michelet.reservation.domain.exception.ReservationErrorCode;
import com.michelet.reservation.domain.state.ReservationState;
import com.michelet.reservation.domain.state.ReservationStateFactory;
import com.michelet.reservation.domain.vo.GuestCount;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {

    private UUID id;
    private boolean isNew;
    private UUID userId;
    private UUID restaurantId;
    private UUID timeSlotId;
    private LocalDate reservedDate;
    private GuestCount guestCount;
    private ReservationStatus status;
    private transient ReservationState state; // JPA 영속화 대상 아님

    private LocalDate cancelDeadline;

    private LocalDate modifyDeadline;

    private LocalDateTime noshowDeadline;

    private LocalDateTime checkedInAt;

    private Long version;

    public static Reservation create(
            UUID userId,
            UUID restaurantId,
            UUID timeSlotId,
            LocalDate reservedDate,
            GuestCount guestCount,
            LocalDateTime noshowDeadline
    ) {
        validateCreateInput(userId, restaurantId, timeSlotId, reservedDate, guestCount);
        Reservation r = new Reservation();
        r.id = UUID.randomUUID();
        r.isNew = true;
        r.userId = userId;
        r.restaurantId = restaurantId;
        r.timeSlotId = timeSlotId;
        r.reservedDate = reservedDate;
        r.guestCount = guestCount;
        r.status = ReservationStatus.WAITING;
        r.state = ReservationStateFactory.from(ReservationStatus.WAITING);
        r.cancelDeadline = reservedDate.minusDays(2);
        r.modifyDeadline = reservedDate.minusDays(2);
        r.noshowDeadline = noshowDeadline;
        return r;
    }

    public static Reservation reconstitute(
            UUID id,
            UUID userId,
            UUID restaurantId,
            UUID timeSlotId,
            LocalDate reservedDate,
            GuestCount guestCount,
            ReservationStatus status,
            LocalDate cancelDeadline,
            LocalDate modifyDeadline,
            LocalDateTime noshowDeadline,
            LocalDateTime checkedInAt,
            Long version
    ) {
        validateReconstituteInput(id, userId, restaurantId, timeSlotId, reservedDate, guestCount, status);
        Reservation r = new Reservation();
        r.id = id;
        r.userId = userId;
        r.restaurantId = restaurantId;
        r.timeSlotId = timeSlotId;
        r.reservedDate = reservedDate;
        r.guestCount = guestCount;
        r.status = status;
        r.state = ReservationStateFactory.from(status);
        r.cancelDeadline = cancelDeadline;
        r.modifyDeadline = modifyDeadline;
        r.noshowDeadline = noshowDeadline;
        r.checkedInAt = checkedInAt;
        r.version = version;
        return r;
    }

    public void confirm() {
        this.status = state.confirm();
        this.state = ReservationStateFactory.from(this.status);
    }

    public void cancel() {
        this.status = state.cancel(this.cancelDeadline);
        this.state = ReservationStateFactory.from(this.status);
    }

    public void cancelUnpaid() {
        this.status = state.cancelUnpaid();
        this.state = ReservationStateFactory.from(this.status);
    }

    public void complete(LocalDateTime now) {
        if (this.status == ReservationStatus.COMPLETED) {
            return; // 이미 완료된 경우 멱등 처리
        }
        if (state.status() != ReservationStatus.CONFIRMED) {
            throw new BusinessException(ReservationErrorCode.INVALID_STATUS_TRANSITION);
        }
        LocalDateTime windowStart = noshowDeadline.minusMinutes(60); // noshowDeadline - 60min (= slotStart - 30min)
        if (now.isBefore(windowStart)) {
            throw new BusinessException(ReservationErrorCode.CHECK_IN_TOO_EARLY);
        }
        if (now.isAfter(noshowDeadline)) {
            throw new BusinessException(ReservationErrorCode.CHECK_IN_TOO_LATE);
        }
        this.status      = state.complete();
        this.state       = ReservationStateFactory.from(this.status);
        this.checkedInAt = now;
    }

    public void markNoShow() {
        this.status = state.markNoShow();
        this.state = ReservationStateFactory.from(this.status);
    }

    public boolean requiresSlotReturn() {
        return state.requiresSlotReturn();
    }

    public boolean isCancellable() {
        return state.status() == ReservationStatus.CONFIRMED
                && !LocalDate.now().isAfter(cancelDeadline);
    }

    public boolean isModifiable() {
        return state.status() == ReservationStatus.CONFIRMED
                && !LocalDate.now().isAfter(modifyDeadline);
    }

    public boolean requiresRefund() {
        return state.requiresRefund();
    }

    public void modify(
            UUID newTimeSlotId,
            LocalDate newReservedDate,
            GuestCount newGuestCount,
            LocalDateTime newNoshowDeadline
    ) {
        if (state.status() != ReservationStatus.CONFIRMED) {
            throw new BusinessException(ReservationErrorCode.INVALID_STATUS_TRANSITION);
        }
        if (LocalDate.now().isAfter(modifyDeadline)) {
            throw new BusinessException(ReservationErrorCode.MODIFY_DEADLINE_EXCEEDED);
        }
        this.timeSlotId      = newTimeSlotId;
        this.reservedDate    = newReservedDate;
        this.guestCount      = newGuestCount;
        this.cancelDeadline  = newReservedDate.minusDays(2);
        this.modifyDeadline  = newReservedDate.minusDays(2);
        this.noshowDeadline  = newNoshowDeadline;
    }

    private static void validateCreateInput(
            UUID userId,
            UUID restaurantId,
            UUID timeSlotId,
            LocalDate reservedDate,
            GuestCount guestCount) {
        if (userId == null) {
            throw new BusinessException(ReservationErrorCode.INVALID_USER_ID);
        }
        if (restaurantId == null) {
            throw new BusinessException(ReservationErrorCode.INVALID_RESTAURANT_ID);
        }
        if (timeSlotId == null) {
            throw new BusinessException(ReservationErrorCode.INVALID_TIME_SLOT_ID);
        }
        if (reservedDate == null) {
            throw new BusinessException(ReservationErrorCode.INVALID_RESERVED_DATE);
        }
        if (guestCount == null) {
            throw new BusinessException(ReservationErrorCode.INVALID_GUEST_COUNT_NULL);
        }
    }

    private static void validateReconstituteInput(
            UUID id,
            UUID userId,
            UUID restaurantId,
            UUID timeSlotId,
            LocalDate reservedDate,
            GuestCount guestCount,
            ReservationStatus status
    ) {
        if (id == null) {
            throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND);
        }
        if (userId == null) {
            throw new BusinessException(ReservationErrorCode.INVALID_USER_ID);
        }
        if (restaurantId == null) {
            throw new BusinessException(ReservationErrorCode.INVALID_RESTAURANT_ID);
        }
        if (timeSlotId == null) {
            throw new BusinessException(ReservationErrorCode.INVALID_TIME_SLOT_ID);
        }
        if (reservedDate == null) {
            throw new BusinessException(ReservationErrorCode.INVALID_RESERVED_DATE);
        }
        if (guestCount == null) {
            throw new BusinessException(ReservationErrorCode.INVALID_GUEST_COUNT_NULL);
        }
        if (status == null) {
            throw new BusinessException(ReservationErrorCode.INVALID_STATUS);
        }
    }
}
