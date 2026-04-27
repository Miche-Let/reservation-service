package com.michelet.reservation.infrastructure.reservation.mapper;

import com.michelet.reservation.domain.entity.ReservationCourse;
import com.michelet.reservation.domain.vo.Money;
import com.michelet.reservation.infrastructure.reservation.entity.ReservationCourseJpaEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

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