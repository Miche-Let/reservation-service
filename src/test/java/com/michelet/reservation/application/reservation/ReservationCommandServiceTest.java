package com.michelet.reservation.application.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.michelet.common.exception.BusinessException;
import com.michelet.reservation.application.event.ReservationCreatedAppEvent;
import com.michelet.reservation.application.port.TimeSlotPort;
import com.michelet.reservation.application.port.WaitingPort;
import com.michelet.reservation.application.port.WaitingTokenResult;
import com.michelet.reservation.application.reservation.command.CancelReservationCommand;
import com.michelet.reservation.application.reservation.command.CheckInCommand;
import com.michelet.reservation.application.reservation.command.CreateReservationCommand;
import com.michelet.reservation.application.reservation.command.DeleteReservationCommand;
import com.michelet.reservation.application.reservation.command.ModifyReservationCommand;
import com.michelet.reservation.application.reservation.result.ReservationResult;
import com.michelet.reservation.domain.entity.Reservation;
import com.michelet.reservation.domain.entity.ReservationCourse;
import com.michelet.reservation.domain.enums.ReservationStatus;
import com.michelet.reservation.domain.exception.ReservationErrorCode;
import com.michelet.reservation.domain.repository.ReservationCourseRepository;
import com.michelet.reservation.domain.repository.ReservationRepository;
import com.michelet.reservation.domain.vo.GuestCount;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ReservationCommandServiceTest {

    @Mock
    ReservationRepository reservationRepository;
    @Mock
    ReservationCourseRepository reservationCourseRepository;
    @Mock
    TimeSlotPort timeSlotPort;
    @Mock
    WaitingPort waitingPort;
    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    ReservationCommandServiceImpl commandService;

    final UUID userId = UUID.randomUUID();
    final UUID restaurantId = UUID.randomUUID();
    final UUID timeSlotId = UUID.randomUUID();
    final UUID reservationId = UUID.randomUUID();
    final LocalDate futureDate = LocalDate.now().plusDays(10);
    final LocalTime slotStartTime = LocalTime.of(19, 0);

    Reservation confirmedReservation() {
        return Reservation.reconstitute(
                reservationId, userId, restaurantId, timeSlotId,
                futureDate, GuestCount.of(2), ReservationStatus.CONFIRMED,
                futureDate.minusDays(2), futureDate.minusDays(2),
                LocalDateTime.of(futureDate, slotStartTime).plusMinutes(30), null
        );
    }

    Reservation confirmedDeadlinePassed() {
        LocalDate today = LocalDate.now();
        LocalDate nearDate = today.plusDays(1);
        LocalDate passedDeadline = today.minusDays(1);
        return Reservation.reconstitute(
                reservationId, userId, restaurantId, timeSlotId,
                nearDate, GuestCount.of(2), ReservationStatus.CONFIRMED,
                passedDeadline, passedDeadline,
                LocalDateTime.of(nearDate, slotStartTime).plusMinutes(30), null
        );
    }

    CreateReservationCommand createCommand() {
        return new CreateReservationCommand(
                userId, "valid-token", restaurantId, timeSlotId,
                futureDate, slotStartTime, 2,
                List.of(new CreateReservationCommand.CourseItem(UUID.randomUUID(), 1, 30000))
        );
    }

    @BeforeEach
    void stubSave() {
        lenient().when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(reservationCourseRepository.save(any(ReservationCourse.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Nested
    class Create {

        final UUID waitingId = UUID.randomUUID();

        @BeforeEach
        void stubDefaults() {
            when(waitingPort.verifyToken(anyString()))
                    .thenReturn(new WaitingTokenResult(waitingId, true));
            lenient().when(reservationRepository.existsByUserIdAndTimeSlotIdAndReservedDateAndStatus(
                    any(), any(), any(), any())).thenReturn(false);
        }

        @Test
        void 정상_생성_시_예약이_저장되고_재고가_차감되고_대기열이_완료된다() {
            ReservationResult result = commandService.create(createCommand());

            assertThat(result).isNotNull();
            verify(reservationRepository, times(2)).save(any(Reservation.class));
            verify(timeSlotPort).decrementStock(eq(timeSlotId), eq(2));
            verify(waitingPort).completeWaiting(eq(waitingId));
            verify(eventPublisher).publishEvent(any(ReservationCreatedAppEvent.class));
        }

        @Test
        void 토큰이_유효하지_않으면_예외를_던진다() {
            when(waitingPort.verifyToken(anyString()))
                    .thenReturn(new WaitingTokenResult(waitingId, false));

            assertThatThrownBy(() -> commandService.create(createCommand()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.INVALID_WAITING_TOKEN.getCode()));
        }

        @Test
        void 중복_예약이_있으면_예외를_던진다() {
            when(reservationRepository.existsByUserIdAndTimeSlotIdAndReservedDateAndStatus(
                    any(), any(), any(), any())).thenReturn(true);

            assertThatThrownBy(() -> commandService.create(createCommand()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.DUPLICATE_RESERVATION.getCode()));
        }
    }

    @Nested
    class Modify {

        @Test
        void 날짜_변경_시_기존_재고_복구_후_신규_재고_차감() {
            Reservation reservation = confirmedReservation();
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
            LocalDate newDate = futureDate.plusDays(3);

            commandService.modify(new ModifyReservationCommand(
                    reservationId, userId, "USER",
                    null, newDate, null, null, null
            ));

            verify(timeSlotPort).incrementStock(timeSlotId, 2);
            verify(timeSlotPort).decrementStock(timeSlotId, 2);
        }

        @Test
        void 타임슬롯과_날짜_동시_변경_시_slotStartTime_포함하면_재고_조정된다() {
            Reservation reservation = confirmedReservation();
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
            UUID newSlotId = UUID.randomUUID();
            LocalDate newDate = futureDate.plusDays(3);
            LocalTime newStartTime = LocalTime.of(20, 0);

            commandService.modify(new ModifyReservationCommand(
                    reservationId, userId, "USER",
                    newSlotId, newDate, newStartTime, null, null
            ));

            verify(timeSlotPort).incrementStock(timeSlotId, 2);
            verify(timeSlotPort).decrementStock(newSlotId, 2);
        }

        @Test
        void 타임슬롯_변경_시_slotStartTime_없으면_예외를_던진다() {
            Reservation reservation = confirmedReservation();
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
            UUID newSlotId = UUID.randomUUID();

            assertThatThrownBy(() -> commandService.modify(new ModifyReservationCommand(
                    reservationId, userId, "USER",
                    newSlotId, null, null, null, null
            ))).isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.SLOT_START_TIME_REQUIRED.getCode()));
        }

        @Test
        void 날짜와_슬롯_모두_미변경_시_재고_조정_없음() {
            Reservation reservation = confirmedReservation();
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

            commandService.modify(new ModifyReservationCommand(
                    reservationId, userId, "USER",
                    null, null, null, 4, null
            ));

            verify(timeSlotPort, never()).incrementStock(any(), anyInt());
            verify(timeSlotPort, never()).decrementStock(any(), anyInt());
        }

        @Test
        void 예약이_없으면_예외를_던진다() {
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commandService.modify(new ModifyReservationCommand(
                    reservationId, userId, "USER", null, null, null, null, null
            ))).isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.RESERVATION_NOT_FOUND.getCode()));
        }

        @Test
        void 소유자가_아니면_예외를_던진다() {
            Reservation reservation = confirmedReservation();
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

            assertThatThrownBy(() -> commandService.modify(new ModifyReservationCommand(
                    reservationId, UUID.randomUUID(), "USER", null, null, null, null, null
            ))).isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.RESERVATION_NOT_FOUND.getCode()));
        }

        @Test
        void MASTER는_소유권_검증을_통과한다() {
            Reservation reservation = confirmedReservation();
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

            ReservationResult result = commandService.modify(new ModifyReservationCommand(
                    reservationId, UUID.randomUUID(), "MASTER", null, null, null, null, null
            ));

            assertThat(result).isNotNull();
        }

        @Test
        void CONFIRMED_아닌_상태에서_수정하면_예외를_던진다() {
            Reservation reservation = Reservation.reconstitute(
                    reservationId, userId, restaurantId, timeSlotId,
                    futureDate, GuestCount.of(2), ReservationStatus.CANCELLED_PAID,
                    futureDate.minusDays(2), futureDate.minusDays(2),
                    LocalDateTime.of(futureDate, slotStartTime).plusMinutes(30), null
            );
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

            assertThatThrownBy(() -> commandService.modify(new ModifyReservationCommand(
                    reservationId, userId, "USER", null, null, null, null, null
            ))).isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.INVALID_STATUS_TRANSITION.getCode()));
        }

        @Test
        void 수정_기한_초과_시_예외를_던진다() {
            Reservation reservation = confirmedDeadlinePassed();
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

            assertThatThrownBy(() -> commandService.modify(new ModifyReservationCommand(
                    reservationId, userId, "USER", null, futureDate, null, null, null
            ))).isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.MODIFY_DEADLINE_EXCEEDED.getCode()));
        }

        @Test
        void 슬롯_변경_시_대상_슬롯에_중복_예약이_있으면_예외를_던진다() {
            Reservation reservation = confirmedReservation();
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
            UUID newSlotId = UUID.randomUUID();
            when(reservationRepository.existsByUserIdAndTimeSlotIdAndReservedDateAndStatus(
                    eq(userId), eq(newSlotId), eq(futureDate), any())).thenReturn(true);

            assertThatThrownBy(() -> commandService.modify(new ModifyReservationCommand(
                    reservationId, userId, "USER", newSlotId, null, LocalTime.of(20, 0), null, null
            ))).isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.DUPLICATE_RESERVATION.getCode()));
        }
    }

    @Nested
    class Cancel {

        @Test
        void 정상_취소_시_재고가_복구된다() {
            Reservation reservation = confirmedReservation();
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

            commandService.cancel(CancelReservationCommand.of(reservationId, userId, "USER"));

            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED_PAID);
            verify(timeSlotPort).incrementStock(timeSlotId, 2);
        }

        @Test
        void 예약이_없으면_예외를_던진다() {
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    commandService.cancel(CancelReservationCommand.of(reservationId, userId, "USER")))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.RESERVATION_NOT_FOUND.getCode()));
        }

        @Test
        void 소유자가_아니면_예외를_던진다() {
            Reservation reservation = confirmedReservation();
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

            assertThatThrownBy(() ->
                    commandService.cancel(CancelReservationCommand.of(reservationId, UUID.randomUUID(), "USER")))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.RESERVATION_NOT_FOUND.getCode()));
        }

        @Test
        void CONFIRMED_아닌_상태에서_취소하면_예외를_던진다() {
            Reservation reservation = Reservation.reconstitute(
                    reservationId, userId, restaurantId, timeSlotId,
                    futureDate, GuestCount.of(2), ReservationStatus.COMPLETED,
                    futureDate.minusDays(2), futureDate.minusDays(2),
                    LocalDateTime.of(futureDate, slotStartTime).plusMinutes(30), null
            );
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

            assertThatThrownBy(() ->
                    commandService.cancel(CancelReservationCommand.of(reservationId, userId, "USER")))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.INVALID_STATUS_TRANSITION.getCode()));
        }

        @Test
        void 취소_기한_초과_시_예외를_던진다() {
            Reservation reservation = confirmedDeadlinePassed();
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

            assertThatThrownBy(() ->
                    commandService.cancel(CancelReservationCommand.of(reservationId, userId, "USER")))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.CANCEL_DEADLINE_EXCEEDED.getCode()));
        }
    }

    @Nested
    class Delete {

        @Test
        void CONFIRMED_예약_삭제_시_재고를_복구한다() {
            Reservation reservation = confirmedReservation();
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

            commandService.delete(new DeleteReservationCommand(reservationId, userId, "USER"));

            verify(reservationRepository).delete(eq(reservationId), eq(userId));
            verify(timeSlotPort).incrementStock(eq(timeSlotId), eq(2));
        }

        @Test
        void CANCELLED_UNPAID_예약_삭제_시_재고를_복구하지_않는다() {
            Reservation reservation = Reservation.reconstitute(
                    reservationId, userId, restaurantId, timeSlotId,
                    futureDate, GuestCount.of(2), ReservationStatus.CANCELLED_UNPAID,
                    futureDate.minusDays(2), futureDate.minusDays(2),
                    LocalDateTime.of(futureDate, slotStartTime).plusMinutes(30), null
            );
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

            commandService.delete(new DeleteReservationCommand(reservationId, userId, "USER"));

            verify(reservationRepository).delete(eq(reservationId), eq(userId));
            verify(timeSlotPort, never()).incrementStock(any(), anyInt());
        }

        @Test
        void COMPLETED_예약_삭제_시_재고를_복구하지_않는다() {
            Reservation reservation = Reservation.reconstitute(
                    reservationId, userId, restaurantId, timeSlotId,
                    futureDate, GuestCount.of(2), ReservationStatus.COMPLETED,
                    futureDate.minusDays(2), futureDate.minusDays(2),
                    LocalDateTime.of(futureDate, slotStartTime).plusMinutes(30), null
            );
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

            commandService.delete(new DeleteReservationCommand(reservationId, userId, "USER"));

            verify(reservationRepository).delete(eq(reservationId), eq(userId));
            verify(timeSlotPort, never()).incrementStock(any(), anyInt());
        }

        @Test
        void 소유자가_아니면_예외를_던진다() {
            Reservation reservation = confirmedReservation();
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

            assertThatThrownBy(() ->
                    commandService.delete(new DeleteReservationCommand(reservationId, UUID.randomUUID(), "USER")))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.RESERVATION_NOT_FOUND.getCode()));
        }
    }

    @Nested
    class CheckIn {

        @Test
        void 정상_체크인_시_COMPLETED_상태가_된다() {
            Reservation reservation = confirmedReservation();
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

            commandService.checkIn(new CheckInCommand(reservationId, restaurantId));

            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
        }

        @Test
        void 예약이_없으면_예외를_던진다() {
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    commandService.checkIn(new CheckInCommand(reservationId, restaurantId)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.RESERVATION_NOT_FOUND.getCode()));
        }

        @Test
        void restaurantId_불일치_시_예외를_던진다() {
            Reservation reservation = confirmedReservation();
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

            assertThatThrownBy(() ->
                    commandService.checkIn(new CheckInCommand(reservationId, UUID.randomUUID())))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.RESERVATION_NOT_FOUND.getCode()));
        }

        @Test
        void CONFIRMED_아닌_상태에서_체크인하면_예외를_던진다() {
            Reservation reservation = Reservation.reconstitute(
                    reservationId, userId, restaurantId, timeSlotId,
                    futureDate, GuestCount.of(2), ReservationStatus.CANCELLED_PAID,
                    futureDate.minusDays(2), futureDate.minusDays(2),
                    LocalDateTime.of(futureDate, slotStartTime).plusMinutes(30), null
            );
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

            assertThatThrownBy(() ->
                    commandService.checkIn(new CheckInCommand(reservationId, restaurantId)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.INVALID_STATUS_TRANSITION.getCode()));
        }
    }
}
