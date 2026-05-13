package com.michelet.reservation.application.port;

import com.michelet.reservation.domain.enums.ReservationStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public interface OutboxEventPort {

    void recordReservationCreated(UUID reservationId, UUID userId, UUID restaurantId,
                                  UUID timeSlotId, LocalDate reservedDate, int guestCount,
                                  LocalDateTime occurredAt);

    void recordReservationCancelled(UUID reservationId, UUID userId, UUID restaurantId,
                                    UUID timeSlotId, LocalDate reservedDate, int guestCount,
                                    ReservationStatus cancelledStatus, LocalDateTime occurredAt);

    void recordWaitingCompleted(UUID waitingId, UUID reservationId, LocalDateTime occurredAt);

    void recordCheckInCompleted(UUID reservationId, UUID restaurantId,
                                LocalDate visitDate, LocalDateTime checkedInAt);
}
