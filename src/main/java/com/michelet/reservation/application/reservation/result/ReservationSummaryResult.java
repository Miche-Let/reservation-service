package com.michelet.reservation.application.reservation.result;

import com.michelet.reservation.domain.entity.Reservation;
import com.michelet.reservation.domain.enums.ReservationStatus;

import java.time.LocalDate;
import java.util.UUID;

public record ReservationSummaryResult(
    UUID reservationId,
    UUID restaurantId,
    LocalDate reservedDate,
    int guestCount,
    ReservationStatus status,
    LocalDate cancelDeadline,
    LocalDate modifyDeadline
) {
  public static ReservationSummaryResult from(Reservation r) {
    return new ReservationSummaryResult(
        r.getId(),
        r.getRestaurantId(),
        r.getReservedDate(),
        r.getGuestCount().value(),
        r.getStatus(),
        r.getCancelDeadline(),
        r.getModifyDeadline()
    );
  }
}
