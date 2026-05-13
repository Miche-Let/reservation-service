package com.michelet.reservation.application.reservation.command;

import java.util.UUID;

public record CheckInCommand(
    UUID reservationId,
    UUID restaurantId,
    UUID checkedInBy
) {}
