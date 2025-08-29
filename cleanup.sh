#!/bin/bash

# Hamalog 임시 파일 정리 스크립트
# 로그 파일, 빌드 산출물, Docker 임시 파일 등을 정리합니다

echo "🧹 Hamalog 임시 파일 정리를 시작합니다..."

# 로그 파일 정리
if [ -d "logs" ]; then
    echo "📄 로그 파일 정리 중..."
    rm -rf logs/*.log 2>/dev/null || true
    echo "   ✅ 로그 파일 삭제 완료"
else
    echo "   ℹ️  로그 디렉토리가 없습니다"
fi

# 빌드 산출물 정리
echo "🔨 빌드 산출물 정리 중..."
if [ -d "build" ]; then
    rm -rf build/
    echo "   ✅ build/ 디렉토리 삭제 완료"
fi

if [ -d ".gradle" ]; then
    rm -rf .gradle/
    echo "   ✅ .gradle/ 디렉토리 삭제 완료"
fi

# Gradle 캐시 정리
./gradlew clean >/dev/null 2>&1 && echo "   ✅ Gradle clean 완료" || echo "   ⚠️  Gradle clean 실행 실패 (무시됨)"

# 임시 파일 정리
echo "📁 기타 임시 파일 정리 중..."
find . -name "*.tmp" -type f -delete 2>/dev/null && echo "   ✅ *.tmp 파일 삭제 완료" || true
find . -name "*.temp" -type f -delete 2>/dev/null && echo "   ✅ *.temp 파일 삭제 완료" || true
find . -name "*.cache" -type f -delete 2>/dev/null && echo "   ✅ *.cache 파일 삭제 완료" || true
find . -name "*~" -type f -delete 2>/dev/null && echo "   ✅ *~ 파일 삭제 완료" || true
find . -name "*.bak" -type f -delete 2>/dev/null && echo "   ✅ *.bak 파일 삭제 완료" || true

# Docker 리소스 정리 (옵션)
if [ "$1" = "--docker" ] || [ "$1" = "-d" ]; then
    echo "🐳 Docker 리소스 정리 중..."
    
    # 로컬 개발 컨테이너 정리
    if docker-compose -f docker-compose.local.yml ps -q >/dev/null 2>&1; then
        echo "   🔄 로컬 개발 컨테이너 중지 및 정리 중..."
        docker-compose -f docker-compose.local.yml down --volumes --remove-orphans >/dev/null 2>&1
        echo "   ✅ 로컬 개발 컨테이너 정리 완료"
    fi
    
    # 사용하지 않는 Docker 이미지 정리
    echo "   🖼️  사용하지 않는 Docker 이미지 정리 중..."
    docker image prune -f >/dev/null 2>&1 && echo "   ✅ Docker 이미지 정리 완료" || echo "   ⚠️  Docker 이미지 정리 실패 (무시됨)"
    
    # 사용하지 않는 Docker 볼륨 정리
    echo "   💾 사용하지 않는 Docker 볼륨 정리 중..."
    docker volume prune -f >/dev/null 2>&1 && echo "   ✅ Docker 볼륨 정리 완료" || echo "   ⚠️  Docker 볼륨 정리 실패 (무시됨)"
    
    # 사용하지 않는 Docker 네트워크 정리
    echo "   🌐 사용하지 않는 Docker 네트워크 정리 중..."
    docker network prune -f >/dev/null 2>&1 && echo "   ✅ Docker 네트워크 정리 완료" || echo "   ⚠️  Docker 네트워크 정리 실패 (무시됨)"
fi

# IDE 임시 파일 정리
echo "💻 IDE 임시 파일 정리 중..."
find . -name ".DS_Store" -type f -delete 2>/dev/null && echo "   ✅ .DS_Store 파일 삭제 완료" || true
find . -name "Thumbs.db" -type f -delete 2>/dev/null && echo "   ✅ Thumbs.db 파일 삭제 완료" || true

echo ""
echo "✨ 정리 완료!"
echo ""
echo "📋 정리된 항목:"
echo "   - 로그 파일 (logs/*.log)"
echo "   - 빌드 산출물 (build/, .gradle/)"
echo "   - 임시 파일 (*.tmp, *.temp, *.cache, *~, *.bak)"
echo "   - IDE 임시 파일 (.DS_Store, Thumbs.db)"

if [ "$1" = "--docker" ] || [ "$1" = "-d" ]; then
    echo "   - Docker 리소스 (컨테이너, 이미지, 볼륨, 네트워크)"
fi

echo ""
echo "💡 사용법:"
echo "   ./cleanup.sh           # 기본 정리"
echo "   ./cleanup.sh --docker  # Docker 리소스 포함 정리"
echo "   ./cleanup.sh -d        # Docker 리소스 포함 정리 (축약형)"