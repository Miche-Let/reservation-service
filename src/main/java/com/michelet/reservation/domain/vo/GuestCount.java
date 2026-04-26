package com.michelet.reservation.domain.vo;

import com.michelet.reservation.domain.exception.ReservationErrorCode;

public record GuestCount(int value) {

  public GuestCount {
    if (value < 1 || value > 20) {
      throw new IllegalArgumentException(
          ReservationErrorCode.INVALID_GUEST_COUNT.getMessage()
      );
    }
  }

  public static GuestCount of(int value) {
    return new GuestCount(value);
  }
}