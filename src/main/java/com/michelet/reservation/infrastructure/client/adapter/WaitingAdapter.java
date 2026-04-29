package com.michelet.reservation.infrastructure.client.adapter;

import com.michelet.reservation.application.port.WaitingPort;
import com.michelet.reservation.application.port.WaitingTokenResult;
import com.michelet.reservation.infrastructure.client.WaitingClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WaitingAdapter implements WaitingPort {

    private final WaitingClient waitingClient;

    @Override
    public WaitingTokenResult verifyToken(String token) {
        var res = waitingClient.verifyToken(token).data();
        return new WaitingTokenResult(res.userId(), res.restaurantId(), res.valid());
    }
}
