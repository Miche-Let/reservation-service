package com.michelet.reservation.domain.state;

import com.michelet.reservation.domain.enums.ReservationStatus;

public class NoShowState implements ReservationState {

    @Override
    public boolean requiresSlotReturn() { return false; }

    @Override
    public boolean requiresRefund() { return false; }

    @Override
    public ReservationStatus status() { return ReservationStatus.NO_SHOW; }
}
