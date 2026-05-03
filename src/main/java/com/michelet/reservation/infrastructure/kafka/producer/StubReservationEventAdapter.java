package com.michelet.reservation.infrastructure.kafka.producer;

import com.michelet.reservation.application.port.ReservationEventPort;
import java.time.LocalDate;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

// local 프로파일 전용 — Kafka 없이 테스트 가능하도록 이벤트 발행을 no-op으로 처리합니다.
@Slf4j
@Primary
@Component
@ConditionalOnProperty(name = "kafka.stub", havingValue = "true")
public class StubReservationEventAdapter implements ReservationEventPort {

    @Override
    public void publishReservationCreated(UUID reservationId, UUID userId, UUID restaurantId,
                                          UUID timeSlotId, LocalDate reservedDate, int guestCount) {
        log.info("[STUB] publishReservationCreated skipped — reservationId={}", reservationId);
    }
}
