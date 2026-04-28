package com.michelet.reservation.presentation.reservation.dto.response;

import com.michelet.reservation.application.reservation.result.ReservationExistsResult;

import java.time.LocalDate;
import java.util.UUID;

public record ReservationExistsResponse(
    UUID reservationId,
    UUID restaurantId,
    LocalDate reservedDate,
    boolean exists
) {
  public static ReservationExistsResponse from(ReservationExistsResult result) {
    return new ReservationExistsResponse(
        result.reservationId(),
        result.restaurantId(),
        result.reservedDate(),
        result.exists()
    );
  }
}
