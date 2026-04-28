package com.michelet.reservation.domain.entity;

import com.michelet.common.exception.BusinessException;
import com.michelet.reservation.domain.enums.ReservationStatus;
import com.michelet.reservation.domain.exception.ReservationErrorCode;
import com.michelet.reservation.domain.vo.GuestCount;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {

  private UUID id;
  private UUID userId;
  private UUID restaurantId;
  private UUID timeSlotId;
  private LocalDate reservedDate;
  private GuestCount guestCount;
  private ReservationStatus status;

  private LocalDate cancelDeadline;

  private LocalDate modifyDeadline;

  private LocalDateTime noshowDeadline;

  public static Reservation create(
      UUID userId,
      UUID restaurantId,
      UUID timeSlotId,
      LocalDate reservedDate,
      GuestCount guestCount,
      LocalDateTime noshowDeadline
  ) {
    validateCreateInput(userId, restaurantId, timeSlotId, reservedDate, guestCount);
    Reservation r = new Reservation();
    r.id             = UUID.randomUUID();
    r.userId         = userId;
    r.restaurantId   = restaurantId;
    r.timeSlotId     = timeSlotId;
    r.reservedDate   = reservedDate;
    r.guestCount     = guestCount;
    r.status         = ReservationStatus.CONFIRMED;
    r.cancelDeadline = reservedDate.minusDays(2);
    r.modifyDeadline = reservedDate.minusDays(2);
    r.noshowDeadline = noshowDeadline; // 타임슬롯 데이터를 가져와서 설정함
    return r;
  }

  public static Reservation reconstitute(
      UUID id,
      UUID userId,
      UUID restaurantId,
      UUID timeSlotId,
      LocalDate reservedDate,
      GuestCount guestCount,
      ReservationStatus status,
      LocalDate cancelDeadline,
      LocalDate modifyDeadline,
      LocalDateTime noshowDeadline
  ) {
    validateReconstituteInput(id,userId, restaurantId, timeSlotId, reservedDate, guestCount);
    Reservation r = new Reservation();
    r.id             = id;
    r.userId         = userId;
    r.restaurantId   = restaurantId;
    r.timeSlotId     = timeSlotId;
    r.reservedDate   = reservedDate;
    r.guestCount     = guestCount;
    r.status         = status;
    r.cancelDeadline = cancelDeadline;
    r.modifyDeadline = modifyDeadline;
    r.noshowDeadline = noshowDeadline;
    return r;
  }

  public void cancel() {
    validateTransition(ReservationStatus.CONFIRMED);
    if (LocalDate.now().isAfter(cancelDeadline)) {
      throw new BusinessException(ReservationErrorCode.CANCEL_DEADLINE_EXCEEDED);
    }
    this.status = ReservationStatus.CANCELLED;
  }

  public void complete() {
    validateTransition(ReservationStatus.CONFIRMED);
    this.status = ReservationStatus.COMPLETED;
  }

  public void markNoShow() {
    validateTransition(ReservationStatus.CONFIRMED);
    this.status = ReservationStatus.NO_SHOW;
  }

  public void modify(
      LocalDate newReservedDate,
      GuestCount newGuestCount,
      LocalDateTime newNoshowDeadline
  ) {
    validateTransition(ReservationStatus.CONFIRMED);
    if (LocalDate.now().isAfter(modifyDeadline)) {
      throw new BusinessException(ReservationErrorCode.MODIFY_DEADLINE_EXCEEDED);
    }
    this.reservedDate   = newReservedDate;
    this.guestCount     = newGuestCount;
    this.cancelDeadline = newReservedDate.minusDays(2);
    this.modifyDeadline = newReservedDate.minusDays(2);
    this.noshowDeadline = newNoshowDeadline;
  }

  private void validateTransition(ReservationStatus required) {
    if (this.status != required) {
      throw new BusinessException(ReservationErrorCode.INVALID_STATUS_TRANSITION);
    }
  }

  private static void validateCreateInput(
      UUID userId,
      UUID restaurantId,
      UUID timeSlotId,
      LocalDate reservedDate,
      GuestCount guestCount) {
    if (userId == null)         throw new BusinessException(ReservationErrorCode.INVALID_USER_ID);
    if (restaurantId == null)   throw new BusinessException(ReservationErrorCode.INVALID_RESTAURANT_ID);
    if (timeSlotId == null)     throw new BusinessException(ReservationErrorCode.INVALID_TIME_SLOT_ID);
    if (reservedDate == null)   throw new BusinessException(ReservationErrorCode.INVALID_RESERVED_DATE);
    if (guestCount == null)     throw new BusinessException(ReservationErrorCode.INVALID_GUEST_COUNT_NULL);
  }

  private static void validateReconstituteInput(
      UUID id,
      UUID userId,
      UUID restaurantId,
      UUID timeSlotId,
      LocalDate reservedDate,
      GuestCount guestCount
  ) {
    if (id == null)            throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND);
    if (userId == null)        throw new BusinessException(ReservationErrorCode.INVALID_USER_ID);
    if (restaurantId == null)  throw new BusinessException(ReservationErrorCode.INVALID_RESTAURANT_ID);
    if (timeSlotId == null)    throw new BusinessException(ReservationErrorCode.INVALID_TIME_SLOT_ID);
    if (reservedDate == null)  throw new BusinessException(ReservationErrorCode.INVALID_RESERVED_DATE);
    if (guestCount == null)    throw new BusinessException(ReservationErrorCode.INVALID_GUEST_COUNT_NULL);
  }
}