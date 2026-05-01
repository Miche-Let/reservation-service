package com.michelet.reservation.infrastructure.reservation;

import com.michelet.reservation.domain.entity.Reservation;
import com.michelet.reservation.domain.enums.ReservationStatus;
import com.michelet.reservation.domain.repository.ReservationRepository;
import com.michelet.reservation.infrastructure.reservation.entity.ReservationJpaEntity;
import com.michelet.reservation.infrastructure.reservation.mapper.ReservationMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ReservationJpaRepository implements ReservationRepository {

    private final ReservationJpaStore jpaStore;

    @Override
    public Reservation save(Reservation reservation) {
        ReservationJpaEntity entity = ReservationMapper.toJpaEntity(reservation);
        ReservationJpaEntity saved = jpaStore.save(entity);
        return ReservationMapper.toDomain(saved);
    }

    @Override
    public Optional<Reservation> findById(UUID id) {
        return jpaStore.findById(id)
                .map(ReservationMapper::toDomain);
    }

    @Override
    public Page<Reservation> findPageByUserId(UUID userId, Pageable pageable) {
        return jpaStore.findAllByUserId(userId, pageable)
                .map(ReservationMapper::toDomain);
    }

    @Override
    public Page<Reservation> findPageByUserIdAndStatus(UUID userId, ReservationStatus status, Pageable pageable) {
        return jpaStore.findAllByUserIdAndStatus(userId, status, pageable)
                .map(ReservationMapper::toDomain);
    }

    @Override
    public boolean existsByUserIdAndTimeSlotIdAndReservedDateAndStatusNot(
            UUID userId, UUID timeSlotId, LocalDate reservedDate, ReservationStatus status
    ) {
        return jpaStore.existsByUserIdAndTimeSlotIdAndReservedDateAndStatusNot(userId, timeSlotId, reservedDate,
                status);
    }

    @Override
    public boolean existsByUserIdAndRestaurantIdAndStatusIn(
            UUID userId, UUID restaurantId, List<ReservationStatus> statuses
    ) {
        return jpaStore.existsByUserIdAndRestaurantIdAndStatusIn(userId, restaurantId, statuses);
    }

    @Override
    public boolean existsByUserIdAndStatus(UUID userId, ReservationStatus status) {
        return jpaStore.existsByUserIdAndStatus(userId, status);
    }

    @Override
    public void delete(UUID id, UUID deletedBy) {
        jpaStore.findById(id).ifPresent(entity -> {
            entity.softDelete(deletedBy);
            jpaStore.save(entity);
        });
    }
}