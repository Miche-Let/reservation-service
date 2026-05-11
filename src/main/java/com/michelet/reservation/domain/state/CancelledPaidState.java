package com.michelet.reservation.domain.state;

import com.michelet.reservation.domain.enums.ReservationStatus;

public class CancelledPaidState implements ReservationState {

    @Override
    public boolean requiresSlotReturn() { return false; }

    @Override
    public boolean requiresRefund() { return true; }

    @Override
    public ReservationStatus status() { return ReservationStatus.CANCELLED_PAID; }
}
