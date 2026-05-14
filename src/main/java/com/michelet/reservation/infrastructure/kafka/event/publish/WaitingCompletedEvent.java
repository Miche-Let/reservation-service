package com.michelet.reservation.infrastructure.kafka.event.publish;

import java.time.LocalDateTime;
import java.util.UUID;

public record WaitingCompletedEvent(
        UUID eventId,
        UUID waitingId,
        UUID reservationId,
        LocalDateTime occurredAt
) {}
