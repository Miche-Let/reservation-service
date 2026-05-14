package com.michelet.reservation.infrastructure.reservation;

import com.michelet.reservation.domain.enums.ReservationStatus;
import com.michelet.reservation.infrastructure.reservation.entity.ReservationJpaEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationJpaStore extends JpaRepository<ReservationJpaEntity, UUID> {

    Page<ReservationJpaEntity> findAllByUserId(UUID userId, Pageable pageable);

    Page<ReservationJpaEntity> findAllByUserIdAndStatus(UUID userId, ReservationStatus status, Pageable pageable);


    boolean existsByUserIdAndTimeSlotIdAndReservedDateAndStatusIn(UUID userId, UUID timeSlotId,
                                                                  LocalDate reservedDate,
                                                                  Collection<ReservationStatus> statuses);

    Optional<ReservationJpaEntity> findTopByUserIdAndRestaurantIdAndStatusInOrderByReservedDateDesc(UUID userId, UUID restaurantId, List<ReservationStatus> statuses);

    boolean existsByUserIdAndStatus(UUID userId, ReservationStatus status);

    @Query(value = """
            SELECT * FROM reservation_service.p_reservations
            WHERE status = 'CONFIRMED'
              AND noshow_deadline <= :now
              AND deleted_at IS NULL
            ORDER BY noshow_deadline
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<ReservationJpaEntity> findExpiredConfirmedForUpdate(
            @Param("now") LocalDateTime now,
            @Param("limit") int limit
    );

    @Query(value = """
            SELECT * FROM reservation_service.p_reservations
            WHERE id = :id
              AND status = 'CONFIRMED'
              AND deleted_at IS NULL
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<ReservationJpaEntity> findByIdAndStatusConfirmedForUpdate(@Param("id") UUID id);
}