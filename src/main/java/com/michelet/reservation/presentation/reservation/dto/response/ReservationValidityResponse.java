package com.michelet.reservation.presentation.reservation.dto.response;

import com.michelet.reservation.domain.enums.ReservationStatus;

import java.time.LocalDate;
import java.util.UUID;

public record ReservationValidityResponse(
    UUID reservationId,
    ReservationStatus status,
    boolean isConfirmed,
    LocalDate reservedDate
) {}