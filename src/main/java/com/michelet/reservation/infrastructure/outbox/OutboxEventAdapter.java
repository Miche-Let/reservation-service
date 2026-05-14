package com.michelet.reservation.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.michelet.reservation.application.port.OutboxEventPort;
import com.michelet.reservation.domain.enums.ReservationStatus;
import com.michelet.reservation.infrastructure.kafka.KafkaTopics;
import com.michelet.reservation.infrastructure.kafka.event.publish.CheckInCompletedEvent;
import com.michelet.reservation.infrastructure.kafka.event.publish.ReservationCancelledEvent;
import com.michelet.reservation.infrastructure.kafka.event.publish.ReservationCreatedEvent;
import com.michelet.reservation.infrastructure.kafka.event.publish.ReservationCreationVoidedEvent;
import com.michelet.reservation.infrastructure.kafka.event.publish.ReservationModificationVoidedEvent;
import com.michelet.reservation.infrastructure.kafka.event.publish.ReservationDeletedEvent;
import com.michelet.reservation.infrastructure.kafka.event.publish.ReservationSlotReleasedEvent;
import com.michelet.reservation.infrastructure.kafka.event.publish.WaitingCompletedEvent;
import com.michelet.reservation.infrastructure.outbox.entity.OutboxEventJpaEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
                new ReservationCreatedEvent(UUID.randomUUID(), reservationId, userId, restaurantId,
                        timeSlotId, reservedDate, guestCount, occurredAt));
    }

    @Override
    public void recordReservationCancelled(UUID reservationId, UUID userId, UUID restaurantId,
                                           UUID timeSlotId, LocalDate reservedDate, int guestCount,
                                           ReservationStatus cancelledStatus, LocalDateTime occurredAt) {
        save(reservationId, AggregateType.RESERVATION, KafkaTopics.RESERVATION_CANCELLED,
                new ReservationCancelledEvent(UUID.randomUUID(), reservationId, userId, restaurantId,
                        timeSlotId, reservedDate, guestCount, cancelledStatus.name(), occurredAt));
    }

    @Override
    public void recordWaitingCompleted(UUID waitingId, UUID reservationId, LocalDateTime occurredAt) {
        save(waitingId, AggregateType.WAITING, KafkaTopics.WAITING_COMPLETED,
                new WaitingCompletedEvent(UUID.randomUUID(), waitingId, reservationId, occurredAt));
    }

    @Override
    public void recordReservationDeleted(UUID reservationId, UUID userId, UUID restaurantId,
                                         UUID timeSlotId, int guestCount, LocalDateTime occurredAt) {
        save(reservationId, AggregateType.RESERVATION, KafkaTopics.RESERVATION_DELETED,
                new ReservationDeletedEvent(UUID.randomUUID(), reservationId, userId, restaurantId,
                        timeSlotId, guestCount, occurredAt));
    }

    @Override
    public void recordCheckInCompleted(UUID reservationId, UUID restaurantId,
                                       LocalDate visitDate, UUID checkedInBy, LocalDateTime checkedInAt) {
        save(reservationId, AggregateType.RESERVATION, KafkaTopics.RESERVATION_CHECKED_IN,
                CheckInCompletedEvent.of(reservationId, restaurantId, visitDate, checkedInBy, checkedInAt));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordReservationCreationVoided(UUID reservationId, UUID timeSlotId, int guestCount,
                                                LocalDateTime occurredAt) {
        save(reservationId, AggregateType.RESERVATION, KafkaTopics.RESERVATION_CREATION_VOIDED,
                new ReservationCreationVoidedEvent(UUID.randomUUID(), reservationId, timeSlotId, guestCount, occurredAt));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordReservationModificationVoided(UUID reservationId, UUID timeSlotId, int capacity,
                                                    LocalDateTime occurredAt) {
        save(reservationId, AggregateType.RESERVATION, KafkaTopics.RESERVATION_MODIFICATION_VOIDED,
                new ReservationModificationVoidedEvent(UUID.randomUUID(), reservationId, timeSlotId, capacity, occurredAt));
    }

    @Override
    public void recordSlotReleased(UUID reservationId, UUID timeSlotId, int capacity,
                                   LocalDateTime occurredAt) {
        save(reservationId, AggregateType.RESERVATION, KafkaTopics.RESERVATION_SLOT_RELEASED,
                new ReservationSlotReleasedEvent(UUID.randomUUID(), reservationId, timeSlotId, capacity, occurredAt));
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
