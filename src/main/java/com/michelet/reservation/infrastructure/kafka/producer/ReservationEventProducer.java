package com.michelet.reservation.infrastructure.kafka.producer;

import com.michelet.reservation.infrastructure.kafka.event.publish.ReservationCancelledEvent;
import com.michelet.reservation.infrastructure.kafka.event.publish.ReservationCreatedEvent;
import org.springframework.stereotype.Component;

/**
 * MVP: no-op 스텁. spring-kafka 의존성 추가 + application.yaml kafka 설정 활성화 후 구현.
 * 토픽: reservation.created, reservation.cancelled
 */
@Component
public class ReservationEventProducer {

  public void publishReservationCreated(ReservationCreatedEvent event) {
    // TODO: kafkaTemplate.send("reservation.created", event.reservationId().toString(), event);
  }

  public void publishReservationCancelled(ReservationCancelledEvent event) {
    // TODO: kafkaTemplate.send("reservation.cancelled", event.reservationId().toString(), event);
  }
}
