package com.michelet.reservation.infrastructure.kafka.producer;

import com.michelet.reservation.application.port.ReservationEventPort;
import com.michelet.reservation.infrastructure.kafka.event.publish.ReservationCreatedEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationEventProducer implements ReservationEventPort {

    private static final String TOPIC_RESERVATION_CREATED = "reservation.created";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishReservationCreated(UUID reservationId, UUID userId, UUID restaurantId,
                                          UUID timeSlotId, LocalDate reservedDate, int guestCount) {
        ReservationCreatedEvent event = new ReservationCreatedEvent(
                reservationId, userId, restaurantId, timeSlotId, reservedDate, guestCount, LocalDateTime.now()
        );
        kafkaTemplate.send(TOPIC_RESERVATION_CREATED, reservationId.toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("[Kafka] reservation.created 발행 실패 — reservationId={}, cause={}", reservationId, ex.getMessage());
                    }
                });
    }
}
