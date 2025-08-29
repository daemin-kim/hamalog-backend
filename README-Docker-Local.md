# 로컬 개발환경 Docker 설정 가이드

## 개요
이 가이드는 배포 환경과 동일한 설정으로 로컬에서 Hamalog 애플리케이션을 실행하는 방법을 설명합니다.

## ⚡ 빠른 시작 (권장)

배포 환경과 동일한 설정으로 애플리케이션을 **즉시** 실행하려면:

```bash
./run-local.sh
```

이 스크립트는 배포 환경과 동일한 순서로 다음을 자동 수행합니다:
- ✅ Docker 상태 확인
- 🔨 애플리케이션 빌드 (Gradle)
- 🚀 컨테이너 빌드 및 시작 (의존성 순서 보장)
- ⏳ 서비스 준비 대기 (30초)
- 🔍 헬스체크 수행 (10회 재시도, 5초 간격)
- 📋 접속 정보 제공

## 📋 배포 환경과 동일한 실행 순서

### 1단계: 사전 준비사항 확인

**반드시 다음 순서대로 확인하세요:**

```bash
# 1-1. Docker 실행 상태 확인 (필수)
docker info
# ❌ 오류 발생시: Docker Desktop을 시작하고 다시 시도

# 1-2. 프로젝트 루트 디렉토리 확인
pwd
# 결과: /path/to/Hamalog (프로젝트 루트여야 함)

# 1-3. 필수 파일 존재 확인
ls -la docker-compose.local.yml gradlew run-local.sh
# 모든 파일이 존재해야 함
```

### 2단계: 애플리케이션 빌드 (배포환경과 동일)

```bash
# 2-1. Gradle 빌드 (배포환경 1단계와 동일)
./gradlew clean build

# 2-2. 빌드 결과 확인
ls -la build/libs/
# Hamalog-*.jar 파일이 생성되어야 함
```

**⚠️ 중요**: 빌드가 실패하면 다음 단계로 진행하지 마세요.

### 3단계: 컨테이너 환경 준비

```bash
# 3-1. 기존 컨테이너 정리 (깨끗한 환경 보장)
docker-compose -f docker-compose.local.yml down --remove-orphans

# 3-2. Docker 이미지 빌드 및 컨테이너 시작
docker-compose -f docker-compose.local.yml up --build -d

# 3-3. 컨테이너 시작 확인
docker-compose -f docker-compose.local.yml ps
```

**예상 출력:**
```
NAME                  IMAGE               COMMAND             SERVICE         CREATED          STATUS          PORTS
hamalog-mysql         mysql:8.0          "docker-entrypoint" mysql-hamalog   X seconds ago    Up X seconds    0.0.0.0:3306->3306/tcp
hamalog-redis         redis:7-alpine     "redis-server"      redis           X seconds ago    Up X seconds    0.0.0.0:6379->6379/tcp  
hamalog-app           hamalog_hamalog-app "sh -c ..."        hamalog-app     X seconds ago    Up X seconds    0.0.0.0:8080->8080/tcp
```

### 4단계: 서비스 초기화 대기 (배포환경과 동일 타이밍)

```bash
# 4-1. 데이터베이스 초기화 대기 (30초 - 배포환경과 동일)
echo "⏳ 데이터베이스 및 애플리케이션 초기화 대기 중... (30초)"
sleep 30
```

**왜 30초인가?**
- MySQL 컨테이너 완전 초기화: ~15초
- Spring Boot 애플리케이션 시작: ~10초
- JPA 스키마 생성 및 검증: ~5초
- ✅ 배포환경과 동일한 대기시간

### 5단계: 헬스체크 및 검증 (배포환경과 동일)

```bash
# 5-1. 애플리케이션 헬스체크 (배포환경과 동일한 재시도 로직)
echo "🔍 애플리케이션 상태 확인 중..."

max_attempts=10
attempt=1

while [ $attempt -le $max_attempts ]; do
    echo "시도 $attempt/$max_attempts: 헬스체크 수행 중..."
    
    if curl -f -s -m 10 http://localhost:8080/actuator/health >/dev/null 2>&1; then
        echo "✅ 애플리케이션이 성공적으로 시작되었습니다!"
        break
    fi
    
    if [ $attempt -eq $max_attempts ]; then
        echo "❌ 애플리케이션 시작 실패 ($max_attempts회 시도 후)"
        echo "📋 로그 확인:"
        docker-compose -f docker-compose.local.yml logs hamalog-app --tail=50
        exit 1
    fi
    
    echo "⏳ 5초 후 재시도..."
    sleep 5
    attempt=$((attempt + 1))
done
```

### 6단계: 최종 확인 및 접속 정보

```bash
# 6-1. 모든 서비스 상태 최종 확인
docker-compose -f docker-compose.local.yml ps

# 6-2. 애플리케이션 접속 테스트
curl -s http://localhost:8080/actuator/health | jq '.' || curl -s http://localhost:8080/actuator/health
```

**✅ 성공시 출력:**
```json
{
  "status": "UP"
}
```

**📌 접속 정보:**
- 🌐 애플리케이션: http://localhost:8080
- 📊 헬스체크: http://localhost:8080/actuator/health
- 🗄️ MySQL: localhost:3306 (hamalog_user/hamalog_password)
- 📦 Redis: localhost:6379

## 🛠️ 수동 실행 방법 (상급자용)

위의 1-6단계를 자동화한 것이 `./run-local.sh` 스크립트입니다. 
문제 발생시 단계별로 수동 실행하여 디버깅하세요.

## 🔧 중지 및 정리

```bash
# 애플리케이션 중지 (데이터 보존)
docker-compose -f docker-compose.local.yml stop

# 완전 삭제 (데이터베이스 포함)
docker-compose -f docker-compose.local.yml down --volumes

# 재시작
./run-local.sh
```

## 🧹 임시 파일 정리

### 자동 정리 (권장)

```bash
# 기본 임시 파일 정리
./cleanup.sh

# Docker 리소스 포함 전체 정리
./cleanup.sh --docker
./cleanup.sh -d  # 축약형

# 정리 후 깨끗한 환경으로 애플리케이션 시작
./run-local.sh --clean
./run-local.sh -c  # 축약형
```

### 정리되는 파일들

**기본 정리 (`./cleanup.sh`)**:
- 📄 로그 파일: `logs/*.log`
- 🔨 빌드 산출물: `build/`, `.gradle/`
- 📁 임시 파일: `*.tmp`, `*.temp`, `*.cache`, `*~`, `*.bak`
- 💻 IDE 임시 파일: `.DS_Store`, `Thumbs.db`

**Docker 포함 정리 (`./cleanup.sh --docker`)**:
- 위의 모든 파일 +
- 🐳 로컬 개발 컨테이너 및 볼륨
- 🖼️ 사용하지 않는 Docker 이미지
- 💾 사용하지 않는 Docker 볼륨
- 🌐 사용하지 않는 Docker 네트워크

## 환경 구성

### 배포환경과 동일한 설정
- **Spring Profile**: `prod` (배포환경과 동일)
- **데이터베이스**: MySQL 8.0 (컨테이너)
- **캐시**: Redis 7 (컨테이너)
- **로그**: JSON 구조화 로그 (배포환경과 동일)

### 컨테이너 구성
- `hamalog-app`: Spring Boot 애플리케이션
- `mysql-hamalog`: MySQL 8.0 데이터베이스
- `redis`: Redis 7 캐시 서버

### 포트 매핑
- 애플리케이션: http://localhost:8080
- MySQL: localhost:3306
- Redis: localhost:6379

### 데이터베이스 접속 정보
- **Host**: localhost:3306
- **Database**: Hamalog
- **Username**: hamalog_user
- **Password**: hamalog_password
- **Root Password**: root_password

## 주요 차이점

### 기존 로컬 개발 환경 (application-local.properties)
- H2 인메모리 데이터베이스
- 로컬호스트 Redis
- 개발용 더미 OAuth2 설정
- 디버그 로깅

### 새로운 Docker 로컬 환경 (docker-compose.local.yml)
- MySQL 데이터베이스 (배포환경과 동일)
- Redis 컨테이너 (배포환경과 동일)
- Production Spring Profile (배포환경과 동일)
- JSON 구조화 로깅 (배포환경과 동일)

## 장점

1. **배포환경 일치**: 로컬에서 배포환경과 동일한 조건으로 테스트 가능
2. **환경 격리**: Docker 컨테이너로 독립적인 환경 제공
3. **쉬운 초기화**: 언제든지 깨끗한 상태로 재시작 가능
4. **팀 협업**: 모든 개발자가 동일한 환경에서 작업

## 문제해결

### 컨테이너 시작 실패
```bash
# 모든 컨테이너 정리
docker-compose -f docker-compose.local.yml down --volumes --remove-orphans

# 이미지 재빌드
docker-compose -f docker-compose.local.yml build --no-cache

# 재시작
docker-compose -f docker-compose.local.yml up -d
```

### 로그 확인
```bash
# 전체 로그
docker-compose -f docker-compose.local.yml logs -f

# 특정 서비스 로그
docker-compose -f docker-compose.local.yml logs -f hamalog-app
docker-compose -f docker-compose.local.yml logs -f mysql-hamalog
docker-compose -f docker-compose.local.yml logs -f redis
```

### 데이터베이스 초기화
```bash
# 볼륨 포함 완전 정리
docker-compose -f docker-compose.local.yml down --volumes

# 재시작 (새로운 데이터베이스로)
docker-compose -f docker-compose.local.yml up -d
```

## 기존 개발환경과 비교

| 구분 | 기존 로컬 | Docker 로컬 | 배포환경 |
|------|-----------|-------------|----------|
| 데이터베이스 | H2 인메모리 | MySQL 8.0 | MySQL 8.0 |
| 캐시 | 로컬 Redis | Redis 컨테이너 | Redis 서버 |
| 프로파일 | local | prod | prod |
| 로깅 | 개발용 | JSON 구조화 | JSON 구조화 |

이제 로컬에서도 배포환경과 동일한 조건으로 애플리케이션을 테스트할 수 있습니다! 🚀