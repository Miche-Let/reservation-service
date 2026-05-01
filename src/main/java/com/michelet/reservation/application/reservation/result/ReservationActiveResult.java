package com.michelet.reservation.application.reservation.result;

public record ReservationActiveResult(boolean exists) {

    public static ReservationActiveResult found() {
        return new ReservationActiveResult(true);
    }

    public static ReservationActiveResult notFound() {
        return new ReservationActiveResult(false);
    }
}
