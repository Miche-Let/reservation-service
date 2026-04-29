package com.michelet.reservation.infrastructure.client.dto;

import java.time.LocalTime;
import java.util.UUID;

public record TimeSlotResponse(
    UUID timeSlotId,
    LocalTime startTime
) {}
