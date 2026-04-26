package com.michelet.reservation.infrastructure.reservation.mapper;

import com.michelet.reservation.domain.entity.ReservationCourse;
import com.michelet.reservation.domain.vo.Money;
import com.michelet.reservation.infrastructure.reservation.entity.ReservationCourseJpaEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 수동 매핑 선택 이유
 *   - Money VO 의 of() 팩토리 사용
 *   - DB 컬럼 오타(cource_id, quntitty)를 매퍼에서 명시적으로 처리
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ReservationCourseMapper {

  public static ReservationCourseJpaEntity toJpaEntity(ReservationCourse domain) {
    return ReservationCourseJpaEntity.of(
        domain.getId(), domain.getReservationId(), domain.getCourseId(),
        domain.getQuantity(), domain.getUnitPrice().value()
    );
  }

  public static ReservationCourse toDomain(ReservationCourseJpaEntity entity) {
    return ReservationCourse.reconstitute(
        entity.getId(), entity.getReservationId(), entity.getCourseId(),
        entity.getQuantity(), Money.of(entity.getUnitPrice())
    );
  }
}