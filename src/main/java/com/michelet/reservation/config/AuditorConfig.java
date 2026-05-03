package com.michelet.reservation.config;

import com.michelet.common.auth.webmvc.context.UserContextHolder;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

@Configuration
public class AuditorConfig {

  private static final UUID SYSTEM_AUDITOR_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000000");

  @Bean
  public AuditorAware<UUID> auditorAware() {
    return () -> {
      var ctx = UserContextHolder.get();
      if (ctx == null || !ctx.isAuthenticated()) {
        return Optional.of(SYSTEM_AUDITOR_ID);
      }
      try {
        return Optional.of(UUID.fromString(ctx.userId()));
      } catch (IllegalArgumentException e) {
        return Optional.of(SYSTEM_AUDITOR_ID);
      }
    };
  }
}
