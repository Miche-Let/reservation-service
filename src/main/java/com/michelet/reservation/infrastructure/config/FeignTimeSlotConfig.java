package com.michelet.reservation.infrastructure.config;

import feign.Retryer;
import org.springframework.context.annotation.Bean;

/**
 * timeslot-service Feign 클라이언트 전용 설정.
 * Spring Cloud OpenFeign 기본값(NEVER_RETRY)과 동일하지만, 이중 차감 방지 의도를 코드로 명시한다.
 *
 * 주의: @Configuration 없이 @FeignClient(configuration = ...) 로만 등록 — 전역 빈 충돌 방지
 */
public class FeignTimeSlotConfig {

    @Bean
    public Retryer timeslotServiceRetryer() {
        // 재시도 시 슬롯 이중 차감 위험 → 절대 재시도 없음
        return Retryer.NEVER_RETRY;
    }
}
