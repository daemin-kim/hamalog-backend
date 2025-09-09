#!/bin/bash

# Hamalog 환경변수 진단 도구
# JWT_SECRET 및 기타 중요한 환경변수 상태를 확인합니다

set -euo pipefail

echo "🔍 Hamalog 환경변수 진단 도구"
echo "================================="
echo ""

# 기본 환경 정보
echo "📋 환경 정보:"
echo "   현재 사용자: $(whoami)"
echo "   현재 디렉토리: $(pwd)"
echo "   셸: ${SHELL:-'unknown'}"
echo "   운영체제: $(uname -s)"
echo "   아키텍처: $(uname -m)"
echo ""

# Spring Profile 확인
echo "🏷️  Spring Profile 설정:"
if [ -n "${SPRING_PROFILES_ACTIVE:-}" ]; then
    echo "   SPRING_PROFILES_ACTIVE: '$SPRING_PROFILES_ACTIVE'"
    case "$SPRING_PROFILES_ACTIVE" in
        *prod*)
            echo "   ⚠️  프로덕션 프로필이 활성화되어 있습니다. JWT_SECRET이 필수입니다."
            REQUIRE_JWT_SECRET=true
            ;;
        *)
            echo "   ℹ️  개발 프로필입니다. JWT_SECRET이 없으면 임시 키가 생성됩니다."
            REQUIRE_JWT_SECRET=false
            ;;
    esac
else
    echo "   SPRING_PROFILES_ACTIVE: 설정되지 않음 (기본값 사용)"
    REQUIRE_JWT_SECRET=false
fi
echo ""

# JWT_SECRET 상세 진단
echo "🔐 JWT_SECRET 상태 진단:"

# JWT_SECRET 존재 여부 확인 - 정확한 구분을 위해 순서 변경
if [ ! "${JWT_SECRET+x}" ]; then
    echo "   상태: ❌ 설정되지 않음 (unset)"
    echo "   값: (없음)"
    echo "   길이: 0"
    JWT_SECRET_STATUS="unset"
elif [ -z "$JWT_SECRET" ]; then
    echo "   상태: ⚠️  빈 문자열로 설정됨 (이것이 배포 오류의 원인!)"
    echo "   값: '$JWT_SECRET'"
    echo "   길이: ${#JWT_SECRET}"
    JWT_SECRET_STATUS="empty"
elif [ "${#JWT_SECRET}" -lt 32 ]; then
    echo "   상태: ⚠️  설정되었으나 너무 짧음"
    echo "   값: ${JWT_SECRET:0:10}... (보안상 일부만 표시)"
    echo "   길이: ${#JWT_SECRET}"
    JWT_SECRET_STATUS="too_short"
else
    echo "   상태: ✅ 정상적으로 설정됨"
    echo "   값: ${JWT_SECRET:0:10}... (보안상 일부만 표시)"
    echo "   길이: ${#JWT_SECRET}"
    JWT_SECRET_STATUS="valid"
fi

# Base64 유효성 검사 (JWT_SECRET이 있는 경우에만)
if [ -n "${JWT_SECRET:-}" ] && [ -n "$JWT_SECRET" ]; then
    echo "   Base64 유효성 검사:"
    if echo "$JWT_SECRET" | base64 -d >/dev/null 2>&1; then
        echo "      ✅ 유효한 Base64 형식"
        DECODED_LENGTH=$(echo "$JWT_SECRET" | base64 -d | wc -c | tr -d ' ')
        echo "      디코딩된 길이: ${DECODED_LENGTH}바이트 ($(( DECODED_LENGTH * 8 ))비트)"
        if [ "$DECODED_LENGTH" -ge 32 ]; then
            echo "      ✅ 256비트 이상의 키 (권장)"
        else
            echo "      ⚠️  256비트 미만의 키 (보안 위험)"
        fi
    else
        echo "      ❌ 유효하지 않은 Base64 형식"
    fi
fi
echo ""

# 데이터베이스 관련 환경변수
echo "🗄️  데이터베이스 설정:"
echo "   SPRING_DATASOURCE_URL: ${SPRING_DATASOURCE_URL:-'설정되지 않음'}"
echo "   SPRING_DATASOURCE_USERNAME: ${SPRING_DATASOURCE_USERNAME:-'설정되지 않음'}"
if [ -n "${SPRING_DATASOURCE_PASSWORD:-}" ]; then
    echo "   SPRING_DATASOURCE_PASSWORD: ✅ 설정됨 (길이: ${#SPRING_DATASOURCE_PASSWORD})"
else
    echo "   SPRING_DATASOURCE_PASSWORD: ❌ 설정되지 않음"
fi
echo ""

# Redis 관련 환경변수
echo "🔴 Redis 설정:"
echo "   SPRING_DATA_REDIS_HOST: ${SPRING_DATA_REDIS_HOST:-'설정되지 않음'}"
echo "   SPRING_DATA_REDIS_PORT: ${SPRING_DATA_REDIS_PORT:-'설정되지 않음'}"
if [ -n "${SPRING_DATA_REDIS_PASSWORD:-}" ]; then
    echo "   SPRING_DATA_REDIS_PASSWORD: ✅ 설정됨 (길이: ${#SPRING_DATA_REDIS_PASSWORD})"
else
    echo "   SPRING_DATA_REDIS_PASSWORD: ❌ 설정되지 않음"
fi
echo ""

# OAuth2 관련 환경변수
echo "🔑 OAuth2 설정:"
echo "   KAKAO_CLIENT_ID: ${KAKAO_CLIENT_ID:-'설정되지 않음'}"
if [ -n "${KAKAO_CLIENT_SECRET:-}" ]; then
    echo "   KAKAO_CLIENT_SECRET: ✅ 설정됨 (길이: ${#KAKAO_CLIENT_SECRET})"
else
    echo "   KAKAO_CLIENT_SECRET: ❌ 설정되지 않음"
fi
echo ""

# 암호화 관련 환경변수
echo "🔒 데이터 암호화 설정:"
if [ -n "${HAMALOG_ENCRYPTION_KEY:-}" ]; then
    if [ -z "$HAMALOG_ENCRYPTION_KEY" ]; then
        echo "   HAMALOG_ENCRYPTION_KEY: ⚠️  빈 문자열로 설정됨"
    else
        echo "   HAMALOG_ENCRYPTION_KEY: ✅ 설정됨 (길이: ${#HAMALOG_ENCRYPTION_KEY})"
    fi
else
    echo "   HAMALOG_ENCRYPTION_KEY: ❌ 설정되지 않음"
fi
echo ""

# 종합 진단 결과
echo "📊 종합 진단 결과:"
echo "==================="

# JWT_SECRET 관련 권장사항
case "$JWT_SECRET_STATUS" in
    "unset")
        if [ "$REQUIRE_JWT_SECRET" = true ]; then
            echo "❌ 치명적: 프로덕션 환경에서 JWT_SECRET이 설정되지 않았습니다."
            echo "   해결방법: export JWT_SECRET=\$(openssl rand -base64 32)"
            OVERALL_STATUS="critical"
        else
            echo "⚠️  경고: JWT_SECRET이 설정되지 않았습니다."
            echo "   개발 환경에서는 자동으로 임시 키가 생성됩니다."
            echo "   권장사항: export JWT_SECRET=\$(openssl rand -base64 32)"
            OVERALL_STATUS="warning"
        fi
        ;;
    "empty")
        echo "❌ 치명적: JWT_SECRET이 빈 문자열로 설정되어 있습니다."
        echo "   해결방법: export JWT_SECRET=\$(openssl rand -base64 32)"
        OVERALL_STATUS="critical"
        ;;
    "too_short")
        echo "❌ 치명적: JWT_SECRET이 너무 짧습니다 (최소 32자 필요)."
        echo "   해결방법: export JWT_SECRET=\$(openssl rand -base64 32)"
        OVERALL_STATUS="critical"
        ;;
    "valid")
        echo "✅ JWT_SECRET이 올바르게 설정되었습니다."
        if [ "${OVERALL_STATUS:-}" != "critical" ]; then
            OVERALL_STATUS="good"
        fi
        ;;
esac

# 기타 중요한 설정 확인
if [ "$REQUIRE_JWT_SECRET" = true ]; then
    if [ -z "${SPRING_DATASOURCE_URL:-}" ] || [[ "${SPRING_DATASOURCE_URL:-}" == *"h2"* ]]; then
        echo "⚠️  경고: 프로덕션 환경에서 H2 데이터베이스를 사용하고 있습니다."
        OVERALL_STATUS="warning"
    fi
fi

echo ""
echo "🎯 최종 상태: "
case "${OVERALL_STATUS:-unknown}" in
    "good")
        echo "✅ 환경설정이 양호합니다."
        ;;
    "warning")
        echo "⚠️  일부 개선이 필요합니다."
        ;;
    "critical")
        echo "❌ 치명적인 설정 문제가 있습니다. 배포 전에 수정이 필요합니다."
        ;;
    *)
        echo "❓ 진단을 완료할 수 없습니다."
        ;;
esac

echo ""
echo "📖 추가 도움말:"
echo "   - JWT_SECRET 생성: openssl rand -base64 32"
echo "   - 설정 확인: echo \$JWT_SECRET"
echo "   - 환경변수 영구 설정: ~/.bashrc 또는 ~/.zshrc에 export 추가"
echo "   - Docker 실행 시: docker run -e JWT_SECRET=\$JWT_SECRET ..."
echo ""

# 진단 결과에 따른 종료 코드 설정
case "${OVERALL_STATUS:-unknown}" in
    "good")
        exit 0
        ;;
    "warning")
        exit 1
        ;;
    "critical")
        exit 2
        ;;
    *)
        exit 3
        ;;
esac