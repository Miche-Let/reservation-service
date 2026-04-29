package com.michelet.reservation.presentation.reservation;

import com.michelet.common.response.ApiResponse;
import com.michelet.reservation.application.reservation.ReservationQueryService;
import com.michelet.reservation.application.reservation.command.CancelReservationCommand;
import com.michelet.reservation.application.reservation.command.CreateReservationCommand;
import com.michelet.reservation.application.reservation.command.ModifyReservationCommand;
import com.michelet.reservation.domain.enums.ReservationStatus;
import com.michelet.reservation.presentation.reservation.dto.request.CreateReservationRequest;
import com.michelet.reservation.presentation.reservation.dto.request.ModifyReservationRequest;
import com.michelet.reservation.presentation.reservation.dto.response.ReservationResponse;
import com.michelet.reservation.presentation.reservation.dto.response.ReservationSummaryResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

  private final ReservationCommandService commandService;
  private final ReservationQueryService queryService;

  /* 권한: USER */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<ReservationResponse> create(
      @RequestHeader("X-User-Id") UUID userId,
      @RequestHeader("X-User-Role") String userRole,
      @RequestBody @Valid CreateReservationRequest request
  ) {
    List<CreateReservationCommand.CourseItem> courses = request.courses().stream()
        .map(c -> new CreateReservationCommand.CourseItem(c.courseId(), c.quantity()))
        .toList();
    ReservationResponse response = ReservationResponse.from(
        commandService.create(new CreateReservationCommand(
            userId, request.restaurantId(), request.timeSlotId(),
            request.reservedDate(), request.guestCount(), courses
        ))
    );
    return ApiResponse.ok(response);
  }

  /* 권한: USER */
  @GetMapping
  public ApiResponse<Page<ReservationSummaryResponse>> getMyReservations(
      @RequestHeader("X-User-Id") UUID userId,
      @RequestParam(required = false) ReservationStatus status,
      @PageableDefault(size = 10) Pageable pageable
  ) {
    Page<ReservationSummaryResponse> response = queryService.getList(userId, status, pageable)
        .map(ReservationSummaryResponse::from);
    return ApiResponse.ok(response);
  }

  /* 권한: USER·OWNER·MASTER */
  @GetMapping("/{reservationId}")
  public ApiResponse<ReservationResponse> getReservation(
      @RequestHeader("X-User-Id") UUID userId,
      @RequestHeader("X-User-Role") String userRole,
      @PathVariable UUID reservationId
  ) {
    ReservationResponse response = ReservationResponse.from(
        queryService.getDetail(userId, userRole, reservationId)
    );
    return ApiResponse.ok(response);
  }

  /* 권한: USER·OWNER·MASTER — 조건: CONFIRMED + modify_deadline 이내 */
  @PatchMapping("/{reservationId}")
  public ApiResponse<ReservationResponse> modify(
      @RequestHeader("X-User-Id") UUID userId,
      @RequestHeader("X-User-Role") String userRole,
      @PathVariable UUID reservationId,
      @RequestBody @Valid ModifyReservationRequest request
  ) {
    List<ModifyReservationCommand.CourseItem> courses = request.courses() == null ? null :
        request.courses().stream()
            .map(c -> new ModifyReservationCommand.CourseItem(c.courseId(), c.quantity()))
            .toList();
    ReservationResponse response = ReservationResponse.from(
        commandService.modify(new ModifyReservationCommand(
            reservationId, userId, userRole,
            request.reservedDate(), request.guestCount(), courses
        ))
    );
    return ApiResponse.ok(response);
  }

  /* 권한: USER — 조건: CONFIRMED + cancel_deadline 이내 */
  @DeleteMapping("/{reservationId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancel(
      @RequestHeader("X-User-Id") UUID userId,
      @RequestHeader("X-User-Role") String userRole,
      @PathVariable UUID reservationId
  ) {
    commandService.cancel(CancelReservationCommand.of(reservationId, userId, userRole));
  }
}
