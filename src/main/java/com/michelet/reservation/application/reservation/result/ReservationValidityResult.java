package com.michelet.reservation.application.reservation.result;

import com.michelet.reservation.domain.entity.Reservation;
import com.michelet.reservation.domain.enums.ReservationStatus;

import java.time.LocalDate;
import java.util.UUID;

public record ReservationValidityResult(
    UUID reservationId,
    ReservationStatus status,
    boolean isConfirmed,
    LocalDate reservedDate
) {
  public static ReservationValidityResult of(Reservation r) {
    return new ReservationValidityResult(
        r.getId(),
        r.getStatus(),
        r.getStatus() == ReservationStatus.CONFIRMED,
        r.getReservedDate()
    );
  }

  public static ReservationValidityResult notFound() {
    return new ReservationValidityResult(null, null, false, null);
  }
}
