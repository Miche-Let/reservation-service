package com.michelet.reservation.domain.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ReservationTransitionTest {

    @Nested
    class AllowedTransitions {

        @Test
        void WAITING에서_CONFIRMED로_전이_허용() {
            assertThat(ReservationTransition.isAllowed(ReservationStatus.WAITING, ReservationStatus.CONFIRMED)).isTrue();
        }

        @Test
        void WAITING에서_CANCELLED_UNPAID로_전이_허용() {
            assertThat(ReservationTransition.isAllowed(ReservationStatus.WAITING, ReservationStatus.CANCELLED_UNPAID)).isTrue();
        }

        @Test
        void CONFIRMED에서_CANCELLED_PAID로_전이_허용() {
            assertThat(ReservationTransition.isAllowed(ReservationStatus.CONFIRMED, ReservationStatus.CANCELLED_PAID)).isTrue();
        }

        @Test
        void CONFIRMED에서_COMPLETED로_전이_허용() {
            assertThat(ReservationTransition.isAllowed(ReservationStatus.CONFIRMED, ReservationStatus.COMPLETED)).isTrue();
        }

        @Test
        void CONFIRMED에서_NO_SHOW로_전이_허용() {
            assertThat(ReservationTransition.isAllowed(ReservationStatus.CONFIRMED, ReservationStatus.NO_SHOW)).isTrue();
        }
    }

    @Nested
    class ForbiddenTransitions {

        @Test
        void CONFIRMED에서_WAITING으로_전이_금지() {
            assertThat(ReservationTransition.isAllowed(ReservationStatus.CONFIRMED, ReservationStatus.WAITING)).isFalse();
        }

        @Test
        void CONFIRMED에서_CONFIRMED로_전이_금지() {
            assertThat(ReservationTransition.isAllowed(ReservationStatus.CONFIRMED, ReservationStatus.CONFIRMED)).isFalse();
        }

        @Test
        void COMPLETED에서_CONFIRMED로_전이_금지() {
            assertThat(ReservationTransition.isAllowed(ReservationStatus.COMPLETED, ReservationStatus.CONFIRMED)).isFalse();
        }

        @Test
        void NO_SHOW에서_CONFIRMED로_전이_금지() {
            assertThat(ReservationTransition.isAllowed(ReservationStatus.NO_SHOW, ReservationStatus.CONFIRMED)).isFalse();
        }

        @Test
        void CANCELLED_PAID에서_CONFIRMED로_전이_금지() {
            assertThat(ReservationTransition.isAllowed(ReservationStatus.CANCELLED_PAID, ReservationStatus.CONFIRMED)).isFalse();
        }

        @Test
        void CANCELLED_UNPAID에서_CONFIRMED로_전이_금지() {
            assertThat(ReservationTransition.isAllowed(ReservationStatus.CANCELLED_UNPAID, ReservationStatus.CONFIRMED)).isFalse();
        }

        @Test
        void WAITING에서_CANCELLED_PAID로_전이_금지() {
            assertThat(ReservationTransition.isAllowed(ReservationStatus.WAITING, ReservationStatus.CANCELLED_PAID)).isFalse();
        }

        @Test
        void WAITING에서_COMPLETED로_전이_금지() {
            assertThat(ReservationTransition.isAllowed(ReservationStatus.WAITING, ReservationStatus.COMPLETED)).isFalse();
        }

        @Test
        void WAITING에서_NO_SHOW로_전이_금지() {
            assertThat(ReservationTransition.isAllowed(ReservationStatus.WAITING, ReservationStatus.NO_SHOW)).isFalse();
        }
    }

    @Nested
    class TransitionCount {

        @Test
        void 허용_전이는_정확히_5개다() {
            long count = 0;
            for (ReservationStatus from : ReservationStatus.values()) {
                for (ReservationStatus to : ReservationStatus.values()) {
                    if (ReservationTransition.isAllowed(from, to)) {
                        count++;
                    }
                }
            }
            assertThat(count).isEqualTo(5);
        }
    }
}
