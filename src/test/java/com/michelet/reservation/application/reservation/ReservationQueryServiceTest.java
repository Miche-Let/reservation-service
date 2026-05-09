package com.michelet.reservation.application.reservation;

import com.michelet.common.exception.BusinessException;
import com.michelet.reservation.application.reservation.result.ReservationActiveResult;
import com.michelet.reservation.application.reservation.result.ReservationExistsResult;
import com.michelet.reservation.application.reservation.result.ReservationValidityResult;
import com.michelet.reservation.domain.entity.Reservation;
import com.michelet.reservation.domain.enums.ReservationStatus;
import com.michelet.reservation.domain.exception.ReservationErrorCode;
import com.michelet.reservation.domain.repository.ReservationCourseRepository;
import com.michelet.reservation.domain.repository.ReservationRepository;
import com.michelet.reservation.domain.vo.GuestCount;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationQueryServiceTest {

    @Mock ReservationRepository reservationRepository;
    @Mock ReservationCourseRepository reservationCourseRepository;

    @InjectMocks ReservationQueryServiceImpl queryService;

    final UUID userId        = UUID.randomUUID();
    final UUID restaurantId  = UUID.randomUUID();
    final UUID timeSlotId    = UUID.randomUUID();
    final UUID reservationId = UUID.randomUUID();
    final LocalDate futureDate = LocalDate.now().plusDays(10);

    Reservation confirmedReservation() {
        return Reservation.reconstitute(
                reservationId, userId, restaurantId, timeSlotId,
                futureDate, GuestCount.of(2), ReservationStatus.CONFIRMED,
                futureDate.minusDays(2), futureDate.minusDays(2),
                LocalDateTime.of(futureDate, LocalTime.of(19, 30)), null
        );
    }

    @Nested
    class GetList {

        @Test
        void status_없으면_userId로_전체_조회한다() {
            Pageable pageable = PageRequest.of(0, 10);
            when(reservationRepository.findPageByUserId(userId, pageable))
                    .thenReturn(new PageImpl<>(List.of(confirmedReservation())));

            Page<?> result = queryService.getList(userId, null, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(reservationRepository).findPageByUserId(userId, pageable);
        }

        @Test
        void status_있으면_userId와_status로_조회한다() {
            Pageable pageable = PageRequest.of(0, 10);
            when(reservationRepository.findPageByUserIdAndStatus(userId, ReservationStatus.CONFIRMED, pageable))
                    .thenReturn(new PageImpl<>(List.of(confirmedReservation())));

            Page<?> result = queryService.getList(userId, ReservationStatus.CONFIRMED, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(reservationRepository).findPageByUserIdAndStatus(userId, ReservationStatus.CONFIRMED, pageable);
        }
    }

    @Nested
    class GetDetail {

        @Test
        void 본인_예약을_정상_조회한다() {
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(confirmedReservation()));
            when(reservationCourseRepository.findAllByReservationId(reservationId)).thenReturn(List.of());

            var result = queryService.getDetail(userId, "USER", reservationId);

            assertThat(result.reservationId()).isEqualTo(reservationId);
        }

        @Test
        void 예약이_없으면_예외를_던진다() {
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> queryService.getDetail(userId, "USER", reservationId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.RESERVATION_NOT_FOUND.getCode()));
        }

        @Test
        void 다른_사용자의_예약은_조회할_수_없다() {
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(confirmedReservation()));

            assertThatThrownBy(() -> queryService.getDetail(UUID.randomUUID(), "USER", reservationId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReservationErrorCode.RESERVATION_NOT_FOUND.getCode()));
        }

        @Test
        void MASTER는_다른_사용자의_예약도_조회할_수_있다() {
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(confirmedReservation()));
            when(reservationCourseRepository.findAllByReservationId(reservationId)).thenReturn(List.of());

            var result = queryService.getDetail(UUID.randomUUID(), "MASTER", reservationId);

            assertThat(result.reservationId()).isEqualTo(reservationId);
        }
    }

    @Nested
    class CheckValidity {

        @Test
        void CONFIRMED_또는_COMPLETED_예약이_있으면_예약_정보를_반환한다() {
            Reservation reservation = confirmedReservation();
            when(reservationRepository.findTopByUserIdAndRestaurantIdAndStatusInOrderByReservedDateDesc(
                    eq(userId), eq(restaurantId),
                    eq(List.of(ReservationStatus.CONFIRMED, ReservationStatus.COMPLETED))))
                    .thenReturn(Optional.of(reservation));

            ReservationValidityResult result = queryService.checkValidity(userId, restaurantId);

            assertThat(result.exists()).isTrue();
            assertThat(result.reservationId()).isEqualTo(reservationId);
            assertThat(result.reservationDate()).isEqualTo(futureDate);
        }

        @Test
        void 해당_예약이_없으면_exists_false_이고_나머지_필드는_null이다() {
            when(reservationRepository.findTopByUserIdAndRestaurantIdAndStatusInOrderByReservedDateDesc(
                    eq(userId), eq(restaurantId), any()))
                    .thenReturn(Optional.empty());

            ReservationValidityResult result = queryService.checkValidity(userId, restaurantId);

            assertThat(result.exists()).isFalse();
            assertThat(result.reservationId()).isNull();
            assertThat(result.reservationDate()).isNull();
        }
    }

    @Nested
    class CheckExists {

        @Test
        void CONFIRMED_예약이_userId_restaurantId와_일치하면_exists_true() {
            when(reservationRepository.findById(reservationId))
                    .thenReturn(Optional.of(confirmedReservation()));

            ReservationExistsResult result = queryService.checkExists(reservationId, userId, restaurantId);

            assertThat(result.exists()).isTrue();
        }

        @Test
        void 예약이_없으면_exists_false() {
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

            ReservationExistsResult result = queryService.checkExists(reservationId, userId, restaurantId);

            assertThat(result.exists()).isFalse();
        }

        @Test
        void userId_불일치_시_exists_false() {
            when(reservationRepository.findById(reservationId))
                    .thenReturn(Optional.of(confirmedReservation()));

            ReservationExistsResult result = queryService.checkExists(reservationId, UUID.randomUUID(), restaurantId);

            assertThat(result.exists()).isFalse();
        }

        @Test
        void restaurantId_불일치_시_exists_false() {
            when(reservationRepository.findById(reservationId))
                    .thenReturn(Optional.of(confirmedReservation()));

            ReservationExistsResult result = queryService.checkExists(reservationId, userId, UUID.randomUUID());

            assertThat(result.exists()).isFalse();
        }

        @Test
        void CONFIRMED_아닌_상태이면_exists_false() {
            Reservation cancelled = Reservation.reconstitute(
                    reservationId, userId, restaurantId, timeSlotId,
                    futureDate, GuestCount.of(2), ReservationStatus.CANCELLED_PAID,
                    futureDate.minusDays(2), futureDate.minusDays(2),
                    LocalDateTime.of(futureDate, LocalTime.of(19, 30)), null
            );
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(cancelled));

            ReservationExistsResult result = queryService.checkExists(reservationId, userId, restaurantId);

            assertThat(result.exists()).isFalse();
        }
    }

    @Nested
    class HasActiveReservation {

        @Test
        void CONFIRMED_예약이_있으면_exists_true() {
            when(reservationRepository.existsByUserIdAndStatus(userId, ReservationStatus.CONFIRMED))
                    .thenReturn(true);

            ReservationActiveResult result = queryService.hasActiveReservation(userId);

            assertThat(result.exists()).isTrue();
        }

        @Test
        void CONFIRMED_예약이_없으면_exists_false() {
            when(reservationRepository.existsByUserIdAndStatus(userId, ReservationStatus.CONFIRMED))
                    .thenReturn(false);

            ReservationActiveResult result = queryService.hasActiveReservation(userId);

            assertThat(result.exists()).isFalse();
        }
    }
}
