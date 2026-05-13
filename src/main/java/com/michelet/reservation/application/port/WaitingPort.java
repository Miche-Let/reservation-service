package com.michelet.reservation.application.port;

import java.util.UUID;

public interface WaitingPort {
    WaitingTokenResult verifyToken(String token);
}
