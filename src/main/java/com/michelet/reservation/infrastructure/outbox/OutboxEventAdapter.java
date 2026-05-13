package com.michelet.reservation.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.michelet.reservation.application.port.OutboxEventPort;
import com.michelet.reservation.domain.enums.ReservationStatus;
import com.michelet.reservation.infrastructure.kafka.KafkaTopics;
import com.michelet.reservation.infrastructure.kafka.event.publish.CheckInCompletedEvent;
import com.michelet.reservation.infrastructure.kafka.event.publish.ReservationCancelledEvent;
import com.michelet.reservation.infrastructure.kafka.event.publish.ReservationCreatedEvent;
import com.michelet.reservation.infrastructure.kafka.event.publish.WaitingCompletedEvent;
import com.michelet.reservation.infrastructure.outbox.entity.OutboxEventJpaEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxEventAdapter implements OutboxEventPort {

    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void recordReservationCreated(UUID reservationId, UUID userId, UUID restaurantId,
                                         UUID timeSlotId, LocalDate reservedDate, int guestCount,
                                         LocalDateTime occurredAt) {
        save(reservationId, AggregateType.RESERVATION, KafkaTopics.RESERVATION_CREATED,
                new ReservationCreatedEvent(reservationId, userId, restaurantId,
                        timeSlotId, reservedDate, guestCount, occurredAt));
    }

    @Override
    public void recordReservationCancelled(UUID reservationId, UUID userId, UUID restaurantId,
                                           UUID timeSlotId, LocalDate reservedDate, int guestCount,
                                           ReservationStatus cancelledStatus, LocalDateTime occurredAt) {
        save(reservationId, AggregateType.RESERVATION, KafkaTopics.RESERVATION_CANCELLED,
                new ReservationCancelledEvent(reservationId, userId, restaurantId,
                        timeSlotId, reservedDate, guestCount, cancelledStatus.name(), occurredAt));
    }

    @Override
    public void recordWaitingCompleted(UUID waitingId, UUID reservationId, LocalDateTime occurredAt) {
        save(waitingId, AggregateType.WAITING, KafkaTopics.WAITING_COMPLETED,
                new WaitingCompletedEvent(waitingId, reservationId, occurredAt));
    }

    @Override
    public void recordCheckInCompleted(UUID reservationId, UUID restaurantId,
                                       LocalDate visitDate, LocalDateTime checkedInAt) {
        save(reservationId, AggregateType.RESERVATION, KafkaTopics.RESERVATION_CHECKED_IN,
                CheckInCompletedEvent.of(reservationId, restaurantId, visitDate, checkedInAt));
    }

    private void save(UUID aggregateId, AggregateType aggregateType, String eventType, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            outboxEventJpaRepository.save(
                    OutboxEventJpaEntity.create(aggregateId, aggregateType, eventType, json));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Outbox payload serialization failed: " + eventType, e);
        }
    }
}
