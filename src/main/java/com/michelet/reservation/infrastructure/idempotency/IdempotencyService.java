package com.michelet.reservation.infrastructure.idempotency;

import java.time.Duration;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdempotencyService {

    private static final Duration TTL = Duration.ofHours(24);
    private static final Pattern SAFE_KEY = Pattern.compile("^[a-zA-Z0-9\\-_]{1,128}$");
    private final StringRedisTemplate redisTemplate;

    public Optional<String> findCachedResponse(String key) {
        if (!SAFE_KEY.matcher(key).matches()) {
            return Optional.empty();
        }
        return Optional.ofNullable(redisTemplate.opsForValue().get("idem:" + key));
    }

    public void cacheResponse(String key, String json) {
        if (!SAFE_KEY.matcher(key).matches()) {
            return;
        }
        redisTemplate.opsForValue().set("idem:" + key, json, TTL);
    }
}
