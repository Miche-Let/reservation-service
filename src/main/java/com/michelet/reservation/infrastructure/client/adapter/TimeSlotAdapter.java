package com.michelet.reservation.infrastructure.client.adapter;

import com.michelet.common.exception.BusinessException;
import com.michelet.reservation.application.exception.ExternalCallFailedException;
import com.michelet.reservation.application.port.TimeSlotPort;
import com.michelet.reservation.domain.exception.ReservationErrorCode;
import com.michelet.reservation.infrastructure.client.TimeSlotClient;
import com.michelet.reservation.infrastructure.client.dto.TimeSlotDeductCapacityRequest;
import feign.FeignException;
import feign.RetryableException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimeSlotAdapter implements TimeSlotPort {

    private final TimeSlotClient timeSlotClient;

    @Override
    public void decrementStock(UUID timeSlotId, int requiredCapacity, UUID reservationId) {
        String idempotencyKey = "reservation:" + reservationId;
        try {
            timeSlotClient.decrementStock(
                    timeSlotId, idempotencyKey, new TimeSlotDeductCapacityRequest(requiredCapacity));
        } catch (RetryableException e) {
            // 타임아웃 또는 네트워크 단절 — 처리 여부 불확실 → 호출자가 WAITING 처리
            log.warn("[timeslot] 네트워크 오류 — timeSlotId={}, reservationId={}: {}",
                    timeSlotId, reservationId, e.getMessage());
            throw new ExternalCallFailedException("timeslot-service 연결 불안정", e);
        } catch (FeignException e) {
            if (e.status() >= 400 && e.status() < 500) {
                // 비즈니스 거부 (슬롯 부족, 슬롯 없음 등) — 차감 미처리 확정 → 롤백
                log.warn("[timeslot] 슬롯 차감 거부 (4xx) — timeSlotId={}, status={}", timeSlotId, e.status());
                throw new BusinessException(ReservationErrorCode.SLOT_NOT_AVAILABLE);
            }
            // 5xx 서버 오류 — 처리 여부 불확실 → 호출자가 WAITING 처리
            log.error("[timeslot] 서버 오류 — timeSlotId={}, reservationId={}, status={}",
                    timeSlotId, reservationId, e.status(), e);
            throw new ExternalCallFailedException("timeslot-service 서버 오류", e);
        }
    }

    @Override
    public void incrementStock(UUID timeSlotId, int requiredCapacity) {
        // TODO: timeslot-service restore API 미구현 — 구현 시 차감과 동일하게 POST /deduct 역방향 or 별도 엔드포인트 호출
        log.warn("incrementStock skipped — timeslot-service restore API not yet implemented. timeSlotId={}, requiredCapacity={}",
                timeSlotId, requiredCapacity);
    }
}
