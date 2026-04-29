package com.michelet.reservation.infrastructure.client;

import com.michelet.common.response.ApiResponse;
import com.michelet.reservation.infrastructure.client.dto.WaitingTokenVerifyResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "waiting-service", url = "${feign.waiting-service.url}")
public interface WaitingClient {

    // 1차: 예약 서비스 내 서명 검증 → 2차: 이 API로 대기열 유효성 확인
    @GetMapping("/internal/waiting/verify-token")
    ApiResponse<WaitingTokenVerifyResponse> verifyToken(
            @RequestHeader("X-Waiting-Token") String token
    );
}
