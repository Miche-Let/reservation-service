package com.michelet.reservation.infrastructure.kafka.handler;

import com.michelet.reservation.application.event.ReservationCreatedAppEvent;
import com.michelet.reservation.application.port.ReservationEventPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ReservationEventHandler {

    private final ReservationEventPort reservationEventPort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReservationCreated(ReservationCreatedAppEvent event) {
        reservationEventPort.publishReservationCreated(
                event.reservationId(),
                event.userId(),
                event.restaurantId(),
                event.timeSlotId(),
                event.reservedDate(),
                event.guestCount()
        );
    }
}
