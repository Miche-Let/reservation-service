package com.michelet.reservation.infrastructure.client.adapter;

import com.michelet.reservation.application.port.WaitingPort;
import com.michelet.reservation.application.port.WaitingTokenResult;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

// local 프로파일 전용 — waiting-service 없이 테스트 가능하도록 토큰 검증을 항상 통과시킵니다.
// X-Waiting-Token 헤더 값에서 "userId:restaurantId" 형식을 파싱하거나,
// 형식이 맞지 않으면 env 파일의 기본 UUID로 응답합니다.
@Primary
@Component
@ConditionalOnProperty(name = "waiting.stub", havingValue = "true")
public class StubWaitingAdapter implements WaitingPort {

    @Override
    public WaitingTokenResult verifyToken(String token) {
        try {
            String[] parts = token.split(":");
            return new WaitingTokenResult(UUID.fromString(parts[0]), UUID.fromString(parts[1]), true);
        } catch (Exception e) {
            return new WaitingTokenResult(
                    UUID.fromString("11111111-1111-1111-1111-111111111111"),
                    UUID.fromString("22222222-2222-2222-2222-222222222222"),
                    true
            );
        }
    }
}
