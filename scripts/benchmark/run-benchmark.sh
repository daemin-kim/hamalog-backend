#!/bin/bash
# ============================================
# Hamalog 성능 벤치마크 실행 스크립트
# ============================================
#
# 용도: Gatling 성능 테스트를 실행하고 결과를 분석합니다.
#
# 사용법:
#   ./scripts/benchmark/run-benchmark.sh [시뮬레이션명]
#
# 예시:
#   ./scripts/benchmark/run-benchmark.sh                    # 모든 시뮬레이션 실행
#   ./scripts/benchmark/run-benchmark.sh medication         # 복약 스케줄 시뮬레이션만
#   ./scripts/benchmark/run-benchmark.sh auth               # 인증 시뮬레이션만

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 기본 설정
BASE_URL="${BASE_URL:-http://localhost:8080}"
SIMULATION_TYPE="${1:-all}"

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}  Hamalog Performance Benchmark${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""
echo -e "${YELLOW}Base URL:${NC} $BASE_URL"
echo -e "${YELLOW}Simulation:${NC} $SIMULATION_TYPE"
echo ""

# ============================================
# 1. 사전 조건 확인
# ============================================
echo -e "${BLUE}[1/5] 사전 조건 확인 중...${NC}"

# 서버 상태 확인
if ! curl -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
    echo -e "${RED}❌ 서버가 응답하지 않습니다: $BASE_URL${NC}"
    echo -e "${YELLOW}서버를 먼저 시작하세요: ./gradlew bootRun${NC}"
    exit 1
fi
echo -e "${GREEN}✅ 서버 상태: 정상${NC}"

# 벤치마크 API 활성화 확인
BENCHMARK_HEALTH=$(curl -s "$BASE_URL/api/v1/benchmark/health" 2>/dev/null || echo "inactive")
if [[ "$BENCHMARK_HEALTH" != *"active"* ]]; then
    echo -e "${RED}❌ 벤치마크 API가 비활성화되어 있습니다.${NC}"
    echo -e "${YELLOW}application.properties에 spring.profiles.active=dev,benchmark 설정하세요.${NC}"
    exit 1
fi
echo -e "${GREEN}✅ 벤치마크 API: 활성화${NC}"

# ============================================
# 2. 테스트 데이터 확인
# ============================================
echo ""
echo -e "${BLUE}[2/5] 테스트 데이터 확인 중...${NC}"

# 벤치마크 사용자 토큰 획득 시도
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"loginId":"benchmark@test.com","password":"Benchmark1234!"}' 2>/dev/null || echo '{"error":"failed"}')

if [[ "$LOGIN_RESPONSE" == *"accessToken"* ]]; then
    echo -e "${GREEN}✅ 벤치마크 사용자 로그인: 성공${NC}"
else
    echo -e "${YELLOW}⚠️ 벤치마크 사용자가 없습니다. 테스트 데이터를 생성하세요:${NC}"
    echo -e "${YELLOW}   mysql -u root -p hamalog < scripts/benchmark/load-test-data.sql${NC}"
    echo ""
fi

# ============================================
# 3. Gatling 시뮬레이션 실행
# ============================================
echo ""
echo -e "${BLUE}[3/5] Gatling 시뮬레이션 실행 중...${NC}"

cd "$PROJECT_ROOT"

case "$SIMULATION_TYPE" in
    "medication")
        echo -e "${YELLOW}복약 스케줄 시뮬레이션 실행...${NC}"
        ./gradlew gatlingRun \
            -Dgatling.simulationClass=com.Hamalog.simulation.MedicationScheduleSimulation \
            -DbaseUrl="$BASE_URL" \
            --no-daemon
        ;;
    "auth")
        echo -e "${YELLOW}인증 시뮬레이션 실행...${NC}"
        ./gradlew gatlingRun \
            -Dgatling.simulationClass=com.Hamalog.simulation.AuthenticationSimulation \
            -DbaseUrl="$BASE_URL" \
            --no-daemon
        ;;
    "all"|*)
        echo -e "${YELLOW}모든 시뮬레이션 실행...${NC}"
        ./gradlew gatlingRun \
            -DbaseUrl="$BASE_URL" \
            --no-daemon
        ;;
esac

# ============================================
# 4. 결과 분석
# ============================================
echo ""
echo -e "${BLUE}[4/5] 결과 분석 중...${NC}"

# 최신 결과 디렉토리 찾기
LATEST_REPORT=$(ls -td "$PROJECT_ROOT/build/reports/gatling/"* 2>/dev/null | head -1)

if [ -n "$LATEST_REPORT" ]; then
    echo -e "${GREEN}✅ 리포트 생성 완료: $LATEST_REPORT${NC}"

    # 결과 요약 출력
    if [ -f "$LATEST_REPORT/js/stats.json" ]; then
        echo ""
        echo -e "${BLUE}========== 성능 요약 ==========${NC}"

        # jq가 설치되어 있으면 JSON 파싱
        if command -v jq &> /dev/null; then
            MEAN_RESPONSE=$(jq -r '.stats.meanResponseTime.total' "$LATEST_REPORT/js/stats.json" 2>/dev/null || echo "N/A")
            P95_RESPONSE=$(jq -r '.stats.percentiles3.total' "$LATEST_REPORT/js/stats.json" 2>/dev/null || echo "N/A")
            SUCCESS_RATE=$(jq -r '.stats.numberOfRequests.ok / .stats.numberOfRequests.total * 100' "$LATEST_REPORT/js/stats.json" 2>/dev/null || echo "N/A")

            echo -e "평균 응답 시간: ${GREEN}${MEAN_RESPONSE}ms${NC}"
            echo -e "P95 응답 시간:  ${GREEN}${P95_RESPONSE}ms${NC}"
            echo -e "성공률:         ${GREEN}${SUCCESS_RATE}%${NC}"
        else
            echo -e "${YELLOW}jq가 설치되어 있지 않아 상세 분석을 건너뜁니다.${NC}"
            echo -e "${YELLOW}설치: brew install jq${NC}"
        fi

        echo -e "${BLUE}================================${NC}"
    fi
else
    echo -e "${RED}❌ 리포트를 찾을 수 없습니다.${NC}"
fi

# ============================================
# 5. 리포트 열기
# ============================================
echo ""
echo -e "${BLUE}[5/5] 리포트 열기...${NC}"

if [ -n "$LATEST_REPORT" ] && [ -f "$LATEST_REPORT/index.html" ]; then
    echo -e "${GREEN}브라우저에서 리포트를 엽니다...${NC}"

    # macOS
    if [[ "$OSTYPE" == "darwin"* ]]; then
        open "$LATEST_REPORT/index.html"
    # Linux
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        xdg-open "$LATEST_REPORT/index.html" 2>/dev/null || echo "리포트: $LATEST_REPORT/index.html"
    else
        echo "리포트 경로: $LATEST_REPORT/index.html"
    fi
fi

echo ""
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}  벤치마크 완료!${NC}"
echo -e "${GREEN}============================================${NC}"

