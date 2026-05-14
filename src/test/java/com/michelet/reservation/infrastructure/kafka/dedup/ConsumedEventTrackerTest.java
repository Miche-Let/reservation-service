package com.michelet.reservation.infrastructure.kafka.dedup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class ConsumedEventTrackerTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;
    @InjectMocks ConsumedEventTracker tracker;

    @Test
    void null_eventId는_false를_반환하고_Redis를_호출하지_않는다() {
        assertThat(tracker.tryMarkAsNew(null)).isFalse();
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void 빈_eventId는_false를_반환하고_Redis를_호출하지_않는다() {
        assertThat(tracker.tryMarkAsNew("  ")).isFalse();
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void 새로운_eventId는_Redis에_등록하고_true를_반환한다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        assertThat(tracker.tryMarkAsNew("event-abc")).isTrue();
        verify(valueOps).setIfAbsent(
                eq("consumed-event:event-abc"), eq("1"), eq(Duration.ofHours(48)));
    }

    @Test
    void 중복_eventId는_false를_반환한다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        assertThat(tracker.tryMarkAsNew("event-abc")).isFalse();
    }

    @Test
    void Redis_장애로_null_반환_시_false를_반환한다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(null);

        assertThat(tracker.tryMarkAsNew("event-abc")).isFalse();
    }
}
