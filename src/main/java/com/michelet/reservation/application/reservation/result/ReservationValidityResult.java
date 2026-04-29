package com.michelet.reservation.application.reservation.result;

public record ReservationValidityResult(boolean exists) {

  public static ReservationValidityResult found() {
    return new ReservationValidityResult(true);
  }

  public static ReservationValidityResult notFound() {
    return new ReservationValidityResult(false);
  }
}
