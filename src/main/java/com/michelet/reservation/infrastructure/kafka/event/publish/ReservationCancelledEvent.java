package com.michelet.reservation.infrastructure.kafka.event.publish;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReservationCancelledEvent(
    UUID reservationId,
    UUID userId,
    UUID restaurantId,
    UUID timeSlotId,
    LocalDate reservedDate,
    LocalDateTime occurredAt
) {}
