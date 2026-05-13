package com.michelet.reservation.infrastructure.outbox;

import com.michelet.reservation.infrastructure.outbox.entity.OutboxEventJpaEntity;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@ConditionalOnProperty(name = "kafka.stub", havingValue = "false", matchIfMissing = true)
public class OutboxEventScheduler {

    private static final int MAX_RETRY = 5;

    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxEventScheduler(
            OutboxEventJpaRepository outboxEventJpaRepository,
            @Qualifier("outboxKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate
    ) {
        this.outboxEventJpaRepository = outboxEventJpaRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${outbox.scheduler.fixed-delay-ms:1000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEventJpaEntity> events = outboxEventJpaRepository.findPendingForProcessing();
        for (OutboxEventJpaEntity event : events) {
            try {
                kafkaTemplate.send(event.getEventType(), event.getAggregateId().toString(), event.getPayload())
                        .get(5, TimeUnit.SECONDS);
                event.markProcessed();
            } catch (Exception e) {
                event.incrementRetry();
                if (event.getRetryCount() >= MAX_RETRY) {
                    event.markFailed();
                    log.error("[Outbox] 최대 재시도 초과 FAILED 마킹 — id={}, type={}", event.getId(), event.getEventType());
                } else {
                    log.warn("[Outbox] Kafka 발행 실패 — id={}, type={}, retry={}", event.getId(), event.getEventType(), event.getRetryCount(), e);
                }
            }
            outboxEventJpaRepository.save(event);
        }
    }
}
