package com.michelet.reservation.infrastructure.reservation;

import com.michelet.reservation.domain.entity.Reservation;
import com.michelet.reservation.domain.enums.ReservationStatus;
import com.michelet.reservation.domain.repository.ReservationRepository;
import com.michelet.reservation.infrastructure.reservation.entity.ReservationJpaEntity;
import com.michelet.reservation.infrastructure.reservation.mapper.ReservationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ReservationJpaRepository implements ReservationRepository {

  private final ReservationJpaStore jpaStore;

  @Override
  public Reservation save(Reservation reservation) {
    ReservationJpaEntity entity = ReservationMapper.toJpaEntity(reservation);
    ReservationJpaEntity saved  = jpaStore.save(entity);
    return ReservationMapper.toDomain(saved);
  }

  @Override
  public Optional<Reservation> findById(UUID id) {
    return jpaStore.findById(id)
        .map(ReservationMapper::toDomain);
  }

  @Override
  public List<Reservation> findAllByUserId(UUID userId) {
    return jpaStore.findAllByUserId(userId)
        .stream()
        .map(ReservationMapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<Reservation> findAllByUserIdAndStatus(UUID userId, ReservationStatus status) {
    return jpaStore.findAllByUserIdAndStatus(userId, status)
        .stream()
        .map(ReservationMapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public boolean existsByUserIdAndTimeSlotIdAndStatusNot(
      UUID userId, UUID timeSlotId, ReservationStatus status
  ) {
    return jpaStore.existsByUserIdAndTimeSlotIdAndStatusNot(userId, timeSlotId, status);
  }
}