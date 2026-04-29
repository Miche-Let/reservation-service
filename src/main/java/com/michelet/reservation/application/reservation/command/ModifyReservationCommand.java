package com.michelet.reservation.application.reservation.command;

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
}
