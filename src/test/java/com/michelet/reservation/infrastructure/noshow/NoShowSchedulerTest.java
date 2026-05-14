package com.michelet.reservation.infrastructure.noshow;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.michelet.reservation.domain.enums.ReservationStatus;
import com.michelet.reservation.infrastructure.outbox.OutboxEventJpaRepository;
import com.michelet.reservation.infrastructure.reservation.ReservationJpaStore;
import com.michelet.reservation.infrastructure.reservation.entity.ReservationJpaEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class NoShowSchedulerTest {

    @Mock ReservationJpaStore jpaStore;
    @Mock OutboxEventJpaRepository outboxEventJpaRepository;
    @Mock TransactionTemplate transactionTemplate;

    ObjectMapper objectMapper;
    NoShowScheduler scheduler;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        scheduler = new NoShowScheduler(jpaStore, outboxEventJpaRepository, objectMapper, transactionTemplate);
        doAnswer(inv -> ((TransactionCallback<Object>) inv.getArgument(0)).doInTransaction(null))
                .when(transactionTemplate).execute(any());
    }

    ReservationJpaEntity confirmedEntity() {
        return ReservationJpaEntity.of(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.now(), 2, ReservationStatus.CONFIRMED,
                LocalDate.now().minusDays(2), LocalDate.now().minusDays(2),
                LocalDateTime.now().minusMinutes(5), null
        );
    }

    @Nested
    class EmptyBatch {

        @Test
        void 만료된_예약이_없으면_아무것도_실행하지_않는다() {
            when(jpaStore.findExpiredConfirmedForUpdate(any(), anyInt())).thenReturn(List.of());

            scheduler.markExpiredReservationsAsNoShow();

            verify(jpaStore, never()).findByIdAndStatusConfirmedForUpdate(any());
            verify(outboxEventJpaRepository, never()).save(any());
        }
    }

    @Nested
    class NoShowTransition {

        @Test
        void 만료된_CONFIRMED_예약을_NO_SHOW로_전이하고_아웃박스를_저장한다() {
            ReservationJpaEntity entity = confirmedEntity();
            when(jpaStore.findExpiredConfirmedForUpdate(any(), anyInt())).thenReturn(List.of(entity));
            when(jpaStore.findByIdAndStatusConfirmedForUpdate(entity.getId())).thenReturn(Optional.of(entity));
            when(jpaStore.save(any())).thenReturn(entity);

            scheduler.markExpiredReservationsAsNoShow();

            verify(outboxEventJpaRepository).save(argThat(e ->
                    "reservation.no-show".equals(e.getEventType())));
        }

        @Test
        void TX2에서_빈_결과_반환_시_이미_처리된_것으로_간주하고_아웃박스를_저장하지_않는다() {
            ReservationJpaEntity entity = confirmedEntity();
            when(jpaStore.findExpiredConfirmedForUpdate(any(), anyInt())).thenReturn(List.of(entity));
            when(jpaStore.findByIdAndStatusConfirmedForUpdate(entity.getId())).thenReturn(Optional.empty());

            scheduler.markExpiredReservationsAsNoShow();

            verify(outboxEventJpaRepository, never()).save(any());
        }
    }
}
