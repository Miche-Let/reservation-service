package com.michelet.reservation.domain.state;

import com.michelet.reservation.domain.enums.ReservationStatus;

public class WaitingState implements ReservationState {

    @Override
    public ReservationStatus confirm() {
        return ReservationStatus.CONFIRMED;
    }

    @Override
    public ReservationStatus cancelUnpaid() {
        return ReservationStatus.CANCELLED_UNPAID;
    }

    @Override
    public boolean requiresSlotReturn() { return false; }

    @Override
    public boolean requiresRefund() { return false; }

    @Override
    public ReservationStatus status() { return ReservationStatus.WAITING; }
}
