-- ============================================================
-- Miche-Let 로컬 테스트 시드 데이터
-- 대상 DB: michelet_db (PostgreSQL)
-- 사전 조건: 모든 서비스를 최소 1회 기동하여 Hibernate DDL로 테이블 생성 완료
--
-- 실행 방법:
--   docker exec -i db psql -U admin -d michelet_db \
--     < http-tests/sql/seed_test_data.sql
--
-- 멱등성: ON CONFLICT DO NOTHING 처리 — 중복 실행 안전
-- ============================================================

-- BCrypt 해시 함수(crypt, gen_salt) 사용을 위해 pgcrypto 활성화
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 시스템 생성자 UUID (시드 데이터용 더미 created_by)
-- created_by 컬럼이 NOT NULL이므로 시드 전용 UUID를 사용
-- 실제 서비스에서는 AuditorConfig가 JWT에서 추출한 userId를 자동 주입함

-- ============================================================
-- 1. 유저 (user_service.p_users)
--    비밀번호: Test123!@#  (BCrypt strength 10)
--
--  ※ testuser01, testuser02는 HTTP 시나리오(signup 단계)가 직접 생성합니다.
--     여기서 삽입하면 시나리오 STEP 1(회원가입)이 409 충돌로 실패합니다.
--     owneruser01만 사전 삽입합니다 (레스토랑의 owner_id 참조 필요).
-- ============================================================
INSERT INTO user_service.p_users (
    id,
    login_id,
    password,
    name,
    email,
    phone,
    role,
    status,
    last_login_at,
    created_at,
    created_by,
    updated_at,
    updated_by,
    deleted_at,
    deleted_by
) VALUES (
    'ffa8b6c9-cc64-41e8-a1b0-2b0622489b78',
    'owneruser01',
    crypt('Test123!@#', gen_salt('bf', 10)),
    '오너유저',
    'owneruser01@example.com',
    '010-0000-0001',
    'OWNER',
    'ACTIVE',
    NULL,
    NOW(),
    'ffa8b6c9-cc64-41e8-a1b0-2b0622489b78',  -- self-reference (seed 데이터)
    NOW(),
    NULL,
    NULL,
    NULL
)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 2. 레스토랑 (restaurant_service.p_restaurant)
-- ============================================================
INSERT INTO restaurant_service.p_restaurant (
    restaurant_id,
    owner_id,
    name,
    address,
    phone,
    description,
    reservation_open_at,
    avg_meal_duration_min,
    status,
    business_hours,
    created_at,
    created_by,
    updated_at,
    updated_by,
    deleted_at,
    deleted_by
) VALUES (
    '2589a05e-db8f-4e62-bb3d-22ce85b750a3',
    'ffa8b6c9-cc64-41e8-a1b0-2b0622489b78',
    '테스트 레스토랑',
    '서울시 강남구 테헤란로 123',
    '02-1234-5678',
    '로컬 통합 테스트용 레스토랑입니다.',
    '09:00',
    60,
    'OPEN',
    '화-일 18:00-22:00 (월요일 정기 휴무)',
    NOW(),
    'ffa8b6c9-cc64-41e8-a1b0-2b0622489b78',
    NOW(),
    NULL,
    NULL,
    NULL
)
ON CONFLICT (restaurant_id) DO NOTHING;

-- ============================================================
-- 3. 코스 (restaurant_service.p_restaurant_course)
-- ============================================================
INSERT INTO restaurant_service.p_restaurant_course (
    course_id,
    restaurant_id,
    name,
    price,
    menu_composition,
    session_type,
    status,
    created_at,
    created_by,
    updated_at,
    updated_by,
    deleted_at,
    deleted_by
) VALUES (
    '7d376f47-8fb3-4be1-916f-4ee0e6ee8b3b',
    '2589a05e-db8f-4e62-bb3d-22ce85b750a3',
    '프리미엄 코스',
    80000,
    '전채 → 생선 요리 → 메인 → 디저트',
    'DINNER',
    'AVAILABLE',
    NOW(),
    'ffa8b6c9-cc64-41e8-a1b0-2b0622489b78',
    NOW(),
    NULL,
    NULL,
    NULL
)
ON CONFLICT (course_id) DO NOTHING;

-- ============================================================
-- 4. 코스 메뉴 (restaurant_service.p_restaurant_course_menu)
--    (course_id, sort_order) 유니크 제약으로 중복 방지
-- ============================================================
INSERT INTO restaurant_service.p_restaurant_course_menu (
    course_menu_id,
    course_id,
    course_part,
    menu_name,
    sort_order,
    created_at,
    created_by,
    updated_at,
    updated_by,
    deleted_at,
    deleted_by
) VALUES
(
    gen_random_uuid(),
    '7d376f47-8fb3-4be1-916f-4ee0e6ee8b3b',
    'APPETIZER', '모듬 샐러드', 1,
    NOW(), 'ffa8b6c9-cc64-41e8-a1b0-2b0622489b78', NOW(), NULL, NULL, NULL
),
(
    gen_random_uuid(),
    '7d376f47-8fb3-4be1-916f-4ee0e6ee8b3b',
    'FISH', '연어 카르파쵸', 2,
    NOW(), 'ffa8b6c9-cc64-41e8-a1b0-2b0622489b78', NOW(), NULL, NULL, NULL
),
(
    gen_random_uuid(),
    '7d376f47-8fb3-4be1-916f-4ee0e6ee8b3b',
    'MAIN', '안심 스테이크', 3,
    NOW(), 'ffa8b6c9-cc64-41e8-a1b0-2b0622489b78', NOW(), NULL, NULL, NULL
),
(
    gen_random_uuid(),
    '7d376f47-8fb3-4be1-916f-4ee0e6ee8b3b',
    'DESSERT', '티라미수', 4,
    NOW(), 'ffa8b6c9-cc64-41e8-a1b0-2b0622489b78', NOW(), NULL, NULL, NULL
)
ON CONFLICT ON CONSTRAINT uk_restaurant_course_menu_course_id_sort_order DO NOTHING;

-- ============================================================
-- 5. 타임슬롯 (timeslot_service.p_time_slot) — 2026년 6월 전체
--    하루 4슬롯: 18:00 / 19:00 / 20:00 / 21:00 (1시간 단위)
--    수용 10명, OPENED, version=0
--    (restaurant_id, target_date, start_time) 유니크 제약으로 중복 방지
-- ============================================================
INSERT INTO timeslot_service.p_time_slot (
    time_slot_id,
    restaurant_id,
    target_date,
    start_time,
    end_time,
    capacity,
    remaining_capacity,
    status,
    version,
    created_at,
    created_by,
    updated_at,
    updated_by,
    deleted_at,
    deleted_by
)
SELECT
    gen_random_uuid(),
    '2589a05e-db8f-4e62-bb3d-22ce85b750a3'::UUID,
    d::DATE,
    t::TIME,
    (t::TIME + INTERVAL '1 hour'),
    10,
    10,
    'OPENED',
    0,
    NOW(),
    'ffa8b6c9-cc64-41e8-a1b0-2b0622489b78'::UUID,
    NOW(),
    NULL,
    NULL,
    NULL
FROM
    generate_series('2026-06-01'::DATE, '2026-06-30'::DATE, '1 day') AS d,
    (VALUES ('18:00'), ('19:00'), ('20:00'), ('21:00')) AS times(t)
ON CONFLICT ON CONSTRAINT uk_time_slot_restaurant_date_start DO NOTHING;

-- ============================================================
-- 6. 예약 (reservation_service.p_reservations) — 시나리오 3 검증용 CONFIRMED 데이터
--    owneruser01이 본인 식당에 예약한 데이터로 생성 (임시)
-- ============================================================
INSERT INTO reservation_service.p_reservations (
    id,
    user_id,
    restaurant_id,
    time_slot_id,
    reserved_date,
    guest_count,
    status,
    cancel_deadline,
    modify_deadline,
    noshow_deadline,
    checked_in_at,
    created_at,
    created_by,
    updated_at,
    updated_by,
    deleted_at,
    deleted_by
) VALUES 
(
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890', -- 시나리오 3 전용 (CONFIRMED 유지)
    'ffa8b6c9-cc64-41e8-a1b0-2b0622489b78',  -- owneruser01
    '2589a05e-db8f-4e62-bb3d-22ce85b750a3',  -- 테스트 레스토랑
    (SELECT time_slot_id FROM timeslot_service.p_time_slot WHERE target_date = '2026-06-14' AND start_time = '18:00' LIMIT 1),
    '2026-06-14',
    2,
    'CONFIRMED',
    '2026-06-12',
    '2026-06-12',
    '2026-06-14 18:30:00',
    NULL,
    NOW(),
    'ffa8b6c9-cc64-41e8-a1b0-2b0622489b78',
    NOW(),
    NULL,
    NULL,
    NULL
),
(
    'b2c3d4e5-f6a7-8901-bcde-f01234567891', -- 시나리오 2 전용 (체크인 테스트용)
    'ffa8b6c9-cc64-41e8-a1b0-2b0622489b78',
    '2589a05e-db8f-4e62-bb3d-22ce85b750a3',
    (SELECT time_slot_id FROM timeslot_service.p_time_slot WHERE target_date = '2026-06-14' AND start_time = '19:00' LIMIT 1),
    '2026-06-14',
    4,
    'CONFIRMED',
    '2026-06-12',
    '2026-06-12',
    '2026-06-14 19:30:00',
    NULL,
    NOW(),
    'ffa8b6c9-cc64-41e8-a1b0-2b0622489b78',
    NOW(),
    NULL,
    NULL,
    NULL
)
ON CONFLICT (id) DO NOTHING;

-- 결과 확인
SELECT 'p_users'                    AS "테이블", COUNT(*) AS "행 수" FROM user_service.p_users
UNION ALL
SELECT 'p_restaurant',              COUNT(*) FROM restaurant_service.p_restaurant
UNION ALL
SELECT 'p_restaurant_course',       COUNT(*) FROM restaurant_service.p_restaurant_course
UNION ALL
SELECT 'p_restaurant_course_menu',  COUNT(*) FROM restaurant_service.p_restaurant_course_menu
UNION ALL
SELECT 'p_time_slot',               COUNT(*) FROM timeslot_service.p_time_slot
UNION ALL
SELECT 'p_reservations',            COUNT(*) FROM reservation_service.p_reservations;
