package com.michelet.reservation.presentation.reservation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.michelet.common.auth.core.annotation.RequireRole;
import com.michelet.common.auth.core.enums.UserRole;
import com.michelet.common.auth.webmvc.context.UserContextHolder;
import com.michelet.common.response.ApiResponse;
import com.michelet.reservation.application.reservation.ReservationCommandService;
import com.michelet.reservation.application.reservation.ReservationQueryService;
import com.michelet.reservation.application.reservation.command.CancelReservationCommand;
import com.michelet.reservation.application.reservation.command.CreateReservationCommand;
import com.michelet.reservation.application.reservation.command.DeleteReservationCommand;
import com.michelet.reservation.application.reservation.command.ModifyReservationCommand;
import com.michelet.reservation.common.GatewayHeaders;
import com.michelet.reservation.domain.enums.ReservationStatus;
import com.michelet.reservation.domain.exception.ReservationSuccessCode;
import com.michelet.reservation.infrastructure.idempotency.IdempotencyService;
import com.michelet.reservation.presentation.reservation.dto.request.CreateReservationRequest;
import com.michelet.reservation.presentation.reservation.dto.request.ModifyReservationRequest;
import com.michelet.reservation.presentation.reservation.dto.response.ReservationResponse;
import com.michelet.reservation.presentation.reservation.dto.response.ReservationSummaryResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
@RequireRole({UserRole.USER, UserRole.OWNER, UserRole.MASTER})
public class ReservationController {

    private final ReservationCommandService commandService;
    private final ReservationQueryService queryService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequireRole(UserRole.USER)
    public ApiResponse<ReservationResponse> create(
            @RequestHeader(GatewayHeaders.WAITING_TOKEN) String waitingToken,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody @Valid CreateReservationRequest request
    ) {
        UUID userId = currentUserId();
        String scopedKey = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? "reservation:create:" + userId + ":" + idempotencyKey
                : null;
        if (scopedKey != null) {
            var cached = idempotencyService.findCachedResponse(scopedKey);
            if (cached.isPresent()) {
                try {
                    return ApiResponse.ok(ReservationSuccessCode.RESERVATION_CREATED,
                            objectMapper.readValue(cached.get(), ReservationResponse.class));
                } catch (JsonProcessingException e) {
                    log.warn("[create] idempotency cache 역직렬화 실패 — key={}", scopedKey, e);
                }
            }
        }

        List<CreateReservationCommand.CourseItem> courses = request.courses().stream()
                .map(c -> new CreateReservationCommand.CourseItem(c.courseId(), c.quantity(), c.unitPrice()))
                .toList();
        ReservationResponse response = ReservationResponse.from(
                commandService.create(new CreateReservationCommand(
                        userId, waitingToken, request.restaurantId(), request.timeSlotId(),
                        request.reservedDate(), request.slotStartTime(),
                        request.guestCount(), courses
                ))
        );

        if (scopedKey != null) {
            try {
                idempotencyService.cacheResponse(scopedKey, objectMapper.writeValueAsString(response));
            } catch (JsonProcessingException e) {
                log.warn("[create] idempotency cache 저장 실패 — key={}", scopedKey, e);
            }
        }

        return ApiResponse.ok(ReservationSuccessCode.RESERVATION_CREATED, response);
    }

    @GetMapping
    @RequireRole(UserRole.USER)
    public ApiResponse<Page<ReservationSummaryResponse>> getMyReservations(
            @RequestParam(required = false) ReservationStatus status,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<ReservationSummaryResponse> response = queryService.getList(currentUserId(), status, pageable)
                .map(ReservationSummaryResponse::from);
        return ApiResponse.ok(ReservationSuccessCode.RESERVATION_FETCHED, response);
    }

    @GetMapping("/{reservationId}")
    public ApiResponse<ReservationResponse> getReservation(
            @PathVariable UUID reservationId
    ) {
        ReservationResponse response = ReservationResponse.from(
                queryService.getDetail(currentUserId(), currentUserRole(), reservationId)
        );
        return ApiResponse.ok(ReservationSuccessCode.RESERVATION_FETCHED, response);
    }

    @PatchMapping("/{reservationId}/cancel")
    public ApiResponse<ReservationResponse> cancel(
            @PathVariable UUID reservationId
    ) {
        ReservationResponse response = ReservationResponse.from(
                commandService.cancel(CancelReservationCommand.of(reservationId, currentUserId(), currentUserRole()))
        );
        return ApiResponse.ok(ReservationSuccessCode.RESERVATION_CANCELLED, response);
    }

    @DeleteMapping("/{reservationId}")
    public ApiResponse<ReservationResponse> delete(
            @PathVariable UUID reservationId
    ) {
        ReservationResponse response = ReservationResponse.from(
                commandService.delete(new DeleteReservationCommand(reservationId, currentUserId(), currentUserRole()))
        );
        return ApiResponse.ok(ReservationSuccessCode.RESERVATION_DELETED, response);
    }

    @PatchMapping("/{reservationId}")
    public ApiResponse<ReservationResponse> modify(
            @PathVariable UUID reservationId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody @Valid ModifyReservationRequest request
    ) {
        UUID userId = currentUserId();
        String scopedKey = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? "reservation:modify:" + reservationId + ":" + userId + ":" + idempotencyKey
                : null;
        if (scopedKey != null) {
            var cached = idempotencyService.findCachedResponse(scopedKey);
            if (cached.isPresent()) {
                try {
                    return ApiResponse.ok(ReservationSuccessCode.RESERVATION_MODIFIED,
                            objectMapper.readValue(cached.get(), ReservationResponse.class));
                } catch (JsonProcessingException e) {
                    log.warn("[modify] idempotency cache 역직렬화 실패 — key={}", scopedKey, e);
                }
            }
        }

        List<ModifyReservationCommand.CourseItem> courses = request.courses() == null ? null :
                request.courses().stream()
                        .map(c -> new ModifyReservationCommand.CourseItem(c.courseId(), c.quantity(), c.unitPrice()))
                        .toList();
        ReservationResponse response = ReservationResponse.from(
                commandService.modify(new ModifyReservationCommand(
                        reservationId, userId, currentUserRole(),
                        request.timeSlotId(), request.reservedDate(), request.slotStartTime(),
                        request.guestCount(), courses
                ))
        );

        if (scopedKey != null) {
            try {
                idempotencyService.cacheResponse(scopedKey, objectMapper.writeValueAsString(response));
            } catch (JsonProcessingException e) {
                log.warn("[modify] idempotency cache 저장 실패 — key={}", scopedKey, e);
            }
        }

        return ApiResponse.ok(ReservationSuccessCode.RESERVATION_MODIFIED, response);
    }

    private UUID currentUserId() {
        return UUID.fromString(UserContextHolder.get().userId());
    }

    private String currentUserRole() {
        return UserContextHolder.get().role().name();
    }
}
