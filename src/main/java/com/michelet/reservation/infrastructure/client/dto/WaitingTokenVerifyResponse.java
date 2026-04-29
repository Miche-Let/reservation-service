package com.michelet.reservation.infrastructure.client.dto;

import java.util.UUID;

public record WaitingTokenVerifyResponse(
    UUID userId,
    UUID restaurantId,
    boolean valid
) {}
