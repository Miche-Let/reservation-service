package com.michelet.reservation.infrastructure.kafka.event.publish;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReservationDeletedEvent(
        UUID reservationId,
        UUID userId,
        UUID restaurantId,
        UUID timeSlotId,
        int guestCount,
        LocalDateTime occurredAt
) {}
