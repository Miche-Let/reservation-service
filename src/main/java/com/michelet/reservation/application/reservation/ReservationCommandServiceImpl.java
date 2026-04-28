package com.michelet.reservation.application.reservation;

import com.michelet.common.exception.BusinessException;
import com.michelet.reservation.application.reservation.command.CancelReservationCommand;
import com.michelet.reservation.application.reservation.command.CheckInCommand;
import com.michelet.reservation.application.reservation.command.CreateReservationCommand;
import com.michelet.reservation.application.reservation.command.ModifyReservationCommand;
import com.michelet.reservation.application.reservation.result.ReservationCourseResult;
import com.michelet.reservation.application.reservation.result.ReservationResult;
import com.michelet.reservation.application.reservation.result.ReservationStatusResult;
import com.michelet.reservation.domain.entity.Reservation;
import com.michelet.reservation.domain.entity.ReservationCourse;
import com.michelet.reservation.domain.enums.ReservationStatus;
import com.michelet.reservation.domain.exception.ReservationErrorCode;
import com.michelet.reservation.domain.repository.ReservationCourseRepository;
import com.michelet.reservation.domain.repository.ReservationRepository;
import com.michelet.reservation.domain.vo.GuestCount;
import com.michelet.reservation.domain.vo.Money;
import com.michelet.reservation.infrastructure.client.TicketClient;
import com.michelet.reservation.infrastructure.client.TimeSlotClient;
import com.michelet.reservation.infrastructure.client.dto.TicketClientResponse;
import com.michelet.reservation.infrastructure.client.dto.TimeSlotResponse;
import com.michelet.reservation.infrastructure.kafka.event.publish.ReservationCancelledEvent;
import com.michelet.reservation.infrastructure.kafka.event.publish.ReservationCreatedEvent;
import com.michelet.reservation.infrastructure.kafka.producer.ReservationEventProducer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationCommandServiceImpl implements ReservationCommandService {

    private final ReservationRepository reservationRepository;
    private final ReservationCourseRepository reservationCourseRepository;
    private final TimeSlotClient timeSlotClient;
    private final TicketClient ticketClient;
    private final ReservationEventProducer eventProducer;

    @Override
    public ReservationResult create(CreateReservationCommand command) {
        checkDuplicate(command.userId(), command.timeSlotId());

        TimeSlotResponse timeSlot = timeSlotClient.getTimeSlot(command.timeSlotId()).data();
        LocalDateTime noshowDeadline = LocalDateTime.of(command.reservedDate(), timeSlot.startTime())
                .minusMinutes(30);

        Reservation reservation = Reservation.create(
                command.userId(),
                command.restaurantId(),
                command.timeSlotId(),
                command.reservedDate(),
                GuestCount.of(command.guestCount()),
                noshowDeadline
        );
        Reservation saved = reservationRepository.save(reservation);

        timeSlotClient.decrementStock(saved.getTimeSlotId(), saved.getReservedDate());

        List<ReservationCourse> savedCourses = saveCourses(saved.getId(), command.courses());

        eventProducer.publishReservationCreated(new ReservationCreatedEvent(
                saved.getId(), saved.getUserId(), saved.getRestaurantId(),
                saved.getTimeSlotId(), saved.getReservedDate(),
                saved.getGuestCount().value(), LocalDateTime.now()
        ));

        return toResult(saved, savedCourses);
    }

    @Override
    public ReservationResult modify(ModifyReservationCommand command) {
        Reservation reservation = findAndVerifyOwnership(command.reservationId(), command.userId(), command.userRole());

        LocalDate newDate = command.reservedDate() != null ? command.reservedDate() : reservation.getReservedDate();
        Integer newGuestCount =
                command.guestCount() != null ? command.guestCount() : reservation.getGuestCount().value();

        LocalDateTime newNoshowDeadline = resolveNoshowDeadline(
                reservation, command.reservedDate(), newDate
        );

        if (command.reservedDate() != null && !command.reservedDate().equals(reservation.getReservedDate())) {
            timeSlotClient.incrementStock(reservation.getTimeSlotId(), reservation.getReservedDate());
            timeSlotClient.decrementStock(reservation.getTimeSlotId(), command.reservedDate());
        }

        reservation.modify(newDate, GuestCount.of(newGuestCount), newNoshowDeadline);
        Reservation saved = reservationRepository.save(reservation);

        List<ReservationCourse> courses = updateCourses(saved.getId(), command.courses());

        return toResult(saved, courses);
    }

    @Override
    public void cancel(CancelReservationCommand command) {
        Reservation reservation = findAndVerifyOwnership(command.reservationId(), command.userId(), command.userRole());

        reservation.cancel();
        reservationRepository.save(reservation);

        timeSlotClient.incrementStock(reservation.getTimeSlotId(), reservation.getReservedDate());

        eventProducer.publishReservationCancelled(new ReservationCancelledEvent(
                reservation.getId(), reservation.getUserId(), reservation.getRestaurantId(),
                reservation.getTimeSlotId(), reservation.getReservedDate(), LocalDateTime.now()
        ));
    }

    @Override
    public ReservationStatusResult checkIn(CheckInCommand command) {
        Reservation reservation = reservationRepository.findById(command.reservationId())
                .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        if (!reservation.getRestaurantId().equals(command.restaurantId())) {
            throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND);
        }

        reservation.complete();
        Reservation saved = reservationRepository.save(reservation);

        return ReservationStatusResult.from(saved);
    }


    private void checkDuplicate(UUID userId, UUID timeSlotId) {
        if (reservationRepository.existsByUserIdAndTimeSlotIdAndStatusNot(
                userId, timeSlotId, ReservationStatus.CANCELLED)) {
            throw new BusinessException(ReservationErrorCode.DUPLICATE_RESERVATION);
        }
    }

    private Reservation findAndVerifyOwnership(UUID reservationId, UUID userId, String userRole) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));
        if (!isPrivileged(userRole) && !reservation.getUserId().equals(userId)) {
            throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND);
        }
        return reservation;
    }

    private LocalDateTime resolveNoshowDeadline(Reservation existing, LocalDate requestedDate,
                                                LocalDate effectiveDate) {
        if (requestedDate == null) {
            return existing.getNoshowDeadline();
        }
        TimeSlotResponse timeSlot = timeSlotClient.getTimeSlot(existing.getTimeSlotId()).data();
        return LocalDateTime.of(effectiveDate, timeSlot.startTime()).minusMinutes(30);
    }

    private List<ReservationCourse> saveCourses(UUID reservationId, List<CreateReservationCommand.CourseItem> items) {
        return items.stream()
                .map(item -> {
                    TicketClientResponse ticket = ticketClient.getCoursePrice(item.courseId()).data();
                    ReservationCourse course = ReservationCourse.create(
                            reservationId, item.courseId(), item.quantity(), Money.of(ticket.unitPrice())
                    );
                    return reservationCourseRepository.save(course);
                })
                .toList();
    }

    /**
     * courses == null  → 기존 코스 유지 (DB 재조회) courses == []    → 전체 삭제 courses != []    → 전체 교체 (삭제 후 새로 저장)
     */
    private List<ReservationCourse> updateCourses(UUID reservationId, List<ModifyReservationCommand.CourseItem> items) {
        if (items == null) {
            return reservationCourseRepository.findAllByReservationId(reservationId);
        }
        reservationCourseRepository.deleteAllByReservationId(reservationId);
        return items.stream()
                .map(item -> {
                    TicketClientResponse ticket = ticketClient.getCoursePrice(item.courseId()).data();
                    ReservationCourse course = ReservationCourse.create(
                            reservationId, item.courseId(), item.quantity(), Money.of(ticket.unitPrice())
                    );
                    return reservationCourseRepository.save(course);
                })
                .toList();
    }

    private ReservationResult toResult(Reservation reservation, List<ReservationCourse> courses) {
        List<ReservationCourseResult> courseResults = courses.stream()
                .map(ReservationCourseResult::from)
                .toList();
        return ReservationResult.of(reservation, courseResults);
    }

    private boolean isPrivileged(String userRole) {
        return "OWNER".equalsIgnoreCase(userRole) || "MASTER".equalsIgnoreCase(userRole);
    }
}
