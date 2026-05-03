package com.michelet.reservation.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;
import java.util.UUID;

@Configuration
public class AuditorConfig {

  private static final UUID SYSTEM_AUDITOR_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000000");

  @Bean
  public AuditorAware<UUID> auditorAware() {
    return () -> {
      String userId = MDC.get(MdcKeys.USER_ID);
      if (userId == null || userId.isBlank()) {
        return Optional.of(SYSTEM_AUDITOR_ID);
      }
      try {
        return Optional.of(UUID.fromString(userId));
      } catch (IllegalArgumentException e) {
        return Optional.of(SYSTEM_AUDITOR_ID);
      }
    };
  }
}