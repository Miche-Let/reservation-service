package com.michelet.reservation.application.exception;

/**
 * 외부 서비스 호출 결과를 확인할 수 없는 상황 — 타임아웃, 네트워크 단절, 5xx 서버 오류.
 * 이 예외가 발생하면 외부 서비스의 처리 여부를 알 수 없으므로 예약은 WAITING으로 유지되며 재처리 대상이 된다.
 */
public class ExternalCallFailedException extends RuntimeException {

    public ExternalCallFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
