package com.michelet.reservation.application.reservation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.michelet.common.exception.BusinessException;
import com.michelet.reservation.application.reservation.result.ReservationActiveResult;
import com.michelet.reservation.application.reservation.result.ReservationCourseResult;
import com.michelet.reservation.application.reservation.result.ReservationExistsResult;
import com.michelet.reservation.application.reservation.result.ReservationResult;
import com.michelet.reservation.application.reservation.result.ReservationSummaryResult;
import com.michelet.reservation.application.reservation.result.ReservationValidityResult;
import com.michelet.reservation.domain.entity.Reservation;
import com.michelet.reservation.domain.enums.ReservationStatus;
import com.michelet.reservation.domain.exception.ReservationErrorCode;
import com.michelet.reservation.domain.repository.ReservationCourseRepository;
import com.michelet.reservation.domain.repository.ReservationRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservationQueryServiceImpl implements ReservationQueryService {

    private final ReservationRepository reservationRepository;
    private final ReservationCourseRepository reservationCourseRepository;
    private final CacheManager cacheManager;

    // @Cacheable이 캐시 히트 시 @Transactional 어드바이스(커넥션 획득)를 건너뛰도록
    // 클래스 레벨이 아닌 메서드 레벨에 선언한다.
    //
    // getList()는 Page<T>를 직접 캐싱하지 않는다.
    // PageImpl에 @JsonCreator가 없어 GenericJackson2JsonRedisSerializer가 역직렬화에 실패하므로
    // non-final 래퍼 클래스 CachedListPage에 content + totalElements만 저장하고
    // 캐시 히트 시 PageImpl로 재조립한다.
    @Override
    @Transactional(readOnly = true)
    public Page<ReservationSummaryResult> getList(UUID userId, ReservationStatus status, Pageable pageable) {
        String key = userId.toString() + ':' + (status != null ? status.name() : "ALL")
                + ':' + pageable.getPageNumber() + ':' + pageable.getPageSize();
        Cache listCache = cacheManager.getCache("reservation:list");

        if (listCache != null) {
            CachedListPage hit = listCache.get(key, CachedListPage.class);
            if (hit != null) {
                return new PageImpl<>(hit.content, pageable, hit.totalElements);
            }
        }

        Page<Reservation> page = (status != null)
                ? reservationRepository.findPageByUserIdAndStatus(userId, status, pageable)
                : reservationRepository.findPageByUserId(userId, pageable);
        List<ReservationSummaryResult> content = page.getContent().stream()
                .map(ReservationSummaryResult::from)
                .toList();
        long total = page.getTotalElements();

        if (listCache != null) {
            listCache.put(key, new CachedListPage(content, total));
        }

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "reservation:detail",
            key = "#reservationId.toString()"
    )
    public ReservationResult getDetail(UUID userId, String userRole, UUID reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        verifyAccess(reservation, userId, userRole);

        List<ReservationCourseResult> courses = reservationCourseRepository
                .findAllByReservationId(reservationId)
                .stream()
                .map(ReservationCourseResult::from)
                .toList();

        return ReservationResult.of(reservation, courses);
    }

    // 이하 메서드는 서비스 간 정확성이 성능보다 중요하므로 캐싱 제외
    @Override
    @Transactional(readOnly = true)
    public ReservationValidityResult checkValidity(UUID userId, UUID restaurantId) {
        return reservationRepository.findTopByUserIdAndRestaurantIdAndStatusInOrderByReservedDateDesc(
                        userId, restaurantId, List.of(ReservationStatus.CONFIRMED, ReservationStatus.COMPLETED))
                .map(ReservationValidityResult::found)
                .orElseGet(ReservationValidityResult::notFound);
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationExistsResult checkExists(UUID reservationId, UUID userId, UUID restaurantId) {
        boolean exists = reservationRepository.findById(reservationId)
                .filter(r -> r.getUserId().equals(userId)
                        && r.getRestaurantId().equals(restaurantId)
                        && r.getStatus() == ReservationStatus.CONFIRMED)
                .isPresent();
        return exists ? ReservationExistsResult.found() : ReservationExistsResult.notFound();
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationActiveResult hasActiveReservation(UUID userId) {
        boolean exists = reservationRepository.existsByUserIdAndStatus(userId, ReservationStatus.CONFIRMED);
        return exists ? ReservationActiveResult.found() : ReservationActiveResult.notFound();
    }

    private void verifyAccess(Reservation reservation, UUID userId, String userRole) {
        if ("MASTER".equalsIgnoreCase(userRole)) {
            return;
        }
        // TODO: OWNER 역할 — restaurant-service RestaurantClient(Feign) 구현 후 소유 식당 예약만 허용
        // 현재 Feign 미구현 → fail-closed: OWNER도 userId 기반 검증 적용 (USER 동일 수준)
        if (!reservation.getUserId().equals(userId)) {
            throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND);
        }
    }

    // Page<T>를 직접 캐싱하면 PageImpl 역직렬화 실패 → content + total만 저장하는 래퍼.
    // non-final class(record 아님)이어야 NON_FINAL typing이 @class를 삽입한다.
    static class CachedListPage {
        public final List<ReservationSummaryResult> content;
        public final long totalElements;

        @JsonCreator
        CachedListPage(
                @JsonProperty("content") List<ReservationSummaryResult> content,
                @JsonProperty("totalElements") long totalElements) {
            this.content = content;
            this.totalElements = totalElements;
        }
    }
}
