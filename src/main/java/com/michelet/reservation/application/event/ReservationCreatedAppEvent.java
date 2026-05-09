package com.michelet.reservation.application.event;

import java.time.LocalDate;
import java.util.UUID;

public record ReservationCreatedAppEvent(
        UUID reservationId,
        UUID userId,
        UUID restaurantId,
        UUID timeSlotId,
        LocalDate reservedDate,
        int guestCount
) {}
