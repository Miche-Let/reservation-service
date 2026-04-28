package com.michelet.reservation.presentation.reservation.dto.response;

import com.michelet.reservation.domain.enums.ReservationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReservationStatusResponse(
    UUID reservationId,
    ReservationStatus status,
    LocalDateTime confirmedAt,
    LocalDateTime endedAt
) {}