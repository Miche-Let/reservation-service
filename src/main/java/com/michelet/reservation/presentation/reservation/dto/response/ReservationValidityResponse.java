package com.michelet.reservation.presentation.reservation.dto.response;

import com.michelet.reservation.application.reservation.result.ReservationValidityResult;
import com.michelet.reservation.domain.enums.ReservationStatus;

import java.time.LocalDate;
import java.util.UUID;

public record ReservationValidityResponse(
    UUID reservationId,
    ReservationStatus status,
    boolean isConfirmed,
    LocalDate reservedDate
) {
  public static ReservationValidityResponse from(ReservationValidityResult result) {
    return new ReservationValidityResponse(
        result.reservationId(),
        result.status(),
        result.isConfirmed(),
        result.reservedDate()
    );
  }
}
