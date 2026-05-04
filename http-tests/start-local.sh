#!/usr/bin/env bash
# Miche-Let 로컬 서비스 기동 스크립트
# 사용법: ./start-local.sh [서비스명]
# 예시:   ./start-local.sh user-service
#         ./start-local.sh all  (모든 서비스 순서대로 기동)

set -e

# infra .env 로드
ENV_FILE="$(dirname "$0")/infra/.env"
if [ -f "$ENV_FILE" ]; then
  export $(grep -v '^#' "$ENV_FILE" | xargs)
fi

ROOT="$(dirname "$0")"

run_service() {
  local svc=$1
  local profile=${2:-local}
  local extra_args=${3:-}

  echo "▶ Starting $svc (profile=$profile)..."
  cd "$ROOT/$svc"
  chmod +x gradlew

  if [ -n "$extra_args" ]; then
    ./gradlew bootRun --args="--spring.profiles.active=$profile $extra_args" &
  else
    ./gradlew bootRun --args="--spring.profiles.active=$profile" &
  fi
  cd "$ROOT"
}

case "${1:-}" in
  user-service)
    run_service user-service local
    ;;
  restaurant-service)
    run_service restaurant-service local
    ;;
  timeslot-service)
    run_service timeslot-service
    ;;
  waiting-service)
    run_service waiting-service
    ;;
  reservation-service)
    run_service reservation-service local
    ;;
  api-gateway)
    run_service api-gateway local
    ;;
  all)
    echo "=== 전체 서비스 기동 (Eureka + Docker 인프라가 이미 실행 중이어야 합니다) ==="
    run_service user-service local
    sleep 5
    run_service restaurant-service local
    run_service timeslot-service
    run_service waiting-service
    sleep 5
    run_service reservation-service local
    sleep 5
    run_service api-gateway local
    echo "=== 모든 서비스 기동 완료. Eureka 확인: http://localhost:8761 ==="
    wait
    ;;
  *)
    echo "사용법: $0 [user-service|restaurant-service|timeslot-service|waiting-service|reservation-service|api-gateway|all]"
    echo ""
    echo "각 터미널에서 개별 실행:"
    echo "  source infra/.env && cd user-service && ./gradlew bootRun --args='--spring.profiles.active=local'"
    echo "  source infra/.env && cd restaurant-service && ./gradlew bootRun --args='--spring.profiles.active=local'"
    echo "  source infra/.env && cd timeslot-service && ./gradlew bootRun"
    echo "  source infra/.env && cd waiting-service && ./gradlew bootRun"
    echo "  source infra/.env && cd reservation-service && ./gradlew bootRun --args='--spring.profiles.active=local'"
    echo "  source infra/.env && cd api-gateway && ./gradlew bootRun --args='--spring.profiles.active=local'"
    ;;
esac
