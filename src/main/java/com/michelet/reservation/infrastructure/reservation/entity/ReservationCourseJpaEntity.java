package com.michelet.reservation.infrastructure.reservation.entity;

import com.michelet.reservation.infrastructure.reservation.common.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "p_reservation_courses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationCourseJpaEntity  extends BaseJpaEntity {

  @Id
  @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "reservation_id", columnDefinition = "uuid", nullable = false)
  private UUID reservationId;

  @Column(name = "course_id", columnDefinition = "uuid", nullable = false)
  private UUID courseId;

  @Column(name = "quantity", nullable = false)
  private int quantity;

  @Column(name = "unit_price", nullable = false)
  private int unitPrice;

  public static ReservationCourseJpaEntity of(
      UUID id, UUID reservationId, UUID courseId, int quantity, int unitPrice
  ) {
    ReservationCourseJpaEntity e = new ReservationCourseJpaEntity();
    e.id            = id;
    e.markNew();
    e.reservationId = reservationId;
    e.courseId      = courseId;
    e.quantity      = quantity;
    e.unitPrice     = unitPrice;
    return e;
  }
}