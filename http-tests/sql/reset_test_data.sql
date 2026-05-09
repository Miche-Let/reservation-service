-- ============================================================
-- Miche-Let 테스트 데이터 초기화 (완전 리셋)
-- 기존 시드 데이터를 삭제하고 seed_test_data.sql을 재실행합니다.
--
-- 실행 방법:
--   docker exec -i db psql -U admin -d michelet_db \
--     < http-tests/sql/reset_test_data.sql
--   docker exec -i db psql -U admin -d michelet_db \
--     < http-tests/sql/seed_test_data.sql
--
-- 주의: 이 파일은 시드 데이터만 삭제합니다.
--       다른 유저/레스토랑 데이터가 있으면 함께 지워질 수 있습니다.
-- ============================================================

-- 외래 키 의존 순서대로 삭제 (자식 → 부모)
DELETE FROM reservation_service.p_reservations
WHERE id IN ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'b2c3d4e5-f6a7-8901-bcde-f01234567891');

DELETE FROM timeslot_service.p_time_slot
WHERE restaurant_id = '2589a05e-db8f-4e62-bb3d-22ce85b750a3';

DELETE FROM restaurant_service.p_restaurant_course_menu
WHERE course_id = '7d376f47-8fb3-4be1-916f-4ee0e6ee8b3b';

DELETE FROM restaurant_service.p_restaurant_course
WHERE course_id = '7d376f47-8fb3-4be1-916f-4ee0e6ee8b3b';

DELETE FROM restaurant_service.p_restaurant
WHERE restaurant_id = '2589a05e-db8f-4e62-bb3d-22ce85b750a3';

-- owneruser01 삭제 (seed에서 다시 삽입됨)
DELETE FROM user_service.p_users
WHERE id = 'ffa8b6c9-cc64-41e8-a1b0-2b0622489b78';

-- 시나리오 실행 중 생성된 테스트 유저 삭제 (signup으로 생성되므로 UUID 미확정 → login_id로 삭제)
DELETE FROM user_service.p_users WHERE login_id IN ('testuser01', 'testuser02');

SELECT '초기화 완료 — seed_test_data.sql을 실행하세요.' AS "상태";
