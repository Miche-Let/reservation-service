package com.michelet.reservation.presentation.reservation.dto.response;

import com.michelet.reservation.application.reservation.result.ReservationStatusResult;
import com.michelet.reservation.domain.enums.ReservationStatus;

import java.util.UUID;

public record ReservationStatusResponse(
    UUID reservationId,
    ReservationStatus status
) {
  public static ReservationStatusResponse from(ReservationStatusResult result) {
    return new ReservationStatusResponse(result.reservationId(), result.status());
  }
}
