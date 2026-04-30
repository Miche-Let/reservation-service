package com.michelet.reservation.infrastructure.client.adapter;

import com.michelet.reservation.application.port.TimeSlotPort;
import com.michelet.reservation.infrastructure.client.TimeSlotClient;
import com.michelet.reservation.infrastructure.client.dto.TimeSlotDeductCapacityRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimeSlotAdapter implements TimeSlotPort {

    private final TimeSlotClient timeSlotClient;

    @Override
    public void decrementStock(UUID timeSlotId, int requiredCapacity) {
        timeSlotClient.decrementStock(timeSlotId, new TimeSlotDeductCapacityRequest(requiredCapacity));
    }

    @Override
    public void incrementStock(UUID timeSlotId, int requiredCapacity) {
        // TODO: timeslot-service restore API 미구현 — 구현 시 차감과 동일하게 POST /deduct 역방향 or 별도 엔드포인트 호출
        log.warn("incrementStock skipped — timeslot-service restore API not yet implemented. timeSlotId={}, requiredCapacity={}", timeSlotId, requiredCapacity);
    }
}
