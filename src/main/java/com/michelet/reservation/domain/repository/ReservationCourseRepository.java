package com.michelet.reservation.domain.repository;

import com.michelet.reservation.domain.entity.ReservationCourse;

import java.util.List;
import java.util.UUID;

public interface ReservationCourseRepository {

  ReservationCourse save(ReservationCourse reservationCourse);

  List<ReservationCourse> findAllByReservationId(UUID reservationId);

  void deleteAllByReservationId(UUID reservationId);
}