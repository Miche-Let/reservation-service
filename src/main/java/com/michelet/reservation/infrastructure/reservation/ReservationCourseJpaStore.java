package com.michelet.reservation.infrastructure.reservation;

import com.michelet.reservation.infrastructure.reservation.entity.ReservationCourseJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReservationCourseJpaStore extends JpaRepository<ReservationCourseJpaEntity, UUID> {

  List<ReservationCourseJpaEntity> findAllByReservationId(UUID reservationId);

  void deleteAllByReservationId(UUID reservationId);
}