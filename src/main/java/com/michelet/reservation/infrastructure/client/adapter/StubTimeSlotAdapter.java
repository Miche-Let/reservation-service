package com.michelet.reservation.infrastructure.client.adapter;

import com.michelet.reservation.application.port.TimeSlotPort;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

// local 프로파일 전용 — timeslot-service 없이 테스트 가능하도록 재고 차감/복구를 no-op으로 처리합니다.
@Slf4j
@Primary
@Component
@ConditionalOnProperty(name = "timeslot.stub", havingValue = "true")
public class StubTimeSlotAdapter implements TimeSlotPort {

    @Override
    public void decrementStock(UUID timeSlotId, int requiredCapacity, UUID reservationId) {
        log.debug("[STUB] decrementStock skipped — timeSlotId={}, requiredCapacity={}, reservationId={}",
                timeSlotId, requiredCapacity, reservationId);
    }

    @Override
    public void incrementStock(UUID timeSlotId, int requiredCapacity, String idempotencyKey) {
        log.debug("[STUB] incrementStock skipped — timeSlotId={}, requiredCapacity={}, idempotencyKey={}",
                timeSlotId, requiredCapacity, idempotencyKey);
    }
}
