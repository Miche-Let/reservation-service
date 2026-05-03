package com.michelet.reservation.domain.repository;

import com.michelet.reservation.domain.entity.Reservation;
import com.michelet.reservation.domain.enums.ReservationStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReservationRepository {

    Reservation save(Reservation reservation);

    Optional<Reservation> findById(UUID id);

    Page<Reservation> findPageByUserId(UUID userId, Pageable pageable);

    Page<Reservation> findPageByUserIdAndStatus(UUID userId, ReservationStatus status, Pageable pageable);

    boolean existsByUserIdAndTimeSlotIdAndReservedDateAndStatus(UUID userId, UUID timeSlotId, LocalDate reservedDate,
                                                                ReservationStatus status);

    Optional<Reservation> findTopByUserIdAndRestaurantIdAndStatusInOrderByReservedDateDesc(UUID userId, UUID restaurantId, List<ReservationStatus> statuses);

    boolean existsByUserIdAndStatus(UUID userId, ReservationStatus status);

    void delete(UUID id, UUID deletedBy);
}
