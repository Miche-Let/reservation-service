-- reservation-service 스키마 사전 생성
-- ddl-auto=create-drop 으로 Hibernate가 테이블을 자동 생성하지만,
-- 스키마 자체는 Hibernate가 생성하지 않으므로 여기서 선행 생성한다.
CREATE SCHEMA IF NOT EXISTS reservation_service;
