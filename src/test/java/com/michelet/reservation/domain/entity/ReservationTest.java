package com.michelet.reservation.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.michelet.common.exception.BusinessException;
import com.michelet.reservation.domain.enums.ReservationStatus;
import com.michelet.reservation.domain.enums.ReservationTransition;
import com.michelet.reservation.domain.exception.ReservationErrorCode;
import com.michelet.reservation.domain.vo.GuestCount;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ReservationTest {

    final UUID userId = UUID.randomUUID();
    final UUID restaurantId = UUID.randomUUID();
    final UUID timeSlotId = UUID.randomUUID();
    final LocalDate futureDate = LocalDate.now().plusDays(10);
    final LocalDateTime noshowDeadline = LocalDateTime.of(futureDate, LocalTime.of(19, 30));

    Reservation waitingFuture() {
        return Reservation.reconstitute(
                UUID.randomUUID(), userId, restaurantId, timeSlotId,
                futureDate, GuestCount.of(2), ReservationStatus.WAITING,
                futureDate.minusDays(2), futureDate.minusDays(2), noshowDeadline, null
        );
    }

    Reservation confirmedFuture() {
        return Reservation.reconstitute(
                UUID.randomUUID(), userId, restaurantId, timeSlotId,
                futureDate, GuestCount.of(2), ReservationStatus.CONFIRMED,
                futureDate.minusDays(2), futureDate.minusDays(2), noshowDeadline, null
        );
    }

    Reservation confirmedDeadlinePassed() {
        LocalDate pastDate = LocalDate.now().plusDays(1); // deadline = now - 1 day
        return Reservation.reconstitute(
                UUID.randomUUID(), userId, restaurantId, timeSlotId,
                pastDate, GuestCount.of(2), ReservationStatus.CONFIRMED,
                LocalDate.now().minusDays(1), LocalDate.now().minusDays(1),
                LocalDateTime.of(pastDate, LocalTime.of(19, 30)), null
        );
    }

    @Nested
    class Create {

        @Test
        void 정상_생성_시_WAITING_상태로_생성된다() {
            Reservation r = Reservation.create(userId, restaurantId, timeSlotId,
                    futureDate, GuestCount.of(2), noshowDeadline);

            assertThat(r.getStatus()).isEqualTo(ReservationStatus.WAITING);
            assertThat(r.getUserId()).isEqualTo(userId);
            assertThat(r.getCancelDeadline()).isEqualTo(futureDate.minusDays(2));
            assertThat(r.getModifyDeadline()).isEqualTo(futureDate.minusDays(2));
            assertThat(r.getNoshowDeadline()).isEqualTo(noshowDeadline);
        }

        @Test
        void userId_null이면_예외를_던진다() {
            assertThatThrownBy(() ->
                    Reservation.create(null, restaurantId, timeSlotId, futureDate, GuestCount.of(2), noshowDeadline))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.INVALID_USER_ID.getCode()));
        }

        @Test
        void restaurantId_null이면_예외를_던진다() {
            assertThatThrownBy(() ->
                    Reservation.create(userId, null, timeSlotId, futureDate, GuestCount.of(2), noshowDeadline))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.INVALID_RESTAURANT_ID.getCode()));
        }

        @Test
        void timeSlotId_null이면_예외를_던진다() {
            assertThatThrownBy(() ->
                    Reservation.create(userId, restaurantId, null, futureDate, GuestCount.of(2), noshowDeadline))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.INVALID_TIME_SLOT_ID.getCode()));
        }
    }

    @Nested
    class Confirm {

        @Test
        void WAITING_상태에서_확정이_성공한다() {
            Reservation r = waitingFuture();
            r.confirm();
            assertThat(r.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        }

        @Test
        void WAITING_아닌_상태에서_확정하면_예외를_던진다() {
            Reservation r = confirmedFuture();

            assertThatThrownBy(r::confirm)
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.INVALID_STATUS_TRANSITION.getCode()));
        }
    }

    @Nested
    class CancelUnpaid {

        @Test
        void WAITING_상태에서_미결제_취소가_성공한다() {
            Reservation r = waitingFuture();
            r.cancelUnpaid();
            assertThat(r.getStatus()).isEqualTo(ReservationStatus.CANCELLED_UNPAID);
        }

        @Test
        void WAITING_아닌_상태에서_미결제_취소하면_예외를_던진다() {
            Reservation r = confirmedFuture();

            assertThatThrownBy(r::cancelUnpaid)
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.INVALID_STATUS_TRANSITION.getCode()));
        }
    }

    @Nested
    class Cancel {

        @Test
        void CONFIRMED_상태에서_취소가_성공한다() {
            Reservation r = confirmedFuture();
            r.cancel();
            assertThat(r.getStatus()).isEqualTo(ReservationStatus.CANCELLED_PAID);
        }

        @Test
        void CONFIRMED_아닌_상태에서_취소하면_예외를_던진다() {
            Reservation r = confirmedFuture();
            r.cancel();

            assertThatThrownBy(r::cancel)
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.INVALID_STATUS_TRANSITION.getCode()));
        }

        @Test
        void 취소_기한_초과_시_예외를_던진다() {
            Reservation r = confirmedDeadlinePassed();

            assertThatThrownBy(r::cancel)
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.CANCEL_DEADLINE_EXCEEDED.getCode()));
        }
    }

    @Nested
    class Complete {

        @Test
        void CONFIRMED_상태에서_완료가_성공한다() {
            Reservation r = confirmedFuture();
            r.complete();
            assertThat(r.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
        }

        @Test
        void CONFIRMED_아닌_상태에서_완료하면_예외를_던진다() {
            Reservation r = confirmedFuture();
            r.cancel();

            assertThatThrownBy(r::complete)
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.INVALID_STATUS_TRANSITION.getCode()));
        }
    }

    @Nested
    class MarkNoShow {

        @Test
        void CONFIRMED_상태에서_노쇼_처리가_성공한다() {
            Reservation r = confirmedFuture();
            r.markNoShow();
            assertThat(r.getStatus()).isEqualTo(ReservationStatus.NO_SHOW);
        }

        @Test
        void CONFIRMED_아닌_상태에서_노쇼_처리하면_예외를_던진다() {
            Reservation r = confirmedFuture();
            r.cancel();

            assertThatThrownBy(r::markNoShow)
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.INVALID_STATUS_TRANSITION.getCode()));
        }
    }

    @Nested
    class PolicyMethods {

        @Test
        void CONFIRMED_상태에서_requiresSlotReturn은_true다() {
            assertThat(confirmedFuture().requiresSlotReturn()).isTrue();
        }

        @Test
        void WAITING_상태에서_requiresSlotReturn은_false다() {
            assertThat(waitingFuture().requiresSlotReturn()).isFalse();
        }

        @Test
        void COMPLETED_상태에서_requiresSlotReturn은_false다() {
            Reservation r = confirmedFuture();
            r.complete();
            assertThat(r.requiresSlotReturn()).isFalse();
        }

        @Test
        void CANCELLED_PAID_상태에서_requiresSlotReturn은_false다() {
            Reservation r = confirmedFuture();
            r.cancel();
            assertThat(r.requiresSlotReturn()).isFalse();
        }

        @Test
        void CONFIRMED_deadline_이내에서_isCancellable은_true다() {
            assertThat(confirmedFuture().isCancellable()).isTrue();
        }

        @Test
        void CONFIRMED_deadline_초과_시_isCancellable은_false다() {
            assertThat(confirmedDeadlinePassed().isCancellable()).isFalse();
        }

        @Test
        void WAITING_상태에서_isCancellable은_false다() {
            assertThat(waitingFuture().isCancellable()).isFalse();
        }

        @Test
        void CONFIRMED_deadline_이내에서_isModifiable은_true다() {
            assertThat(confirmedFuture().isModifiable()).isTrue();
        }

        @Test
        void CONFIRMED_deadline_초과_시_isModifiable은_false다() {
            assertThat(confirmedDeadlinePassed().isModifiable()).isFalse();
        }

        @Test
        void WAITING_상태에서_isModifiable은_false다() {
            assertThat(waitingFuture().isModifiable()).isFalse();
        }

        @Test
        void CANCELLED_PAID_상태에서_requiresRefund는_true다() {
            Reservation r = confirmedFuture();
            r.cancel();
            assertThat(r.requiresRefund()).isTrue();
        }

        @Test
        void CONFIRMED_상태에서_requiresRefund는_false다() {
            assertThat(confirmedFuture().requiresRefund()).isFalse();
        }

        @Test
        void CANCELLED_UNPAID_상태에서_requiresRefund는_false다() {
            Reservation r = waitingFuture();
            r.cancelUnpaid();
            assertThat(r.requiresRefund()).isFalse();
        }
    }

    @Nested
    class ForbiddenTransitions {

        @Test
        void COMPLETED_상태에서_confirm_호출_시_예외를_던진다() {
            Reservation r = confirmedFuture();
            r.complete();

            assertThatThrownBy(r::confirm)
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.INVALID_STATUS_TRANSITION.getCode()));
        }

        @Test
        void NO_SHOW_상태에서_cancel_호출_시_예외를_던진다() {
            Reservation r = confirmedFuture();
            r.markNoShow();

            assertThatThrownBy(r::cancel)
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.INVALID_STATUS_TRANSITION.getCode()));
        }

        @Test
        void CANCELLED_PAID_상태에서_complete_호출_시_예외를_던진다() {
            Reservation r = confirmedFuture();
            r.cancel();

            assertThatThrownBy(r::complete)
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.INVALID_STATUS_TRANSITION.getCode()));
        }

        @Test
        void WAITING_상태에서_cancel_호출_시_예외를_던진다() {
            Reservation r = waitingFuture();

            assertThatThrownBy(r::cancel)
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.INVALID_STATUS_TRANSITION.getCode()));
        }

        @Test
        void CANCELLED_UNPAID_상태에서_cancelUnpaid_호출_시_예외를_던진다() {
            Reservation r = waitingFuture();
            r.cancelUnpaid();

            assertThatThrownBy(r::cancelUnpaid)
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.INVALID_STATUS_TRANSITION.getCode()));
        }
    }

    @Nested
    class Modify {

        @Test
        void 정상_수정_시_필드가_갱신된다() {
            Reservation r = confirmedFuture();
            UUID newSlotId = UUID.randomUUID();
            LocalDate newDate = futureDate.plusDays(3);
            LocalDateTime newDeadline = LocalDateTime.of(newDate, LocalTime.of(20, 30));

            r.modify(newSlotId, newDate, GuestCount.of(4), newDeadline);

            assertThat(r.getTimeSlotId()).isEqualTo(newSlotId);
            assertThat(r.getReservedDate()).isEqualTo(newDate);
            assertThat(r.getGuestCount().value()).isEqualTo(4);
            assertThat(r.getNoshowDeadline()).isEqualTo(newDeadline);
            assertThat(r.getCancelDeadline()).isEqualTo(newDate.minusDays(2));
            assertThat(r.getModifyDeadline()).isEqualTo(newDate.minusDays(2));
        }

        @Test
        void CONFIRMED_아닌_상태에서_수정하면_예외를_던진다() {
            Reservation r = confirmedFuture();
            r.cancel();

            assertThatThrownBy(() -> r.modify(timeSlotId, futureDate, GuestCount.of(2), noshowDeadline))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.INVALID_STATUS_TRANSITION.getCode()));
        }

        @Test
        void 수정_기한_초과_시_예외를_던진다() {
            Reservation r = confirmedDeadlinePassed();

            assertThatThrownBy(() -> r.modify(timeSlotId, futureDate, GuestCount.of(2), noshowDeadline))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.MODIFY_DEADLINE_EXCEEDED.getCode()));
        }
    }
}
