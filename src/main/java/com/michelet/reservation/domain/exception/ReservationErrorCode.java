package com.michelet.reservation.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReservationErrorCode {

  RESERVATION_NOT_FOUND("RESERVATION_001", "예약을 찾을 수 없습니다.", 404),
  ALREADY_CANCELLED("RESERVATION_002", "이미 취소된 예약입니다.", 400),
  INVALID_STATUS_TRANSITION("RESERVATION_003", "허용되지 않는 상태 전이입니다.", 400),
  CANCEL_DEADLINE_EXCEEDED("RESERVATION_004", "취소 가능 기한이 지났습니다.", 400),
  MODIFY_DEADLINE_EXCEEDED("RESERVATION_005", "수정 가능 기한이 지났습니다.", 400),
  DUPLICATE_RESERVATION("RESERVATION_006", "해당 타임슬롯에 이미 예약이 존재합니다.", 409),
  INVALID_GUEST_COUNT("RESERVATION_007", "인원수는 1명 이상 20명 이하여야 합니다.", 400);

  private final String code;
  private final String message;
  private final int httpStatus;
}