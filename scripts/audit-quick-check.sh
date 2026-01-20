#!/usr/bin/env bash
# =============================================================================
# Hamalog 프로젝트 전체 점검 Quick Check 스크립트
# =============================================================================
# 사용법: ./scripts/audit-quick-check.sh
# 권한 부여: chmod +x scripts/audit-quick-check.sh
# =============================================================================

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 프로젝트 루트 디렉토리 확인
if [ ! -f "build.gradle" ]; then
    echo -e "${RED}오류: 프로젝트 루트 디렉토리에서 실행해주세요.${NC}"
    exit 1
fi

echo ""
echo "╔══════════════════════════════════════════════════════════════════════╗"
echo "║           🔍 Hamalog 프로젝트 전체 점검 Quick Check                  ║"
echo "╚══════════════════════════════════════════════════════════════════════╝"
echo ""

# 카운터 초기화
CRITICAL_ISSUES=0
HIGH_ISSUES=0
WARNINGS=0
PASSED=0

# =============================================================================
# Part 1: 코드 품질
# =============================================================================
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Part 1: 코드 품질 및 아키텍처${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# @Data 사용 확인 (Entity)
echo -n "[1.1] @Data 사용 확인 (Entity): "
if grep -rq "@Data" src/main/java/com/Hamalog/domain --include="*.java" 2>/dev/null; then
    echo -e "${RED}❌ @Data 발견 - Entity에서 @Data 사용 금지${NC}"
    ((CRITICAL_ISSUES++))
else
    echo -e "${GREEN}✅ 정상 - @Data 미사용${NC}"
    ((PASSED++))
fi

# FetchType.EAGER 사용 확인
echo -n "[1.2] FetchType.EAGER 사용 확인: "
if grep -rq "FetchType.EAGER" src/main/java --include="*.java" 2>/dev/null; then
    echo -e "${RED}❌ EAGER 발견 - N+1 문제 위험${NC}"
    ((CRITICAL_ISSUES++))
else
    echo -e "${GREEN}✅ 정상 - EAGER 미사용${NC}"
    ((PASSED++))
fi

# Controller에서 Repository 사용 확인 (private 필드 또는 생성자 주입 확인)
echo -n "[1.3] Controller에서 Repository 직접 사용: "
if grep -rE "private[[:space:]]+[A-Za-z]+Repository|@Autowired.*Repository" src/main/java/com/Hamalog/controller --include="*.java" 2>/dev/null | grep -qv "import"; then
    echo -e "${RED}❌ 레이어 위반 - Controller에서 Repository 직접 사용${NC}"
    ((CRITICAL_ISSUES++))
else
    echo -e "${GREEN}✅ 정상 - 레이어 분리 준수${NC}"
    ((PASSED++))
fi

# @Valid 누락 확인
echo -n "[1.4] @Valid 어노테이션 적용: "
MISSING_VALID=$(grep -rn "@RequestBody" src/main/java/com/Hamalog/controller --include="*.java" 2>/dev/null | grep -v "@Valid" | wc -l | tr -d ' ')
if [ "$MISSING_VALID" -gt "0" ]; then
    echo -e "${YELLOW}⚠️ @Valid 누락 가능성 ${MISSING_VALID}건${NC}"
    ((WARNINGS++))
else
    echo -e "${GREEN}✅ 정상 - @Valid 적용됨${NC}"
    ((PASSED++))
fi

echo ""

# =============================================================================
# Part 2: 보안
# =============================================================================
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Part 2: 인증/인가 및 보안${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# JWT Secret 환경변수 확인 (yml 또는 properties)
echo -n "[2.1] JWT Secret 환경변수 사용: "
if grep -rq 'jwt.*secret.*\${\|jwt\.secret=\${' src/main/resources/application*.yml src/main/resources/application*.properties 2>/dev/null; then
    echo -e "${GREEN}✅ 정상 - 환경변수 사용${NC}"
    ((PASSED++))
else
    echo -e "${RED}❌ 위험 - JWT Secret 하드코딩 가능성${NC}"
    ((CRITICAL_ISSUES++))
fi

# @RequireResourceOwnership 사용 현황
echo -n "[2.2] @RequireResourceOwnership 사용 현황: "
OWNERSHIP_COUNT=$(grep -rn "@RequireResourceOwnership" src/main/java/com/Hamalog/controller --include="*.java" 2>/dev/null | wc -l | tr -d ' ')
if [ "$OWNERSHIP_COUNT" -gt "0" ]; then
    echo -e "${GREEN}✅ ${OWNERSHIP_COUNT}개 API에 적용됨${NC}"
    ((PASSED++))
else
    echo -e "${YELLOW}⚠️ 리소스 소유권 검증 미적용${NC}"
    ((WARNINGS++))
fi

# 비밀번호 로그 출력 확인 (실제 로그 메서드에서 password 변수 사용)
echo -n "[2.3] 비밀번호 로그 출력 확인: "
if grep -rn "log\.\(info\|debug\|warn\|error\).*password\|log\.\(info\|debug\|warn\|error\).*getPassword" src/main/java --include="*.java" 2>/dev/null | grep -v "PasswordEncoder\|passwordEncoder" | grep -q .; then
    echo -e "${RED}❌ 위험 - 비밀번호 로깅 가능성${NC}"
    ((CRITICAL_ISSUES++))
else
    echo -e "${GREEN}✅ 정상 - 민감 정보 로깅 없음${NC}"
    ((PASSED++))
fi

# CORS 와일드카드 확인
echo -n "[2.4] CORS 와일드카드 확인: "
if grep -rq 'allowedOrigins.*"\*"' src/main/java --include="*.java" 2>/dev/null; then
    echo -e "${RED}❌ 위험 - CORS 와일드카드 발견${NC}"
    ((CRITICAL_ISSUES++))
else
    echo -e "${GREEN}✅ 정상 - CORS 제한 적용${NC}"
    ((PASSED++))
fi

# Rate Limiting 설정
echo -n "[2.5] Rate Limiting 설정: "
if grep -rq "rate-limit" src/main/resources/application*.yml 2>/dev/null; then
    echo -e "${GREEN}✅ 정상 - Rate Limiting 설정됨${NC}"
    ((PASSED++))
else
    echo -e "${YELLOW}⚠️ Rate Limiting 미설정${NC}"
    ((WARNINGS++))
fi

echo ""

# =============================================================================
# Part 4: JPA/DB
# =============================================================================
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Part 4: 데이터베이스 및 JPA${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# @EntityGraph 사용 현황
echo -n "[4.1] @EntityGraph 사용 현황: "
ENTITYGRAPH_COUNT=$(grep -rn "@EntityGraph" src/main/java/com/Hamalog/repository --include="*.java" 2>/dev/null | wc -l | tr -d ' ')
echo -e "${GREEN}✅ ${ENTITYGRAPH_COUNT}개 사용${NC}"
((PASSED++))

# @Version 필드 확인
echo -n "[4.2] @Version 필드 (낙관적 락): "
VERSION_COUNT=$(grep -rl "@Version" src/main/java/com/Hamalog/domain --include="*.java" 2>/dev/null | wc -l | tr -d ' ')
ENTITY_COUNT=$(find src/main/java/com/Hamalog/domain -name "*.java" -exec grep -l "@Entity" {} \; 2>/dev/null | wc -l | tr -d ' ')
if [ "$VERSION_COUNT" -gt "0" ]; then
    echo -e "${GREEN}✅ ${VERSION_COUNT}/${ENTITY_COUNT} Entity에 적용${NC}"
    ((PASSED++))
else
    echo -e "${YELLOW}⚠️ @Version 미적용${NC}"
    ((WARNINGS++))
fi

# HikariCP 설정
echo -n "[4.3] HikariCP 설정: "
if grep -rq "hikari\|maximum-pool-size" src/main/resources/application*.yml 2>/dev/null; then
    echo -e "${GREEN}✅ 정상 - 커넥션 풀 설정됨${NC}"
    ((PASSED++))
else
    echo -e "${YELLOW}⚠️ 기본값 사용 중${NC}"
    ((WARNINGS++))
fi

echo ""

# =============================================================================
# Part 5: 트랜잭션
# =============================================================================
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Part 5: 트랜잭션 관리${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# Controller에서 @Transactional 사용
echo -n "[5.1] Controller에서 @Transactional 사용: "
if grep -rq "@Transactional" src/main/java/com/Hamalog/controller --include="*.java" 2>/dev/null; then
    echo -e "${RED}❌ 위반 - Controller에 @Transactional 발견${NC}"
    ((HIGH_ISSUES++))
else
    echo -e "${GREEN}✅ 정상 - Controller에 @Transactional 없음${NC}"
    ((PASSED++))
fi

# open-in-view 설정
echo -n "[5.2] OSIV 설정: "
if grep -rq "open-in-view.*false\|open-in-view=false" src/main/resources/application*.yml src/main/resources/application*.properties 2>/dev/null; then
    echo -e "${GREEN}✅ 정상 - OSIV 비활성화${NC}"
    ((PASSED++))
else
    echo -e "${YELLOW}⚠️ OSIV 기본값(true) 사용 중${NC}"
    ((WARNINGS++))
fi

echo ""

# =============================================================================
# Part 6: 캐싱
# =============================================================================
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Part 6: 캐싱 및 성능${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# @EnableCaching 설정 또는 spring.cache 설정
echo -n "[6.1] 캐싱 설정: "
if grep -rq "@EnableCaching" src/main/java --include="*.java" 2>/dev/null || grep -rq "spring.cache" src/main/resources/application*.properties 2>/dev/null; then
    echo -e "${GREEN}✅ 캐싱 활성화됨${NC}"
    ((PASSED++))
else
    echo -e "${YELLOW}⚠️ 캐싱 미설정 (성능 개선 권고)${NC}"
    ((WARNINGS++))
fi

# @Cacheable 사용 현황
CACHEABLE_COUNT=$(grep -rn "@Cacheable" src/main/java/com/Hamalog/service --include="*.java" 2>/dev/null | wc -l | tr -d ' ')
echo "[6.2] @Cacheable 사용: ${CACHEABLE_COUNT}개"

# @CacheEvict 사용 현황
CACHEEVICT_COUNT=$(grep -rn "@CacheEvict" src/main/java/com/Hamalog/service --include="*.java" 2>/dev/null | wc -l | tr -d ' ')
echo "[6.3] @CacheEvict 사용: ${CACHEEVICT_COUNT}개"

echo ""

# =============================================================================
# Part 8: 테스트
# =============================================================================
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Part 8: 테스트 커버리지${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# 테스트 파일 수
TEST_COUNT=$(find src/test/java -name "*Test.java" 2>/dev/null | wc -l | tr -d ' ')
echo "[8.1] 테스트 파일 수: ${TEST_COUNT}개"

# Service 테스트 수
SERVICE_TEST_COUNT=$(find src/test/java/com/Hamalog/service -name "*Test.java" 2>/dev/null | wc -l | tr -d ' ')
echo "[8.2] Service 테스트 수: ${SERVICE_TEST_COUNT}개"

# Controller 테스트 수
CONTROLLER_TEST_COUNT=$(find src/test/java/com/Hamalog/controller -name "*Test.java" 2>/dev/null | wc -l | tr -d ' ')
echo "[8.3] Controller 테스트 수: ${CONTROLLER_TEST_COUNT}개"

echo ""

# =============================================================================
# Part 10: 인프라
# =============================================================================
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Part 10: 인프라 및 배포${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# Dockerfile 존재
echo -n "[10.1] Dockerfile 존재: "
if [ -f "Dockerfile" ]; then
    echo -e "${GREEN}✅ 존재${NC}"
    ((PASSED++))
else
    echo -e "${YELLOW}⚠️ 없음${NC}"
    ((WARNINGS++))
fi

# docker-compose.yml 존재
echo -n "[10.2] docker-compose.yml 존재: "
if [ -f "docker-compose.yml" ]; then
    echo -e "${GREEN}✅ 존재${NC}"
    ((PASSED++))
else
    echo -e "${YELLOW}⚠️ 없음${NC}"
    ((WARNINGS++))
fi

# 환경변수 사용
echo -n "[10.3] 환경변수 사용: "
ENV_VAR_COUNT=$(grep -rn '\${' src/main/resources/application*.yml src/main/resources/application*.properties 2>/dev/null | wc -l | tr -d ' ')
echo -e "${GREEN}✅ ${ENV_VAR_COUNT}개 환경변수 참조${NC}"
((PASSED++))

echo ""

# =============================================================================
# 결과 요약
# =============================================================================
echo "╔══════════════════════════════════════════════════════════════════════╗"
echo "║                         📊 점검 결과 요약                            ║"
echo "╚══════════════════════════════════════════════════════════════════════╝"
echo ""
echo -e "  🔴 Critical Issues: ${RED}${CRITICAL_ISSUES}${NC}"
echo -e "  🟠 High Issues:     ${YELLOW}${HIGH_ISSUES}${NC}"
echo -e "  🟡 Warnings:        ${YELLOW}${WARNINGS}${NC}"
echo -e "  🟢 Passed:          ${GREEN}${PASSED}${NC}"
echo ""

TOTAL_ISSUES=$((CRITICAL_ISSUES + HIGH_ISSUES))

if [ "$CRITICAL_ISSUES" -gt "0" ]; then
    echo -e "${RED}⚠️  Critical 이슈가 발견되었습니다. 즉시 조치가 필요합니다.${NC}"
    echo ""
    echo "📚 상세 점검 가이드: docs/internal/PROJECT-AUDIT-GUIDE.md"
    exit 1
elif [ "$HIGH_ISSUES" -gt "0" ]; then
    echo -e "${YELLOW}⚠️  High 이슈가 발견되었습니다. 빠른 조치를 권장합니다.${NC}"
    echo ""
    echo "📚 상세 점검 가이드: docs/internal/PROJECT-AUDIT-GUIDE.md"
    exit 0
else
    echo -e "${GREEN}✅ 주요 점검 항목을 모두 통과했습니다!${NC}"
    echo ""
    echo "📚 상세 점검 가이드: docs/internal/PROJECT-AUDIT-GUIDE.md"
    exit 0
fi
