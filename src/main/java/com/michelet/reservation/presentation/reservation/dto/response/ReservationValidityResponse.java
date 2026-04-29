package com.michelet.reservation.presentation.reservation.dto.response;

import com.michelet.reservation.application.reservation.result.ReservationValidityResult;

public record ReservationValidityResponse(boolean exists) {

  public static ReservationValidityResponse from(ReservationValidityResult result) {
    return new ReservationValidityResponse(result.exists());
  }
}
