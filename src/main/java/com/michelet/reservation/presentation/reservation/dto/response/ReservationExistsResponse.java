package com.michelet.reservation.presentation.reservation.dto.response;

import com.michelet.reservation.application.reservation.result.ReservationExistsResult;

public record ReservationExistsResponse(boolean exists) {

  public static ReservationExistsResponse from(ReservationExistsResult result) {
    return new ReservationExistsResponse(result.exists());
  }
}
