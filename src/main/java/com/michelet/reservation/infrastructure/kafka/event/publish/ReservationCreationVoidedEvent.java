package com.michelet.reservation.infrastructure.kafka.event.publish;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReservationCreationVoidedEvent(
        UUID reservationId,
        UUID timeSlotId,
        int guestCount,
        LocalDateTime occurredAt
) {}
