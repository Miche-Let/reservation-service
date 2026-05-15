export const BASE_URL = 'http://localhost:19500';

export const FIXED = {
  RESTAURANT_ID: '22222222-2222-2222-2222-222222222222',
  TIME_SLOT_ID:  '33333333-3333-3333-3333-333333333333',
  COURSE_ID:     '44444444-4444-4444-4444-444444444444',
};

/**
 * VU 번호 기반 고유 userId.
 * 형식: 00000000-0000-0000-{VU 4자리 hex}-000000000001
 * 최대 65535 VU 지원. 동일 VU 내에서는 항상 같은 userId → 중복 예약 방지는 날짜로 처리.
 */
export function vuUserId() {
  const hex = __VU.toString(16).padStart(4, '0');
  return `00000000-0000-0000-${hex}-000000000001`;
}

/**
 * setup()에서 사용하는 고정 userId.
 * 형식: 00000000-0000-0000-0000-{index 12자리}
 * VU userId(4번째 그룹 변동)와 형식이 달라 충돌 없음.
 */
export function setupUserId(index) {
  return `00000000-0000-0000-0000-${String(index).padStart(12, '0')}`;
}

// setup() 호출 시각 기반 날짜 오프셋 — 5분 단위로 변화해 재실행 시 날짜 충돌 방지
const SETUP_DATE_SEED = Math.floor(Date.now() / 300000); // 5분마다 증가

export function setupReservedDate(index) {
  return futureDate(30 + (SETUP_DATE_SEED + index) % 700);
}

/**
 * 오늘로부터 offsetDays 후의 날짜를 yyyy-MM-dd 형식으로 반환.
 * 부하테스트 중 중복 예약(409) 방지를 위해 VU별 날짜를 분산할 때 사용.
 */
export function futureDate(offsetDays) {
  const d = new Date();
  d.setDate(d.getDate() + offsetDays);
  return d.toISOString().slice(0, 10);
}

/**
 * 현재 VU + 이터레이션 기반으로 예약 날짜 오프셋을 계산.
 * 30일 뒤를 기준으로 이터레이션마다 다른 날짜를 사용해 같은 VU의 반복 409를 방지.
 * 700일로 설정: write-heavy 80 VU × 5분에서 VU당 최대 ~885 create 시도 시
 * 날짜 순환이 발생하지 않도록 365 → 700으로 확장.
 */
export function vuReservedDate() {
  return futureDate(30 + (__ITER % 700));
}

/**
 * 요청별 고유 멱등성 키. 캐시 히트 없이 실제 처리 성능을 측정하기 위해 매번 다르게 생성.
 */
export function idempotencyKey() {
  return `vu${__VU}-iter${__ITER}-${Date.now()}`;
}

export function commonHeaders(userId) {
  return {
    'Content-Type': 'application/json',
    'X-User-Id': userId,
    'X-User-Role': 'USER',
  };
}

export function reservationBody(userId) {
  return JSON.stringify({
    restaurantId:  FIXED.RESTAURANT_ID,
    timeSlotId:    FIXED.TIME_SLOT_ID,
    reservedDate:  vuReservedDate(),
    slotStartTime: '18:00:00',
    guestCount:    2,
    courses: [{ courseId: FIXED.COURSE_ID, quantity: 1, unitPrice: 50000 }],
  });
}
