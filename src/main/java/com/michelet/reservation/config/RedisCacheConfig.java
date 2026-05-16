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
import org.springframework.cache.annotation.EnableCaching;
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
public class RedisCacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // NON_FINAL: non-final 타입에 @class 포함 → 역직렬화 시 정확한 타입 복원
                .activateDefaultTyping(
                        BasicPolymorphicTypeValidator.builder()
                                .allowIfSubType(Object.class)
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

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    abstract static class TypedMixin {}
}
