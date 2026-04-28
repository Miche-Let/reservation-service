package com.michelet.reservation.infrastructure.reservation;

import com.michelet.reservation.domain.enums.ReservationStatus;
import com.michelet.reservation.infrastructure.reservation.entity.ReservationJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationJpaStore extends JpaRepository<ReservationJpaEntity, UUID> {

  List<ReservationJpaEntity> findAllByUserId(UUID userId);

  List<ReservationJpaEntity> findAllByUserIdAndStatus(UUID userId, ReservationStatus status);

  Page<ReservationJpaEntity> findAllByUserId(UUID userId, Pageable pageable);

  Page<ReservationJpaEntity> findAllByUserIdAndStatus(UUID userId, ReservationStatus status, Pageable pageable);

  Optional<ReservationJpaEntity> findFirstByUserIdAndRestaurantIdAndStatus(
      UUID userId, UUID restaurantId, ReservationStatus status);

  boolean existsByUserIdAndTimeSlotIdAndStatusNot(UUID userId, UUID timeSlotId, ReservationStatus status);
}