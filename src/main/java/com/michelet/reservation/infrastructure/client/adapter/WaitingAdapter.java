package com.michelet.reservation.infrastructure.client.adapter;

import com.michelet.reservation.application.port.WaitingPort;
import com.michelet.reservation.application.port.WaitingTokenResult;
import com.michelet.reservation.infrastructure.client.WaitingClient;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WaitingAdapter implements WaitingPort {

    private final WaitingClient waitingClient;

    @Override
    public WaitingTokenResult verifyToken(String token) {
        var res = waitingClient.verifyToken(token).data();
        boolean valid = "ACTIVE".equalsIgnoreCase(res.status());
        return new WaitingTokenResult(res.waitingId(), valid);
    }

    @Override
    public void completeWaiting(UUID waitingId) {
        waitingClient.completeWaiting(waitingId);
    }
}
