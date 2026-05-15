/**
 * 시나리오: Write-Heavy
 *
 * 목적: 쓰기 경로(POST 예약 생성, PATCH 예약 수정)의 한계를 집중 측정한다.
 *       JPA 트랜잭션, Outbox INSERT, Redis 멱등성 저장이 포함된 경로에서
 *       0.5 CPU / -Xmx256m 제약 하의 GC 압박과 스레드 경합을 관찰한다.
 *
 * 엔드포인트 비율:
 *   80% — POST  /api/v1/reservations       (예약 생성)
 *   20% — PATCH /api/v1/reservations/{id}  (예약 수정)
 *
 * 부하 프로파일:
 *   Warm-up  :  5 VU, 30s
 *   Ramp-up  :  5 → 40 VU, 60s
 *   Steady   : 40 VU, 2m
 *   Peak     : 40 → 80 VU, 30s
 *   Peak hold: 80 VU, 1m
 *   Ramp-down: 80 → 0 VU, 30s
 *   총 소요  : ~5분
 *
 * 실행: k6 run k6/scripts/write-heavy.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import {
  BASE_URL, FIXED, setupUserId, setupReservedDate,
  vuUserId, vuReservedDate, idempotencyKey, commonHeaders,
} from './helpers/uuid.js';

// 중복 예약·낙관적 잠금 409는 예상 응답 — http_req_failed 집계에서 제외
http.setResponseCallback(http.expectedStatuses({ min: 200, max: 299 }, 409));

const createLatency = new Trend('write_create_duration', true);
const modifyLatency = new Trend('write_modify_duration', true);
const errorRate     = new Rate('write_error_rate');
const conflictCount = new Counter('write_409_conflicts');

export const options = {
  // cold JVM에서 setup() 내 create 10건이 건당 ~6s 걸릴 수 있으므로 기본값(60s) 초과 방지
  setupTimeout: '3m',
  stages: [
    { duration: '30s', target: 5  },
    { duration: '1m',  target: 40 },
    { duration: '2m',  target: 40 },
    { duration: '30s', target: 80 },
    { duration: '1m',  target: 80 },
    { duration: '30s', target: 0  },
  ],
  thresholds: {
    http_req_duration:     ['p(95)<1000', 'p(99)<2000'],
    http_req_failed:       ['rate<0.01'],
    write_create_duration: ['p(95)<800'],
    write_modify_duration: ['p(95)<800'],
    write_error_rate:      ['rate<0.01'],
  },
};

export function setup() {
  // 수정 테스트용 예약 10건 미리 생성
  const reservations = [];
  for (let i = 0; i < 10; i++) {
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

// VU별 직접 생성한 예약 — 소유권 보장으로 수정 테스트에 재사용
const myReservations = [];

export default function (data) {
  if (Math.random() < 0.80) {
    doCreate();
  } else {
    doModify(data.reservations);
  }
  sleep(0.2);
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

  // 이후 수정 테스트용으로 VU 로컬 상태에 저장
  const created = res.json('data');
  if (created && created.reservationId) {
    myReservations.push({ id: created.reservationId, userId });
    if (myReservations.length > 20) myReservations.shift();
  }
}

function doModify(setupReservations) {
  // 직접 생성한 예약 우선 (소유권 보장), 없으면 setup 예약 사용
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
    conflictCount.add(1);
    return;
  }

  const ok = check(res, { 'modify: 200': (r) => r.status === 200 });
  if (!ok) errorRate.add(1);
}
