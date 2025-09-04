#!/bin/bash

# Hamalog 프로덕션 배포 스크립트
# 이 스크립트는 Docker Compose를 사용하여 Hamalog 애플리케이션 배포를 처리합니다

set -euo pipefail  # 오류, 정의되지 않은 변수, 파이프 실패 시 종료

# 설정
PROJECT_NAME="hamalog"
REGISTRY="ghcr.io"
IMAGE_NAME="${REGISTRY}/${GITHUB_REPOSITORY:-daemin-kim/hamalog-backend}"
IMAGE_TAG="${GITHUB_SHA:-latest}"
COMPOSE_FILE="docker-compose.prod.yml"

echo "🚀 Hamalog 배포 프로세스를 시작합니다..."
echo "📋 배포 설정:"
echo "   - 레지스트리: ${REGISTRY}"
echo "   - 이미지: ${IMAGE_NAME}:${IMAGE_TAG}"
echo "   - 프로젝트: ${PROJECT_NAME}"

# 스크립트 종료 시 정리 작업을 처리하는 함수
cleanup() {
    local exit_code=$?
    if [ $exit_code -ne 0 ]; then
        echo "❌ 배포가 실패했습니다. 종료 코드: $exit_code"
        echo "🔍 컨테이너 상태를 확인하는 중..."
        docker compose -p ${PROJECT_NAME} ps || true
        echo "📋 최근 로그:"
        docker compose -p ${PROJECT_NAME} logs --tail=50 || true
    fi
    exit $exit_code
}

trap cleanup EXIT

# 1단계: 프로덕션 docker-compose 파일 생성
echo "📝 프로덕션 docker-compose 설정을 생성하는 중..."
cat > ${COMPOSE_FILE} << EOF
services:
  hamalog-app:
    image: ${IMAGE_NAME}:${IMAGE_TAG}
    ports:
      - "8080:8080"
    environment:
      # Spring Profile Configuration
      - SPRING_PROFILES_ACTIVE=\${SPRING_PROFILES_ACTIVE:-prod}
      
      # Database Configuration
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql-hamalog:3306/\${DB_NAME:-Hamalog}
      - SPRING_DATASOURCE_USERNAME=\${DB_USERNAME:-hamalog_user}
      - SPRING_DATASOURCE_PASSWORD=\${DB_PASSWORD:-hamalog_password}
      
      # Redis Configuration
      - SPRING_DATA_REDIS_HOST=redis
      - SPRING_DATA_REDIS_PORT=6379
      
      # JWT Configuration
      - JWT_SECRET=\${JWT_SECRET:-EzUuJwKK4vLnvk5r7yAgdNP/sa1dL87febZhlayPGjI=}
      - JWT_EXPIRY=\${JWT_EXPIRY:-3600000}
      
      # OAuth2 Configuration
      - KAKAO_CLIENT_ID=\${KAKAO_CLIENT_ID:-dummy-client-id}
      - KAKAO_CLIENT_SECRET=\${KAKAO_CLIENT_SECRET:-dummy-client-secret}
    depends_on:
      mysql-hamalog:
        condition: service_healthy
      redis:
        condition: service_healthy
    volumes:
      - hamalog-uploads:/data/hamalog/images
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes
    volumes:
      - redis-data:/data
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 30s
      timeout: 10s
      retries: 3

  mysql-hamalog:
    image: mysql:8.0
    ports:
      - "3306:3306"
    environment:
      - MYSQL_ROOT_PASSWORD=\${MYSQL_ROOT_PASSWORD:-root_password}
      - MYSQL_DATABASE=\${DB_NAME:-Hamalog}
      - MYSQL_USER=\${DB_USERNAME:-hamalog_user}
      - MYSQL_PASSWORD=\${DB_PASSWORD:-hamalog_password}
    volumes:
      - mysql-data:/var/lib/mysql
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p\${MYSQL_ROOT_PASSWORD:-root_password}"]
      interval: 30s
      timeout: 10s
      retries: 3

volumes:
  mysql-data:
  redis-data:
  hamalog-uploads:
EOF

echo "✅ 프로덕션 docker-compose 설정이 생성되었습니다"

# 2단계: 기존 컨테이너 중지 및 제거
echo "🛑 기존 컨테이너를 중지하는 중..."
if docker compose -p ${PROJECT_NAME} ps -q | grep -q .; then
    docker compose -p ${PROJECT_NAME} down --remove-orphans || {
        echo "⚠️  컨테이너를 정상적으로 중지하지 못했습니다. 강제 제거 중..."
        docker compose -p ${PROJECT_NAME} kill || true
        docker compose -p ${PROJECT_NAME} rm -f || true
    }
    echo "✅ 기존 컨테이너가 중지되고 제거되었습니다"
else
    echo "ℹ️  기존 컨테이너를 찾을 수 없습니다"
fi

# 3단계: 최신 이미지 가져오기
echo "⬇️  최신 이미지를 가져오는 중..."
echo "🔍 대상 이미지: ${IMAGE_NAME}:${IMAGE_TAG}"

# 레지스트리에 특정 이미지가 존재하는지 먼저 확인
echo "🔍 레지스트리에서 이미지 존재 여부를 확인하는 중..."
echo "주요 대상: ${IMAGE_NAME}:${IMAGE_TAG}"

# 주요 SHA 기반 태그를 먼저 시도
if docker manifest inspect "${IMAGE_NAME}:${IMAGE_TAG}" > /dev/null 2>&1; then
    echo "✅ 주요 SHA 기반 태그를 찾았습니다: ${IMAGE_NAME}:${IMAGE_TAG}"
else
    echo "❌ 주요 SHA 기반 태그를 찾을 수 없습니다: ${IMAGE_NAME}:${IMAGE_TAG}"
    
    # 일치할 수 있는 최근 태그가 있는지 확인
    echo "🔍 대체 태그를 확인하는 중..."
    
    # sha- 접두사로 시도 (metadata-action이 접두사를 추가하는 경우)
    ALT_TAG="sha-${IMAGE_TAG}"
    echo "대체 태그 시도 중: ${IMAGE_NAME}:${ALT_TAG}"
    if docker manifest inspect "${IMAGE_NAME}:${ALT_TAG}" > /dev/null 2>&1; then
        echo "✅ 대체 SHA 태그를 찾았습니다: ${IMAGE_NAME}:${ALT_TAG}"
        IMAGE_TAG="${ALT_TAG}"
        sed -i "s|image: ${IMAGE_NAME}:.*|image: ${IMAGE_NAME}:${IMAGE_TAG}|" ${COMPOSE_FILE}
    else
        echo "❌ 대체 SHA 태그도 찾을 수 없습니다"
        
        # 최종 대체 방안으로 latest 시도
        echo "🔄 최종 대체 방안으로 'latest' 태그 사용을 시도하는 중..."
        if docker manifest inspect "${IMAGE_NAME}:latest" > /dev/null 2>&1; then
            IMAGE_TAG="latest"
            echo "✅ latest 태그를 사용합니다: ${IMAGE_NAME}:${IMAGE_TAG}"
            sed -i "s|image: ${IMAGE_NAME}:.*|image: ${IMAGE_NAME}:${IMAGE_TAG}|" ${COMPOSE_FILE}
        else
            echo "❌ 'latest' 태그도 찾을 수 없습니다. 심각한 빌드/푸시 문제를 나타냅니다."
            echo "🔍 레지스트리 연결성을 확인하는 중..."
            curl -s "https://ghcr.io/v2/" > /dev/null && echo "✅ 레지스트리에 접근 가능합니다" || echo "❌ 레지스트리 접근이 실패했습니다"
        fi
    fi
fi

echo "📝 최종 대상 이미지: ${IMAGE_NAME}:${IMAGE_TAG}"

docker compose -f ${COMPOSE_FILE} -p ${PROJECT_NAME} pull || {
    echo "❌ 이미지를 가져오는데 실패했습니다. 로컬에 이미지가 있는지 확인하는 중..."
    
    # 이 저장소 이름을 가진 모든 이미지 나열
    echo "🔍 저장소 ${IMAGE_NAME}의 로컬 이미지:"
    docker images "${IMAGE_NAME}" 2>/dev/null || echo "${IMAGE_NAME}에 대한 로컬 이미지를 찾을 수 없습니다"
    
    # 특정 태그가 로컬에 존재하는지 확인
    if docker images "${IMAGE_NAME}:${IMAGE_TAG}" --format "{{.Repository}}:{{.Tag}}" | grep -q "${IMAGE_NAME}:${IMAGE_TAG}"; then
        echo "ℹ️  로컬 이미지 ${IMAGE_NAME}:${IMAGE_TAG}를 사용합니다"
    else
        echo "❌ 이미지 ${IMAGE_NAME}:${IMAGE_TAG}를 로컬에서도 찾을 수 없습니다"
        echo "🔍 사용 가능한 모든 로컬 이미지 나열:"
        docker images | head -20
        exit 1
    fi
}

# 4단계: 애플리케이션 시작
echo "🚀 애플리케이션 서비스를 시작하는 중..."
docker compose -f ${COMPOSE_FILE} -p ${PROJECT_NAME} up -d

# 5단계: 서비스가 정상 상태가 될 때까지 대기
echo "⏳ 서비스가 준비될 때까지 대기 중..."
max_wait=300  # 5 minutes (increased for database initialization)
wait_time=0

echo "🔍 서비스 의존성을 먼저 확인하는 중..."
echo "  - MySQL 상태 확인 간격: 30초"
echo "  - Redis 상태 확인 간격: 30초" 
echo "  - 애플리케이션 상태 확인 간격: 30초"
echo "  - 예상 총 시작 시간: ~2-3분"

while [ $wait_time -lt $max_wait ]; do
    # 개별 서비스 상태 확인
    mysql_healthy=$(docker compose -f ${COMPOSE_FILE} -p ${PROJECT_NAME} ps mysql-hamalog --format "{{.Health}}" 2>/dev/null || echo "starting")
    redis_healthy=$(docker compose -f ${COMPOSE_FILE} -p ${PROJECT_NAME} ps redis --format "{{.Health}}" 2>/dev/null || echo "starting")
    app_healthy=$(docker compose -f ${COMPOSE_FILE} -p ${PROJECT_NAME} ps hamalog-app --format "{{.Health}}" 2>/dev/null || echo "starting")
    
    echo "⏳ 서비스 상태 (${wait_time}초/${max_wait}초):"
    echo "  - MySQL: ${mysql_healthy}"
    echo "  - Redis: ${redis_healthy}"
    echo "  - 애플리케이션: ${app_healthy}"
    
    if [ "$app_healthy" = "healthy" ]; then
        echo "✅ 애플리케이션이 정상 상태이며 준비되었습니다!"
        break
    fi
    
    # 너무 오래 걸리면 더 자세한 정보 표시
    if [ $wait_time -gt 120 ]; then
        echo "🔍 자세한 서비스 상태:"
        docker compose -f ${COMPOSE_FILE} -p ${PROJECT_NAME} ps
        
        # 너무 오래 시도하고 있으면 앱 로그 표시
        if [ $wait_time -gt 180 ]; then
            echo "📋 최근 애플리케이션 로그:"
            docker compose -f ${COMPOSE_FILE} -p ${PROJECT_NAME} logs hamalog-app --tail=20
        fi
    fi
    
    sleep 15
    wait_time=$((wait_time + 15))
done

if [ $wait_time -ge $max_wait ]; then
    echo "❌ 서비스가 ${max_wait}초 내에 정상 상태가 되지 않았습니다"
    echo "📋 최종 서비스 상태:"
    docker compose -f ${COMPOSE_FILE} -p ${PROJECT_NAME} ps
    echo "📋 애플리케이션 로그:"
    docker compose -f ${COMPOSE_FILE} -p ${PROJECT_NAME} logs hamalog-app --tail=100
    exit 1
fi

# 6단계: 최종 확인
echo "🔍 최종 배포 확인 중..."
docker compose -f ${COMPOSE_FILE} -p ${PROJECT_NAME} ps

echo "🎉 배포가 성공적으로 완료되었습니다!"
echo "✅ Hamalog 애플리케이션이 현재 실행 중입니다"
echo "🌐 애플리케이션 접근 주소: http://localhost:8080"
echo "📊 상태 확인 엔드포인트: http://localhost:8080/actuator/health"

# 임시 파일 정리
rm -f ${COMPOSE_FILE}
echo "🧹 정리 작업이 완료되었습니다"