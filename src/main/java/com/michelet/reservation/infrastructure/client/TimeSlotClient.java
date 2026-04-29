package com.michelet.reservation.infrastructure.client;

import com.michelet.common.response.ApiResponse;
import com.michelet.reservation.infrastructure.client.dto.TimeSlotResponse;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "timeslot-service", url = "${feign.timeslot-service.url}")
public interface TimeSlotClient {

    /**
     * 타임슬롯 단건 조회 — noshowDeadline 계산에 필요한 startTime을 가져온다.
     */
    @GetMapping("/internal/v1/time-slots/{timeSlotId}")
    ApiResponse<TimeSlotResponse> getTimeSlot(
            @PathVariable("timeSlotId") UUID timeSlotId
    );

    /**
     * 예약 확정 시 남은 수용 인원 차감 (-1). 호출 시점: create() 저장 완료 후, modify() 날짜 변경 시 신규 날짜
     */
    @PatchMapping("/internal/v1/time-slots/{timeSlotId}/inventory/deduct")
    ApiResponse<Void> decrementStock(
            @PathVariable("timeSlotId") UUID timeSlotId,
            @RequestParam("date") LocalDate date
    );

    /**
     * 예약 취소 / 날짜 변경 시 기존 날짜 슬롯 수용 인원 복구 (+1). 호출 시점: cancel() 완료 후, modify() 날짜 변경 시 원래 날짜
     */
    @PatchMapping("/internal/v1/time-slots/{timeSlotId}/inventory/restore")
    ApiResponse<Void> incrementStock(
            @PathVariable("timeSlotId") UUID timeSlotId,
            @RequestParam("date") LocalDate date
    );
}