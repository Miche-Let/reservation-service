package com.michelet.reservation.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.michelet.reservation.application.port.OutboxEventPort;
import com.michelet.reservation.infrastructure.outbox.entity.OutboxEventJpaEntity;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxEventAdapter implements OutboxEventPort {

    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void record(UUID aggregateId, String aggregateType, String eventType, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            outboxEventJpaRepository.save(
                    OutboxEventJpaEntity.create(aggregateId, aggregateType, eventType, json)
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Outbox payload serialization failed: " + eventType, e);
        }
    }
}
