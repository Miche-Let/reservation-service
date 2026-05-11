package com.michelet.reservation.domain.state;

import com.michelet.reservation.domain.enums.ReservationStatus;

public class ReservationStateFactory {

    private ReservationStateFactory() {}

    // 신규 enum 값 추가 시 컴파일 에러로 누락을 방지하기 위해 exhaustive switch 사용
    public static ReservationState from(ReservationStatus status) {
        return switch (status) {
            case WAITING          -> new WaitingState();
            case CONFIRMED        -> new ConfirmedState();
            case CANCELLED_UNPAID -> new CancelledUnpaidState();
            case CANCELLED_PAID   -> new CancelledPaidState();
            case COMPLETED        -> new CompletedState();
            case NO_SHOW          -> new NoShowState();
        };
    }
}
