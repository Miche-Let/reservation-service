package com.michelet.reservation.application.reservation.result;

import com.michelet.reservation.domain.entity.Reservation;
import com.michelet.reservation.domain.enums.ReservationStatus;

import java.util.UUID;

public record ReservationStatusResult(
    UUID reservationId,
    ReservationStatus status
) {
  public static ReservationStatusResult from(Reservation reservation) {
    return new ReservationStatusResult(reservation.getId(), reservation.getStatus());
  }
}
