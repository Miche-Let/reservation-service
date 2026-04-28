package com.michelet.reservation.application.reservation;

import com.michelet.reservation.application.reservation.result.ReservationExistsResult;
import com.michelet.reservation.application.reservation.result.ReservationResult;
import com.michelet.reservation.application.reservation.result.ReservationSummaryResult;
import com.michelet.reservation.application.reservation.result.ReservationValidityResult;
import com.michelet.reservation.domain.enums.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReservationQueryService {

  Page<ReservationSummaryResult> getList(UUID userId, ReservationStatus status, Pageable pageable);

  ReservationResult getDetail(UUID userId, String userRole, UUID reservationId);

  ReservationValidityResult checkValidity(UUID userId, UUID restaurantId);

  ReservationExistsResult checkExists(UUID reservationId, UUID userId, UUID restaurantId);
}
