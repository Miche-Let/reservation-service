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
        if (reservation.isNew()) {
            ReservationJpaEntity newEntity = ReservationMapper.toJpaEntity(reservation);
            return ReservationMapper.toDomain(jpaStore.save(newEntity));
        }
        // version 보존을 위해 managed entity를 가져온 뒤 값을 덮어씀
        ReservationJpaEntity entity = jpaStore.findById(reservation.getId())
                .orElseThrow(() -> new com.michelet.common.exception.BusinessException(
                        com.michelet.reservation.domain.exception.ReservationErrorCode.RESERVATION_NOT_FOUND));
        entity.applyFrom(reservation);
        return ReservationMapper.toDomain(jpaStore.save(entity));
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
    public boolean existsByUserIdAndTimeSlotIdAndReservedDateAndStatus(
            UUID userId, UUID timeSlotId, LocalDate reservedDate, ReservationStatus status
    ) {
        return jpaStore.existsByUserIdAndTimeSlotIdAndReservedDateAndStatus(userId, timeSlotId, reservedDate,
                status);
    }

    @Override
    public Optional<Reservation> findTopByUserIdAndRestaurantIdAndStatusInOrderByReservedDateDesc(
            UUID userId, UUID restaurantId, List<ReservationStatus> statuses
    ) {
        return jpaStore.findTopByUserIdAndRestaurantIdAndStatusInOrderByReservedDateDesc(userId, restaurantId, statuses)
                .map(ReservationMapper::toDomain);
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