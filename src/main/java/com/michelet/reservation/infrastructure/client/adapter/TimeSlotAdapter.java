package com.michelet.reservation.infrastructure.client.adapter;

import com.michelet.reservation.application.port.TimeSlotPort;
import com.michelet.reservation.infrastructure.client.TimeSlotClient;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TimeSlotAdapter implements TimeSlotPort {

    private final TimeSlotClient timeSlotClient;

    @Override
    public void decrementStock(UUID timeSlotId, LocalDate date) {
        timeSlotClient.decrementStock(timeSlotId, date);
    }

    @Override
    public void incrementStock(UUID timeSlotId, LocalDate date) {
        timeSlotClient.incrementStock(timeSlotId, date);
    }
}
