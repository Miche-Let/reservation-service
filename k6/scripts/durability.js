/**
 * 시나리오 4: Durability (내구성)
 *
 * 목적: 인덱스 3개 추가 이후 100 VU × 12분 지속 부하에서
 *       - 쓰기 성능 drift (인덱스 B-tree 유지 비용 누적)
 *       - 읽기 안정성 (index scan 지속성)
 *       - GC 압박 누적 (latency 분포 변화로 추론)
 *       를 관찰한다.
 *
 * drift 측정 방법:
 *   steady 12분 구간을 3개 창으로 분리해 지표를 별도 수집한다.
 *   창 경계는 setup() 반환 시각(startTime)을 기준으로 계산한다.
 *
 *   warm   : startTime + 0    ~ +150s  (warmup + ramp-up — 측정 제외)
 *   early  : startTime + 150s ~ +450s  (steady 전반 5분)
 *   mid    : startTime + 450s ~ +600s  (전환 구간)
 *   late   : startTime + 600s ~        (steady 후반 5분 이상)
 *
 * 부하 프로파일:
 *   Warm-up  :  20 VU, 90s
 *   Ramp-up  :  20 → 100 VU, 60s
 *   Steady   : 100 VU, 12m   ← 핵심 관찰 구간
 *   Ramp-down: 100 → 0 VU, 30s
 *   총 소요  : ~15분
 *
 * 실행: ./k6/run.sh durability [--clean]
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

// ── 전체 구간 통합 지표 ──────────────────────────────────────
const listAll   = new Trend('dur_list_all',   true);
const detailAll = new Trend('dur_detail_all', true);
const createAll = new Trend('dur_create_all', true);
const modifyAll = new Trend('dur_modify_all', true);
const errorRate = new Rate('dur_error_rate');
const conflicts = new Counter('dur_409_conflicts');

// ── 창별 쓰기 지표 (drift 감지 핵심) ─────────────────────────
const createEarly = new Trend('dur_create_early', true);
const createLate  = new Trend('dur_create_late',  true);
const modifyEarly = new Trend('dur_modify_early', true);
const modifyLate  = new Trend('dur_modify_late',  true);

// ── 창별 읽기 지표 (안정성 확인) ─────────────────────────────
const listEarly = new Trend('dur_list_early', true);
const listLate  = new Trend('dur_list_late',  true);

export const options = {
  // cold JVM에서 setup() 내 create 20건이 건당 ~6s 걸릴 수 있으므로 기본값(60s) 초과
  setupTimeout: '3m',
  stages: [
    { duration: '90s', target: 20  },   // JVM warm-up
    { duration: '60s', target: 100 },   // ramp-up
    { duration: '12m', target: 100 },   // 내구성 관찰 구간
    { duration: '30s', target: 0   },   // ramp-down
  ],
  thresholds: {
    // 전체 구간 기준선
    http_req_duration:  ['p(95)<600', 'p(99)<1200'],
    http_req_failed:    ['rate<0.01'],
    dur_error_rate:     ['rate<0.01'],
    dur_list_all:       ['p(95)<500'],
    dur_create_all:     ['p(95)<900'],
    dur_modify_all:     ['p(95)<900'],

    // drift 허용 한계: late가 early의 1.25배 이내여야 한다
    // (k6 임계값은 단일 수치만 지원하므로 절대값으로 표현)
    dur_create_early:   ['p(95)<800'],
    dur_create_late:    ['p(95)<1000'],  // early 대비 25% drift 허용 (1.25배)
    dur_modify_early:   ['p(95)<800'],
    dur_modify_late:    ['p(95)<1000'],
    dur_list_early:     ['p(95)<400'],
    dur_list_late:      ['p(95)<400'],   // 읽기는 drift 없어야 함
  },
};

export function setup() {
  const startTime = Date.now();

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

  return { startTime, reservations };
}

// VU별 로컬 상태 — 직접 생성한 예약 ID를 수정 테스트에 재사용
const myReservations = [];

/**
 * elapsed 기반 측정 창 반환.
 * 'warm' 구간은 drift 집계에서 제외된다.
 */
function phase(startTime) {
  const elapsed = (Date.now() - startTime) / 1000;
  if (elapsed < 150) return 'warm';
  if (elapsed < 450) return 'early';
  if (elapsed < 600) return 'mid';
  return 'late';
}

export default function (data) {
  const rand = Math.random();

  if (rand < 0.50) {
    doGetList(data);
  } else if (rand < 0.70) {
    doGetDetail(data);
  } else if (rand < 0.90) {
    doCreate(data);
  } else {
    doModify(data);
  }

  sleep(0.3);
}

function doGetList(data) {
  const userId  = vuUserId();
  const res     = http.get(`${BASE_URL}/api/v1/reservations?page=0&size=10`, {
    headers: commonHeaders(userId),
  });
  listAll.add(res.timings.duration);

  const p = phase(data.startTime);
  if (p === 'early') listEarly.add(res.timings.duration);
  if (p === 'late')  listLate.add(res.timings.duration);

  const ok = check(res, { 'list: 200': (r) => r.status === 200 });
  if (!ok) errorRate.add(1);
}

function doGetDetail(data) {
  const { reservations } = data;
  if (reservations.length === 0) return;

  const target  = reservations[__ITER % reservations.length];
  const res     = http.get(`${BASE_URL}/api/v1/reservations/${target.id}`, {
    headers: commonHeaders(target.userId),
  });
  detailAll.add(res.timings.duration);

  const ok = check(res, { 'detail: 200': (r) => r.status === 200 });
  if (!ok) errorRate.add(1);
}

function doCreate(data) {
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
  createAll.add(res.timings.duration);

  if (res.status === 409) {
    conflicts.add(1);
    return;
  }

  const p = phase(data.startTime);
  if (p === 'early') createEarly.add(res.timings.duration);
  if (p === 'late')  createLate.add(res.timings.duration);

  const ok = check(res, { 'create: 201': (r) => r.status === 201 });
  if (!ok) {
    errorRate.add(1);
    return;
  }

  const created = res.json('data');
  if (created && created.reservationId) {
    myReservations.push({ id: created.reservationId, userId });
    if (myReservations.length > 20) myReservations.shift();
  }
}

function doModify(data) {
  const pool = myReservations.length > 0 ? myReservations : data.reservations;
  if (pool.length === 0) return;

  const target  = pool[__ITER % pool.length];
  const headers = {
    ...commonHeaders(target.userId),
    'Idempotency-Key': idempotencyKey(),
  };
  const body = JSON.stringify({ guestCount: 3 });
  const res  = http.patch(`${BASE_URL}/api/v1/reservations/${target.id}`, body, { headers });
  modifyAll.add(res.timings.duration);

  if (res.status === 409) {
    conflicts.add(1);
    return;
  }

  const p = phase(data.startTime);
  if (p === 'early') modifyEarly.add(res.timings.duration);
  if (p === 'late')  modifyLate.add(res.timings.duration);

  const ok = check(res, { 'modify: 200': (r) => r.status === 200 });
  if (!ok) errorRate.add(1);
}
