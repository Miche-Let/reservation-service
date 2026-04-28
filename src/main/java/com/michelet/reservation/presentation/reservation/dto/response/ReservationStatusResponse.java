package com.michelet.reservation.presentation.reservation.dto.response;

import com.michelet.reservation.domain.enums.ReservationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReservationStatusResponse(
    UUID reservationId,
    ReservationStatus status,
    LocalDateTime confirmedAt,   // confirm 전용, expire 시 null
    LocalDateTime expiredAt      // expire 전용, confirm 시 null
) {}