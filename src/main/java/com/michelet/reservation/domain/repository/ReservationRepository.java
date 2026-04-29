package com.michelet.reservation.domain.repository;

import com.michelet.reservation.domain.entity.Reservation;
import com.michelet.reservation.domain.enums.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository {

  Reservation save(Reservation reservation);

  Optional<Reservation> findById(UUID id);

  List<Reservation> findAllByUserId(UUID userId);

  List<Reservation> findAllByUserIdAndStatus(UUID userId, ReservationStatus status);

  Page<Reservation> findPageByUserId(UUID userId, Pageable pageable);

  Page<Reservation> findPageByUserIdAndStatus(UUID userId, ReservationStatus status, Pageable pageable);

  Optional<Reservation> findConfirmedByUserIdAndRestaurantId(UUID userId, UUID restaurantId);

  boolean existsByUserIdAndTimeSlotIdAndReservedDateAndStatusNot(UUID userId, UUID timeSlotId, LocalDate reservedDate, ReservationStatus status);

  boolean existsByUserIdAndRestaurantIdAndStatusIn(UUID userId, UUID restaurantId, List<ReservationStatus> statuses);
}
