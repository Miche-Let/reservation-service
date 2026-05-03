package com.michelet.reservation.application.reservation.result;

import com.michelet.reservation.domain.entity.Reservation;
import java.time.LocalDate;
import java.util.UUID;

public record ReservationValidityResult(
    boolean exists,
    UUID reservationId,
    LocalDate reservationDate
) {

  public static ReservationValidityResult found(Reservation reservation) {
    return new ReservationValidityResult(true, reservation.getId(), reservation.getReservedDate());
  }

  public static ReservationValidityResult notFound() {
    return new ReservationValidityResult(false, null, null);
  }
}
