package com.michelet.reservation.presentation.reservation.dto.response;

import com.michelet.reservation.application.reservation.result.ReservationActiveResult;

public record ReservationActiveResponse(boolean exists) {

    public static ReservationActiveResponse from(ReservationActiveResult result) {
        return new ReservationActiveResponse(result.exists());
    }
}
