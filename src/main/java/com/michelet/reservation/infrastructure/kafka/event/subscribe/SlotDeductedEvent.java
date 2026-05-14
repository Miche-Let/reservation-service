package com.michelet.reservation.infrastructure.kafka.event.subscribe;

import java.time.LocalDateTime;
import java.util.UUID;

public record SlotDeductedEvent(
        UUID eventId,
        UUID reservationId,
        UUID timeSlotId,
        int guestCount,
        LocalDateTime occurredAt
) {}
