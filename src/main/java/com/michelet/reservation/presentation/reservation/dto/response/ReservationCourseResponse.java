package com.michelet.reservation.presentation.reservation.dto.response;

import com.michelet.reservation.application.reservation.result.ReservationCourseResult;

import java.util.UUID;

public record ReservationCourseResponse(
    UUID courseId,
    int quantity,
    int unitPrice
) {
  public static ReservationCourseResponse from(ReservationCourseResult result) {
    return new ReservationCourseResponse(
        result.courseId(),
        result.quantity(),
        result.unitPrice()
    );
  }
}
