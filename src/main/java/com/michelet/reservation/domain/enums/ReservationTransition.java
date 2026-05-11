package com.michelet.reservation.domain.enums;

import java.util.Arrays;

public enum ReservationTransition {

    WAITING_TO_CONFIRMED        (ReservationStatus.WAITING,   ReservationStatus.CONFIRMED),
    WAITING_TO_CANCELLED_UNPAID (ReservationStatus.WAITING,   ReservationStatus.CANCELLED_UNPAID),
    CONFIRMED_TO_CANCELLED_PAID (ReservationStatus.CONFIRMED, ReservationStatus.CANCELLED_PAID),
    CONFIRMED_TO_COMPLETED      (ReservationStatus.CONFIRMED, ReservationStatus.COMPLETED),
    CONFIRMED_TO_NO_SHOW        (ReservationStatus.CONFIRMED, ReservationStatus.NO_SHOW);

    private final ReservationStatus from;
    private final ReservationStatus to;

    ReservationTransition(ReservationStatus from, ReservationStatus to) {
        this.from = from;
        this.to   = to;
    }

    public ReservationStatus from() { return from; }
    public ReservationStatus to()   { return to; }

    public static boolean isAllowed(ReservationStatus from, ReservationStatus to) {
        return Arrays.stream(values()).anyMatch(t -> t.from == from && t.to == to);
    }
}
