#!/bin/bash
# =============================================================================
# Redis 캐시 성능 벤치마크 스크립트
# 캐시 HIT vs DB 직접 조회 응답시간 비교
# =============================================================================

set -e

# 색상 설정
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 설정
BASE_URL="${BASE_URL:-http://localhost:8080}"
MEMBER_ID="${MEMBER_ID:-1}"
ITERATIONS="${ITERATIONS:-10}"

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}   Redis 캐시 성능 벤치마크${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""
echo "대상 URL: $BASE_URL"
echo "회원 ID: $MEMBER_ID"
echo "반복 횟수: $ITERATIONS"
echo ""

# 헬스 체크
echo -e "${YELLOW}[1/5] 서버 상태 확인 중...${NC}"
HEALTH_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/v1/benchmark/health" 2>/dev/null || echo "000")
if [ "$HEALTH_RESPONSE" != "200" ]; then
    echo -e "${RED}❌ 서버에 연결할 수 없습니다. (HTTP $HEALTH_RESPONSE)${NC}"
    echo "Docker 벤치마크 환경을 먼저 시작하세요:"
    echo "  docker-compose -f docker-compose-benchmark.yml up -d --build"
    exit 1
fi
echo -e "${GREEN}✅ 서버 상태: 정상${NC}"
echo ""

# 결과 저장용 배열
declare -a CACHE_TIMES
declare -a DB_TIMES

# 캐시 무효화
echo -e "${YELLOW}[2/5] 캐시 무효화 중...${NC}"
curl -s -X DELETE "$BASE_URL/api/v1/benchmark/member/cache/$MEMBER_ID" > /dev/null
echo -e "${GREEN}✅ 캐시 무효화 완료${NC}"
echo ""

# DB 직접 조회 측정
echo -e "${YELLOW}[3/5] DB 직접 조회 측정 중... ($ITERATIONS회)${NC}"
for i in $(seq 1 $ITERATIONS); do
    RESPONSE=$(curl -s "$BASE_URL/api/v1/benchmark/member/db/$MEMBER_ID")
    TIME_MS=$(echo "$RESPONSE" | grep -o '"queryTimeMs":[0-9.]*' | cut -d':' -f2)
    DB_TIMES+=("$TIME_MS")
    printf "  [%2d/$ITERATIONS] %.3f ms\n" $i "$TIME_MS"
done
echo ""

# 캐시 워밍업
echo -e "${YELLOW}[4/5] 캐시 워밍업 중...${NC}"
WARMUP_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/benchmark/member/cache/warmup/$MEMBER_ID")
WARMUP_TIME=$(echo "$WARMUP_RESPONSE" | grep -o '"queryTimeMs":[0-9.]*' | cut -d':' -f2)
echo -e "${GREEN}✅ 캐시 워밍업 완료 (${WARMUP_TIME}ms)${NC}"
echo ""

# 캐시 HIT 측정
echo -e "${YELLOW}[5/5] 캐시 HIT 측정 중... ($ITERATIONS회)${NC}"
for i in $(seq 1 $ITERATIONS); do
    RESPONSE=$(curl -s "$BASE_URL/api/v1/benchmark/member/cache/$MEMBER_ID")
    TIME_MS=$(echo "$RESPONSE" | grep -o '"queryTimeMs":[0-9.]*' | cut -d':' -f2)
    SOURCE=$(echo "$RESPONSE" | grep -o '"source":"[^"]*' | cut -d'"' -f4)
    CACHE_TIMES+=("$TIME_MS")
    printf "  [%2d/$ITERATIONS] %.3f ms (%s)\n" $i "$TIME_MS" "$SOURCE"
done
echo ""

# 통계 계산 함수
calc_stats() {
    local arr=("$@")
    local sum=0
    local min=999999
    local max=0
    local count=${#arr[@]}

    for val in "${arr[@]}"; do
        sum=$(echo "$sum + $val" | bc -l)
        if (( $(echo "$val < $min" | bc -l) )); then min=$val; fi
        if (( $(echo "$val > $max" | bc -l) )); then max=$val; fi
    done

    local avg=$(echo "scale=3; $sum / $count" | bc -l)
    echo "$avg $min $max"
}

# 통계 계산
DB_STATS=$(calc_stats "${DB_TIMES[@]}")
CACHE_STATS=$(calc_stats "${CACHE_TIMES[@]}")

DB_AVG=$(echo "$DB_STATS" | awk '{print $1}')
DB_MIN=$(echo "$DB_STATS" | awk '{print $2}')
DB_MAX=$(echo "$DB_STATS" | awk '{print $3}')

CACHE_AVG=$(echo "$CACHE_STATS" | awk '{print $1}')
CACHE_MIN=$(echo "$CACHE_STATS" | awk '{print $2}')
CACHE_MAX=$(echo "$CACHE_STATS" | awk '{print $3}')

# 개선율 계산
IMPROVEMENT=$(echo "scale=1; ($DB_AVG - $CACHE_AVG) / $DB_AVG * 100" | bc -l)
SPEEDUP=$(echo "scale=1; $DB_AVG / $CACHE_AVG" | bc -l)

# 결과 출력
echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}   벤치마크 결과${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""
echo "┌──────────────────┬────────────┬────────────┬────────────┐"
echo "│ 방식             │ 평균(ms)   │ 최소(ms)   │ 최대(ms)   │"
echo "├──────────────────┼────────────┼────────────┼────────────┤"
printf "│ %-16s │ %10.3f │ %10.3f │ %10.3f │\n" "DB 직접 조회" "$DB_AVG" "$DB_MIN" "$DB_MAX"
printf "│ %-16s │ %10.3f │ %10.3f │ %10.3f │\n" "캐시 HIT" "$CACHE_AVG" "$CACHE_MIN" "$CACHE_MAX"
echo "└──────────────────┴────────────┴────────────┴────────────┘"
echo ""
echo -e "${GREEN}🚀 개선율: ${IMPROVEMENT}% (${SPEEDUP}배 향상)${NC}"
echo ""

# 결과 파일 저장
RESULT_FILE="benchmark-results/CACHE-BENCHMARK-$(date +%Y%m%d_%H%M%S).md"
mkdir -p benchmark-results

cat > "$RESULT_FILE" << EOF
# Redis 캐시 성능 벤치마크 결과

**측정일시**: $(date '+%Y-%m-%d %H:%M:%S')

## 테스트 조건

| 항목 | 값 |
|------|-----|
| 대상 API | 회원 정보 조회 |
| 반복 횟수 | ${ITERATIONS}회 |
| 회원 ID | ${MEMBER_ID} |

## 측정 결과

| 방식 | 평균 | 최소 | 최대 |
|------|------|------|------|
| **DB 직접 조회** | ${DB_AVG}ms | ${DB_MIN}ms | ${DB_MAX}ms |
| **캐시 HIT** | ${CACHE_AVG}ms | ${CACHE_MIN}ms | ${CACHE_MAX}ms |

## 개선 효과

- **응답시간 단축**: ${IMPROVEMENT}%
- **성능 향상**: ${SPEEDUP}배

## Raw Data

### DB 직접 조회 (ms)
${DB_TIMES[*]}

### 캐시 HIT (ms)
${CACHE_TIMES[*]}
EOF

echo -e "${GREEN}✅ 결과 저장: $RESULT_FILE${NC}"
echo ""

