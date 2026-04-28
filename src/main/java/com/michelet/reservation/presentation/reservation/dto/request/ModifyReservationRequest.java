package com.michelet.reservation.presentation.reservation.dto.request;

import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ModifyReservationRequest(

    LocalDate reservedDate,

    Integer guestCount,

    @Valid
    List<CourseItem> courses
) {
  public record CourseItem(UUID courseId, int quantity) {}
}