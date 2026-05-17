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
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
@ConditionalOnProperty(name = "kafka.stub", havingValue = "false", matchIfMissing = true)
public class OutboxEventScheduler {

    private static final int MAX_RETRY = 5;

    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TransactionTemplate transactionTemplate;

    public OutboxEventScheduler(
            OutboxEventJpaRepository outboxEventJpaRepository,
            @Qualifier("outboxKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate,
            TransactionTemplate transactionTemplate
    ) {
        this.outboxEventJpaRepository = outboxEventJpaRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Scheduled(fixedDelayString = "${outbox.scheduler.fixed-delay-ms:1000}")
    public void publishPendingEvents() {
        // PENDING 조회 + PROCESSING 전이를 한 트랜잭션으로 원자적 처리.
        // 커밋 후 다른 인스턴스가 같은 이벤트를 조회해도 PENDING이 아니므로 중복 발행 방지.
        List<OutboxEventJpaEntity> events = transactionTemplate.execute(tx -> {
            List<OutboxEventJpaEntity> pending = outboxEventJpaRepository.findPendingForProcessing();
            if (pending == null || pending.isEmpty()) return pending;
            pending.forEach(OutboxEventJpaEntity::markProcessing);
            outboxEventJpaRepository.saveAll(pending);
            return pending;
        });
        if (events == null || events.isEmpty()) {
            return;
        }

        for (OutboxEventJpaEntity event : events) {
            // Kafka 발행은 트랜잭션 외부 — DB 락을 쥔 채 I/O 대기하지 않음
            boolean sent = false;
            try {
                kafkaTemplate.send(event.getEventType(), event.getAggregateId().toString(), event.getPayload())
                        .get(5, TimeUnit.SECONDS);
                sent = true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[Outbox] 스레드 인터럽트 — 스케줄러 종료 (id={})", event.getId());
                return;
            } catch (Exception e) {
                log.warn("[Outbox] Kafka 발행 실패 — id={}, type={}, retry={}", event.getId(), event.getEventType(), event.getRetryCount() + 1, e);
            }

            // 상태 업데이트는 이벤트 단위 짧은 트랜잭션
            final boolean wasSent = sent;
            transactionTemplate.execute(tx -> {
                OutboxEventJpaEntity managed = outboxEventJpaRepository.findById(event.getId()).orElse(null);
                if (managed == null) {
                    return null;
                }
                if (wasSent) {
                    managed.markProcessed();
                } else {
                    managed.incrementRetry();
                    if (managed.getRetryCount() >= MAX_RETRY) {
                        managed.markFailed();
                        log.error("[Outbox] 최대 재시도 초과 FAILED 마킹 — id={}, type={}", managed.getId(), managed.getEventType());
                    } else {
                        // 다음 폴링 주기에 재시도되도록 PENDING으로 복귀
                        managed.markPending();
                    }
                }
                return outboxEventJpaRepository.save(managed);
            });
        }
    }
}
