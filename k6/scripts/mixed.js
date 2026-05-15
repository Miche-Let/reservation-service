/**
 * 시나리오 3: Mixed (메인)
 *
 * 목적: 실제 트래픽 분포를 반영한 통합 성능 측정.
 *       읽기/쓰기가 동시에 경합하는 상황에서 전체 시스템 동작을 관찰한다.
 *
 * 엔드포인트 비율:
 *   50% — GET  /api/v1/reservations          (목록 조회)
 *   20% — GET  /api/v1/reservations/{id}     (상세 조회)
 *   20% — POST /api/v1/reservations          (예약 생성)
 *   10% — PATCH /api/v1/reservations/{id}    (예약 수정)
 *
 * 부하 프로파일:
 *   Warm-up  :  20 VU, 90s   (JVM JIT warm-up — WarmUpRunner + k6 쓰기 경로 합산)
 *   Ramp-up  :  20 → 50 VU, 60s
 *   Steady   :  50 VU, 3m
 *   Peak     :  50 → 100 VU, 60s
 *   Peak hold: 100 VU, 1m
 *   Ramp-down: 100 → 0 VU, 30s
 *   총 소요  : ~8.5분
 *
 * 실행: k6 run k6/scripts/mixed.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import {
  BASE_URL, FIXED, setupUserId, setupReservedDate,
  vuUserId, vuReservedDate, idempotencyKey, commonHeaders,
} from './helpers/uuid.js';

// 409는 중복 예약·낙관적 잠금 충돌로 인한 예상 응답 — http_req_failed 집계에서 제외
http.setResponseCallback(http.expectedStatuses({ min: 200, max: 299 }, 409));

// 엔드포인트별 커스텀 지표
const listLatency   = new Trend('mixed_list_duration',   true);
const detailLatency = new Trend('mixed_detail_duration', true);
const createLatency = new Trend('mixed_create_duration', true);
const modifyLatency = new Trend('mixed_modify_duration', true);
const errorRate     = new Rate('mixed_error_rate');
const conflictCount = new Counter('mixed_409_conflicts');

export const options = {
  stages: [
    { duration: '90s', target: 20  },
    { duration: '1m',  target: 50  },
    { duration: '3m',  target: 50  },
    { duration: '1m',  target: 100 },
    { duration: '1m',  target: 100 },
    { duration: '30s', target: 0   },
  ],
  thresholds: {
    http_req_duration:      ['p(95)<500', 'p(99)<1000'],
    http_req_failed:        ['rate<0.01'],
    mixed_error_rate:       ['rate<0.01'],
    mixed_list_duration:    ['p(95)<300'],
    mixed_detail_duration:  ['p(95)<300'],
    mixed_create_duration:  ['p(95)<800'],
    mixed_modify_duration:  ['p(95)<800'],
  },
};

export function setup() {
  // 상세 조회 / 수정 테스트용 예약 20건 생성
  const reservations = [];
  for (let i = 0; i < 20; i++) {
    const userId = setupUserId(i);
    const body = JSON.stringify({
      restaurantId:  FIXED.RESTAURANT_ID,
      timeSlotId:    FIXED.TIME_SLOT_ID,
      reservedDate:  setupReservedDate(i),
      slotStartTime: '18:00:00',
      guestCount:    2,
      courses: [{ courseId: FIXED.COURSE_ID, quantity: 1, unitPrice: 50000 }],
    });
    const res = http.post(`${BASE_URL}/api/v1/reservations`, body, {
      headers: { ...commonHeaders(userId), 'X-Waiting-Token': 'setup-token' },
    });
    if (res.status === 201) {
      const data = res.json('data');
      if (data && data.reservationId) {
        reservations.push({ id: data.reservationId, userId });
      }
    } else {
      console.error(`setup[${i}]: ${res.status} — ${res.body}`);
    }
  }
  console.log(`setup: ${reservations.length}건 예약 생성 완료`);
  return { reservations };
}

// VU별 로컬 상태 — 직접 생성한 예약 ID를 수정 테스트에 재사용
const myReservations = [];

export default function (data) {
  const rand = Math.random();

  if (rand < 0.50) {
    doGetList();
  } else if (rand < 0.70) {
    doGetDetail(data.reservations);
  } else if (rand < 0.90) {
    doCreate();
  } else {
    doModify(data.reservations);
  }

  sleep(0.3);
}

function doGetList() {
  const userId  = vuUserId();
  const headers = commonHeaders(userId);
  const res     = http.get(`${BASE_URL}/api/v1/reservations?page=0&size=10`, { headers });
  listLatency.add(res.timings.duration);
  const ok = check(res, { 'list: 200': (r) => r.status === 200 });
  if (!ok) errorRate.add(1);
}

function doGetDetail(setupReservations) {
  if (setupReservations.length === 0) return;
  const target  = setupReservations[__ITER % setupReservations.length];
  const headers = commonHeaders(target.userId);
  const res     = http.get(`${BASE_URL}/api/v1/reservations/${target.id}`, { headers });
  detailLatency.add(res.timings.duration);
  const ok = check(res, { 'detail: 200': (r) => r.status === 200 });
  if (!ok) errorRate.add(1);
}

function doCreate() {
  const userId  = vuUserId();
  const headers = {
    ...commonHeaders(userId),
    'Idempotency-Key': idempotencyKey(),
    'X-Waiting-Token': `token-${__VU}`,
  };
  const body = JSON.stringify({
    restaurantId:  FIXED.RESTAURANT_ID,
    timeSlotId:    FIXED.TIME_SLOT_ID,
    reservedDate:  vuReservedDate(),
    slotStartTime: '18:00:00',
    guestCount:    2,
    courses: [{ courseId: FIXED.COURSE_ID, quantity: 1, unitPrice: 50000 }],
  });

  const res = http.post(`${BASE_URL}/api/v1/reservations`, body, { headers });
  createLatency.add(res.timings.duration);

  if (res.status === 409) {
    conflictCount.add(1);
    return;
  }

  const ok = check(res, { 'create: 201': (r) => r.status === 201 });
  if (!ok) {
    errorRate.add(1);
    return;
  }

  // 이후 수정 테스트에서 재사용하기 위해 VU 로컬 상태에 저장
  const created = res.json('data');
  if (created && created.reservationId) {
    myReservations.push({ id: created.reservationId, userId });
    if (myReservations.length > 20) myReservations.shift(); // 메모리 상한
  }
}

function doModify(setupReservations) {
  // 직접 생성한 예약이 있으면 그걸 우선 사용 (소유권 보장)
  // 없으면 setup 예약 사용 (낙관적 잠금 409 발생 가능)
  const pool = myReservations.length > 0 ? myReservations : setupReservations;
  if (pool.length === 0) return;

  const target  = pool[__ITER % pool.length];
  const headers = {
    ...commonHeaders(target.userId),
    'Idempotency-Key': idempotencyKey(),
  };
  const body = JSON.stringify({ guestCount: 3 });
  const res  = http.patch(`${BASE_URL}/api/v1/reservations/${target.id}`, body, { headers });
  modifyLatency.add(res.timings.duration);

  if (res.status === 409) {
    // 낙관적 잠금 충돌 — 다중 VU가 같은 예약을 동시에 수정할 때 발생, 비정상 아님
    conflictCount.add(1);
    return;
  }

  const ok = check(res, { 'modify: 200': (r) => r.status === 200 });
  if (!ok) errorRate.add(1);
}
