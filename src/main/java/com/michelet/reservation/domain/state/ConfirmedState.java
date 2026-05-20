package com.michelet.reservation.domain.state;

import com.michelet.common.exception.BusinessException;
import com.michelet.reservation.domain.enums.ReservationStatus;
import com.michelet.reservation.domain.exception.ReservationErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ConfirmedState implements ReservationState {

    @Override
    public ReservationStatus cancel(LocalDate cancelDeadline) {
        if (LocalDate.now().isAfter(cancelDeadline)) {
            throw new BusinessException(ReservationErrorCode.CANCEL_DEADLINE_EXCEEDED);
        }
        return ReservationStatus.CANCELLED_PAID;
    }

    @Override
    public ReservationStatus complete() {
        return ReservationStatus.COMPLETED;
    }

    @Override
    public ReservationStatus markNoShow() {
        return ReservationStatus.NO_SHOW;
    }

    @Override
    public boolean isCancellable(LocalDate cancelDeadline) {
        return !LocalDate.now().isAfter(cancelDeadline);
    }

    @Override
    public boolean isModifiable(LocalDate modifyDeadline) {
        return !LocalDate.now().isAfter(modifyDeadline);
    }

    @Override
    public void assertModifiable(LocalDate modifyDeadline) {
        if (LocalDate.now().isAfter(modifyDeadline)) {
            throw new BusinessException(ReservationErrorCode.MODIFY_DEADLINE_EXCEEDED);
        }
    }

    @Override
    public void assertCompletable(LocalDateTime now, LocalDateTime noshowDeadline) {
        LocalDateTime windowStart = noshowDeadline.minusMinutes(60);
        if (now.isBefore(windowStart)) {
            throw new BusinessException(ReservationErrorCode.CHECK_IN_TOO_EARLY);
        }
        if (now.isAfter(noshowDeadline)) {
            throw new BusinessException(ReservationErrorCode.CHECK_IN_TOO_LATE);
        }
    }

    @Override
    public boolean requiresSlotReturn() { return true; }

    @Override
    public boolean requiresRefund() { return false; }

    @Override
    public ReservationStatus status() { return ReservationStatus.CONFIRMED; }
}
