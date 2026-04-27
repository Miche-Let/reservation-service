package com.michelet.reservation.domain.repository;

import com.michelet.reservation.domain.entity.Reservation;
import com.michelet.reservation.domain.enums.ReservationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository {

  Reservation save(Reservation reservation);

  Optional<Reservation> findById(UUID id);

  List<Reservation> findAllByUserId(UUID userId);

  List<Reservation> findAllByUserIdAndStatus(UUID userId, ReservationStatus status);

  boolean existsByUserIdAndTimeSlotIdAndStatusNot(UUID userId, UUID timeSlotId, ReservationStatus status);
}