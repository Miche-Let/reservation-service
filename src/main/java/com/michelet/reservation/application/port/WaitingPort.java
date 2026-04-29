package com.michelet.reservation.application.port;

public interface WaitingPort {
    WaitingTokenResult verifyToken(String token);
}
