package com.michelet.reservation.application.reservation.command;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record CreateReservationCommand(
    UUID userId,
    String waitingToken,
    UUID restaurantId,
    UUID timeSlotId,
    LocalDate reservedDate,
    LocalTime slotStartTime,
    int guestCount,
    List<CourseItem> courses
) {
  public record CourseItem(UUID courseId, int quantity, int unitPrice) {}
}
