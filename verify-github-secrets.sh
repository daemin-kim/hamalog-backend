#!/bin/bash

# GitHub Secrets 배포 전 검증 스크립트
# 이 스크립트는 모든 필수 GitHub Secrets이 설정되었는지 확인합니다.

set -e

echo "================================================"
echo "Hamalog GitHub Secrets 검증 스크립트"
echo "================================================"
echo ""
echo "⚠️  주의: 이 스크립트는 로컬에서 실행되며, 실제 GitHub Secrets을 확인하려면"
echo "GitHub Repository 웹 인터페이스에서 직접 확인해야 합니다."
echo ""

# 체크리스트
REQUIRED_SECRETS=(
    "SSH_PRIVATE_KEY"
    "SERVER_HOST"
    "SERVER_USER"
    "SERVER_PORT"
    "JWT_SECRET"
    "JWT_EXPIRY"
    "JWT_REFRESH_TOKEN_EXPIRY"
    "HAMALOG_ENCRYPTION_KEY"
    "KAKAO_CLIENT_ID"
    "KAKAO_CLIENT_SECRET"
    "KAKAO_REDIRECT_URI"
    "DB_NAME"
    "DB_USERNAME"
    "DB_PASSWORD"
    "MYSQL_ROOT_PASSWORD"
    "SPRING_DATA_REDIS_PASSWORD"
    "FRONTEND_URL"
    "ALLOWED_ORIGINS"
)

echo "필수 GitHub Secrets 체크리스트:"
echo "================================================"
echo ""

for secret in "${REQUIRED_SECRETS[@]}"; do
    echo "[ ] $secret"
done

echo ""
echo "================================================"
echo "✅ 확인 방법:"
echo "================================================"
echo ""
echo "1. GitHub 저장소 방문"
echo "   https://github.com/<USERNAME>/<REPOSITORY>/settings/secrets/actions"
echo ""
echo "2. 다음 18개의 Secrets이 모두 설정되었는지 확인:"
echo "   - SSH/서버 연결: SSH_PRIVATE_KEY, SERVER_HOST, SERVER_USER, SERVER_PORT (4개)"
echo "   - JWT 설정: JWT_SECRET, JWT_EXPIRY, JWT_REFRESH_TOKEN_EXPIRY (3개)"
echo "   - 암호화: HAMALOG_ENCRYPTION_KEY (1개)"
echo "   - OAuth2: KAKAO_CLIENT_ID, KAKAO_CLIENT_SECRET, KAKAO_REDIRECT_URI (3개)"
echo "   - 데이터베이스: DB_NAME, DB_USERNAME, DB_PASSWORD, MYSQL_ROOT_PASSWORD (4개)"
echo "   - Redis: SPRING_DATA_REDIS_PASSWORD (1개)"
echo "   - 프론트엔드: FRONTEND_URL, ALLOWED_ORIGINS (2개)"
echo ""
echo "3. 모든 Secrets이 설정되었으면:"
echo "   $ git push origin main"
echo ""
echo "4. GitHub Actions 탭에서 배포 상황 모니터링"
echo ""

echo "================================================"
echo "📝 Secrets 설정 값 참고:"
echo "================================================"
echo ""
echo "로컬에서 생성한 값들 (필요시 활용):"
echo ""

echo "JWT_SECRET 예시:"
openssl rand -base64 32
echo ""

echo "HAMALOG_ENCRYPTION_KEY 예시:"
openssl rand -base64 32
echo ""

echo "강력한 비밀번호 생성:"
echo "DB_PASSWORD: $(openssl rand -base64 32 | tr -d "=+/" | cut -c1-32)"
echo "MYSQL_ROOT_PASSWORD: $(openssl rand -base64 32 | tr -d "=+/" | cut -c1-32)"
echo "SPRING_DATA_REDIS_PASSWORD: $(openssl rand -base64 32 | tr -d "=+/" | cut -c1-32)"
echo ""

echo "================================================"
echo "🔒 보안 주의사항:"
echo "================================================"
echo ""
echo "1. 생성한 값들을 절대 GitHub에 커밋하지 마세요"
echo "2. 강력한 비밀번호를 사용하세요 (12자 이상, 특수문자 포함)"
echo "3. SSH_PRIVATE_KEY는 반드시 개인 키를 사용하세요"
echo "4. 정기적으로 비밀번호를 변경하세요 (월 1회)"
echo ""

