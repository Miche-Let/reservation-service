package com.michelet.reservation.presentation.reservation.dto.response;

import com.michelet.reservation.application.reservation.result.ReservationSummaryResult;
import com.michelet.reservation.domain.enums.ReservationStatus;

import java.time.LocalDate;
import java.util.UUID;

public record ReservationSummaryResponse(
    UUID reservationId,
    UUID restaurantId,
    LocalDate reservedDate,
    int guestCount,
    ReservationStatus status,
    LocalDate cancelDeadline,
    LocalDate modifyDeadline
) {
  public static ReservationSummaryResponse from(ReservationSummaryResult result) {
    return new ReservationSummaryResponse(
        result.reservationId(),
        result.restaurantId(),
        result.reservedDate(),
        result.guestCount(),
        result.status(),
        result.cancelDeadline(),
        result.modifyDeadline()
    );
  }
}
