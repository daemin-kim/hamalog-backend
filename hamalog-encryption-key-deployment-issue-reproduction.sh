#!/bin/bash

# HAMALOG_ENCRYPTION_KEY 배포 환경 이슈 재현 스크립트
# 이 스크립트는 HAMALOG_ENCRYPTION_KEY가 공백으로 설정될 때 발생하는 문제를 재현합니다

set -e

echo "🔍 HAMALOG_ENCRYPTION_KEY 배포 이슈 재현 테스트"
echo "=============================================="

# 테스트 1: HAMALOG_ENCRYPTION_KEY이 설정되지 않은 경우 (정상적인 개발 모드)
echo ""
echo "📋 테스트 1: HAMALOG_ENCRYPTION_KEY 미설정 (개발 모드)"
echo "환경변수: (HAMALOG_ENCRYPTION_KEY 미설정)"
unset HAMALOG_ENCRYPTION_KEY
echo "Spring Profile: default (개발)"
echo "예상 결과: 애플리케이션 정상 시작 (임시 키 생성)"

# 테스트 2: HAMALOG_ENCRYPTION_KEY이 빈 문자열로 설정된 경우 (문제 상황)
echo ""
echo "📋 테스트 2: HAMALOG_ENCRYPTION_KEY이 빈 문자열 (프로덕션 모드)"
export HAMALOG_ENCRYPTION_KEY=""
echo "환경변수: HAMALOG_ENCRYPTION_KEY=''"
echo "Spring Profile: prod"
echo "예상 결과: 오류 발생 - '데이터 암호화 키가 설정되지 않았습니다. 프로덕션 환경에서는 HAMALOG_ENCRYPTION_KEY 환경변수를 반드시 설정해야 합니다.'"

# 테스트 3: Docker Compose 환경변수 확장 시뮬레이션
echo ""
echo "📋 테스트 3: Docker Compose 환경변수 확장 시뮬레이션"

# 시나리오 A: HAMALOG_ENCRYPTION_KEY이 설정되지 않은 경우
unset HAMALOG_ENCRYPTION_KEY
ENCRYPTION_KEY_RESOLVED="${HAMALOG_ENCRYPTION_KEY:-}"
echo "시나리오 A - HAMALOG_ENCRYPTION_KEY 미설정:"
echo "  원본: \${HAMALOG_ENCRYPTION_KEY:-}"
echo "  결과: '$ENCRYPTION_KEY_RESOLVED'"
echo "  길이: ${#ENCRYPTION_KEY_RESOLVED} 문자"
echo "  ✅ 정상: 빈 문자열로 설정됨 (개발 환경에서는 임시 키 생성)"

# 시나리오 B: HAMALOG_ENCRYPTION_KEY이 빈 문자열로 설정된 경우 (문제 상황)
export HAMALOG_ENCRYPTION_KEY=""
ENCRYPTION_KEY_RESOLVED="${HAMALOG_ENCRYPTION_KEY:-fallback-key}"
echo "시나리오 B - HAMALOG_ENCRYPTION_KEY이 빈 문자열:"
echo "  원본: \${HAMALOG_ENCRYPTION_KEY:-fallback-key}"
echo "  결과: '$ENCRYPTION_KEY_RESOLVED'"
echo "  길이: ${#ENCRYPTION_KEY_RESOLVED} 문자"
echo "  ⚠️  문제: 빈 문자열이 기본값 대신 사용됨!"

# 시나리오 C: HAMALOG_ENCRYPTION_KEY이 올바른 값으로 설정된 경우
export HAMALOG_ENCRYPTION_KEY="dGVzdC1lbmNyeXB0aW9uLWtleS0xMjMtNDU2Nzg="
ENCRYPTION_KEY_RESOLVED="${HAMALOG_ENCRYPTION_KEY:-fallback-key}"
echo "시나리오 C - HAMALOG_ENCRYPTION_KEY이 올바르게 설정:"
echo "  원본: \${HAMALOG_ENCRYPTION_KEY:-fallback-key}"
echo "  결과: '$ENCRYPTION_KEY_RESOLVED'"
echo "  길이: ${#ENCRYPTION_KEY_RESOLVED} 문자"

echo ""
echo "🔧 올바른 해결 방법 시뮬레이션"
echo "=============================="

# 올바른 방법 1: ${HAMALOG_ENCRYPTION_KEY:+$HAMALOG_ENCRYPTION_KEY} 사용
export HAMALOG_ENCRYPTION_KEY=""
ENCRYPTION_KEY_CORRECT="${HAMALOG_ENCRYPTION_KEY:+$HAMALOG_ENCRYPTION_KEY}"
if [ -z "$ENCRYPTION_KEY_CORRECT" ]; then
    ENCRYPTION_KEY_CORRECT="generated-in-application"
fi
echo "방법 1 - 조건부 확장 사용:"
echo "  HAMALOG_ENCRYPTION_KEY이 비어있음: '$HAMALOG_ENCRYPTION_KEY'"
echo "  처리 결과: '$ENCRYPTION_KEY_CORRECT'"

# 올바른 방법 2: 스크립트에서 명시적 검사
export HAMALOG_ENCRYPTION_KEY=""
if [ -z "$HAMALOG_ENCRYPTION_KEY" ]; then
    ENCRYPTION_KEY_EXPLICIT="generated-in-application"
else
    ENCRYPTION_KEY_EXPLICIT="$HAMALOG_ENCRYPTION_KEY"
fi
echo "방법 2 - 명시적 검사 사용:"
echo "  HAMALOG_ENCRYPTION_KEY이 비어있음: '$HAMALOG_ENCRYPTION_KEY'"
echo "  처리 결과: '$ENCRYPTION_KEY_EXPLICIT'"

echo ""
echo "📝 Docker Compose 사용 시 문제 재현"
echo "================================="

# 임시 docker-compose 파일 생성
cat > docker-compose.encryption-test.yml << 'EOF'
services:
  test-app:
    image: alpine:latest
    environment:
      - TEST_ENCRYPTION_KEY=${HAMALOG_ENCRYPTION_KEY:-}
    command: sh -c 'echo "Received TEST_ENCRYPTION_KEY: [$TEST_ENCRYPTION_KEY]"; if [ -z "$TEST_ENCRYPTION_KEY" ]; then echo "ENCRYPTION_KEY is empty!"; else echo "ENCRYPTION_KEY length: ${#TEST_ENCRYPTION_KEY}"; fi'
EOF

echo "Docker Compose 파일 생성됨: docker-compose.encryption-test.yml"
echo ""

echo "테스트 A - HAMALOG_ENCRYPTION_KEY 미설정:"
unset HAMALOG_ENCRYPTION_KEY
docker-compose -f docker-compose.encryption-test.yml run --rm test-app 2>/dev/null || echo "Docker가 사용 불가능합니다"

echo ""
echo "테스트 B - HAMALOG_ENCRYPTION_KEY이 빈 문자열 (문제 상황):"
export HAMALOG_ENCRYPTION_KEY=""
docker-compose -f docker-compose.encryption-test.yml run --rm test-app 2>/dev/null || echo "Docker가 사용 불가능합니다"

# 정리
rm -f docker-compose.encryption-test.yml

echo ""
echo "🎯 문제 요약"
echo "==========="
echo "1. 문제: Docker Compose에서 HAMALOG_ENCRYPTION_KEY가 누락되었음"
echo "2. 원인: docker-compose.yml에 HAMALOG_ENCRYPTION_KEY 환경변수 설정 없음"
echo "3. 결과: 컨테이너에서 빈 문자열 수신으로 인한 프로덕션 시작 실패"
echo "4. 오류: '데이터 암호화 키가 설정되지 않았습니다. 프로덕션 환경에서는 HAMALOG_ENCRYPTION_KEY 환경변수를 반드시 설정해야 합니다.'"
echo ""
echo "🔧 권장 해결 방법:"
echo "- Docker Compose에 HAMALOG_ENCRYPTION_KEY 환경변수 추가"
echo "- 배포 스크립트에서 환경변수 검증 로직 추가"
echo "- 애플리케이션에서 상세한 에러 메시지 제공"
echo ""
echo "📝 배포 전 확인사항:"
echo "- HAMALOG_ENCRYPTION_KEY 환경변수 설정: export HAMALOG_ENCRYPTION_KEY=\$(openssl rand -base64 32)"
echo "- JWT_SECRET도 함께 설정: export JWT_SECRET=\$(openssl rand -base64 32)"
echo "- 환경변수 진단 도구 실행: ./environment-diagnostic.sh"
echo "- 배포 검증: ./deploy.sh (사전 검증 수행)"