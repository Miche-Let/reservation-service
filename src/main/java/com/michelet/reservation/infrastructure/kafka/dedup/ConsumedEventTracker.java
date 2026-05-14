package com.michelet.reservation.infrastructure.kafka.dedup;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConsumedEventTracker {

    private static final Duration TTL = Duration.ofHours(48);
    private final StringRedisTemplate redisTemplate;

    public boolean tryMarkAsNew(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            log.warn("[dedup] eventId가 null 또는 빈 값입니다 — dedup 없이 처리 불가로 판단");
            return false;
        }
        Boolean isNew = redisTemplate.opsForValue()
                .setIfAbsent("consumed-event:" + eventId, "1", TTL);
        return Boolean.TRUE.equals(isNew);
    }
}
