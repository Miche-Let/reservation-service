package com.michelet.reservation.infrastructure.client.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

public class InternalSecretFeignConfig {

    @Value("${internal.secret:}")
    private String internalSecret;

    @Bean
    public RequestInterceptor internalSecretInterceptor() {
        if (!StringUtils.hasText(internalSecret)) {
            throw new IllegalStateException(
                    "Missing required configuration: 'internal.secret'. Set via application.yaml or environment variable INTERNAL_SECRET."
            );
        }
        return template -> template.header("X-Internal-Secret", internalSecret);
    }
}
