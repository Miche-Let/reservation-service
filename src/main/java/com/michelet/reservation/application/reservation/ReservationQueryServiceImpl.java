package com.michelet.reservation.application.reservation;

import com.michelet.common.exception.BusinessException;
import com.michelet.reservation.application.reservation.result.ReservationActiveResult;
import com.michelet.reservation.application.reservation.result.ReservationCourseResult;
import com.michelet.reservation.application.reservation.result.ReservationExistsResult;
import com.michelet.reservation.application.reservation.result.ReservationListCachePage;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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
    // non-final 래퍼 클래스 ReservationListCachePage에 content + totalElements만 저장하고
    // 캐시 히트 시 PageImpl로 재조립한다.
    @Override
    @Transactional(readOnly = true)
    public Page<ReservationSummaryResult> getList(UUID userId, ReservationStatus status, Pageable pageable) {
        // sort 정보를 키에 포함 — 동일 page/size라도 정렬 기준이 다르면 별개 캐시 항목이어야 한다.
        String sortKey = pageable.getSort().isSorted() ? pageable.getSort().toString() : "UNSORTED";
        String key = userId.toString() + ':' + (status != null ? status.name() : "ALL")
                + ':' + pageable.getPageNumber() + ':' + pageable.getPageSize() + ':' + sortKey;
        Cache listCache = cacheManager.getCache("reservation:list");

        if (listCache != null) {
            try {
                ReservationListCachePage hit = listCache.get(key, ReservationListCachePage.class);
                if (hit != null) {
                    return new PageImpl<>(hit.content, pageable, hit.totalElements);
                }
            } catch (RuntimeException e) {
                log.warn("[Cache] list cache get 실패 — key={}", key, e);
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
            try {
                listCache.put(key, new ReservationListCachePage(content, total));
            } catch (RuntimeException e) {
                log.warn("[Cache] list cache put 실패 — key={}", key, e);
            }
        }

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    @Transactional(readOnly = true)
    // userId를 키에 포함 — 캐시 히트 시 verifyAccess()가 실행되지 않으므로
    // 키 자체가 소유권을 보장한다. reservationId만으로 캐싱하면 타 사용자가 UUID를
    // 알 경우 캐시 히트로 무단 조회 가능.
    // eviction은 CommandService에서 #result.userId()를 키로 지정한 단건 DEL(O(1)).
    // MASTER가 타인 예약을 조작할 때도 result에는 실제 소유자 userId가 담기므로 정확히 무효화된다.
    @Cacheable(
            cacheNames = "reservation:detail",
            key = "#userId.toString() + ':' + #reservationId.toString()"
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

}
