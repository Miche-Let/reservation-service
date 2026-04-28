package com.michelet.reservation.application.reservation.result;

import com.michelet.reservation.domain.entity.ReservationCourse;

import java.util.UUID;

public record ReservationCourseResult(
    UUID courseId,
    int quantity,
    int unitPrice
) {
  public static ReservationCourseResult from(ReservationCourse course) {
    return new ReservationCourseResult(
        course.getCourseId(),
        course.getQuantity(),
        course.getUnitPrice().value()
    );
  }
}
