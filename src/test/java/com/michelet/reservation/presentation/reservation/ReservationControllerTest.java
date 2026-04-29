package com.michelet.reservation.presentation.reservation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.michelet.common.exception.BusinessException;
import com.michelet.common.exception.GlobalExceptionHandler;
import com.michelet.reservation.application.reservation.ReservationCommandService;
import com.michelet.reservation.application.reservation.ReservationQueryService;
import com.michelet.reservation.application.reservation.result.ReservationResult;
import com.michelet.reservation.application.reservation.result.ReservationSummaryResult;
import com.michelet.reservation.domain.enums.ReservationStatus;
import com.michelet.reservation.domain.exception.ReservationErrorCode;
import com.michelet.reservation.presentation.reservation.dto.request.CreateReservationRequest;
import com.michelet.reservation.presentation.reservation.dto.request.ModifyReservationRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReservationController.class)
@Import(GlobalExceptionHandler.class)
class ReservationControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @MockitoBean
    ReservationCommandService commandService;
    @MockitoBean
    ReservationQueryService queryService;

    final UUID userId = UUID.randomUUID();
    final UUID restaurantId = UUID.randomUUID();
    final UUID timeSlotId = UUID.randomUUID();
    final UUID reservationId = UUID.randomUUID();
    final LocalDate futureDate = LocalDate.now().plusDays(10);
    final LocalTime slotStartTime = LocalTime.of(19, 0);

    ReservationResult reservationResult() {
        return new ReservationResult(
                reservationId, userId, restaurantId, timeSlotId,
                futureDate, 2, ReservationStatus.CONFIRMED,
                futureDate.minusDays(2), futureDate.minusDays(2),
                LocalDateTime.of(futureDate, slotStartTime).plusMinutes(30),
                List.of()
        );
    }

    CreateReservationRequest validCreateRequest() {
        return new CreateReservationRequest(
                restaurantId, timeSlotId, futureDate, slotStartTime, 2,
                List.of(new CreateReservationRequest.CourseItem(UUID.randomUUID(), 1, 30000))
        );
    }

    @Nested
    class Create {

        @Test
        void 정상_요청_시_201을_반환한다() throws Exception {
            when(commandService.create(any())).thenReturn(reservationResult());

            mockMvc.perform(post("/api/v1/reservations")
                            .header("X-User-Id", userId)
                            .header("X-User-Role", "USER")
                            .header("X-Waiting-Token", "valid-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.reservationId").value(reservationId.toString()))
                    .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
        }

        @Test
        void restaurantId_없으면_400을_반환한다() throws Exception {
            CreateReservationRequest req = new CreateReservationRequest(
                    null, timeSlotId, futureDate, slotStartTime, 2,
                    List.of(new CreateReservationRequest.CourseItem(UUID.randomUUID(), 1, 30000))
            );

            mockMvc.perform(post("/api/v1/reservations")
                            .header("X-User-Id", userId)
                            .header("X-User-Role", "USER")
                            .header("X-Waiting-Token", "valid-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        void courses_비어있으면_400을_반환한다() throws Exception {
            CreateReservationRequest req = new CreateReservationRequest(
                    restaurantId, timeSlotId, futureDate, slotStartTime, 2, List.of()
            );

            mockMvc.perform(post("/api/v1/reservations")
                            .header("X-User-Id", userId)
                            .header("X-User-Role", "USER")
                            .header("X-Waiting-Token", "valid-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void 대기열_토큰_유효하지_않으면_403을_반환한다() throws Exception {
            when(commandService.create(any()))
                    .thenThrow(new BusinessException(ReservationErrorCode.INVALID_WAITING_TOKEN));

            mockMvc.perform(post("/api/v1/reservations")
                            .header("X-User-Id", userId)
                            .header("X-User-Role", "USER")
                            .header("X-Waiting-Token", "bad-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest())))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ReservationErrorCode.INVALID_WAITING_TOKEN.getCode()));
        }
    }

    @Nested
    class GetList {

        @Test
        void status_없이_요청_시_전체_목록을_반환한다() throws Exception {
            ReservationSummaryResult summary = new ReservationSummaryResult(
                    reservationId, restaurantId, futureDate, 2, ReservationStatus.CONFIRMED,
                    futureDate.minusDays(2), futureDate.minusDays(2)
            );
            when(queryService.getList(eq(userId), isNull(), any()))
                    .thenReturn(new PageImpl<>(List.of(summary)));

            mockMvc.perform(get("/api/v1/reservations")
                            .header("X-User-Id", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].reservationId").value(reservationId.toString()));
        }

        @Test
        void status_파라미터로_필터링된_목록을_반환한다() throws Exception {
            when(queryService.getList(eq(userId), eq(ReservationStatus.CONFIRMED), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/v1/reservations")
                            .header("X-User-Id", userId)
                            .param("status", "CONFIRMED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray());
        }
    }

    @Nested
    class GetDetail {

        @Test
        void 정상_조회_시_200을_반환한다() throws Exception {
            when(queryService.getDetail(eq(userId), eq("USER"), eq(reservationId)))
                    .thenReturn(reservationResult());

            mockMvc.perform(get("/api/v1/reservations/{id}", reservationId)
                            .header("X-User-Id", userId)
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.reservationId").value(reservationId.toString()));
        }

        @Test
        void 예약이_없으면_404를_반환한다() throws Exception {
            when(queryService.getDetail(any(), any(), any()))
                    .thenThrow(new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));

            mockMvc.perform(get("/api/v1/reservations/{id}", reservationId)
                            .header("X-User-Id", userId)
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ReservationErrorCode.RESERVATION_NOT_FOUND.getCode()));
        }
    }

    @Nested
    class Modify {

        @Test
        void 정상_수정_시_200을_반환한다() throws Exception {
            when(commandService.modify(any())).thenReturn(reservationResult());

            ModifyReservationRequest req = new ModifyReservationRequest(null, null, null, null, null);

            mockMvc.perform(patch("/api/v1/reservations/{id}", reservationId)
                            .header("X-User-Id", userId)
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.reservationId").value(reservationId.toString()));
        }

        @Test
        void 수정_기한_초과_시_400을_반환한다() throws Exception {
            when(commandService.modify(any()))
                    .thenThrow(new BusinessException(ReservationErrorCode.MODIFY_DEADLINE_EXCEEDED));

            ModifyReservationRequest req = new ModifyReservationRequest(null, futureDate, null, null, null);

            mockMvc.perform(patch("/api/v1/reservations/{id}", reservationId)
                            .header("X-User-Id", userId)
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ReservationErrorCode.MODIFY_DEADLINE_EXCEEDED.getCode()));
        }
    }

    @Nested
    class Cancel {

        @Test
        void 정상_취소_시_204를_반환한다() throws Exception {
            doNothing().when(commandService).cancel(any());

            mockMvc.perform(delete("/api/v1/reservations/{id}", reservationId)
                            .header("X-User-Id", userId)
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isNoContent());
        }

        @Test
        void 취소_기한_초과_시_400을_반환한다() throws Exception {
            doThrow(new BusinessException(ReservationErrorCode.CANCEL_DEADLINE_EXCEEDED))
                    .when(commandService).cancel(any());

            mockMvc.perform(delete("/api/v1/reservations/{id}", reservationId)
                            .header("X-User-Id", userId)
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ReservationErrorCode.CANCEL_DEADLINE_EXCEEDED.getCode()));
        }
    }
}
