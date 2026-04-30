package com.michelet.reservation.domain.exception;

import com.michelet.common.response.SuccessCode;

public enum ReservationSuccessCode implements SuccessCode {

    RESERVATION_CREATED("RESERVATION_SUCCESS_001", "예약이 생성되었습니다."),
    RESERVATION_FETCHED("RESERVATION_SUCCESS_002", "예약 조회가 완료되었습니다."),
    RESERVATION_MODIFIED("RESERVATION_SUCCESS_003", "예약이 수정되었습니다."),
    RESERVATION_VALIDITY_CHECKED("RESERVATION_SUCCESS_004", "예약 유효성 확인이 완료되었습니다."),
    RESERVATION_CHECKED_IN("RESERVATION_SUCCESS_005", "체크인이 완료되었습니다."),
    RESERVATION_EXISTS_CHECKED("RESERVATION_SUCCESS_006", "예약 존재 여부 확인이 완료되었습니다.");

    private final String code;
    private final String message;

    ReservationSuccessCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() { return code; }

    @Override
    public String getMessage() { return message; }
}
