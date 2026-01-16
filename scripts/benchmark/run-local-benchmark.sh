#!/bin/bash
# ============================================
# Hamalog 로컬 성능 벤치마크 실행 스크립트
# ============================================
#
# 용도: 로컬 Docker 환경에서 N+1 vs Optimized 성능 비교
#
# 사용법:
#   ./scripts/benchmark/run-local-benchmark.sh
#
# 요구사항:
#   - Docker & Docker Compose
#   - Java 21+
#   - Gradle

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

COMPOSE_FILE="$PROJECT_ROOT/docker-compose-benchmark.yml"
RESULTS_DIR="$PROJECT_ROOT/benchmark-results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║     Hamalog Local Performance Benchmark                     ║${NC}"
echo -e "${CYAN}║     N+1 vs Optimized (@EntityGraph) Comparison              ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# ============================================
# 1. 사전 조건 확인
# ============================================
echo -e "${BLUE}[1/6] 사전 조건 확인 중...${NC}"

if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker가 설치되어 있지 않습니다.${NC}"
    exit 1
fi

if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo -e "${RED}❌ Docker Compose가 설치되어 있지 않습니다.${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Docker 환경 확인 완료${NC}"

# ============================================
# 2. 기존 환경 정리
# ============================================
echo ""
echo -e "${BLUE}[2/6] 기존 벤치마크 환경 정리 중...${NC}"

cd "$PROJECT_ROOT"

# Docker Compose 명령어 확인 (docker-compose vs docker compose)
if docker compose version &> /dev/null 2>&1; then
    DOCKER_COMPOSE="docker compose"
else
    DOCKER_COMPOSE="docker-compose"
fi

$DOCKER_COMPOSE -f "$COMPOSE_FILE" down -v 2>/dev/null || true
echo -e "${GREEN}✅ 기존 환경 정리 완료${NC}"

# ============================================
# 3. 애플리케이션 빌드
# ============================================
echo ""
echo -e "${BLUE}[3/6] 애플리케이션 빌드 중...${NC}"
echo -e "${YELLOW}(이 작업은 처음 실행 시 몇 분 소요될 수 있습니다)${NC}"

./gradlew bootJar -x test --no-daemon -q
echo -e "${GREEN}✅ 빌드 완료${NC}"

# ============================================
# 4. Docker 환경 시작
# ============================================
echo ""
echo -e "${BLUE}[4/6] Docker 환경 시작 중...${NC}"

$DOCKER_COMPOSE -f "$COMPOSE_FILE" up -d --build

echo -e "${YELLOW}서비스 시작 대기 중...${NC}"

# 헬스 체크 (최대 120초 대기)
MAX_WAIT=120
WAITED=0
while [ $WAITED -lt $MAX_WAIT ]; do
    if curl -s http://localhost:8080/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
        break
    fi
    echo -n "."
    sleep 2
    WAITED=$((WAITED + 2))
done
echo ""

if [ $WAITED -ge $MAX_WAIT ]; then
    echo -e "${RED}❌ 서비스 시작 시간 초과 (${MAX_WAIT}초)${NC}"
    echo -e "${YELLOW}로그 확인:${NC}"
    $DOCKER_COMPOSE -f "$COMPOSE_FILE" logs app | tail -50
    exit 1
fi

echo -e "${GREEN}✅ 서비스 시작 완료${NC}"

# ============================================
# 5. Gatling 벤치마크 실행
# ============================================
echo ""
echo -e "${BLUE}[5/6] Gatling 벤치마크 실행 중...${NC}"
echo -e "${YELLOW}(약 1분 소요)${NC}"
echo ""

# Gatling 실행
./gradlew gatlingRun \
    -Dgatling.simulationClass=com.Hamalog.simulation.LocalMedicationBenchmark \
    --no-daemon 2>&1 | tee /tmp/gatling-output.log

# ============================================
# 6. 결과 저장 및 정리
# ============================================
echo ""
echo -e "${BLUE}[6/6] 결과 저장 및 정리 중...${NC}"

# 결과 디렉토리 생성
mkdir -p "$RESULTS_DIR"

# 최신 리포트 찾기
LATEST_REPORT=$(ls -td "$PROJECT_ROOT/build/reports/gatling/"* 2>/dev/null | head -1)

if [ -n "$LATEST_REPORT" ] && [ -d "$LATEST_REPORT" ]; then
    RESULT_NAME="local_benchmark_${TIMESTAMP}"
    cp -r "$LATEST_REPORT" "$RESULTS_DIR/$RESULT_NAME"

    echo -e "${GREEN}✅ 결과 저장 완료: $RESULTS_DIR/$RESULT_NAME${NC}"

    # 결과 요약 출력
    echo ""
    echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║                    벤치마크 결과 요약                       ║${NC}"
    echo -e "${CYAN}╚════════════════════════════════════════════════════════════╝${NC}"

    # stats.json에서 결과 추출
    if [ -f "$LATEST_REPORT/js/stats.json" ] && command -v jq &> /dev/null; then
        echo ""

        # Before (N+1) 결과
        BEFORE_MEAN=$(jq -r '.contents.req."before--n-1-query".stats.meanResponseTime.total // "N/A"' "$LATEST_REPORT/js/stats.json" 2>/dev/null || echo "N/A")
        BEFORE_P95=$(jq -r '.contents.req."before--n-1-query".stats.percentiles3.total // "N/A"' "$LATEST_REPORT/js/stats.json" 2>/dev/null || echo "N/A")
        BEFORE_P99=$(jq -r '.contents.req."before--n-1-query".stats.percentiles4.total // "N/A"' "$LATEST_REPORT/js/stats.json" 2>/dev/null || echo "N/A")

        # After (Optimized) 결과
        AFTER_MEAN=$(jq -r '.contents.req."after--optimized-query---entitygraph-".stats.meanResponseTime.total // "N/A"' "$LATEST_REPORT/js/stats.json" 2>/dev/null || echo "N/A")
        AFTER_P95=$(jq -r '.contents.req."after--optimized-query---entitygraph-".stats.percentiles3.total // "N/A"' "$LATEST_REPORT/js/stats.json" 2>/dev/null || echo "N/A")
        AFTER_P99=$(jq -r '.contents.req."after--optimized-query---entitygraph-".stats.percentiles4.total // "N/A"' "$LATEST_REPORT/js/stats.json" 2>/dev/null || echo "N/A")

        echo -e "${YELLOW}Before (N+1 Problem):${NC}"
        echo -e "  Mean Response Time: ${RED}${BEFORE_MEAN}ms${NC}"
        echo -e "  P95 Response Time:  ${RED}${BEFORE_P95}ms${NC}"
        echo -e "  P99 Response Time:  ${RED}${BEFORE_P99}ms${NC}"
        echo ""
        echo -e "${YELLOW}After (Optimized - @EntityGraph):${NC}"
        echo -e "  Mean Response Time: ${GREEN}${AFTER_MEAN}ms${NC}"
        echo -e "  P95 Response Time:  ${GREEN}${AFTER_P95}ms${NC}"
        echo -e "  P99 Response Time:  ${GREEN}${AFTER_P99}ms${NC}"

        # 개선율 계산
        if [ "$BEFORE_MEAN" != "N/A" ] && [ "$AFTER_MEAN" != "N/A" ] && [ "$BEFORE_MEAN" != "0" ]; then
            IMPROVEMENT=$(echo "scale=1; (1 - $AFTER_MEAN / $BEFORE_MEAN) * 100" | bc 2>/dev/null || echo "N/A")
            echo ""
            echo -e "${CYAN}📈 성능 개선율: ${GREEN}${IMPROVEMENT}%${NC}"
        fi
    else
        echo -e "${YELLOW}⚠️ jq가 설치되어 있지 않아 상세 분석을 건너뜁니다.${NC}"
        echo -e "${YELLOW}   설치: brew install jq${NC}"
    fi

    # 리포트 열기
    echo ""
    echo -e "${GREEN}📊 HTML 리포트 열기...${NC}"
    if [[ "$OSTYPE" == "darwin"* ]]; then
        open "$RESULTS_DIR/$RESULT_NAME/index.html"
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        xdg-open "$RESULTS_DIR/$RESULT_NAME/index.html" 2>/dev/null || true
    fi
else
    echo -e "${RED}❌ 리포트를 찾을 수 없습니다.${NC}"
fi

# Docker 환경 정리
echo ""
echo -e "${BLUE}Docker 환경 정리 중...${NC}"
$DOCKER_COMPOSE -f "$COMPOSE_FILE" down -v

echo ""
echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║                    벤치마크 완료!                           ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}결과 위치: $RESULTS_DIR/$RESULT_NAME${NC}"
echo ""

