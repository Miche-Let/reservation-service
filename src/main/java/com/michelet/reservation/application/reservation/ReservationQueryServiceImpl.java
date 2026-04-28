package com.michelet.reservation.application.reservation;

import com.michelet.common.exception.BusinessException;
import com.michelet.reservation.application.reservation.result.ReservationCourseResult;
import com.michelet.reservation.application.reservation.result.ReservationExistsResult;
import com.michelet.reservation.application.reservation.result.ReservationResult;
import com.michelet.reservation.application.reservation.result.ReservationSummaryResult;
import com.michelet.reservation.application.reservation.result.ReservationValidityResult;
import com.michelet.reservation.domain.entity.Reservation;
import com.michelet.reservation.domain.enums.ReservationStatus;
import com.michelet.reservation.domain.exception.ReservationErrorCode;
import com.michelet.reservation.domain.repository.ReservationCourseRepository;
import com.michelet.reservation.domain.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationQueryServiceImpl implements ReservationQueryService {

  private final ReservationRepository reservationRepository;
  private final ReservationCourseRepository reservationCourseRepository;

  @Override
  public Page<ReservationSummaryResult> getList(UUID userId, ReservationStatus status, Pageable pageable) {
    Page<Reservation> page = (status != null)
        ? reservationRepository.findPageByUserIdAndStatus(userId, status, pageable)
        : reservationRepository.findPageByUserId(userId, pageable);
    return page.map(ReservationSummaryResult::from);
  }

  @Override
  public ReservationResult getDetail(UUID userId, String userRole, UUID reservationId) {
    Reservation reservation = reservationRepository.findById(reservationId)
        .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));

    if (!isPrivileged(userRole) && !reservation.getUserId().equals(userId)) {
      throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND);
    }

    List<ReservationCourseResult> courses = reservationCourseRepository
        .findAllByReservationId(reservationId)
        .stream()
        .map(ReservationCourseResult::from)
        .toList();

    return ReservationResult.of(reservation, courses);
  }

  @Override
  public ReservationValidityResult checkValidity(UUID userId, UUID restaurantId) {
    return reservationRepository.findConfirmedByUserIdAndRestaurantId(userId, restaurantId)
        .map(ReservationValidityResult::of)
        .orElseGet(ReservationValidityResult::notFound);
  }

  @Override
  public ReservationExistsResult checkExists(UUID reservationId, UUID userId, UUID restaurantId) {
    return reservationRepository.findById(reservationId)
        .filter(r -> r.getUserId().equals(userId) && r.getRestaurantId().equals(restaurantId))
        .map(ReservationExistsResult::of)
        .orElseGet(ReservationExistsResult::notFound);
  }

  private boolean isPrivileged(String userRole) {
    return "OWNER".equalsIgnoreCase(userRole) || "MASTER".equalsIgnoreCase(userRole);
  }
}
