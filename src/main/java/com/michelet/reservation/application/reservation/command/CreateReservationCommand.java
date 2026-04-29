package com.michelet.reservation.application.reservation.command;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateReservationCommand(
    UUID userId,
    UUID restaurantId,
    UUID timeSlotId,
    LocalDate reservedDate,
    int guestCount,
    List<CourseItem> courses
) {
  public record CourseItem(UUID courseId, int quantity) {}
}
