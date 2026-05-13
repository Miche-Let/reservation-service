package com.michelet.reservation.infrastructure.client.adapter;

import com.michelet.common.exception.BusinessException;
import com.michelet.reservation.application.exception.ExternalCallFailedException;
import com.michelet.reservation.application.port.TimeSlotPort;
import com.michelet.reservation.domain.exception.ReservationErrorCode;
import com.michelet.reservation.infrastructure.client.TimeSlotClient;
import com.michelet.reservation.infrastructure.client.dto.TimeSlotDeductCapacityRequest;
import com.michelet.reservation.infrastructure.client.dto.TimeSlotRestoreCapacityRequest;
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
        String idempotencyKey = "deduct:" + timeSlotId + ":" + reservationId;
        try {
            timeSlotClient.decrementStock(
                    timeSlotId, idempotencyKey, new TimeSlotDeductCapacityRequest(requiredCapacity));
        } catch (RetryableException e) {
            // 타임아웃 또는 네트워크 단절 — 처리 여부 불확실 → 호출자가 WAITING 처리
            log.warn("[timeslot] 네트워크 오류 — timeSlotId={}, reservationId={}: {}",
                    timeSlotId, reservationId, e.getMessage());
            throw new ExternalCallFailedException("timeslot-service 연결 불안정", e);
        } catch (FeignException e) {
            if (e.status() == 409) {
                // 명시적 비즈니스 거부(잔여 좌석 부족) — 차감 미처리 확정 → 롤백
                log.warn("[timeslot] 슬롯 차감 거부 (409) — timeSlotId={}, reservationId={}", timeSlotId, reservationId);
                throw new BusinessException(ReservationErrorCode.SLOT_NOT_AVAILABLE);
            }
            if (e.status() >= 400 && e.status() < 500) {
                // 인증·인가·요청 오류 등 — 비즈니스 거부로 단정하지 않음 → 호출자가 503 처리
                log.error("[timeslot] 클라이언트 오류 — timeSlotId={}, reservationId={}, status={}",
                        timeSlotId, reservationId, e.status(), e);
                throw new ExternalCallFailedException("timeslot-service 클라이언트 오류", e);
            }
            // 5xx 서버 오류 — 처리 여부 불확실 → 호출자가 WAITING 처리
            log.error("[timeslot] 서버 오류 — timeSlotId={}, reservationId={}, status={}",
                    timeSlotId, reservationId, e.status(), e);
            throw new ExternalCallFailedException("timeslot-service 서버 오류", e);
        }
    }

    @Override
    public void incrementStock(UUID timeSlotId, int requiredCapacity) {
        try {
            timeSlotClient.incrementStock(timeSlotId, new TimeSlotRestoreCapacityRequest(requiredCapacity));
        } catch (RetryableException e) {
            log.warn("[timeslot] 네트워크 오류 (restore) — timeSlotId={}: {}", timeSlotId, e.getMessage());
            throw new ExternalCallFailedException("timeslot-service 연결 불안정", e);
        } catch (FeignException e) {
            log.error("[timeslot] restore 오류 — timeSlotId={}, status={}", timeSlotId, e.status(), e);
            throw new ExternalCallFailedException("timeslot-service restore 오류", e);
        }
    }
}
