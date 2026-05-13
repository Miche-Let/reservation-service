package com.michelet.reservation.infrastructure.kafka.event.publish;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReservationSlotReleasedEvent(
        UUID reservationId,
        UUID timeSlotId,
        int capacity,
        LocalDateTime occurredAt
) {}
