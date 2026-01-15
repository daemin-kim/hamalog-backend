#!/bin/bash
# ============================================
# Hamalog 원격 서버 성능 벤치마크 실행 스크립트
# ============================================
#
# 용도: 프로덕션 서버(https://api.hamalog.shop)를 대상으로 벤치마크 실행
#
# 사전 조건:
# 1. GitHub Actions에서 "Deploy Benchmark Mode" 워크플로우 실행
# 2. BENCHMARK_API_KEY 환경변수 또는 .env.benchmark 파일 설정
#
# 사용법:
#   ./scripts/benchmark/run-remote-benchmark.sh [시뮬레이션명]
#
# 예시:
#   ./scripts/benchmark/run-remote-benchmark.sh           # 모든 시뮬레이션
#   ./scripts/benchmark/run-remote-benchmark.sh medication # 복약 스케줄만
#   ./scripts/benchmark/run-remote-benchmark.sh auth       # 인증 테스트만

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 기본 설정
REMOTE_URL="${REMOTE_URL:-https://api.hamalog.shop}"
SIMULATION_TYPE="${1:-all}"
RESULTS_DIR="$PROJECT_ROOT/benchmark-results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# .env.benchmark 파일에서 API Key 로드
if [ -f "$PROJECT_ROOT/.env.benchmark" ]; then
    source "$PROJECT_ROOT/.env.benchmark"
fi

echo -e "${CYAN}============================================${NC}"
echo -e "${CYAN}  Hamalog Remote Performance Benchmark${NC}"
echo -e "${CYAN}============================================${NC}"
echo ""
echo -e "${YELLOW}Target URL:${NC} $REMOTE_URL"
echo -e "${YELLOW}Simulation:${NC} $SIMULATION_TYPE"
echo -e "${YELLOW}Timestamp:${NC} $TIMESTAMP"
echo ""

# ============================================
# 1. API Key 확인
# ============================================
echo -e "${BLUE}[1/6] API Key 확인 중...${NC}"

if [ -z "$BENCHMARK_API_KEY" ]; then
    echo -e "${RED}❌ BENCHMARK_API_KEY 환경변수가 설정되지 않았습니다.${NC}"
    echo -e "${YELLOW}설정 방법:${NC}"
    echo -e "  export BENCHMARK_API_KEY='your-api-key'"
    echo -e "${YELLOW}또는 .env.benchmark 파일 생성:${NC}"
    echo -e "  echo 'BENCHMARK_API_KEY=your-api-key' > .env.benchmark"
    exit 1
fi
echo -e "${GREEN}✅ API Key: 설정됨${NC}"

# ============================================
# 2. 원격 서버 상태 확인
# ============================================
echo ""
echo -e "${BLUE}[2/6] 원격 서버 상태 확인 중...${NC}"

# 일반 헬스 체크
HEALTH_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$REMOTE_URL/actuator/health" 2>/dev/null || echo "000")
if [ "$HEALTH_RESPONSE" != "200" ]; then
    echo -e "${RED}❌ 서버가 응답하지 않습니다: $REMOTE_URL (HTTP $HEALTH_RESPONSE)${NC}"
    exit 1
fi
echo -e "${GREEN}✅ 서버 상태: 정상${NC}"

# 벤치마크 API 활성화 확인
BENCHMARK_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" \
    -H "X-Benchmark-API-Key: $BENCHMARK_API_KEY" \
    "$REMOTE_URL/api/v1/benchmark/health" 2>/dev/null || echo "000")

if [ "$BENCHMARK_RESPONSE" != "200" ]; then
    echo -e "${RED}❌ 벤치마크 API가 비활성화되어 있거나 API Key가 잘못되었습니다. (HTTP $BENCHMARK_RESPONSE)${NC}"
    echo -e "${YELLOW}GitHub Actions에서 'Deploy Benchmark Mode' 워크플로우를 먼저 실행하세요.${NC}"
    exit 1
fi
echo -e "${GREEN}✅ 벤치마크 API: 활성화${NC}"

# ============================================
# 3. 테스트 사용자 인증 확인
# ============================================
echo ""
echo -e "${BLUE}[3/6] 테스트 사용자 인증 확인 중...${NC}"

LOGIN_RESPONSE=$(curl -s -X POST "$REMOTE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -H "X-Benchmark-API-Key: $BENCHMARK_API_KEY" \
    -d '{"loginId":"benchmark@test.com","password":"Benchmark1234!"}' 2>/dev/null || echo '{"error":"failed"}')

if [[ "$LOGIN_RESPONSE" == *"access_token"* ]]; then
    echo -e "${GREEN}✅ 벤치마크 사용자 로그인: 성공${NC}"

    # Member ID 추출 (jq가 있으면 사용)
    if command -v jq &> /dev/null; then
        MEMBER_ID=$(echo "$LOGIN_RESPONSE" | jq -r '.member_id // "1"')
        echo -e "${GREEN}✅ Member ID: $MEMBER_ID${NC}"
    else
        MEMBER_ID="1"
        echo -e "${YELLOW}⚠️ jq가 없어 Member ID를 기본값(1)으로 설정합니다.${NC}"
    fi
else
    echo -e "${RED}❌ 벤치마크 사용자 로그인 실패${NC}"
    echo -e "${YELLOW}응답: $LOGIN_RESPONSE${NC}"
    echo ""
    echo -e "${YELLOW}원인 및 해결방법:${NC}"
    echo -e "  1. 테스트 데이터가 로드되지 않았을 수 있습니다."
    echo -e "     → Deploy Benchmark Mode 워크플로우 실행 시 '테스트 데이터 자동 생성: true' 선택"
    echo -e ""
    echo -e "  2. 수동으로 테스트 데이터 로드 (서버에서 실행):"
    echo -e "     docker exec -i mysql-hamalog mysql -u root -p<password> <dbname> < scripts/benchmark/load-test-data.sql"
    echo -e ""
    echo -e "  3. 벤치마크 계정 정보:"
    echo -e "     - 이메일: benchmark@test.com"
    echo -e "     - 비밀번호: Benchmark1234!"
    exit 1
fi

# ============================================
# 4. 결과 디렉토리 준비
# ============================================
echo ""
echo -e "${BLUE}[4/6] 결과 디렉토리 준비 중...${NC}"

mkdir -p "$RESULTS_DIR"
echo -e "${GREEN}✅ 결과 저장 위치: $RESULTS_DIR${NC}"

# ============================================
# 5. Gatling 시뮬레이션 실행
# ============================================
echo ""
echo -e "${BLUE}[5/6] Gatling 시뮬레이션 실행 중...${NC}"
echo -e "${YELLOW}(이 작업은 몇 분 정도 소요될 수 있습니다)${NC}"
echo ""

cd "$PROJECT_ROOT"

GATLING_ARGS="-DbaseUrl=$REMOTE_URL -DbenchmarkApiKey=$BENCHMARK_API_KEY"

case "$SIMULATION_TYPE" in
    "medication")
        echo -e "${YELLOW}▶ 복약 스케줄 시뮬레이션 실행...${NC}"
        ./gradlew gatlingRun \
            -Dgatling.simulationClass=com.Hamalog.simulation.MedicationScheduleSimulation \
            $GATLING_ARGS \
            --no-daemon
        ;;
    "auth")
        echo -e "${YELLOW}▶ 인증 시뮬레이션 실행...${NC}"
        ./gradlew gatlingRun \
            -Dgatling.simulationClass=com.Hamalog.simulation.AuthenticationSimulation \
            $GATLING_ARGS \
            --no-daemon
        ;;
    "all"|*)
        echo -e "${YELLOW}▶ 모든 시뮬레이션 실행...${NC}"
        ./gradlew gatlingRun \
            $GATLING_ARGS \
            --no-daemon
        ;;
esac

# ============================================
# 6. 결과 분석 및 저장
# ============================================
echo ""
echo -e "${BLUE}[6/6] 결과 분석 및 저장 중...${NC}"

# 최신 결과 디렉토리 찾기
LATEST_REPORT=$(ls -td "$PROJECT_ROOT/build/reports/gatling/"* 2>/dev/null | head -1)

if [ -n "$LATEST_REPORT" ]; then
    echo -e "${GREEN}✅ 리포트 생성 완료${NC}"

    # 결과 복사
    RESULT_NAME="benchmark_${SIMULATION_TYPE}_${TIMESTAMP}"
    cp -r "$LATEST_REPORT" "$RESULTS_DIR/$RESULT_NAME"

    # 결과 요약 출력
    if [ -f "$LATEST_REPORT/js/stats.json" ] && command -v jq &> /dev/null; then
        echo ""
        echo -e "${CYAN}========== 성능 요약 ==========${NC}"

        STATS_FILE="$LATEST_REPORT/js/stats.json"

        # Before/After 비교를 위한 세부 지표 추출
        echo ""
        echo -e "${YELLOW}[전체 결과]${NC}"
        MEAN_RESPONSE=$(jq -r '.stats.meanResponseTime.total // "N/A"' "$STATS_FILE" 2>/dev/null)
        P95_RESPONSE=$(jq -r '.stats.percentiles3.total // "N/A"' "$STATS_FILE" 2>/dev/null)
        P99_RESPONSE=$(jq -r '.stats.percentiles4.total // "N/A"' "$STATS_FILE" 2>/dev/null)
        OK_COUNT=$(jq -r '.stats.numberOfRequests.ok // 0' "$STATS_FILE" 2>/dev/null)
        KO_COUNT=$(jq -r '.stats.numberOfRequests.ko // 0' "$STATS_FILE" 2>/dev/null)
        TOTAL=$((OK_COUNT + KO_COUNT))
        if [ $TOTAL -gt 0 ]; then
            SUCCESS_RATE=$(echo "scale=2; $OK_COUNT * 100 / $TOTAL" | bc)
        else
            SUCCESS_RATE="N/A"
        fi

        echo -e "  평균 응답 시간: ${GREEN}${MEAN_RESPONSE}ms${NC}"
        echo -e "  P95 응답 시간:  ${GREEN}${P95_RESPONSE}ms${NC}"
        echo -e "  P99 응답 시간:  ${GREEN}${P99_RESPONSE}ms${NC}"
        echo -e "  성공률:         ${GREEN}${SUCCESS_RATE}%${NC} ($OK_COUNT/$TOTAL)"

        # 개별 요청별 결과 (N+1 비교용)
        echo ""
        echo -e "${YELLOW}[요청별 결과]${NC}"
        jq -r '.contents.req | to_entries[] | "  \(.value.stats.name): Mean=\(.value.stats.meanResponseTime.total)ms, P95=\(.value.stats.percentiles3.total)ms"' "$STATS_FILE" 2>/dev/null || echo "  (상세 분석 실패)"

        echo -e "${CYAN}================================${NC}"

        # 결과를 Markdown으로 저장
        SUMMARY_FILE="$RESULTS_DIR/$RESULT_NAME/SUMMARY.md"
        cat > "$SUMMARY_FILE" << EOF
# 벤치마크 결과 - $TIMESTAMP

## 환경
- **Target**: $REMOTE_URL
- **Simulation**: $SIMULATION_TYPE
- **Date**: $(date +"%Y-%m-%d %H:%M:%S")

## 전체 결과
| 지표 | 값 |
|------|-----|
| Mean Response Time | ${MEAN_RESPONSE}ms |
| P95 Response Time | ${P95_RESPONSE}ms |
| P99 Response Time | ${P99_RESPONSE}ms |
| Success Rate | ${SUCCESS_RATE}% ($OK_COUNT/$TOTAL) |

## 상세 리포트
[index.html](./index.html) 파일을 브라우저에서 열어 확인하세요.
EOF
        echo -e "${GREEN}✅ 요약 저장: $SUMMARY_FILE${NC}"
    else
        if ! command -v jq &> /dev/null; then
            echo -e "${YELLOW}⚠️ jq가 설치되어 있지 않아 상세 분석을 건너뜁니다.${NC}"
            echo -e "${YELLOW}   설치: brew install jq${NC}"
        fi
    fi

    # 리포트 열기
    echo ""
    echo -e "${GREEN}브라우저에서 리포트를 엽니다...${NC}"
    if [[ "$OSTYPE" == "darwin"* ]]; then
        open "$RESULTS_DIR/$RESULT_NAME/index.html"
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        xdg-open "$RESULTS_DIR/$RESULT_NAME/index.html" 2>/dev/null || true
    fi
else
    echo -e "${RED}❌ 리포트를 찾을 수 없습니다.${NC}"
fi

echo ""
echo -e "${CYAN}============================================${NC}"
echo -e "${CYAN}  벤치마크 완료!${NC}"
echo -e "${CYAN}============================================${NC}"
echo ""
echo -e "${YELLOW}다음 단계:${NC}"
echo -e "  1. 결과 분석: $RESULTS_DIR/$RESULT_NAME/index.html"
echo -e "  2. 프로덕션 롤백: main 브랜치에 push 또는 GitHub Actions에서 deploy-main 실행"
echo ""

