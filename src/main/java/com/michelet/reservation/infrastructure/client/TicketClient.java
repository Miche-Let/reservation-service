package com.michelet.reservation.infrastructure.client;

import com.michelet.common.response.ApiResponse;
import com.michelet.reservation.infrastructure.client.dto.TicketClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "ticket-service", url = "${feign.ticket-service.url}")
public interface TicketClient {

  @GetMapping("/internal/courses/{courseId}/price")
  ApiResponse<TicketClientResponse> getCoursePrice(@PathVariable UUID courseId);
}
