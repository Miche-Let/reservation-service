package com.michelet.reservation.application.reservation.command;

import com.michelet.reservation.presentation.reservation.dto.request.CreateReservationRequest;

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

  public static CreateReservationCommand of(UUID userId, CreateReservationRequest request) {
    List<CourseItem> courses = request.courses().stream()
        .map(c -> new CourseItem(c.courseId(), c.quantity()))
        .toList();
    return new CreateReservationCommand(
        userId,
        request.restaurantId(),
        request.timeSlotId(),
        request.reservedDate(),
        request.guestCount(),
        courses
    );
  }
}
