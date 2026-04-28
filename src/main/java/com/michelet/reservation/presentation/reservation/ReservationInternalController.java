package com.michelet.reservation.presentation.reservation;

import com.michelet.common.response.ApiResponse;
import com.michelet.reservation.application.reservation.ReservationCommandService;
import com.michelet.reservation.application.reservation.ReservationQueryService;
import com.michelet.reservation.application.reservation.command.CheckInCommand;
import com.michelet.reservation.presentation.reservation.dto.request.CheckInRequest;
import com.michelet.reservation.presentation.reservation.dto.response.ReservationExistsResponse;
import com.michelet.reservation.presentation.reservation.dto.response.ReservationStatusResponse;
import com.michelet.reservation.presentation.reservation.dto.response.ReservationValidityResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/reservations")
@RequiredArgsConstructor
public class ReservationInternalController {

  private final ReservationCommandService commandService;
  private final ReservationQueryService queryService;

  @GetMapping("/validity")
  public ApiResponse<ReservationValidityResponse> checkValidity(
      @RequestParam UUID userId,
      @RequestParam UUID restaurantId
  ) {
    return ApiResponse.ok(ReservationValidityResponse.from(
        queryService.checkValidity(userId, restaurantId)
    ));
  }

  @PatchMapping("/check-in")
  public ApiResponse<ReservationStatusResponse> checkIn(
      @RequestBody @Valid CheckInRequest request
  ) {
    return ApiResponse.ok(ReservationStatusResponse.from(
        commandService.checkIn(CheckInCommand.from(request))
    ));
  }

  @GetMapping("/exists")
  public ApiResponse<ReservationExistsResponse> checkExists(
      @RequestParam UUID reservationId,
      @RequestParam UUID userId,
      @RequestParam UUID restaurantId
  ) {
    return ApiResponse.ok(ReservationExistsResponse.from(
        queryService.checkExists(reservationId, userId, restaurantId)
    ));
  }
}
