package com.michelet.reservation.domain.vo;

import com.michelet.common.exception.BusinessException;
import com.michelet.reservation.domain.exception.ReservationErrorCode;

public record Money(int value) {

  public Money {
    if (value < 0) {
      throw new BusinessException(ReservationErrorCode.INVALID_MONEY_AMOUNT);
    }
  }

  public static Money of(int value) {
    return new Money(value);
  }

  public Money add(Money other) {
    try {
      return new Money(Math.addExact(this.value, other.value));
    } catch (ArithmeticException e) {
      throw new BusinessException(ReservationErrorCode.MONEY_OVERFLOW);
    }
  }

  public Money multiply(int quantity) {
    if (quantity < 0) {
      throw new BusinessException(ReservationErrorCode.INVALID_MULTIPLY_QUANTITY);
    }
    try {
      return new Money(Math.multiplyExact(this.value, quantity));
    } catch (ArithmeticException e) {
      throw new BusinessException(ReservationErrorCode.MONEY_OVERFLOW);
    }
  }
}