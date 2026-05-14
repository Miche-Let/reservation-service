package com.michelet.reservation.infrastructure.kafka.consumer;

import com.michelet.reservation.infrastructure.kafka.dedup.ConsumedEventTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// TODO: timeslot-service 이벤트 발행 스펙 확정 후 @KafkaListener 추가 및 비즈니스 로직 구현
// - slot.deducted  → 예약 CONFIRMED 처리 (타임아웃으로 WAITING 상태인 경우)
// - slot.unavailable → 예약 취소 보상 처리
// 리스너를 달기 전까지 offset이 소비되지 않으므로, 스펙 확정 후 earliest 오프셋부터 처리 가능
//
// 구현 시 멱등 처리 패턴:
//
// @KafkaListener(topics = {KafkaTopics.SLOT_DEDUCTED, KafkaTopics.SLOT_UNAVAILABLE},
//                containerFactory = "slotEventListenerContainerFactory")
// public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
//     SlotDeductedEvent event = objectMapper.readValue(record.value(), SlotDeductedEvent.class);
//     if (!consumedEventTracker.tryMarkAsNew(event.eventId().toString())) {
//         log.info("[SlotEventConsumer] 중복 이벤트 skip — eventId={}", event.eventId());
//         ack.acknowledge();
//         return;
//     }
//     // 비즈니스 로직
//     ack.acknowledge();
// }
@Slf4j
@Component
@RequiredArgsConstructor
public class SlotEventConsumer {

    private final ConsumedEventTracker consumedEventTracker;
}
