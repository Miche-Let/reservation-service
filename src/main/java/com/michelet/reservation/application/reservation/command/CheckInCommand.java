package com.michelet.reservation.application.reservation.command;

import com.michelet.reservation.presentation.reservation.dto.request.CheckInRequest;

import java.util.UUID;

public record CheckInCommand(
    UUID reservationId,
    UUID restaurantId
) {
  public static CheckInCommand from(CheckInRequest request) {
    return new CheckInCommand(request.reservationId(), request.restaurantId());
  }
}
