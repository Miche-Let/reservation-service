package com.michelet.reservation.application.port;

import java.time.LocalDate;
import java.util.UUID;

public interface TimeSlotPort {
    void decrementStock(UUID timeSlotId, int requiredCapacity);
    void incrementStock(UUID timeSlotId, LocalDate date);
}
