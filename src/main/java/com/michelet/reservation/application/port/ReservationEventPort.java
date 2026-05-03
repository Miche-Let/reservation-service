package com.michelet.reservation.application.port;

import java.time.LocalDate;
import java.util.UUID;

public interface ReservationEventPort {
    void publishReservationCreated(UUID reservationId, UUID userId, UUID restaurantId,
                                   UUID timeSlotId, LocalDate reservedDate, int guestCount);
}
