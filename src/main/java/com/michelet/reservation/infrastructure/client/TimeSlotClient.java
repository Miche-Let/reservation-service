package com.michelet.reservation.infrastructure.client;

import com.michelet.common.response.ApiResponse;
import com.michelet.reservation.infrastructure.client.dto.TimeSlotDeductCapacityRequest;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "timeslot-service"
//        , configuration = InternalSecretFeignConfig.class
        // todo : mvp 이후 InternalSecret 적용여부 판단
)
public interface TimeSlotClient {

    /**
     * 예약 확정 시 남은 수용 인원 차감. 호출 시점: create() 저장 완료 후, modify() 날짜 변경 시 신규 날짜
     */
    @PostMapping("/internal/v1/timeslots/{timeSlotId}/deduct")
    ApiResponse<Void> decrementStock(
            @PathVariable("timeSlotId") UUID timeSlotId,
            @RequestBody TimeSlotDeductCapacityRequest request
    );

    // 현재 버전에서 미사용 (담당자 확인)
    // /**
    //  * 예약 취소 / 날짜 변경 시 기존 날짜 슬롯 수용 인원 복구 (+1). 호출 시점: cancel() 완료 후, modify() 날짜 변경 시 원래 날짜
    //  */
    // @PatchMapping("/internal/v1/time-slots/{timeSlotId}/restore")
    // ApiResponse<Void> incrementStock(
    //         @PathVariable("timeSlotId") UUID timeSlotId,
    //         @RequestParam("date") LocalDate date
    // );
}