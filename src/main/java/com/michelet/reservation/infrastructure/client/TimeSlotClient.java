package com.michelet.reservation.infrastructure.client;

import com.michelet.common.response.ApiResponse;
import com.michelet.reservation.infrastructure.client.dto.TimeSlotResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.UUID;

@FeignClient(name = "timeslot-service", url = "${feign.timeslot-service.url}")
public interface TimeSlotClient {

  @GetMapping("/internal/timeslots/{timeSlotId}")
  ApiResponse<TimeSlotResponse> getTimeSlot(@PathVariable UUID timeSlotId);

  @PatchMapping("/internal/timeslots/{timeSlotId}/decrement")
  ApiResponse<Void> decrementStock(@PathVariable UUID timeSlotId, @RequestParam LocalDate date);

  @PatchMapping("/internal/timeslots/{timeSlotId}/increment")
  ApiResponse<Void> incrementStock(@PathVariable UUID timeSlotId, @RequestParam LocalDate date);
}
