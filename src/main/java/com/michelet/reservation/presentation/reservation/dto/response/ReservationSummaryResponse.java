package com.michelet.reservation.presentation.reservation.dto.response;

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
) {}