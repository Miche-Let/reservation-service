package com.michelet.reservation.presentation.reservation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ModifyReservationRequest(

    LocalDate reservedDate,

    @Min(value = 1, message = "인원수는 1명 이상이어야 합니다.")
    Integer guestCount,

    List<@NotNull @Valid CourseItem> courses // todo: 서비스 코드에서 진행 null = 수정 안 함, [] = 전체 삭제
) {
  public record CourseItem(
      @NotNull UUID courseId,
      @Positive int quantity,
      @Positive int unitPrice
  ) {}
}