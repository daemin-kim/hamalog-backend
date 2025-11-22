#!/bin/bash

# GitHub Secrets 자동 생성 스크립트
# 이 스크립트는 안전한 크레덴셜을 생성하고 GitHub Secrets 설정을 위한 값을 출력합니다.

set -e

echo "================================================"
echo "Hamalog GitHub Secrets Generator"
echo "================================================"
echo ""
echo "이 스크립트는 GitHub Secrets에 설정할 안전한 크레덴셜을 생성합니다."
echo ""

# Generate credentials
JWT_SECRET=$(openssl rand -base64 32)
HAMALOG_ENCRYPTION_KEY=$(openssl rand -base64 32)
DB_PASSWORD=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-32)
MYSQL_ROOT_PASSWORD=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-32)
REDIS_PASSWORD=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-32)

echo "✅ 크레덴셜 생성 완료!"
echo ""
echo "================================================"
echo "다음 값들을 GitHub Repository Secrets에 설정하세요:"
echo "================================================"
echo ""
echo "Repository → Settings → Secrets and variables → Actions → New repository secret"
echo ""

cat << EOF
---------------------------------------------------
필수 Secrets (보안 크레덴셜):
---------------------------------------------------

Name: JWT_SECRET
Value: ${JWT_SECRET}

Name: HAMALOG_ENCRYPTION_KEY
Value: ${HAMALOG_ENCRYPTION_KEY}

Name: DB_PASSWORD
Value: ${DB_PASSWORD}

Name: MYSQL_ROOT_PASSWORD
Value: ${MYSQL_ROOT_PASSWORD}

Name: SPRING_DATA_REDIS_PASSWORD
Value: ${REDIS_PASSWORD}

---------------------------------------------------
필수 Secrets (카카오 OAuth2 - 수동 설정 필요):
---------------------------------------------------

Name: KAKAO_CLIENT_ID
Value: <카카오_개발자_콘솔에서_발급받은_REST_API_키>

Name: KAKAO_CLIENT_SECRET
Value: <카카오_개발자_콘솔에서_발급받은_Client_Secret>

---------------------------------------------------
선택 Secrets (기본값 있음):
---------------------------------------------------

Name: JWT_EXPIRY
Value: 900000

Name: JWT_REFRESH_TOKEN_EXPIRY
Value: 604800000

Name: KAKAO_REDIRECT_URI
Value: http://49.142.154.182:8080/oauth2/auth/kakao/callback

Name: DB_NAME
Value: Hamalog

Name: DB_USERNAME
Value: hamalog_user

Name: FRONTEND_URL
Value: http://49.142.154.182:3000

Name: ALLOWED_ORIGINS
Value: http://49.142.154.182:3000,http://49.142.154.182:8080

---------------------------------------------------
서버 연결 Secrets:
---------------------------------------------------

Name: SERVER_HOST
Value: 49.142.154.182

Name: SERVER_USER
Value: <서버_SSH_사용자명_예:_ubuntu>

Name: SERVER_PORT
Value: 22

Name: SSH_PRIVATE_KEY
Value: <서버_SSH_개인_키_전체_내용>

---------------------------------------------------
EOF

echo ""
echo "================================================"
echo "설정 방법:"
echo "================================================"
echo ""
echo "1. GitHub Repository → Settings → Secrets and variables → Actions"
echo "2. 'New repository secret' 클릭"
echo "3. 위 Name과 Value를 복사하여 하나씩 추가"
echo "4. 모든 Secrets 추가 후 git push origin main"
echo ""
echo "⚠️  중요: KAKAO_CLIENT_ID와 KAKAO_CLIENT_SECRET는"
echo "   카카오 개발자 콘솔(https://developers.kakao.com)에서 발급받아야 합니다."
echo ""
echo "================================================"
echo ""

# Save to file for reference
SECRETS_FILE="github-secrets-$(date +%Y%m%d_%H%M%S).txt"
cat > "$SECRETS_FILE" << EOF
# Hamalog GitHub Secrets Configuration
# Generated: $(date)
# ⚠️  이 파일은 절대 Git에 커밋하지 마세요!

JWT_SECRET=${JWT_SECRET}
HAMALOG_ENCRYPTION_KEY=${HAMALOG_ENCRYPTION_KEY}
DB_PASSWORD=${DB_PASSWORD}
MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
SPRING_DATA_REDIS_PASSWORD=${REDIS_PASSWORD}

# 카카오 개발자 콘솔에서 발급받아 설정:
# KAKAO_CLIENT_ID=
# KAKAO_CLIENT_SECRET=

# 기본값 (필요시 변경):
JWT_EXPIRY=900000
JWT_REFRESH_TOKEN_EXPIRY=604800000
KAKAO_REDIRECT_URI=http://49.142.154.182:8080/oauth2/auth/kakao/callback
DB_NAME=Hamalog
DB_USERNAME=hamalog_user
FRONTEND_URL=http://49.142.154.182:3000
ALLOWED_ORIGINS=http://49.142.154.182:3000,http://49.142.154.182:8080

# 서버 설정:
SERVER_HOST=49.142.154.182
# SERVER_USER=
# SERVER_PORT=22
# SSH_PRIVATE_KEY=
EOF

chmod 600 "$SECRETS_FILE"

echo "📁 크레덴셜이 다음 파일에 저장되었습니다: $SECRETS_FILE"
echo "🔒 파일 권한: 600 (소유자만 읽기/쓰기)"
echo ""
echo "⚠️  이 파일을 안전한 곳에 백업하세요 (비밀번호 관리자 등)"
echo "⚠️  절대 Git에 커밋하지 마세요!"
echo ""

