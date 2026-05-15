#!/usr/bin/env bash
#
# 예약 서비스 k6 부하테스트 실행 스크립트
#
# 사용법:
#   ./k6/run.sh [mixed|write|read|durability] [--clean]
#
# 시나리오:
#   mixed      — 전체 부하테스트, 기본값 (GET 50% / POST 20% / PATCH 10% / GET detail 20%)
#   write      — 쓰기 위주 (POST 80% / PATCH 20%), 피크 80 VU
#   read       — 읽기 위주 (GET list 70% / GET detail 30%), 피크 150 VU
#   durability — 내구성 테스트 (100 VU × 12분 지속, early/late drift 측정)
#
# 옵션:
#   --clean — 컨테이너와 볼륨을 삭제 후 재시작 (DB 초기화, 재실행 시 권장)
#
# 사전 요건:
#   - Docker Desktop 실행 중
#   - k6 설치: brew install k6
#

set -e

SCENARIO=${1:-mixed}
CLEAN=${2:-}
K6_DIR="$(cd "$(dirname "$0")" && pwd)"

# 시나리오 이름 → 파일명 매핑
case "$SCENARIO" in
  mixed)      SCRIPT_FILE="mixed"       ;;
  write)      SCRIPT_FILE="write-heavy" ;;
  read)       SCRIPT_FILE="read-only"   ;;
  durability) SCRIPT_FILE="durability"  ;;
  *)
    echo "✗ 알 수 없는 시나리오: '$SCENARIO'"
    echo "  사용법: $0 [mixed|write|read|durability] [--clean]"
    exit 1
    ;;
esac

# --clean 플래그: 기존 컨테이너 및 볼륨 제거
if [[ "$CLEAN" == "--clean" ]]; then
  echo "▶ 기존 환경 정리..."
  docker compose -f "$K6_DIR/docker-compose.load-test.yml" down -v --remove-orphans
fi

echo "▶ 인프라 시작 (빌드 포함)..."
docker compose -f "$K6_DIR/docker-compose.load-test.yml" up -d --build

echo "▶ reservation-service 준비 대기..."
RETRY=0
MAX_RETRY=30
until curl -sf -o /dev/null \
  -H 'X-User-Id: 00000000-0000-0000-0000-000000000001' \
  -H 'X-User-Role: USER' \
  http://localhost:19500/api/v1/reservations; do
  RETRY=$((RETRY + 1))
  if [[ $RETRY -ge $MAX_RETRY ]]; then
    echo "✗ reservation-service 기동 실패 (${MAX_RETRY}회 재시도 초과)"
    docker compose -f "$K6_DIR/docker-compose.load-test.yml" logs reservation-service | tail -30
    exit 1
  fi
  echo "  대기 중... (${RETRY}/${MAX_RETRY})"
  sleep 5
done

echo "✓ reservation-service 준비 완료"
echo ""
echo "▶ k6 실행: ${SCRIPT_FILE}.js  [시나리오: ${SCENARIO}]"
echo "──────────────────────────────────────────"

k6 run "$K6_DIR/scripts/${SCRIPT_FILE}.js"

echo ""
echo "▶ 테스트 완료. 컨테이너는 계속 실행 중입니다."
echo "   정리하려면: docker compose -f k6/docker-compose.load-test.yml down -v"
