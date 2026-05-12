package com.michelet.reservation.presentation;

import com.michelet.common.response.ApiResponse;
import com.michelet.reservation.application.exception.ExternalCallFailedException;
import com.michelet.reservation.domain.exception.ReservationErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@Order(1)
public class ReservationExceptionHandler {

    @ExceptionHandler(OptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleOptimisticLock(OptimisticLockingFailureException e) {
        log.warn("[concurrency] 낙관적 락 충돌 발생 — 동시 상태 전이 경쟁: {}", e.getMessage());
        return ApiResponse.fail(
                ReservationErrorCode.CONCURRENT_UPDATE_CONFLICT.getCode(),
                ReservationErrorCode.CONCURRENT_UPDATE_CONFLICT.getMessage()
        );
    }

    /**
     * modify() 등에서 timeslot-service 연결 실패 시 → 503
     * create()는 서비스 내에서 catch하여 WAITING 반환하므로 이 핸들러에 도달하지 않는다.
     */
    @ExceptionHandler(ExternalCallFailedException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiResponse<Void> handleExternalCallFailed(ExternalCallFailedException e) {
        log.error("[external] 외부 서비스 연결 실패 — 일시적 장애: {}", e.getMessage());
        return ApiResponse.fail(
                ReservationErrorCode.TIMESLOT_SERVICE_UNAVAILABLE.getCode(),
                ReservationErrorCode.TIMESLOT_SERVICE_UNAVAILABLE.getMessage()
        );
    }
}
