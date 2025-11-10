# Hamalog - 복약 관리 시스템

Hamalog는 개인의 복약 스케줄을 체계적으로 관리하고 복약 기록, 부작용 추적을 지원하는 Spring Boot 기반 웹 애플리케이션입니다.

## 🚀 주요 기능

### 인증 및 보안
- **JWT 기반 인증**: 보안이 강화된 토큰 기반 인증 시스템
- **카카오 OAuth2 로그인**: 소셜 로그인을 통한 간편한 회원가입/로그인
- **토큰 블랙리스트**: Redis 기반 실시간 토큰 무효화
- **데이터 암호화**: 민감한 개인정보 필드별 암호화 저장
- **리소스 소유권 검증**: AOP 기반 데이터 접근 권한 제어

### 복약 관리
- **복약 스케줄 관리**: 약물별 복용 시간 및 주기 설정
- **복약 기록 추적**: 실제 복용 기록 및 누락 관리
- **부작용 모니터링**: 부작용 발생 기록 및 추적
- **이미지 업로드**: 처방전 및 약물 사진 첨부 지원

### 시스템 특징
- **AOP 기반 횡단 관심사**:
  - API 로깅 (ApiLoggingAspect)
  - 비즈니스 감사 (BusinessAuditAspect)
  - 캐싱 관리 (CachingAspect)
  - 성능 모니터링 (PerformanceMonitoringAspect)
  - 재시도 로직 (RetryAspect)
- **구조화된 로깅**: 
  - MDC 기반 로그 추적
  - JSON 형태의 체계적인 로그 관리
  - 비즈니스 이벤트 로깅
  - 보안 감사 로깅
- **성능 최적화**: 
  - Redis 캐싱
  - N+1 쿼리 최적화
  - 성능 메트릭 수집
- **보안 강화**:
  - CSRF 보호
  - 사용자 정의 보안 처리
  - 암호화 구현

## 🛠 기술 스택

### Backend Framework
- **Spring Boot 3.4.5**: 최신 Spring Boot 프레임워크
- **Spring Security**: JWT 및 OAuth2 보안 구현
- **Spring Data JPA**: 데이터베이스 ORM 및 Repository 패턴
- **Spring Cloud 2024.0.0**: 클라우드 네이티브 기능 지원

### Database & Cache
- **MySQL**: 프로덕션 환경 주 데이터베이스
- **H2**: 개발 및 테스트용 인메모리 데이터베이스
- **Redis**: 캐싱, 세션 관리, 토큰 블랙리스트

### Security & Authentication
- **JWT (JSON Web Token)**: 상태 비저장 인증
- **OAuth2 (Kakao)**: 소셜 로그인 연동
- **AES 암호화**: 개인정보 필드별 암호화

### Monitoring & Logging
- **Logback**: 구조화된 로깅 시스템
- **Logstash Encoder**: JSON 형태 로그 출력
- **MDC (Mapped Diagnostic Context)**: 요청 추적 및 로그 상관관계
- **AOP**: 
  - API 호출 로깅
  - 성능 모니터링
  - 비즈니스 감사
  - 보안 이벤트 추적

### Build & DevOps
- **Gradle**: 빌드 및 의존성 관리
- **Docker**: 컨테이너화 배포
- **Docker Compose**: 로컬 개발 환경 구성

## 📋 사전 요구사항

### 필수 요구사항
- **Java 21** (OpenJDK 21 권장)
- **Docker** & **Docker Compose** (컨테이너 실행용)
- **Redis** (캐싱 및 세션 관리용)
- **MySQL** (프로덕션 환경용)

### 개발 환경 권장사항
- **IntelliJ IDEA** 또는 **Visual Studio Code**
- **Postman** (API 테스트용)

## 🚀 빠른 시작

### 1. 프로젝트 클론
```bash
git clone [repository-url]
cd Hamalog
```

### 2. 로컬 개발 환경 실행
```bash
# Docker Compose로 Redis 실행
docker-compose -f docker-compose.local.yml up -d

# 애플리케이션 실행 (개발 모드)
./gradlew bootRun
```

### 3. 전체 환경 Docker로 실행
```bash
# 전체 서비스 실행 (애플리케이션 + Redis + MySQL)
docker-compose up -d
```

## 🔧 환경 설정

### 개발 환경 (기본)
- **데이터베이스**: H2 인메모리 DB (자동 설정)
- **로깅**: 콘솔 출력, 개발자 친화적 형태
- **프로필**: `default` (별도 설정 없음)

### 프로덕션 환경
프로덕션 배포 시 다음 프로필과 환경변수 설정이 필요합니다:

```bash
# 프로필 설정
export SPRING_PROFILES_ACTIVE=prod

# 데이터베이스 설정
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/hamalog
export SPRING_DATASOURCE_USERNAME=your_username
export SPRING_DATASOURCE_PASSWORD=your_password

# Redis 설정
export SPRING_REDIS_HOST=localhost
export SPRING_REDIS_PORT=6379

# JWT 설정
export JWT_SECRET=your_jwt_secret_key
export JWT_EXPIRATION=3600000

# OAuth2 설정 (카카오)
export OAUTH2_KAKAO_CLIENT_ID=your_kakao_client_id
export OAUTH2_KAKAO_CLIENT_SECRET=your_kakao_client_secret
```

### 테스트 환경
테스트 실행을 위한 설정:

```bash
# 테스트 프로필 활성화
export SPRING_PROFILES_ACTIVE=test

# 테스트 실행
./gradlew test
```

## 📊 코드 품질 및 테스트

### 테스트 커버리지 확인
```bash
./gradlew test jacocoTestReport
```

### 정적 코드 분석
```bash
./gradlew check
```
export SPRING_DATASOURCE_USERNAME="hamalog_user"
export SPRING_DATASOURCE_PASSWORD="your_password"

# 보안 키 관리는 HashiCorp Vault를 통해 자동으로 처리됩니다

# 애플리케이션 설정
export FRONTEND_URL="https://your-frontend-domain.com"
export ALLOWED_ORIGINS="https://your-frontend-domain.com"

# Redis 설정
export SPRING_DATA_REDIS_HOST="localhost"
export SPRING_DATA_REDIS_PORT="6379"
export SPRING_DATA_REDIS_PASSWORD="your_redis_password"

# 파일 업로드 경로
export UPLOAD_DIR="/data/hamalog/images"

# 프로덕션 모드로 실행
export SPRING_PROFILES_ACTIVE="prod"
```

### 보안 키 관리
모든 보안 키는 HashiCorp Vault를 통해 중앙 집중식으로 관리됩니다. 수동 키 관리가 필요하지 않습니다.

## 🏗 빌드 및 실행

### 개발용 빌드
```bash
# 프로젝트 빌드 (테스트 포함)
./gradlew build

# 테스트만 실행
./gradlew test

# 테스트 커버리지 리포트 생성
./gradlew test jacocoTestReport
```

### 프로덕션 빌드
```bash
# 프로덕션용 JAR 파일 생성
./gradlew build -x test

# Docker 이미지 빌드
docker build -t hamalog:latest .
```

### 실행 옵션
```bash
# 로컬 개발 모드
./gradlew bootRun

# 특정 프로필로 실행
./gradlew bootRun --args='--spring.profiles.active=prod'

# 디버그 로깅 활성화
./gradlew bootRun --args='--logging.level.com.Hamalog=DEBUG'
```

## 📚 API 문서

### API 명세서
자세한 API 문서는 [API-specification.md](./API-specification.md)를 참조하세요.

### Swagger UI
애플리케이션 실행 후 다음 URL에서 대화형 API 문서를 확인할 수 있습니다:
```
http://localhost:8080/swagger-ui.html
```

### 주요 엔드포인트
- **인증**: `/auth/*`, `/oauth2/*`
- **복약 스케줄**: `/medication-schedule/*`
- **복약 기록**: `/medication-record/*`
- **부작용 기록**: `/side-effect/*`
- **헬스체크**: `/actuator/health`

## 🔍 모니터링 및 로깅

### 로그 파일 위치
```
./logs/
├── hamalog.log         # 애플리케이션 전체 로그
├── audit.log           # 비즈니스 감사 로그
├── security.log        # 보안 관련 로그
├── performance.log     # 성능 모니터링 로그
└── error.log          # 에러 전용 로그
```

### 성능 모니터링
- **AOP 기반 메서드 실행 시간 추적**
- **Redis 캐시 히트율 모니터링**
- **데이터베이스 쿼리 성능 추적**

### 보안 감사
- **모든 인증/인가 시도 로깅**
- **데이터 접근 권한 검증 기록**
- **비정상적인 API 호출 패턴 탐지**

## 🧪 테스트

### 테스트 실행
```bash
# 전체 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests "com.Hamalog.controller.auth.AuthControllerTest"

# 테스트 결과 상세 출력
./gradlew test --info
```

### 테스트 구조
- **단위 테스트**: Mockito 기반 서비스 계층 테스트
- **통합 테스트**: MockMvc 기반 컨트롤러 테스트
- **보안 테스트**: Spring Security Test 기반 인증/인가 테스트

## 🐳 Docker 배포

### 로컬 개발용 (Redis만 Docker로)
```bash
docker-compose -f docker-compose.local.yml up -d
```

### 전체 서비스 배포
```bash
# 전체 서비스 시작
docker-compose up -d

# 서비스 상태 확인
docker-compose ps

# 로그 확인
docker-compose logs -f hamalog
```

### Docker 이미지 관리
```bash
# 이미지 빌드
docker build -t hamalog:latest .

# 개별 컨테이너 실행
docker run -d -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host:3306/hamalog \
  hamalog:latest
```

## 🛠 개발 가이드라인

### 코딩 규칙
- **패키지 구조**: 계층별 분리 (controller → service → repository)
- **네이밍**: 카멜케이스 메서드, 파스칼케이스 클래스
- **Lombok 사용**: `@Slf4j`, `@Data`, `@RequiredArgsConstructor`
- **테스트 메서드**: `methodName_Condition_ExpectedBehavior` 형태

### 아키텍처 특징
- **AOP 기반 횡단 관심사 분리**
- **JWT + OAuth2 복합 인증**
- **Redis 캐싱 전략**
- **JPA 성능 최적화**

### 보안 고려사항
- **모든 엔드포인트 JWT 인증 필수** (회원가입/로그인 제외)
- **리소스 소유권 검증**
- **개인정보 암호화 저장**
- **HTTPS 필수** (프로덕션 환경)

## 📞 문제 해결

### 일반적인 문제
1. **애플리케이션 시작 실패**: 환경변수 설정 확인
2. **JWT 토큰 오류**: BASE64 인코딩된 256bit 키 사용 확인
3. **Redis 연결 실패**: Redis 서버 상태 및 연결 정보 확인
4. **MySQL 연결 실패**: 데이터베이스 서버 및 권한 설정 확인

### 헬스체크
```bash
# 애플리케이션 상태 확인
curl http://localhost:8080/actuator/health

# 응답 예시
{"status":"UP"}
```

## 📄 라이센스

이 프로젝트는 개인 프로젝트로 제작되었습니다.

## 🤝 기여

이 프로젝트는 개인 프로젝트입니다. 문의사항이 있으시면 이슈를 등록해 주세요.

---
**Hamalog v0.0.1-SNAPSHOT**  
*마지막 업데이트: 2025-11-10*