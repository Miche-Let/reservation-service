package com.michelet.reservation.application.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.michelet.common.exception.BusinessException;
import com.michelet.reservation.application.exception.ExternalCallFailedException;
import com.michelet.reservation.application.port.OutboxEventPort;
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
    OutboxEventPort outboxEventPort;

    @InjectMocks
    ReservationCommandServiceImpl commandService;

    final UUID userId = UUID.randomUUID();
    final UUID restaurantId = UUID.randomUUID();
    final UUID timeSlotId = UUID.randomUUID();
    final UUID reservationId = UUID.randomUUID();
    final UUID staffId = UUID.randomUUID();
    final LocalDate futureDate   = LocalDate.now().plusDays(10);
    final LocalTime slotStartTime = LocalTime.of(19, 0);

    Reservation confirmedReservation() {
        return Reservation.reconstitute(
                reservationId, userId, restaurantId, timeSlotId,
                futureDate, GuestCount.of(2), ReservationStatus.CONFIRMED,
                futureDate.minusDays(2), futureDate.minusDays(2),
                LocalDateTime.of(futureDate, slotStartTime).plusMinutes(30), null
        );
    }

    // 체크인 시간대 검증이 통과하도록 현재 시각이 허용 범위에 포함되는 예약
    Reservation confirmedNow() {
        LocalDateTime now = LocalDateTime.now();
        return Reservation.reconstitute(
                reservationId, userId, restaurantId, timeSlotId,
                now.toLocalDate(), GuestCount.of(2), ReservationStatus.CONFIRMED,
                now.toLocalDate().minusDays(2), now.toLocalDate().minusDays(2),
                now.plusMinutes(30),   // noshowDeadline → windowStart = now - 30min, upper = now + 30min
                null
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
            assertThat(result.status()).isEqualTo(ReservationStatus.CONFIRMED);
            verify(reservationRepository, times(2)).save(any(Reservation.class));
            verify(timeSlotPort).decrementStock(eq(timeSlotId), eq(2), any(UUID.class));
            verify(waitingPort).completeWaiting(eq(waitingId));
            // reservation.created + waiting.completed 2개 적재
            verify(outboxEventPort).recordReservationCreated(any(), any(), any(), any(), any(), anyInt(), any());
            verify(outboxEventPort).recordWaitingCompleted(any(), any(), any());
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

        @Test
        void 슬롯_차감_타임아웃_시_즉시_예외를_던진다() {
            doThrow(new ExternalCallFailedException("read timeout", new RuntimeException()))
                    .when(timeSlotPort).decrementStock(any(), anyInt(), any());

            assertThatThrownBy(() -> commandService.create(createCommand()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.TIMESLOT_SERVICE_UNAVAILABLE.getCode()));

            verify(outboxEventPort, never()).recordReservationCreated(any(), any(), any(), any(), any(), anyInt(), any());
            verify(outboxEventPort, never()).recordWaitingCompleted(any(), any(), any());
        }

        @Test
        void 슬롯_차감_네트워크_오류_시_즉시_예외를_던진다() {
            doThrow(new ExternalCallFailedException("connection refused", new RuntimeException()))
                    .when(timeSlotPort).decrementStock(any(), anyInt(), any());

            assertThatThrownBy(() -> commandService.create(createCommand()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.TIMESLOT_SERVICE_UNAVAILABLE.getCode()));

            verify(outboxEventPort, never()).recordReservationCreated(any(), any(), any(), any(), any(), anyInt(), any());
            verify(outboxEventPort, never()).recordWaitingCompleted(any(), any(), any());
        }

        @Test
        void 슬롯_부족으로_차감_거부_시_BusinessException이_발생한다() {
            // timeslot-service 4xx 응답 → adapter가 BusinessException으로 변환
            doThrow(new BusinessException(ReservationErrorCode.SLOT_NOT_AVAILABLE))
                    .when(timeSlotPort).decrementStock(any(), anyInt(), any());

            assertThatThrownBy(() -> commandService.create(createCommand()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.SLOT_NOT_AVAILABLE.getCode()));

            // BusinessException 전파 → 트랜잭션 롤백 (WAITING save도 취소됨)
            // 실제 롤백 검증은 통합 테스트에서 확인
            verify(waitingPort, never()).completeWaiting(any());
            verify(outboxEventPort, never()).recordReservationCreated(any(), any(), any(), any(), any(), anyInt(), any());
            verify(outboxEventPort, never()).recordWaitingCompleted(any(), any(), any());
        }

        @Test
        void 대기열_완료_Feign_실패_시에도_예약이_CONFIRMED_상태로_저장된다() {
            // Feign 실패해도 예약 확정 계속, outbox 이벤트가 재처리 보장
            doThrow(new RuntimeException("waiting-service unavailable"))
                    .when(waitingPort).completeWaiting(any());

            ReservationResult result = commandService.create(createCommand());

            assertThat(result.status()).isEqualTo(ReservationStatus.CONFIRMED);
            verify(reservationRepository, times(2)).save(any(Reservation.class));
            verify(outboxEventPort).recordReservationCreated(any(), any(), any(), any(), any(), anyInt(), any());
            verify(outboxEventPort).recordWaitingCompleted(any(), any(), any());
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
            verify(timeSlotPort).decrementStock(eq(timeSlotId), eq(2), any(UUID.class));
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
            verify(timeSlotPort).decrementStock(eq(newSlotId), eq(2), any(UUID.class));
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
                    null, null, null, null, null  // 모든 필드 미변경
            ));

            verify(timeSlotPort, never()).incrementStock(any(), anyInt());
            verify(timeSlotPort, never()).decrementStock(any(), anyInt(), any());
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
        void 정상_취소_시_재고가_복구되고_이벤트가_적재된다() {
            Reservation reservation = confirmedReservation();
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

            commandService.cancel(CancelReservationCommand.of(reservationId, userId, "USER"));

            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED_PAID);
            verify(timeSlotPort).incrementStock(timeSlotId, 2);
            verify(outboxEventPort).recordReservationCancelled(any(), any(), any(), any(), any(), anyInt(), any(), any());
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
        void 정상_체크인_시_COMPLETED_상태가_되고_이벤트가_적재된다() {
            Reservation reservation = confirmedNow();
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

            commandService.checkIn(new CheckInCommand(reservationId, restaurantId, staffId));

            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
            verify(outboxEventPort).recordCheckInCompleted(any(), any(), any(), any(), any());
        }

        @Test
        void 예약이_없으면_예외를_던진다() {
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    commandService.checkIn(new CheckInCommand(reservationId, restaurantId, staffId)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.RESERVATION_NOT_FOUND.getCode()));
        }

        @Test
        void restaurantId_불일치_시_예외를_던진다() {
            Reservation reservation = confirmedReservation();
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

            assertThatThrownBy(() ->
                    commandService.checkIn(new CheckInCommand(reservationId, UUID.randomUUID(), staffId)))
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
                    commandService.checkIn(new CheckInCommand(reservationId, restaurantId, staffId)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.INVALID_STATUS_TRANSITION.getCode()));
        }
    }
}
