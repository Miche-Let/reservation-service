package com.michelet.reservation.domain.state;

import com.michelet.reservation.domain.enums.ReservationStatus;

public class CompletedState implements ReservationState {

    @Override
    public boolean requiresSlotReturn() { return false; }

    @Override
    public boolean requiresRefund() { return false; }

    @Override
    public ReservationStatus status() { return ReservationStatus.COMPLETED; }
}
