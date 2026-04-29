package com.michelet.reservation.domain.exception;

import com.michelet.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReservationErrorCode implements ErrorCode {

  RESERVATION_NOT_FOUND           ("RESERVATION_001", "예약을 찾을 수 없습니다.", 404),

  ALREADY_CANCELLED               ("RESERVATION_002", "이미 취소된 예약입니다.", 400),
  INVALID_STATUS_TRANSITION       ("RESERVATION_003", "허용되지 않는 상태 전이입니다.", 400),

  CANCEL_DEADLINE_EXCEEDED        ("RESERVATION_004", "취소 가능 기한이 지났습니다.", 400),
  MODIFY_DEADLINE_EXCEEDED        ("RESERVATION_005", "수정 가능 기한이 지났습니다.", 400),

  DUPLICATE_RESERVATION           ("RESERVATION_006", "해당 타임슬롯에 이미 예약이 존재합니다.", 409),

  INVALID_RESERVATION_INSTANCE_ID ("RESERVATION_007", "예약 ID는 필수입니다.", 400),
  INVALID_USER_ID                 ("RESERVATION_008", "사용자 ID는 필수입니다.", 400),
  INVALID_RESTAURANT_ID           ("RESERVATION_009", "레스토랑 ID는 필수입니다.", 400),
  INVALID_TIME_SLOT_ID            ("RESERVATION_010", "타임슬롯 ID는 필수입니다.", 400),
  INVALID_RESERVED_DATE           ("RESERVATION_011", "예약일은 필수입니다.", 400),
  INVALID_GUEST_COUNT_NULL        ("RESERVATION_012", "인원수는 필수입니다.", 400),

  INVALID_GUEST_COUNT             ("RESERVATION_013", "인원수는 1명 이상 20명 이하여야 합니다.", 400),

  INVALID_MONEY_AMOUNT            ("RESERVATION_101", "금액은 0원 이상이어야 합니다.", 400),
  INVALID_MULTIPLY_QUANTITY       ("RESERVATION_102", "수량은 0 이상이어야 합니다.", 400),
  MONEY_OVERFLOW                  ("RESERVATION_103", "금액 계산 결과가 허용 범위를 초과하였습니다.", 400),

  INVALID_COURSE_INSTANCE_ID      ("RESERVATION_201", "코스 항목 ID는 필수입니다.", 400),
  INVALID_RESERVATION_ID          ("RESERVATION_202", "예약 ID는 필수입니다.", 400),
  INVALID_COURSE_ID               ("RESERVATION_203", "코스 ID는 필수입니다.", 400),
  INVALID_UNIT_PRICE              ("RESERVATION_204", "단가 정보는 필수입니다.", 400),

  INVALID_COURSE_QUANTITY         ("RESERVATION_205", "코스 수량은 1개 이상이어야 합니다.", 400),

  INVALID_WAITING_TOKEN           ("RESERVATION_301", "유효하지 않은 대기열 토큰입니다.", 403);

  private final String code;
  private final String message;
  private final int httpStatus;

  @Override public String getCode()     { return code; }
  @Override public String getMessage()  { return message; }
  @Override public int getHttpStatus()  { return httpStatus; }
}