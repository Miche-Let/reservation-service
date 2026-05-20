package com.michelet.reservation.domain.state;

import com.michelet.common.exception.BusinessException;
import com.michelet.reservation.domain.enums.ReservationStatus;
import com.michelet.reservation.domain.exception.ReservationErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface ReservationState {

    // 전이 메서드 — 허용하지 않는 전이는 default에서 예외를 던진다
    default ReservationStatus confirm() {
        throw new BusinessException(ReservationErrorCode.INVALID_STATUS_TRANSITION);
    }

    default ReservationStatus cancel(LocalDate cancelDeadline) {
        throw new BusinessException(ReservationErrorCode.INVALID_STATUS_TRANSITION);
    }

    default ReservationStatus cancelUnpaid() {
        throw new BusinessException(ReservationErrorCode.INVALID_STATUS_TRANSITION);
    }

    default ReservationStatus complete() {
        throw new BusinessException(ReservationErrorCode.INVALID_STATUS_TRANSITION);
    }

    default ReservationStatus markNoShow() {
        throw new BusinessException(ReservationErrorCode.INVALID_STATUS_TRANSITION);
    }

    // 조건 판단 메서드 — CONFIRMED 외 상태는 false / 예외
    default boolean isCancellable(LocalDate cancelDeadline) {
        return false;
    }

    default boolean isModifiable(LocalDate modifyDeadline) {
        return false;
    }

    default void assertModifiable(LocalDate modifyDeadline) {
        throw new BusinessException(ReservationErrorCode.INVALID_STATUS_TRANSITION);
    }

    default void assertCompletable(LocalDateTime now, LocalDateTime noshowDeadline) {
        throw new BusinessException(ReservationErrorCode.INVALID_STATUS_TRANSITION);
    }

    // 정책 메서드 — 상태별 구현 필수
    boolean requiresSlotReturn();
    boolean requiresRefund();
    ReservationStatus status();
}
