package com.michelet.reservation.presentation.reservation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.michelet.common.exception.BusinessException;
import com.michelet.common.exception.GlobalExceptionHandler;
import com.michelet.reservation.application.reservation.ReservationCommandService;
import com.michelet.reservation.application.reservation.ReservationQueryService;
import com.michelet.reservation.application.reservation.result.ReservationActiveResult;
import com.michelet.reservation.application.reservation.result.ReservationExistsResult;
import com.michelet.reservation.application.reservation.result.ReservationStatusResult;
import com.michelet.reservation.application.reservation.result.ReservationValidityResult;
import com.michelet.reservation.domain.enums.ReservationStatus;
import com.michelet.reservation.domain.exception.ReservationErrorCode;
import com.michelet.reservation.presentation.reservation.dto.request.CheckInRequest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReservationInternalController.class)
@Import(GlobalExceptionHandler.class)
class ReservationInternalControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean ReservationCommandService commandService;
    @MockitoBean ReservationQueryService queryService;

    final UUID userId        = UUID.randomUUID();
    final UUID restaurantId  = UUID.randomUUID();
    final UUID reservationId = UUID.randomUUID();

    @Nested
    class CheckValidity {

        @Test
        void 예약이_있으면_exists_true를_반환한다() throws Exception {
            when(queryService.checkValidity(userId, restaurantId))
                    .thenReturn(ReservationValidityResult.found());

            mockMvc.perform(get("/internal/reservations/verify")
                            .param("userId", userId.toString())
                            .param("restaurantId", restaurantId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.exists").value(true));
        }

        @Test
        void 예약이_없으면_exists_false를_반환한다() throws Exception {
            when(queryService.checkValidity(userId, restaurantId))
                    .thenReturn(ReservationValidityResult.notFound());

            mockMvc.perform(get("/internal/reservations/verify")
                            .param("userId", userId.toString())
                            .param("restaurantId", restaurantId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.exists").value(false));
        }
    }

    @Nested
    class CheckIn {

        @Test
        void 정상_체크인_시_200과_COMPLETED_상태를_반환한다() throws Exception {
            when(commandService.checkIn(any()))
                    .thenReturn(new ReservationStatusResult(reservationId, ReservationStatus.COMPLETED,
                            java.time.LocalDate.of(2026, 6, 1), java.time.LocalDateTime.now()));

            CheckInRequest req = new CheckInRequest(reservationId, restaurantId);

            mockMvc.perform(patch("/internal/reservations/check-in")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.reservationId").value(reservationId.toString()))
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        }

        @Test
        void reservationId_없으면_400을_반환한다() throws Exception {
            CheckInRequest req = new CheckInRequest(null, restaurantId);

            mockMvc.perform(patch("/internal/reservations/check-in")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        void restaurantId_없으면_400을_반환한다() throws Exception {
            CheckInRequest req = new CheckInRequest(reservationId, null);

            mockMvc.perform(patch("/internal/reservations/check-in")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void 예약이_없으면_404를_반환한다() throws Exception {
            when(commandService.checkIn(any()))
                    .thenThrow(new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));

            CheckInRequest req = new CheckInRequest(reservationId, restaurantId);

            mockMvc.perform(patch("/internal/reservations/check-in")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ReservationErrorCode.RESERVATION_NOT_FOUND.getCode()));
        }

        @Test
        void CONFIRMED_아닌_상태에서_체크인하면_400을_반환한다() throws Exception {
            when(commandService.checkIn(any()))
                    .thenThrow(new BusinessException(ReservationErrorCode.INVALID_STATUS_TRANSITION));

            CheckInRequest req = new CheckInRequest(reservationId, restaurantId);

            mockMvc.perform(patch("/internal/reservations/check-in")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ReservationErrorCode.INVALID_STATUS_TRANSITION.getCode()));
        }
    }

    @Nested
    class CheckExists {

        @Test
        void 조건_일치_시_exists_true를_반환한다() throws Exception {
            when(queryService.checkExists(reservationId, userId, restaurantId))
                    .thenReturn(ReservationExistsResult.found());

            mockMvc.perform(get("/internal/reservations/exists")
                            .param("reservationId", reservationId.toString())
                            .param("userId", userId.toString())
                            .param("restaurantId", restaurantId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.exists").value(true));
        }

        @Test
        void 조건_불일치_시_exists_false를_반환한다() throws Exception {
            when(queryService.checkExists(reservationId, userId, restaurantId))
                    .thenReturn(ReservationExistsResult.notFound());

            mockMvc.perform(get("/internal/reservations/exists")
                            .param("reservationId", reservationId.toString())
                            .param("userId", userId.toString())
                            .param("restaurantId", restaurantId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.exists").value(false));
        }
    }

    @Nested
    class CheckActiveReservation {

        @Test
        void 진행중인_예약이_있으면_exists_true를_반환한다() throws Exception {
            when(queryService.hasActiveReservation(userId))
                    .thenReturn(ReservationActiveResult.found());

            mockMvc.perform(get("/internal/reservations/active")
                            .param("userId", userId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.exists").value(true));
        }

        @Test
        void 진행중인_예약이_없으면_exists_false를_반환한다() throws Exception {
            when(queryService.hasActiveReservation(userId))
                    .thenReturn(ReservationActiveResult.notFound());

            mockMvc.perform(get("/internal/reservations/active")
                            .param("userId", userId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.exists").value(false));
        }
    }
}
