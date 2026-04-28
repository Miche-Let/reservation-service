package com.michelet.reservation.application.reservation;

import com.michelet.reservation.application.reservation.command.CancelReservationCommand;
import com.michelet.reservation.application.reservation.command.CheckInCommand;
import com.michelet.reservation.application.reservation.command.CreateReservationCommand;
import com.michelet.reservation.application.reservation.command.ModifyReservationCommand;
import com.michelet.reservation.application.reservation.result.ReservationResult;
import com.michelet.reservation.application.reservation.result.ReservationStatusResult;

public interface ReservationCommandService {

  ReservationResult create(CreateReservationCommand command);

  ReservationResult modify(ModifyReservationCommand command);

  void cancel(CancelReservationCommand command);

  ReservationStatusResult checkIn(CheckInCommand command);
}
