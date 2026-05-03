package com.michelet.reservation.presentation.reservation.dto.response;

import com.michelet.reservation.application.reservation.result.ReservationValidityResult;
import java.time.LocalDate;
import java.util.UUID;

public record ReservationValidityResponse(
    boolean exists,
    UUID reservationId,
    LocalDate reservationDate
) {

  public static ReservationValidityResponse from(ReservationValidityResult result) {
    return new ReservationValidityResponse(result.exists(), result.reservationId(), result.reservationDate());
  }
}
