# Local HTTP Client Test Guide

IntelliJ HTTP Client 기반 로컬 통합 테스트 가이드.
모든 서비스가 Eureka에 등록되고, FeignClient는 `lb://` 로드밸런싱으로 내부 서비스를 호출합니다.

단, 이는 예약 생성 시나리오, 타 시나리오상 예약 서비스가 활용되는 내부 api 만한정하여 작성함.

---

## 환경 설정

### 서비스 설정 및 Java 코드 변경

# waiting-service

    application.yml
    - Eureka 클라이언트 설정 추가 (서비스 등록 및 검색)
    - port 19200
    build.gradle
    - Spring Cloud Netflix Eureka Client 의존성 추가

# user-service

    application.yml 설정

    eureka:
      client:
        service-url:
          defaultZone: http://localhost:${EUREKA_PORT:8761}/eureka/
    
    spring:
      datasource:
        url: jdbc:postgresql://localhost:${POSTGRES_PORT:5432}/${POSTGRES_DB:michelet_db}
        username: ${POSTGRES_USER:admin}
        password: ${POSTGRES_PASSWORD:admin}
        driver-class-name: org.postgresql.Driver
      data:
        redis:
          host: localhost
          port: ${REDIS_PORT:6379}
    
      jpa:
        hibernate:
          ddl-auto: create
        properties:
          hibernate:
            default_schema: user_service
            format_sql: true
        show-sql: true

# timeslot-service

    application.yml 설정

    eureka:
      client:
        service-url:
          defaultZone: http://localhost:${EUREKA_PORT:8761}/eureka/
    
    spring:
      datasource:
        url: jdbc:postgresql://localhost:${POSTGRES_PORT:5432/${POSTGRES_DB:michelet_db}
        username: ${POSTGRES_USER:admin}
        password: ${POSTGRES_PASSWORD:admin}
        driver-class-name: org.postgresql.Driver
    
      jpa:
        hibernate:
          ddl-auto: create
        properties:
          hibernate:
            default_schema: timeslot_service
            format_sql: true
        show-sql: true

# restaurant-service

    application.yml 설정

    eureka:
      client:
        service-url:
          defaultZone: http://localhost:${EUREKA_PORT:8761}/eureka/
    
    spring:
      datasource:
        url: jdbc:postgresql://localhost:${POSTGRES_PORT:5432/${POSTGRES_DB:michelet_db}
        username: ${POSTGRES_USER:admin}
        password: ${POSTGRES_PASSWORD:admin}
        driver-class-name: org.postgresql.Driver
    
      jpa:
        hibernate:
          ddl-auto: create
        properties:
          hibernate:
            default_schema: restaurant_service
            format_sql: true
        show-sql: true

---

## 실행 순서

### 1. 인프라 및 DB 준비

```bash
# 1. 인프라 시작 (PostgreSQL, Redis 등)
cd infra && docker-compose -f docker-compose.infra.yml up -d

# 2. 테스트 데이터 리셋 및 시드 투입 (필수)
docker exec -i db psql -U admin -d michelet_db < http-tests/sql/reset_test_data.sql
docker exec -i db psql -U admin -d michelet_db < http-tests/sql/seed_test_data.sql
```

### 2. 서비스 기동

`start-local.sh`를 사용하거나 별도 터미널에서 순서대로 기동합니다. (Eureka → User → Restaurant → Timeslot → Waiting → Reservation → Gateway)
실행후 Eureka 대시보드(http://localhost:8761)에서 모든 서비스가 `UP` 상태인지 확인합니다.

### 3. 테스트 실행 (IntelliJ HTTP Client)

#### **Scenario 1: 소비자 예약 플로우 (분리 실행)**

1. **`scenario1a_setup.http`**: 회원가입부터 대기열 등록(STEP 6)까지 한 번에 실행.
2. **10~20초 대기**: 스케줄러가 대기 토큰을 `ACTIVE`로 바꿀 때까지 기다립니다.
3. **`scenario1b_reservation.http`**:
    - **STEP 7**을 먼저 실행하여 `ACTIVE` 상태를 확인합니다.
    - 활성화가 확인되면 나머지 단계(예약 생성 등)를 진행합니다.

#### **Scenario 2: 체크인 플로우 (`scenario2_checkin.http`)**

- 시드 데이터의 고정 예약(`b2c3d4e5-...`)을 사용하여 체크인(`COMPLETED`)을 테스트합니다.
- `http-client.env.json`에 미리 설정된 ID를 사용하므로 바로 실행 가능합니다.

#### **Scenario 3: 방문 이력 검증 (`scenario3_history_check.http`)**

- 시드 데이터의 고정 예약(`a1b2c3d4-...`)을 사용합니다.
- 이 데이터는 항상 `CONFIRMED` 상태를 유지하므로, 시나리오 2 실행 여부와 관계없이 항상 성공합니다.
