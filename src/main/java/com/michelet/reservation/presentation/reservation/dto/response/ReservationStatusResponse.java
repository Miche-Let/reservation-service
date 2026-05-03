package com.michelet.reservation.presentation.reservation.dto.response;

import com.michelet.reservation.application.reservation.result.ReservationStatusResult;
import com.michelet.reservation.domain.enums.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReservationStatusResponse(
    UUID reservationId,
    ReservationStatus status,
    LocalDate visitDate,
    LocalDateTime checkedInAt
) {
  public static ReservationStatusResponse from(ReservationStatusResult result) {
    return new ReservationStatusResponse(
        result.reservationId(),
        result.status(),
        result.visitDate(),
        result.checkedInAt()
    );
  }
}
