package com.michelet.reservation.domain.entity;

import com.michelet.reservation.domain.vo.Money;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationCourse {

  private UUID id;
  private UUID reservationId;

  private UUID courseId;

  private int quantity;

  private Money unitPrice;

  public static ReservationCourse create(
      UUID reservationId,
      UUID courseId,
      int quantity,
      Money unitPrice
  ) {
    ReservationCourse c = new ReservationCourse();
    c.id            = UUID.randomUUID();
    c.reservationId = reservationId;
    c.courseId      = courseId;
    c.quantity      = quantity;
    c.unitPrice     = unitPrice;
    return c;
  }

  public static ReservationCourse reconstitute(
      UUID id,
      UUID reservationId,
      UUID courseId,
      int quantity,
      Money unitPrice
  ) {
    ReservationCourse c = new ReservationCourse();
    c.id            = id;
    c.reservationId = reservationId;
    c.courseId      = courseId;
    c.quantity      = quantity;
    c.unitPrice     = unitPrice;
    return c;
  }

  public Money totalPrice() {
    return unitPrice.multiply(quantity);
  }
}