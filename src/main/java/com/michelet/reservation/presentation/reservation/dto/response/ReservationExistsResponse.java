package com.michelet.reservation.presentation.reservation.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record ReservationExistsResponse(
    UUID reservationId,
    UUID restaurantId,
    LocalDate reservedDate,
    boolean exists
) {}