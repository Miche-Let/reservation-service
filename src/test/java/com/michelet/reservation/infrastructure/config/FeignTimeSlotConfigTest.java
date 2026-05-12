package com.michelet.reservation.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import feign.Retryer;
import org.junit.jupiter.api.Test;

class FeignTimeSlotConfigTest {

    @Test
    void timeslot_service_retryer가_NEVER_RETRY로_설정된다() {
        FeignTimeSlotConfig config = new FeignTimeSlotConfig();
        assertThat(config.timeslotServiceRetryer()).isSameAs(Retryer.NEVER_RETRY);
    }
}
