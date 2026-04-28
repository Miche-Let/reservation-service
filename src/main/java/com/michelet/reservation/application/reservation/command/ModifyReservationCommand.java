package com.michelet.reservation.application.reservation.command;

import com.michelet.reservation.presentation.reservation.dto.request.ModifyReservationRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ModifyReservationCommand(
    UUID reservationId,
    UUID userId,
    String userRole,
    LocalDate reservedDate,   // null = 변경 없음
    Integer guestCount,       // null = 변경 없음
    List<CourseItem> courses  // null = 변경 없음 / [] = 전체 삭제
) {
  public record CourseItem(UUID courseId, int quantity) {}

  public static ModifyReservationCommand of(
      UUID reservationId, UUID userId, String userRole, ModifyReservationRequest request
  ) {
    List<CourseItem> courses = request.courses() == null ? null :
        request.courses().stream()
            .map(c -> new CourseItem(c.courseId(), c.quantity()))
            .toList();
    return new ModifyReservationCommand(
        reservationId,
        userId,
        userRole,
        request.reservedDate(),
        request.guestCount(),
        courses
    );
  }
}
