package com.michelet.reservation.infrastructure.reservation;

import com.michelet.reservation.domain.enums.ReservationStatus;
import com.michelet.reservation.infrastructure.reservation.entity.ReservationJpaEntity;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationJpaStore extends JpaRepository<ReservationJpaEntity, UUID> {

    Page<ReservationJpaEntity> findAllByUserId(UUID userId, Pageable pageable);

    Page<ReservationJpaEntity> findAllByUserIdAndStatus(UUID userId, ReservationStatus status, Pageable pageable);


    boolean existsByUserIdAndTimeSlotIdAndReservedDateAndStatus(UUID userId, UUID timeSlotId, LocalDate reservedDate,
                                                               ReservationStatus status);

    boolean existsByUserIdAndTimeSlotIdAndReservedDateAndStatusIn(UUID userId, UUID timeSlotId,
                                                                  LocalDate reservedDate,
                                                                  Collection<ReservationStatus> statuses);

    Optional<ReservationJpaEntity> findTopByUserIdAndRestaurantIdAndStatusInOrderByReservedDateDesc(UUID userId, UUID restaurantId, List<ReservationStatus> statuses);

    boolean existsByUserIdAndStatus(UUID userId, ReservationStatus status);
}