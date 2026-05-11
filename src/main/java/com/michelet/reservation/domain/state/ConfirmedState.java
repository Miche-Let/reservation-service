package com.michelet.reservation.domain.state;

import com.michelet.common.exception.BusinessException;
import com.michelet.reservation.domain.enums.ReservationStatus;
import com.michelet.reservation.domain.exception.ReservationErrorCode;
import java.time.LocalDate;

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
    public boolean requiresSlotReturn() { return true; }

    @Override
    public boolean requiresRefund() { return false; }

    @Override
    public ReservationStatus status() { return ReservationStatus.CONFIRMED; }
}
