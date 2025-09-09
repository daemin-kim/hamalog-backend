#!/bin/bash

# JWT 배포 환경 이슈 재현 스크립트
# 이 스크립트는 JWT_SECRET이 공백으로 설정될 때 발생하는 문제를 재현합니다

set -e

echo "🔍 JWT_SECRET 배포 이슈 재현 테스트"
echo "=================================="

# 테스트 1: JWT_SECRET이 설정되지 않은 경우 (정상적인 개발 모드)
echo ""
echo "📋 테스트 1: JWT_SECRET 미설정 (개발 모드)"
echo "환경변수: (JWT_SECRET 미설정)"
unset JWT_SECRET
echo "Spring Profile: default (개발)"
echo "예상 결과: 애플리케이션 정상 시작 (임시 키 생성)"

# 테스트 2: JWT_SECRET이 빈 문자열로 설정된 경우 (문제 상황)
echo ""
echo "📋 테스트 2: JWT_SECRET이 빈 문자열 (프로덕션 모드)"
export JWT_SECRET=""
echo "환경변수: JWT_SECRET=''"
echo "Spring Profile: prod"
echo "예상 결과: 오류 발생 - 'JWT 비밀키가 설정되지 않았습니다. 프로덕션 환경에서는 JWT_SECRET 환경변수를 반드시 설정해야 합니다.'"

# 테스트 3: Docker Compose 환경변수 확장 시뮬레이션
echo ""
echo "📋 테스트 3: Docker Compose 환경변수 확장 시뮬레이션"

# 시나리오 A: JWT_SECRET이 설정되지 않은 경우
unset JWT_SECRET
JWT_SECRET_RESOLVED="${JWT_SECRET:-EzUuJwKK4vLnvk5r7yAgdNP/sa1dL87febZhlayPGjI=}"
echo "시나리오 A - JWT_SECRET 미설정:"
echo "  원본: \${JWT_SECRET:-EzUuJwKK4vLnvk5r7yAgdNP/sa1dL87febZhlayPGjI=}"
echo "  결과: '$JWT_SECRET_RESOLVED'"
echo "  길이: ${#JWT_SECRET_RESOLVED} 문자"

# 시나리오 B: JWT_SECRET이 빈 문자열로 설정된 경우 (문제 상황)
export JWT_SECRET=""
JWT_SECRET_RESOLVED="${JWT_SECRET:-EzUuJwKK4vLnvk5r7yAgdNP/sa1dL87febZhlayPGjI=}"
echo "시나리오 B - JWT_SECRET이 빈 문자열:"
echo "  원본: \${JWT_SECRET:-EzUuJwKK4vLnvk5r7yAgdNP/sa1dL87febZhlayPGjI=}"
echo "  결과: '$JWT_SECRET_RESOLVED'"
echo "  길이: ${#JWT_SECRET_RESOLVED} 문자"
echo "  ⚠️  문제: 빈 문자열이 기본값 대신 사용됨!"

# 시나리오 C: JWT_SECRET이 올바른 값으로 설정된 경우
export JWT_SECRET="dGVzdC1qd3Qtc2VjcmV0LWtleS0xMjMtNDU2Nzg5MA=="
JWT_SECRET_RESOLVED="${JWT_SECRET:-EzUuJwKK4vLnvk5r7yAgdNP/sa1dL87febZhlayPGjI=}"
echo "시나리오 C - JWT_SECRET이 올바르게 설정:"
echo "  원본: \${JWT_SECRET:-EzUuJwKK4vLnvk5r7yAgdNP/sa1dL87febZhlayPGjI=}"
echo "  결과: '$JWT_SECRET_RESOLVED'"
echo "  길이: ${#JWT_SECRET_RESOLVED} 문자"

echo ""
echo "🔧 올바른 해결 방법 시뮬레이션"
echo "=============================="

# 올바른 방법 1: ${JWT_SECRET:+$JWT_SECRET} 사용
export JWT_SECRET=""
JWT_SECRET_CORRECT="${JWT_SECRET:+$JWT_SECRET}"
if [ -z "$JWT_SECRET_CORRECT" ]; then
    JWT_SECRET_CORRECT="EzUuJwKK4vLnvk5r7yAgdNP/sa1dL87febZhlayPGjI="
fi
echo "방법 1 - 조건부 확장 사용:"
echo "  JWT_SECRET이 비어있음: '$JWT_SECRET'"
echo "  처리 결과: '$JWT_SECRET_CORRECT'"

# 올바른 방법 2: 스크립트에서 명시적 검사
export JWT_SECRET=""
if [ -z "$JWT_SECRET" ]; then
    JWT_SECRET_EXPLICIT="EzUuJwKK4vLnvk5r7yAgdNP/sa1dL87febZhlayPGjI="
else
    JWT_SECRET_EXPLICIT="$JWT_SECRET"
fi
echo "방법 2 - 명시적 검사 사용:"
echo "  JWT_SECRET이 비어있음: '$JWT_SECRET'"
echo "  처리 결과: '$JWT_SECRET_EXPLICIT'"

echo ""
echo "📝 Docker Compose 사용 시 문제 재현"
echo "================================="

# 임시 docker-compose 파일 생성
cat > docker-compose.test.yml << 'EOF'
services:
  test-app:
    image: alpine:latest
    environment:
      - TEST_SECRET=${JWT_SECRET:-default-secret}
    command: sh -c 'echo "Received TEST_SECRET: [$TEST_SECRET]"; if [ -z "$TEST_SECRET" ]; then echo "SECRET is empty!"; else echo "SECRET length: ${#TEST_SECRET}"; fi'
EOF

echo "Docker Compose 파일 생성됨: docker-compose.test.yml"
echo ""

echo "테스트 A - JWT_SECRET 미설정:"
unset JWT_SECRET
docker-compose -f docker-compose.test.yml run --rm test-app 2>/dev/null || echo "Docker가 사용 불가능합니다"

echo ""
echo "테스트 B - JWT_SECRET이 빈 문자열 (문제 상황):"
export JWT_SECRET=""
docker-compose -f docker-compose.test.yml run --rm test-app 2>/dev/null || echo "Docker가 사용 불가능합니다"

# 정리
rm -f docker-compose.test.yml

echo ""
echo "🎯 문제 요약"
echo "==========="
echo "1. 문제: Docker Compose에서 \${JWT_SECRET:-default} 사용"
echo "2. 원인: 빈 문자열과 미설정 변수를 다르게 처리"
echo "3. 결과: JWT_SECRET=''일 때 빈 문자열이 컨테이너로 전달됨"
echo "4. 오류: 'JWT 비밀키가 설정되지 않았습니다. 프로덕션 환경에서는 JWT_SECRET 환경변수를 반드시 설정해야 합니다.'"
echo ""
echo "🔧 권장 해결 방법:"
echo "- Docker Compose에서 JWT_SECRET 기본값 제거"
echo "- 애플리케이션 시작 전 환경변수 유효성 검사 추가"
echo "- 배포 스크립트에서 환경변수 검증 로직 추가"