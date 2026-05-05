package com.michelet.reservation.infrastructure.client.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record WaitingTokenVerifyResponse(
        UUID waitingId,
        String token,
        Long position,
        String status,
        LocalDateTime enteredAt,
        LocalDateTime activatedAt,
        Long estimatedWaitSeconds,
        String accessToken
) {
}
