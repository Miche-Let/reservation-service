package com.michelet.reservation.infrastructure.reservation.entity;

import com.michelet.reservation.domain.enums.ReservationStatus;
import com.michelet.reservation.infrastructure.reservation.common.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_reservations")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationJpaEntity  extends BaseJpaEntity {

  @Id
  @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "user_id", columnDefinition = "uuid", nullable = false)
  private UUID userId;

  @Column(name = "restaurant_id", columnDefinition = "uuid", nullable = false)
  private UUID restaurantId;

  @Column(name = "time_slot_id", columnDefinition = "uuid", nullable = false)
  private UUID timeSlotId;

  @Column(name = "reserved_date", nullable = false)
  private LocalDate reservedDate;

  @Column(name = "guest_count", nullable = false)
  private int guestCount;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 20, nullable = false)
  private ReservationStatus status;

  @Column(name = "cancel_deadline", nullable = false)
  private LocalDate cancelDeadline;

  @Column(name = "modify_deadline", nullable = false)
  private LocalDate modifyDeadline;

  @Column(name = "noshow_deadline", nullable = false)
  private LocalDateTime noshowDeadline;

  @Column(name = "checked_in_at")
  private LocalDateTime checkedInAt;

  public static ReservationJpaEntity of(
      UUID id, UUID userId, UUID restaurantId, UUID timeSlotId,
      LocalDate reservedDate, int guestCount, ReservationStatus status,
      LocalDate cancelDeadline, LocalDate modifyDeadline, LocalDateTime noshowDeadline,
      LocalDateTime checkedInAt
  ) {
    ReservationJpaEntity e = new ReservationJpaEntity();
    e.id             = id;
    e.userId         = userId;
    e.restaurantId   = restaurantId;
    e.timeSlotId     = timeSlotId;
    e.reservedDate   = reservedDate;
    e.guestCount     = guestCount;
    e.status         = status;
    e.cancelDeadline = cancelDeadline;
    e.modifyDeadline = modifyDeadline;
    e.noshowDeadline = noshowDeadline;
    e.checkedInAt    = checkedInAt;
    return e;
  }

  public void updateStatus(ReservationStatus status) {
    this.status = status;
  }

  public void updateSchedule(
      LocalDate reservedDate, int guestCount,
      LocalDate cancelDeadline, LocalDate modifyDeadline, LocalDateTime noshowDeadline
  ) {
    this.reservedDate   = reservedDate;
    this.guestCount     = guestCount;
    this.cancelDeadline = cancelDeadline;
    this.modifyDeadline = modifyDeadline;
    this.noshowDeadline = noshowDeadline;
  }
}