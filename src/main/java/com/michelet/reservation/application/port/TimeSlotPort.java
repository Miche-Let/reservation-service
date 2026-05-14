package com.michelet.reservation.application.port;

import java.util.UUID;

public interface TimeSlotPort {

    /**
     * @param reservationId 멱등성 키 — 네트워크 재전송 시 이중 차감 방지용 (X-Idempotency-Key 헤더로 전달)
     */
    void decrementStock(UUID timeSlotId, int requiredCapacity, UUID reservationId);

    /**
     * @param idempotencyKey 멱등성 키 — 네트워크 재전송 시 이중 복구 방지용 (X-Idempotency-Key 헤더로 전달)
     *                       패턴: "restore:{timeSlotId}:{reservationId}:{operation}"
     */
    void incrementStock(UUID timeSlotId, int requiredCapacity, String idempotencyKey);
}
