package com.michelet.reservation.presentation.reservation;

import com.michelet.common.response.ApiResponse;
import com.michelet.reservation.presentation.reservation.dto.request.CheckInRequest;
import com.michelet.reservation.presentation.reservation.dto.response.ReservationExistsResponse;
import com.michelet.reservation.presentation.reservation.dto.response.ReservationStatusResponse;
import com.michelet.reservation.presentation.reservation.dto.response.ReservationValidityResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/reservations")
@RequiredArgsConstructor
public class ReservationInternalController {

  // TODO: ReservationCommandService commandService, ReservationQueryService queryService 주입

  @GetMapping("/validity")
  public ApiResponse<ReservationValidityResponse> checkValidity(
      @RequestParam UUID userId,
      @RequestParam UUID restaurantId
  ) {
    // TODO: return ApiResponse.ok(queryService.checkValidity(userId, restaurantId));
    throw new UnsupportedOperationException("구현 예정");
  }

  @PatchMapping("/check-in")
  public ApiResponse<ReservationStatusResponse> checkIn(
      @RequestBody @Valid CheckInRequest request
  ) {
    // TODO: return ApiResponse.ok(commandService.checkIn(request));
    throw new UnsupportedOperationException("구현 예정");
  }

  @GetMapping("/exists")
  public ApiResponse<ReservationExistsResponse> checkExists(
      @RequestParam UUID userId,
      @RequestParam UUID restaurantId
  ) {
    // TODO: return ApiResponse.ok(queryService.checkExists(userId, restaurantId));
    throw new UnsupportedOperationException("구현 예정");
  }
}