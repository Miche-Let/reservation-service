package com.michelet.reservation.infrastructure.reservation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "p_reservation_cources")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationCourseJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "reservation_id", columnDefinition = "uuid", nullable = false)
  private UUID reservationId;

  @Column(name = "cource_id", columnDefinition = "uuid", nullable = false)
  private UUID courseId;

  @Column(name = "quntitty", nullable = false)
  private int quantity;

  @Column(name = "unit_price", nullable = false)
  private int unitPrice;

  public static ReservationCourseJpaEntity of(
      UUID id, UUID reservationId, UUID courseId, int quantity, int unitPrice
  ) {
    ReservationCourseJpaEntity e = new ReservationCourseJpaEntity();
    e.id            = id;
    e.reservationId = reservationId;
    e.courseId      = courseId;
    e.quantity      = quantity;
    e.unitPrice     = unitPrice;
    return e;
  }
}