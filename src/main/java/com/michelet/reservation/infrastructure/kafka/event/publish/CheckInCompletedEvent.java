package com.michelet.reservation.infrastructure.kafka.event.publish;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record CheckInCompletedEvent(
        UUID eventId,
        String eventType,
        UUID reservationId,
        UUID restaurantId,
        LocalDate visitDate,
        LocalDateTime checkedInAt,
        LocalDateTime eventCreatedAt
) {
    public static CheckInCompletedEvent of(UUID reservationId, UUID restaurantId,
                                           LocalDate visitDate, LocalDateTime checkedInAt) {
        return new CheckInCompletedEvent(
                UUID.randomUUID(),
                "CHECK_IN_COMPLETED",
                reservationId,
                restaurantId,
                visitDate,
                checkedInAt,
                LocalDateTime.now()
        );
    }
}
