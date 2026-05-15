/**
 * 시나리오: Read-Only
 *
 * 목적: 읽기 경로(GET 목록 조회, GET 상세 조회)의 한계를 집중 측정한다.
 *       쓰기 없이 DB 읽기 경로만 집중 부하를 주어
 *       커넥션 풀 포화, 페이지네이션 쿼리 성능, tail latency 패턴을 관찰한다.
 *
 * 엔드포인트 비율:
 *   70% — GET /api/v1/reservations        (목록 조회)
 *   30% — GET /api/v1/reservations/{id}   (상세 조회)
 *
 * 부하 프로파일:
 *   Warm-up  :  10 VU, 30s
 *   Ramp-up  :  10 → 80 VU, 60s
 *   Steady   :  80 VU, 2m
 *   Peak     :  80 → 150 VU, 60s
 *   Peak hold: 150 VU, 2m
 *   Ramp-down: 150 → 0 VU, 30s
 *   총 소요  : ~6분
 *
 * sleep: 0.5s → 0.1s (더 가혹한 읽기 부하)
 *   원본(0.5s): 실효 동시 요청 ≈ 4건 (150 VU × 14ms / 514ms)
 *   변경(0.1s): 실효 동시 요청 ≈ 17건 (150 VU × 14ms / 114ms) — 약 4배 증가
 *   목적: 읽기 경로의 실제 처리 한계(커넥션 포화 임계점)를 탐색한다.
 *
 * 실행: k6 run k6/scripts/read-only.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import {
  BASE_URL, FIXED, setupUserId, setupReservedDate,
  vuUserId, commonHeaders,
} from './helpers/uuid.js';

const listLatency   = new Trend('read_list_duration',   true);
const detailLatency = new Trend('read_detail_duration', true);
const errorRate     = new Rate('read_error_rate');

export const options = {
  // cold JVM에서 setup() 내 create 30건이 건당 ~6s 걸릴 수 있으므로 기본값(60s) 초과
  setupTimeout: '3m',
  stages: [
    { duration: '30s', target: 10  },
    { duration: '1m',  target: 80  },
    { duration: '2m',  target: 80  },
    { duration: '1m',  target: 150 },
    { duration: '2m',  target: 150 },
    { duration: '30s', target: 0   },
  ],
  thresholds: {
    http_req_duration:    ['p(95)<500', 'p(99)<1000'],
    http_req_failed:      ['rate<0.01'],
    read_list_duration:   ['p(95)<300'],
    read_detail_duration: ['p(95)<300'],
    read_error_rate:      ['rate<0.01'],
  },
};

export function setup() {
  // 상세 조회 테스트용 예약 30건 미리 생성
  const reservations = [];
  for (let i = 0; i < 30; i++) {
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

export default function (data) {
  if (Math.random() < 0.70) {
    doGetList();
  } else {
    doGetDetail(data.reservations);
  }
  sleep(0.1);
}

function doGetList() {
  const res = http.get(`${BASE_URL}/api/v1/reservations?page=0&size=10`, {
    headers: commonHeaders(vuUserId()),
  });
  listLatency.add(res.timings.duration);
  const ok = check(res, { 'list: 200': (r) => r.status === 200 });
  if (!ok) errorRate.add(1);
}

function doGetDetail(setupReservations) {
  if (setupReservations.length === 0) return;
  const target = setupReservations[__ITER % setupReservations.length];
  const res    = http.get(`${BASE_URL}/api/v1/reservations/${target.id}`, {
    headers: commonHeaders(target.userId),
  });
  detailLatency.add(res.timings.duration);
  const ok = check(res, { 'detail: 200': (r) => r.status === 200 });
  if (!ok) errorRate.add(1);
}
