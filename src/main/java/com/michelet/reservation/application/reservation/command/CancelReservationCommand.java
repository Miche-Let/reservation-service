package com.michelet.reservation.application.reservation.command;

import java.util.UUID;

public record CancelReservationCommand(
    UUID reservationId,
    UUID userId,
    String userRole
) {
  public static CancelReservationCommand of(UUID reservationId, UUID userId, String userRole) {
    return new CancelReservationCommand(reservationId, userId, userRole);
  }
}
