package com.michelet.reservation.presentation.reservation.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CheckInRequest(

    @NotNull(message = "예약 ID는 필수입니다.")
    UUID reservationId,

    @NotNull(message = "레스토랑 ID는 필수입니다.")
    UUID restaurantId,

    @NotNull(message = "체크인 처리자 ID는 필수입니다.")
    UUID checkedInBy
) {}