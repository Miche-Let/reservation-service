package com.michelet.reservation.application.reservation.result;

import com.michelet.reservation.domain.entity.Reservation;

import java.time.LocalDate;
import java.util.UUID;

public record ReservationExistsResult(
    UUID reservationId,
    UUID restaurantId,
    LocalDate reservedDate,
    boolean exists
) {
  public static ReservationExistsResult of(Reservation r) {
    return new ReservationExistsResult(
        r.getId(),
        r.getRestaurantId(),
        r.getReservedDate(),
        true
    );
  }

  public static ReservationExistsResult notFound() {
    return new ReservationExistsResult(null, null, null, false);
  }
}
