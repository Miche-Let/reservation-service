package com.michelet.reservation.infrastructure.client.dto;

public record TimeSlotDeductCapacityRequest(
        int requiredCapacity
) {
}
