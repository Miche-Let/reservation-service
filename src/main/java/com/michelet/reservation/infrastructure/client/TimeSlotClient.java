package com.michelet.reservation.infrastructure.client;

import com.michelet.common.response.ApiResponse;
import com.michelet.reservation.infrastructure.client.dto.TimeSlotDeductCapacityRequest;
import com.michelet.reservation.infrastructure.client.dto.TimeSlotRestoreCapacityRequest;
import com.michelet.reservation.infrastructure.config.FeignTimeSlotConfig;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "timeslot-service",
        url = "${feign.timeslot-service.url}",
        configuration = FeignTimeSlotConfig.class
)
public interface TimeSlotClient {

    @PatchMapping("/internal/v1/time-slots/{timeSlotId}/deduct")
    ApiResponse<Void> decrementStock(
            @PathVariable("timeSlotId") UUID timeSlotId,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @RequestBody TimeSlotDeductCapacityRequest request
    );

    @PatchMapping("/internal/v1/time-slots/{timeSlotId}/restore")
    ApiResponse<Void> incrementStock(
            @PathVariable("timeSlotId") UUID timeSlotId,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @RequestBody TimeSlotRestoreCapacityRequest request
    );
}
