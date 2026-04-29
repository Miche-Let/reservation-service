package com.michelet.reservation.application.port;

import java.util.UUID;

public record WaitingTokenResult(UUID userId, UUID restaurantId, boolean valid) {}
