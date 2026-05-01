package com.michelet.reservation.application.port;

import java.util.UUID;

public record WaitingTokenResult(UUID waitingId, boolean valid) {}
