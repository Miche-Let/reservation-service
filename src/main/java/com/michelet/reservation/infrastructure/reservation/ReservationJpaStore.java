package com.michelet.reservation.infrastructure.reservation;

import com.michelet.reservation.domain.enums.ReservationStatus;
import com.michelet.reservation.infrastructure.reservation.entity.ReservationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReservationJpaStore extends JpaRepository<ReservationJpaEntity, UUID> {

  List<ReservationJpaEntity> findAllByUserId(UUID userId);

  List<ReservationJpaEntity> findAllByUserIdAndStatus(UUID userId, ReservationStatus status);

  boolean existsByUserIdAndTimeSlotIdAndStatusNot(UUID userId, UUID timeSlotId, ReservationStatus status);
}