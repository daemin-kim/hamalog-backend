#!/bin/bash

# GitHub Actions 배포 준비 상태 점검 스크립트

echo "================================================"
echo "📊 Hamalog GitHub Actions 배포 준비 상태 점검"
echo "================================================"
echo ""

# 1. SSH 키 확인
echo "1️⃣ SSH 키 확인..."
if [ -f ~/.ssh/id_rsa ]; then
    echo "✅ SSH 개인키: ~/.ssh/id_rsa 존재"
    echo "   크기: $(wc -c < ~/.ssh/id_rsa) bytes"
    echo "   권한: $(stat -f %OLp ~/.ssh/id_rsa 2>/dev/null || stat -c %a ~/.ssh/id_rsa)"
else
    echo "❌ SSH 개인키 없음"
fi

if [ -f ~/.ssh/id_rsa.pub ]; then
    echo "✅ SSH 공개키: ~/.ssh/id_rsa.pub 존재"
else
    echo "❌ SSH 공개키 없음"
fi
echo ""

# 2. Git 저장소 확인
echo "2️⃣ Git 저장소 확인..."
if [ -d .git ]; then
    echo "✅ Git 저장소: 존재"
    echo "   현재 브랜치: $(git rev-parse --abbrev-ref HEAD)"
    echo "   최신 커밋: $(git log --oneline -1)"
else
    echo "❌ Git 저장소: 없음"
fi
echo ""

# 3. GitHub CLI 확인
echo "3️⃣ GitHub CLI 확인..."
if command -v gh &> /dev/null; then
    echo "✅ GitHub CLI: 설치됨 ($(gh --version))"

    # GitHub CLI 로그인 상태 확인
    if gh auth status > /dev/null 2>&1; then
        echo "✅ GitHub CLI: 로그인됨"
        echo "   계정: $(gh api user --jq .login)"
    else
        echo "⚠️  GitHub CLI: 로그인 필요"
        echo "   해결: gh auth login"
    fi
else
    echo "❌ GitHub CLI: 설치 안됨"
    echo "   설치: brew install gh"
fi
echo ""

# 4. GitHub Secrets 확인
echo "4️⃣ GitHub Secrets 확인..."
if command -v gh &> /dev/null && gh auth status > /dev/null 2>&1; then
    echo "✅ 설정된 Secrets:"
    gh secret list 2>/dev/null | while read line; do
        echo "   $line"
    done
else
    echo "⚠️  GitHub CLI 로그인 필요 (로그인 후 확인 가능)"
fi
echo ""

# 5. 필수 문서 확인
echo "5️⃣ 설정 문서 확인..."
docs=(
    "SSH_KEY_SETUP_FINAL.md"
    "GITHUB_SECRETS_TODO.md"
    "SSH_KEY_QUICK_SETUP.md"
)

for doc in "${docs[@]}"; do
    if [ -f "$doc" ]; then
        echo "✅ $doc: 존재"
    else
        echo "❌ $doc: 없음"
    fi
done
echo ""

# 6. 최종 요약
echo "================================================"
echo "📋 최종 요약"
echo "================================================"
echo ""
echo "다음 단계:"
echo ""
echo "1️⃣ GitHub CLI 로그인 (아직 안 했으면):"
echo "   $ gh auth login"
echo ""
echo "2️⃣ Secrets 설정 (CLI 자동화):"
echo "   $ gh secret set SSH_PRIVATE_KEY < ~/.ssh/id_rsa"
echo "   $ gh secret set SERVER_PORT -b '22'"
echo "   $ gh secret set JWT_EXPIRY -b '900000'"
echo "   $ gh secret set JWT_REFRESH_TOKEN_EXPIRY -b '604800000'"
echo "   $ gh secret set KAKAO_REDIRECT_URI -b 'http://49.142.154.182:8080/oauth2/auth/kakao/callback'"
echo "   $ gh secret set DB_NAME -b 'Hamalog'"
echo "   $ gh secret set DB_USERNAME -b 'hamalog_user'"
echo ""
echo "3️⃣ 배포:"
echo "   $ git push origin main"
echo ""
echo "또는 GitHub UI에서 수동으로 7개 Secrets 추가 후:"
echo "   $ git push origin main"
echo ""

