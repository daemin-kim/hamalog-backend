# Hamalog 자동 배포 시스템

이 문서는 Hamalog 애플리케이션의 자동 배포 시스템에 대해 설명합니다. main 브랜치에 푸시할 때 기존 컨테이너를 자동으로 정리하고 새로 빌드하는 시스템을 구현했습니다.

## 🚀 자동 배포 시스템 개요

### 구성 요소
1. **deploy.sh** - 로컬 배포 스크립트
2. **.github/workflows/deploy-main.yml** - GitHub Actions 워크플로우
3. **docker-compose.yml** - Docker 컨테이너 구성 (기존 파일 활용)

### 동작 방식
main 브랜치에 코드가 푸시되면:
1. 기존 컨테이너들을 중지하고 제거
2. 이전 Docker 이미지들을 정리
3. 새로운 애플리케이션 JAR 빌드
4. 새로운 Docker 이미지 빌드 및 컨테이너 실행
5. 배포 검증 수행

## 📋 GitHub Actions 워크플로우

### 트리거 조건
- `main` 브랜치에 push 이벤트 발생 시
- 수동으로 GitHub UI에서 실행 가능 (workflow_dispatch)

### 워크플로우 단계
1. **코드 체크아웃** - 최신 코드 가져오기
2. **Java 21 설정** - Temurin 배포판 사용
3. **Gradle 캐싱** - 빌드 속도 향상
4. **Docker 설정** - Buildx 및 레지스트리 로그인
5. **애플리케이션 빌드 및 테스트** - `./gradlew clean build`
6. **Docker 이미지 빌드 및 푸시** - GitHub Container Registry에 푸시
7. **프로덕션 배포** - deploy.sh 스크립트 실행
8. **배포 검증** - 애플리케이션 상태 확인
9. **이미지 정리** - 오래된 Docker 이미지 삭제

## 🔧 필요한 GitHub Secrets 설정

GitHub 레포지토리의 Settings > Secrets and variables > Actions에서 다음 secrets을 설정해야 합니다:

### 필수 Secrets
```bash
JWT_SECRET=your_jwt_secret_key_here
MYSQL_ROOT_PASSWORD=your_mysql_root_password
```

### 선택적 Secrets (기본값 있음)
```bash
JWT_EXPIRY=3600000
DB_NAME=Hamalog
DB_USERNAME=hamalog_user
DB_PASSWORD=hamalog_password
KAKAO_CLIENT_ID=your_kakao_client_id
KAKAO_CLIENT_SECRET=your_kakao_client_secret
```

### JWT Secret 생성 방법
```bash
# OpenSSL을 사용한 안전한 JWT 시크릿 생성
openssl rand -base64 32
```

## 🔨 로컬 수동 배포

GitHub Actions를 사용하지 않고 로컬에서 직접 배포하려면:

### 전제 조건
- Docker 및 Docker Compose 설치
- Java 21 설치
- 환경 변수 설정

### 배포 명령어
```bash
# 배포 스크립트에 실행 권한 부여 (최초 1회)
chmod +x deploy.sh

# 배포 실행
./deploy.sh
```

### 환경 변수 설정 예시
```bash
export JWT_SECRET="your-jwt-secret-here"
export MYSQL_ROOT_PASSWORD="your-mysql-password"
export DB_NAME="Hamalog"
export DB_USERNAME="hamalog_user"
export DB_PASSWORD="hamalog_password"

# 선택적 OAuth2 설정
export KAKAO_CLIENT_ID="your-kakao-client-id"
export KAKAO_CLIENT_SECRET="your-kakao-client-secret"
```

## 📊 배포 스크립트 기능

### deploy.sh의 주요 기능
1. **Docker 상태 확인** - Docker 데몬 실행 상태 검증
2. **기존 컨테이너 정리** - 실행 중인 컨테이너 중지 및 제거
3. **이미지 정리** - 사용하지 않는 Docker 이미지 제거
4. **애플리케이션 빌드** - Gradle을 사용한 JAR 파일 생성
5. **컨테이너 재시작** - 새로운 이미지로 모든 서비스 시작
6. **배포 검증** - 애플리케이션 상태 및 헬스 체크
7. **상태 리포트** - 배포 결과 및 접근 URL 안내

### 로그 및 모니터링
```bash
# 애플리케이션 로그 확인
docker-compose -f docker-compose.yml -p hamalog logs -f hamalog-app

# 모든 서비스 상태 확인
docker-compose -f docker-compose.yml -p hamalog ps

# 특정 서비스 로그 확인
docker-compose -f docker-compose.yml -p hamalog logs mysql-hamalog
docker-compose -f docker-compose.yml -p hamalog logs redis
```

## 🔍 트러블슈팅

### 일반적인 문제들

#### 1. Docker 데몬이 실행되지 않음
```bash
# macOS/Windows
Docker Desktop을 시작하세요

# Linux
sudo systemctl start docker
```

#### 2. 포트 충돌
```bash
# 8080 포트를 사용하는 프로세스 확인
lsof -i :8080

# 프로세스 종료
kill -9 <PID>
```

#### 3. 권한 문제
```bash
# 스크립트 실행 권한 부여
chmod +x deploy.sh

# Docker 권한 문제 (Linux)
sudo usermod -aG docker $USER
# 로그아웃 후 다시 로그인 필요
```

#### 4. 빌드 실패
```bash
# Gradle 캐시 정리
./gradlew clean

# Gradle 래퍼 권한 확인
chmod +x ./gradlew
```

#### 5. 컨테이너 시작 실패
```bash
# 상세한 로그 확인
docker-compose -f docker-compose.yml -p hamalog logs hamalog-app

# 컨테이너 상태 확인
docker-compose -f docker-compose.yml -p hamalog ps
```

## 📈 성능 최적화

### GitHub Actions 캐싱
- Gradle 의존성 캐시
- Docker 레이어 캐시
- Java 설정 캐시

### Docker 이미지 최적화
- 멀티 스테이지 빌드 고려 (향후 개선사항)
- 베이스 이미지 최적화
- 이미지 크기 최소화

## 🛡️ 보안 고려사항

### GitHub Secrets 관리
- 민감한 정보는 반드시 GitHub Secrets에 저장
- 로그에 민감한 정보 노출 방지
- 정기적인 시크릿 로테이션

### Docker 보안
- 최신 베이스 이미지 사용
- 불필요한 패키지 제거
- 컨테이너 권한 최소화

## 🔄 향후 개선사항

1. **롤링 업데이트** - 무중단 배포 구현
2. **헬스 체크 강화** - 더 정교한 애플리케이션 상태 검증
3. **알림 시스템** - 배포 결과를 Slack/Discord로 전송
4. **스테이징 환경** - 프로덕션 배포 전 테스트 환경
5. **롤백 기능** - 문제 발생 시 이전 버전으로 복구

## 📞 지원

배포 시스템 관련 문제가 발생하면:
1. GitHub Actions 로그 확인
2. Docker 컨테이너 로그 확인
3. 이슈 등록 또는 개발팀 문의

---

> 이 배포 시스템은 main 브랜치에 푸시할 때마다 자동으로 실행되어 기존 컨테이너를 정리하고 새로운 버전을 배포합니다.