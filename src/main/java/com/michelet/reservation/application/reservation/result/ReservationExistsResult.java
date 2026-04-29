package com.michelet.reservation.application.reservation.result;

public record ReservationExistsResult(boolean exists) {

  public static ReservationExistsResult found() {
    return new ReservationExistsResult(true);
  }

  public static ReservationExistsResult notFound() {
    return new ReservationExistsResult(false);
  }
}
