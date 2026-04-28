package com.michelet.reservation.presentation.reservation.dto.response;

import com.michelet.reservation.application.reservation.result.ReservationResult;
import com.michelet.reservation.domain.enums.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReservationResponse(
    UUID reservationId,
    UUID userId,
    UUID restaurantId,
    UUID timeSlotId,
    LocalDate reservedDate,
    int guestCount,
    ReservationStatus status,
    LocalDate cancelDeadline,
    LocalDate modifyDeadline,
    LocalDateTime noshowDeadline,
    List<ReservationCourseResponse> courses
) {
  public static ReservationResponse from(ReservationResult result) {
    List<ReservationCourseResponse> courses = result.courses().stream()
        .map(ReservationCourseResponse::from)
        .toList();
    return new ReservationResponse(
        result.reservationId(),
        result.userId(),
        result.restaurantId(),
        result.timeSlotId(),
        result.reservedDate(),
        result.guestCount(),
        result.status(),
        result.cancelDeadline(),
        result.modifyDeadline(),
        result.noshowDeadline(),
        courses
    );
  }
}
