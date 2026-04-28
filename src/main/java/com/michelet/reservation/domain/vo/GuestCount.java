package com.michelet.reservation.domain.vo;

import com.michelet.common.exception.BusinessException;
import com.michelet.reservation.domain.exception.ReservationErrorCode;

public record GuestCount(int value) {

  public GuestCount {
    if (value < 1 || value > 20) {
      throw  new BusinessException(ReservationErrorCode.INVALID_GUEST_COUNT);
    }
  }

  public static GuestCount of(int value) {
    return new GuestCount(value);
  }
}