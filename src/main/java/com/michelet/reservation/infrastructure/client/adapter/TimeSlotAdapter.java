package com.michelet.reservation.infrastructure.client.adapter;

import com.michelet.reservation.application.port.TimeSlotPort;
import com.michelet.reservation.infrastructure.client.TimeSlotClient;
import com.michelet.reservation.infrastructure.client.dto.TimeSlotDeductCapacityRequest;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TimeSlotAdapter implements TimeSlotPort {

    private final TimeSlotClient timeSlotClient;

    @Override
    public void decrementStock(UUID timeSlotId, int requiredCapacity) {
        timeSlotClient.decrementStock(timeSlotId, new TimeSlotDeductCapacityRequest(requiredCapacity));
    }

    @Override
    public void incrementStock(UUID timeSlotId, LocalDate date) {
        // TODO: timeslot-service restore API 미구현 — 현재 버전에서는 호출 생략
    }
}
