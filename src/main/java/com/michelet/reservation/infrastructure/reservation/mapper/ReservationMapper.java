package com.michelet.reservation.infrastructure.reservation.mapper;

import com.michelet.reservation.domain.entity.Reservation;
import com.michelet.reservation.domain.vo.GuestCount;
import com.michelet.reservation.infrastructure.reservation.entity.ReservationJpaEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ReservationMapper {

  public static ReservationJpaEntity toJpaEntity(Reservation domain) {
    ReservationJpaEntity entity = ReservationJpaEntity.of(
        domain.getId(), domain.getUserId(), domain.getRestaurantId(),
        domain.getTimeSlotId(), domain.getReservedDate(),
        domain.getGuestCount().value(), domain.getStatus(),
        domain.getCancelDeadline(), domain.getModifyDeadline(), domain.getNoshowDeadline()
    );
    if (domain.isNew()) {
      entity.markNew();
    }
    return entity;
  }

  public static Reservation toDomain(ReservationJpaEntity entity) {
    return Reservation.reconstitute(
        entity.getId(), entity.getUserId(), entity.getRestaurantId(),
        entity.getTimeSlotId(), entity.getReservedDate(),
        GuestCount.of(entity.getGuestCount()), entity.getStatus(),
        entity.getCancelDeadline(), entity.getModifyDeadline(), entity.getNoshowDeadline()
    );
  }
}