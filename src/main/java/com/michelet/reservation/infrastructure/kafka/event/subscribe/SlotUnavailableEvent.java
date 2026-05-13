package com.michelet.reservation.infrastructure.kafka.event.subscribe;

import java.time.LocalDateTime;
import java.util.UUID;

public record SlotUnavailableEvent(
        UUID reservationId,
        UUID timeSlotId,
        String reason,
        LocalDateTime occurredAt
) {}
