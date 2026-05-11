package com.michelet.reservation.domain.state;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.michelet.common.exception.BusinessException;
import com.michelet.reservation.domain.enums.ReservationStatus;
import com.michelet.reservation.domain.exception.ReservationErrorCode;
import java.time.LocalDate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ReservationStateTest {

    final LocalDate futureDeadline = LocalDate.now().plusDays(5);
    final LocalDate pastDeadline   = LocalDate.now().minusDays(1);

    // ─────────────────────── Factory ───────────────────────

    @Nested
    class Factory {

        @Test
        void 각_상태값에_대해_올바른_구현체를_반환한다() {
            assertThat(ReservationStateFactory.from(ReservationStatus.WAITING))
                    .isInstanceOf(WaitingState.class);
            assertThat(ReservationStateFactory.from(ReservationStatus.CONFIRMED))
                    .isInstanceOf(ConfirmedState.class);
            assertThat(ReservationStateFactory.from(ReservationStatus.CANCELLED_UNPAID))
                    .isInstanceOf(CancelledUnpaidState.class);
            assertThat(ReservationStateFactory.from(ReservationStatus.CANCELLED_PAID))
                    .isInstanceOf(CancelledPaidState.class);
            assertThat(ReservationStateFactory.from(ReservationStatus.COMPLETED))
                    .isInstanceOf(CompletedState.class);
            assertThat(ReservationStateFactory.from(ReservationStatus.NO_SHOW))
                    .isInstanceOf(NoShowState.class);
        }

        @Test
        void 모든_enum_값에_대해_누락_없이_구현체를_반환한다() {
            for (ReservationStatus status : ReservationStatus.values()) {
                assertThat(ReservationStateFactory.from(status)).isNotNull();
            }
        }
    }

    // ─────────────────────── WaitingState ───────────────────────

    @Nested
    class Waiting {

        final ReservationState state = new WaitingState();

        @Test
        void confirm은_CONFIRMED를_반환한다() {
            assertThat(state.confirm()).isEqualTo(ReservationStatus.CONFIRMED);
        }

        @Test
        void cancelUnpaid는_CANCELLED_UNPAID를_반환한다() {
            assertThat(state.cancelUnpaid()).isEqualTo(ReservationStatus.CANCELLED_UNPAID);
        }

        @Test
        void cancel은_예외를_던진다() {
            assertThatThrownBy(() -> state.cancel(futureDeadline))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.INVALID_STATUS_TRANSITION.getCode()));
        }

        @Test
        void complete는_예외를_던진다() {
            assertThatThrownBy(state::complete)
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.INVALID_STATUS_TRANSITION.getCode()));
        }

        @Test
        void markNoShow는_예외를_던진다() {
            assertThatThrownBy(state::markNoShow)
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.INVALID_STATUS_TRANSITION.getCode()));
        }

        @Test
        void requiresSlotReturn은_false다() {
            assertThat(state.requiresSlotReturn()).isFalse();
        }

        @Test
        void requiresRefund는_false다() {
            assertThat(state.requiresRefund()).isFalse();
        }

        @Test
        void status는_WAITING이다() {
            assertThat(state.status()).isEqualTo(ReservationStatus.WAITING);
        }
    }

    // ─────────────────────── ConfirmedState ───────────────────────

    @Nested
    class Confirmed {

        final ReservationState state = new ConfirmedState();

        @Test
        void deadline_이내에서_cancel은_CANCELLED_PAID를_반환한다() {
            assertThat(state.cancel(futureDeadline)).isEqualTo(ReservationStatus.CANCELLED_PAID);
        }

        @Test
        void deadline_초과_시_cancel은_CANCEL_DEADLINE_EXCEEDED_예외를_던진다() {
            assertThatThrownBy(() -> state.cancel(pastDeadline))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.CANCEL_DEADLINE_EXCEEDED.getCode()));
        }

        @Test
        void complete는_COMPLETED를_반환한다() {
            assertThat(state.complete()).isEqualTo(ReservationStatus.COMPLETED);
        }

        @Test
        void markNoShow는_NO_SHOW를_반환한다() {
            assertThat(state.markNoShow()).isEqualTo(ReservationStatus.NO_SHOW);
        }

        @Test
        void confirm은_예외를_던진다() {
            assertThatThrownBy(state::confirm)
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.INVALID_STATUS_TRANSITION.getCode()));
        }

        @Test
        void cancelUnpaid는_예외를_던진다() {
            assertThatThrownBy(state::cancelUnpaid)
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.INVALID_STATUS_TRANSITION.getCode()));
        }

        @Test
        void requiresSlotReturn은_true다() {
            assertThat(state.requiresSlotReturn()).isTrue();
        }

        @Test
        void requiresRefund는_false다() {
            assertThat(state.requiresRefund()).isFalse();
        }

        @Test
        void status는_CONFIRMED다() {
            assertThat(state.status()).isEqualTo(ReservationStatus.CONFIRMED);
        }
    }

    // ─────────────────────── Terminal States ───────────────────────

    @Nested
    class TerminalStates {

        @Test
        void CancelledUnpaid_모든_전이_메서드가_예외를_던진다() {
            ReservationState state = new CancelledUnpaidState();
            assertAllTransitionsForbidden(state);
            assertThat(state.requiresSlotReturn()).isFalse();
            assertThat(state.requiresRefund()).isFalse();
            assertThat(state.status()).isEqualTo(ReservationStatus.CANCELLED_UNPAID);
        }

        @Test
        void CancelledPaid_모든_전이_메서드가_예외를_던진다() {
            ReservationState state = new CancelledPaidState();
            assertAllTransitionsForbidden(state);
            assertThat(state.requiresSlotReturn()).isFalse();
            assertThat(state.requiresRefund()).isTrue();
            assertThat(state.status()).isEqualTo(ReservationStatus.CANCELLED_PAID);
        }

        @Test
        void Completed_모든_전이_메서드가_예외를_던진다() {
            ReservationState state = new CompletedState();
            assertAllTransitionsForbidden(state);
            assertThat(state.requiresSlotReturn()).isFalse();
            assertThat(state.requiresRefund()).isFalse();
            assertThat(state.status()).isEqualTo(ReservationStatus.COMPLETED);
        }

        @Test
        void NoShow_모든_전이_메서드가_예외를_던진다() {
            ReservationState state = new NoShowState();
            assertAllTransitionsForbidden(state);
            assertThat(state.requiresSlotReturn()).isFalse();
            assertThat(state.requiresRefund()).isFalse();
            assertThat(state.status()).isEqualTo(ReservationStatus.NO_SHOW);
        }

        private void assertAllTransitionsForbidden(ReservationState state) {
            assertThatThrownBy(state::confirm)
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.INVALID_STATUS_TRANSITION.getCode()));

            assertThatThrownBy(() -> state.cancel(futureDeadline))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.INVALID_STATUS_TRANSITION.getCode()));

            assertThatThrownBy(state::cancelUnpaid)
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.INVALID_STATUS_TRANSITION.getCode()));

            assertThatThrownBy(state::complete)
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.INVALID_STATUS_TRANSITION.getCode()));

            assertThatThrownBy(state::markNoShow)
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.INVALID_STATUS_TRANSITION.getCode()));
        }
    }
}
