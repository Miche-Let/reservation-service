package com.michelet.reservation.presentation.reservation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateReservationRequest(

    @NotNull(message = "레스토랑 ID는 필수입니다.")
    UUID restaurantId,

    @NotNull(message = "타임슬롯 ID는 필수입니다.")
    UUID timeSlotId,

    @NotNull(message = "예약일은 필수입니다.")
    LocalDate reservedDate,

    @Positive(message = "인원수는 1명 이상이어야 합니다.")
    int guestCount,

    @NotEmpty(message = "코스를 1개 이상 선택해야 합니다.")
    @Valid
    List<CourseItem> courses
) {
  public record CourseItem(
      @NotNull UUID courseId,
      @Positive int quantity
  ) {}
}