package com.michelet.reservation.infrastructure.reservation;

import com.michelet.reservation.domain.enums.ReservationStatus;
import com.michelet.reservation.infrastructure.reservation.entity.ReservationJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationJpaStore extends JpaRepository<ReservationJpaEntity, UUID> {

  List<ReservationJpaEntity> findAllByUserId(UUID userId);

  List<ReservationJpaEntity> findAllByUserIdAndStatus(UUID userId, ReservationStatus status);

  Page<ReservationJpaEntity> findAllByUserId(UUID userId, Pageable pageable);

  Page<ReservationJpaEntity> findAllByUserIdAndStatus(UUID userId, ReservationStatus status, Pageable pageable);

  // 존재 여부 확인 용도 — 여러 건 중 어느 것이 반환되어도 무방하므로 ORDER BY 생략 (LIMIT 1 성능 우선)
  Optional<ReservationJpaEntity> findFirstByUserIdAndRestaurantIdAndStatus(
      UUID userId, UUID restaurantId, ReservationStatus status);

  boolean existsByUserIdAndTimeSlotIdAndReservedDateAndStatusNot(UUID userId, UUID timeSlotId, LocalDate reservedDate, ReservationStatus status);

  boolean existsByUserIdAndRestaurantIdAndStatusIn(UUID userId, UUID restaurantId, List<ReservationStatus> statuses);
}