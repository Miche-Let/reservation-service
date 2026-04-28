package com.michelet.reservation.presentation.reservation;

import com.michelet.common.response.ApiResponse;
import com.michelet.reservation.presentation.reservation.dto.request.CreateReservationRequest;
import com.michelet.reservation.presentation.reservation.dto.request.ModifyReservationRequest;
import com.michelet.reservation.presentation.reservation.dto.response.ReservationResponse;
import com.michelet.reservation.presentation.reservation.dto.response.ReservationSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

  // TODO: ReservationCommandService commandService, ReservationQueryService queryService 주입

  /* 권한: USER */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<ReservationResponse> create(
      @RequestHeader("X-User-Id") UUID userId,
      @RequestHeader("X-User-Role") String userRole,
      @RequestBody @Valid CreateReservationRequest request
  ) {
    // TODO: return ApiResponse.ok(commandService.create(userId, request));
    throw new UnsupportedOperationException("구현 예정");
  }

  /* 권한: USER */
  @GetMapping
  public ApiResponse<Page<ReservationSummaryResponse>> getMyReservations(
      @RequestHeader("X-User-Id") UUID userId,
      @RequestHeader("X-User-Role") String userRole,
      @RequestParam(required = false) String status,
      @PageableDefault(size = 10) Pageable pageable
  ) {
    // TODO: return ApiResponse.ok(queryService.getList(userId, status, pageable));
    throw new UnsupportedOperationException("구현 예정");
  }

  /* 권한: USER·OWNER·MASTER */
  @GetMapping("/{reservationId}")
  public ApiResponse<ReservationResponse> getReservation(
      @RequestHeader("X-User-Id") UUID userId,
      @RequestHeader("X-User-Role") String userRole,
      @PathVariable UUID reservationId
  ) {
    // TODO: return ApiResponse.ok(queryService.getDetail(userId, userRole, reservationId));
    throw new UnsupportedOperationException("구현 예정");
  }

  /**
   * 권한: USER·OWNER·MASTER
   * 조건: CONFIRMED + modify_deadline 이내
   */
  @PatchMapping("/{reservationId}")
  public ApiResponse<ReservationResponse> modify(
      @RequestHeader("X-User-Id") UUID userId,
      @RequestHeader("X-User-Role") String userRole,
      @PathVariable UUID reservationId,
      @RequestBody @Valid ModifyReservationRequest request
  ) {
    // TODO: return ApiResponse.ok(commandService.modify(userId, userRole, reservationId, request));
    throw new UnsupportedOperationException("구현 예정");
  }

}