package com.michelet.reservation.infrastructure.config;

import com.michelet.reservation.infrastructure.reservation.ReservationCourseJpaStore;
import com.michelet.reservation.infrastructure.reservation.ReservationJpaStore;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "warmup.enabled", havingValue = "true")
public class WarmUpRunner implements ApplicationRunner {

    private final ReservationJpaStore reservationJpaStore;
    private final ReservationCourseJpaStore reservationCourseJpaStore;

    @Value("${warmup.iterations:200}")
    private int iterations;

    private static final UUID DUMMY = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final PageRequest PAGE = PageRequest.of(0, 10);

    @Override
    public void run(ApplicationArguments args) {
        log.info("[WarmUp] JVM warm-up 시작 — {}회 × 3 쿼리", iterations);
        for (int i = 0; i < iterations; i++) {
            reservationJpaStore.findAllByUserId(DUMMY, PAGE);
            reservationJpaStore.findById(DUMMY);
            reservationCourseJpaStore.findAllByReservationId(DUMMY);
        }
        log.info("[WarmUp] JVM warm-up 완료 — {}회", iterations);
    }
}
