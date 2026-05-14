package com.michelet.reservation.infrastructure.idempotency;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdempotencyService {

    private static final Duration TTL = Duration.ofHours(24);
    private final StringRedisTemplate redisTemplate;

    public Optional<String> findCachedResponse(String key) {
        return Optional.ofNullable(redisTemplate.opsForValue().get("idem:" + key));
    }

    public void cacheResponse(String key, String json) {
        redisTemplate.opsForValue().set("idem:" + key, json, TTL);
    }
}
