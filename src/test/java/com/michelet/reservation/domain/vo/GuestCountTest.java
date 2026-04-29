package com.michelet.reservation.domain.vo;

import com.michelet.common.exception.BusinessException;
import com.michelet.reservation.domain.exception.ReservationErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuestCountTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 10, 20})
    void 유효한_인원수는_생성된다(int value) {
        GuestCount count = GuestCount.of(value);
        assertThat(count.value()).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 21, 100})
    void 범위_밖_인원수는_예외를_던진다(int value) {
        assertThatThrownBy(() -> GuestCount.of(value))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ReservationErrorCode.INVALID_GUEST_COUNT.getCode()));
    }
}
