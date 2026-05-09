package com.michelet.reservation.infrastructure.client;

import com.michelet.common.response.ApiResponse;
import com.michelet.reservation.infrastructure.client.dto.WaitingTokenVerifyResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "waiting-service")
public interface WaitingClient {

    @GetMapping("/internal/waitings/verify-token")
    ApiResponse<WaitingTokenVerifyResponse> verifyToken(
            @RequestParam("token") String token
    );

    @DeleteMapping("/internal/waitings/{waitingId}/complete")
    ApiResponse<Void> completeWaiting(
            @PathVariable("waitingId") UUID waitingId
    );
}
