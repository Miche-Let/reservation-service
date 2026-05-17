package com.michelet.reservation.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.michelet.reservation.application.reservation.result.ReservationCourseResult;
import com.michelet.reservation.application.reservation.result.ReservationResult;
import com.michelet.reservation.application.reservation.result.ReservationSummaryResult;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class RedisCacheConfig implements CachingConfigurer {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // NON_FINAL: non-final 타입에 @class 포함 → 역직렬화 시 정확한 타입 복원.
                // allowlist를 com.michelet.reservation + java.util.List(및 구현체)로 한정해
                // Jackson gadget 공격 위험을 줄인다.
                // record(final) 타입은 mix-in @JsonTypeInfo로 별도 처리하므로 validator 범위 불필요.
                .activateDefaultTyping(
                        BasicPolymorphicTypeValidator.builder()
                                .allowIfSubType("com.michelet.reservation.")
                                .allowIfSubType(java.util.List.class)
                                .build(),
                        ObjectMapper.DefaultTyping.NON_FINAL,
                        JsonTypeInfo.As.PROPERTY
                );

        // Java record는 암묵적으로 final → NON_FINAL typing이 @class를 삽입하지 않는다.
        // Mix-in으로 @JsonTypeInfo를 명시 적용해 캐시 저장 시 class 정보를 포함시킨다.
        objectMapper.addMixIn(ReservationResult.class, TypedMixin.class);
        objectMapper.addMixIn(ReservationCourseResult.class, TypedMixin.class);
        objectMapper.addMixIn(ReservationSummaryResult.class, TypedMixin.class);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(60))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer(objectMapper)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                // list: 30초 — 생성·취소가 반영되는 신선도 요구가 높음
                .withCacheConfiguration("reservation:list",
                        defaultConfig.entryTtl(Duration.ofSeconds(30)))
                // detail: 60초 — 단건 조회는 변경 빈도가 낮음
                .withCacheConfiguration("reservation:detail",
                        defaultConfig.entryTtl(Duration.ofSeconds(60)))
                .build();
    }

    // Redis 오류(직렬화 실패, 연결 끊김 등)를 WARN 로그로 남기고 예외를 삼킴.
    // 캐시 오류가 500으로 전파되지 않고 실제 메서드(DB 조회)로 fallback된다.
    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }

    private static class LoggingCacheErrorHandler implements CacheErrorHandler {
        private static final Logger log = LoggerFactory.getLogger(LoggingCacheErrorHandler.class);

        @Override
        public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
            log.warn("Cache get error [{}] key={}", cache.getName(), key, e);
        }

        @Override
        public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
            log.warn("Cache put error [{}] key={}", cache.getName(), key, e);
        }

        @Override
        public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
            log.warn("Cache evict error [{}] key={}", cache.getName(), key, e);
        }

        @Override
        public void handleCacheClearError(RuntimeException e, Cache cache) {
            log.warn("Cache clear error [{}]", cache.getName(), e);
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    abstract static class TypedMixin {}
}
