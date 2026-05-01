package com.michelet.reservation.domain.vo;

import com.michelet.common.exception.BusinessException;
import com.michelet.reservation.domain.exception.ReservationErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void 금액_0원은_유효하다() {
        assertThat(Money.of(0).value()).isZero();
    }

    @Test
    void 양수_금액은_유효하다() {
        assertThat(Money.of(10000).value()).isEqualTo(10000);
    }

    @Test
    void 음수_금액은_예외를_던진다() {
        assertThatThrownBy(() -> Money.of(-1))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ReservationErrorCode.INVALID_MONEY_AMOUNT.getCode()));
    }

    @Test
    void 금액_더하기_성공() {
        Money result = Money.of(10000).add(Money.of(5000));
        assertThat(result.value()).isEqualTo(15000);
    }

    @Test
    void 금액_더하기_오버플로우_예외() {
        assertThatThrownBy(() -> Money.of(Integer.MAX_VALUE).add(Money.of(1)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ReservationErrorCode.MONEY_OVERFLOW.getCode()));
    }

    @Test
    void 금액_곱하기_성공() {
        Money result = Money.of(10000).multiply(3);
        assertThat(result.value()).isEqualTo(30000);
    }

    @Test
    void 금액_곱하기_수량_0은_0원() {
        assertThat(Money.of(10000).multiply(0).value()).isZero();
    }

    @Test
    void 금액_곱하기_음수_수량_예외() {
        assertThatThrownBy(() -> Money.of(10000).multiply(-1))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ReservationErrorCode.INVALID_MULTIPLY_QUANTITY.getCode()));
    }

    @Test
    void 금액_곱하기_오버플로우_예외() {
        assertThatThrownBy(() -> Money.of(Integer.MAX_VALUE).multiply(2))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ReservationErrorCode.MONEY_OVERFLOW.getCode()));
    }
}
