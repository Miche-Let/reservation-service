package com.michelet.reservation.application.reservation.command;

import java.util.UUID;

public record DeleteReservationCommand(
        UUID reservationId,
        UUID userId,
        String userRole
) {}
