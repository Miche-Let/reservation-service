package com.michelet.reservation.infrastructure.kafka.consumer;

import com.michelet.reservation.infrastructure.kafka.KafkaTopics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "kafka.stub", havingValue = "false", matchIfMissing = true)
public class SlotEventConsumer {

    // TODO: timeslot-service 이벤트 발행 스펙 확정 후 비즈니스 로직 구현
    // slot.deducted  → 예약 CONFIRMED 처리 (타임아웃으로 WAITING 상태인 경우)
    // slot.unavailable → 예약 취소 보상 처리

    @KafkaListener(
            topics = KafkaTopics.SLOT_DEDUCTED,
            groupId = "reservation-service-consumer",
            containerFactory = "slotEventListenerContainerFactory"
    )
    public void onSlotDeducted(String payload) {
        log.info("[SlotEventConsumer] slot.deducted 수신 (payload={})", payload);
    }

    @KafkaListener(
            topics = KafkaTopics.SLOT_UNAVAILABLE,
            groupId = "reservation-service-consumer",
            containerFactory = "slotEventListenerContainerFactory"
    )
    public void onSlotUnavailable(String payload) {
        log.info("[SlotEventConsumer] slot.unavailable 수신 (payload={})", payload);
    }
}
