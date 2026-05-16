package com.michelet.reservation.application.reservation.result;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

// non-final class — NON_FINAL typing이 @class를 삽입할 수 있도록 record 대신 class 사용.
// Redis 저장 시 @class 키에 이 클래스의 FQCN이 포함된다.
// 이름 변경 시 기존 캐시 엔트리 역직렬화가 실패하므로, 이름을 안정적으로 유지할 것.
public class ReservationListCachePage {

    public final List<ReservationSummaryResult> content;
    public final long totalElements;

    @JsonCreator
    public ReservationListCachePage(
            @JsonProperty("content") List<ReservationSummaryResult> content,
            @JsonProperty("totalElements") long totalElements) {
        this.content = content;
        this.totalElements = totalElements;
    }
}
