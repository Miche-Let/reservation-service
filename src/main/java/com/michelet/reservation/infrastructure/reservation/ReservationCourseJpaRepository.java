package com.michelet.reservation.infrastructure.reservation;

import com.michelet.reservation.domain.entity.ReservationCourse;
import com.michelet.reservation.domain.repository.ReservationCourseRepository;
import com.michelet.reservation.infrastructure.reservation.entity.ReservationCourseJpaEntity;
import com.michelet.reservation.infrastructure.reservation.mapper.ReservationCourseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ReservationCourseJpaRepository implements ReservationCourseRepository {

  private final ReservationCourseJpaStore jpaStore;

  @Override
  public ReservationCourse save(ReservationCourse reservationCourse) {
    ReservationCourseJpaEntity entity = ReservationCourseMapper.toJpaEntity(reservationCourse);
    ReservationCourseJpaEntity saved  = jpaStore.save(entity);
    return ReservationCourseMapper.toDomain(saved);
  }

  @Override
  public List<ReservationCourse> findAllByReservationId(UUID reservationId) {
    return jpaStore.findAllByReservationId(reservationId)
        .stream()
        .map(ReservationCourseMapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public void deleteAllByReservationId(UUID reservationId) {
    jpaStore.deleteAllByReservationId(reservationId);
  }
}