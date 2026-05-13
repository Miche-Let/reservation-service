package com.michelet.reservation.infrastructure.client.adapter;

import com.michelet.reservation.application.port.WaitingPort;
import com.michelet.reservation.application.port.WaitingTokenResult;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

// local 프로파일 전용 — waiting-service 없이 테스트 가능하도록 토큰 검증을 항상 통과시킵니다.
@Slf4j
@Primary
@Component
@ConditionalOnProperty(name = "waiting.stub", havingValue = "true")
public class StubWaitingAdapter implements WaitingPort {

    @Override
    public WaitingTokenResult verifyToken(String token) {
        return new WaitingTokenResult(UUID.fromString("00000000-0000-0000-0000-000000000001"), true);
    }

    @Override
    public void completeWaiting(UUID waitingId) {
        log.debug("[STUB] completeWaiting skipped — waitingId={}", waitingId);
    }
}
