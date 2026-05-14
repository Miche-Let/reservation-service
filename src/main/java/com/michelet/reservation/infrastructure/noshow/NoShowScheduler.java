package com.michelet.reservation.infrastructure.noshow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.michelet.reservation.domain.entity.Reservation;
import com.michelet.reservation.infrastructure.kafka.KafkaTopics;
import com.michelet.reservation.infrastructure.kafka.event.publish.ReservationNoShowEvent;
import com.michelet.reservation.infrastructure.outbox.AggregateType;
import com.michelet.reservation.infrastructure.outbox.OutboxEventJpaRepository;
import com.michelet.reservation.infrastructure.outbox.entity.OutboxEventJpaEntity;
import com.michelet.reservation.infrastructure.reservation.ReservationJpaStore;
import com.michelet.reservation.infrastructure.reservation.entity.ReservationJpaEntity;
import com.michelet.reservation.infrastructure.reservation.mapper.ReservationMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
@ConditionalOnProperty(name = "kafka.stub", havingValue = "false", matchIfMissing = true)
public class NoShowScheduler {

    private static final int BATCH_SIZE = 50;

    private final ReservationJpaStore jpaStore;
    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public NoShowScheduler(
            ReservationJpaStore jpaStore,
            OutboxEventJpaRepository outboxEventJpaRepository,
            ObjectMapper objectMapper,
            TransactionTemplate transactionTemplate
    ) {
        this.jpaStore = jpaStore;
        this.outboxEventJpaRepository = outboxEventJpaRepository;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
    }

    @Scheduled(fixedDelayString = "${noshow.scheduler.fixed-delay-ms:60000}")
    public void markExpiredReservationsAsNoShow() {
        // TX-1: 짧은 읽기 트랜잭션 — 만료된 CONFIRMED 예약 배치 조회 후 즉시 커밋
        List<ReservationJpaEntity> candidates = transactionTemplate.execute(
                tx -> jpaStore.findExpiredConfirmedForUpdate(LocalDateTime.now(), BATCH_SIZE)
        );
        if (candidates == null || candidates.isEmpty()) {
            return;
        }

        for (ReservationJpaEntity candidate : candidates) {
            try {
                // TX-2: 건당 쓰기 트랜잭션 — 상태 재검증 후 NO_SHOW 전이 + 아웃박스 저장
                transactionTemplate.execute(tx -> {
                    Optional<ReservationJpaEntity> managedOpt =
                            jpaStore.findByIdAndStatusConfirmedForUpdate(candidate.getId());
                    if (managedOpt.isEmpty()) {
                        // TX-1과 TX-2 사이에 체크인 또는 취소가 완료된 경우
                        return null;
                    }

                    ReservationJpaEntity managed = managedOpt.get();
                    Reservation domain = ReservationMapper.toDomain(managed);
                    domain.markNoShow();
                    managed.applyFrom(domain);
                    jpaStore.save(managed);

                    LocalDateTime now = LocalDateTime.now();
                    ReservationNoShowEvent event = new ReservationNoShowEvent(
                            UUID.randomUUID(), managed.getId(), managed.getUserId(),
                            managed.getRestaurantId(), managed.getTimeSlotId(),
                            managed.getReservedDate(), managed.getGuestCount(), now
                    );
                    try {
                        String json = objectMapper.writeValueAsString(event);
                        outboxEventJpaRepository.save(
                                OutboxEventJpaEntity.create(managed.getId(),
                                        AggregateType.RESERVATION, KafkaTopics.RESERVATION_NO_SHOW, json)
                        );
                    } catch (JsonProcessingException e) {
                        throw new IllegalStateException("NoShow 이벤트 직렬화 실패 — reservationId=" + managed.getId(), e);
                    }

                    log.info("[NoShow] 처리 완료 — reservationId={}", managed.getId());
                    return null;
                });
            } catch (Exception e) {
                log.error("[NoShow] 처리 실패 — id={}", candidate.getId(), e);
            }
        }
    }
}
