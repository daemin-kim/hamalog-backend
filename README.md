# Hamalog - 복약 관리 시스템

<div align="center">

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-green?style=flat-square&logo=springboot)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=flat-square&logo=mysql)
![Docker](https://img.shields.io/badge/Docker-Enabled-blue?style=flat-square&logo=docker)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)

*JWT 인증, OAuth2 연동, 부작용 추적 기능을 갖춘 포괄적인 Spring Boot 복약 관리 애플리케이션*

</div>

## 📋 목차

- [개요](#-개요)
- [주요 기능](#-주요-기능)
- [기술 스택](#-기술-스택)
- [사전 요구사항](#-사전-요구사항)
- [빠른 시작](#-빠른-시작)
- [설정](#-설정)
- [API 문서](#-api-문서)
- [프로젝트 구조](#-프로젝트-구조)
- [개발 가이드](#-개발-가이드)
- [테스트](#-테스트)
- [배포](#-배포)
- [보안](#-보안)
- [기여 방법](#-기여-방법)
- [라이선스](#-라이선스)

## 🎯 개요

Hamalog는 사용자가 복약 일정을 추적하고, 부작용을 모니터링하며, 포괄적인 건강 기록을 유지할 수 있도록 설계된 최신 복약 관리 시스템입니다. Spring Boot 3.4.5와 Java 21로 구축되어 개인 건강 관리를 위한 안전하고 확장 가능한 플랫폼을 제공합니다.

### 핵심 목표
- **환자 안전**: 신뢰할 수 있는 복약 추적 및 복약 순응도 모니터링
- **사용자 경험**: 복약 관리를 위한 직관적인 인터페이스
- **데이터 보안**: HIPAA 수준의 보안 및 개인정보 보호 준수
- **의료 연동**: 의료 제공자와의 원활한 연동 기능

## ✨ 주요 기능

### 🔐 인증 및 권한 부여
- **JWT 인증**: 안전한 토큰 기반 인증
- **OAuth2 연동**: 카카오 소셜 로그인 지원
- **역할 기반 접근 제어**: 사용자 권한 관리
- **토큰 블랙리스트**: 토큰 무효화를 통한 안전한 로그아웃

### 💊 복약 관리
- **복약 스케줄링**: 복약 일정 생성 및 관리
- **처방전 추적**: 병원 정보와 함께 처방전 추적
- **복약 기록**: 상세한 복약 이력 및 기록
- **파일 업로드 지원**: 복약 일정에 이미지 첨부
- **알림 시스템**: 설정 가능한 복약 알림

### 📊 부작용 추적
- **부작용 기록**: 약물 부작용 추적 및 모니터링
- **최근 부작용**: 최근 부작용 기록에 빠른 접근
- **부작용 분석**: 포괄적인 부작용 추적 시스템

### 🔒 보안 기능
- **CORS 설정**: 설정 가능한 교차 출처 리소스 공유
- **보안 헤더**: CSP, 프레임 옵션, 리퍼러 정책
- **입력 검증**: 포괄적인 요청 검증
- **권한 검사**: 소유권 검증이 포함된 엔드포인트 수준 보안
- **속도 제한**: Redis 기반 API 호출 및 인증 시도 제한
- **요청 크기 모니터링**: DoS 공격 방지를 위한 요청 크기 제한
- **토큰 블랙리스트**: 안전한 로그아웃을 위한 JWT 토큰 무효화
- **구조화된 로깅**: 보안 이벤트 추적 및 모니터링

## 🛠 기술 스택

### 백엔드 프레임워크
- **Java 21** - 최신 언어 기능이 포함된 최신 LTS 버전
- **Spring Boot 3.4.5** - 엔터프라이즈급 애플리케이션 프레임워크
- **Spring Security** - 포괄적인 보안 프레임워크
- **Spring Data JPA** - 데이터베이스 추상화 레이어
- **Spring OAuth2 Client** - OAuth2 연동
- **Spring Data Redis** - Redis 연동 및 캐싱

### 데이터베이스 & 캐싱
- **MySQL 8.0** - 주요 운영 데이터베이스
- **H2 Database** - 개발/테스트용 인메모리 데이터베이스
- **Redis 7** - 캐싱, 세션 관리, 토큰 블랙리스트, 속도 제한

### 보안 및 인증
- **JWT (jjwt 0.12.6)** - JSON Web Token 구현
- **BCrypt** - 비밀번호 암호화
- **OAuth2** - 소셜 로그인 연동
- **Bucket4j** - Redis 기반 속도 제한
- **Logstash Encoder** - 구조화된 보안 로깅

### 문서화 및 테스트
- **SpringDoc OpenAPI 2.0.2** - API 문서 자동 생성
- **JUnit 5** - 테스트 프레임워크
- **Mockito** - 단위 테스트용 모킹 프레임워크
- **Spring Boot Test** - 통합 테스트 지원

### 빌드 및 배포
- **Gradle 8.13+** - 빌드 자동화
- **Docker** - 컨테이너화
- **GitHub Actions** - CI/CD 파이프라인

## 📋 사전 요구사항

- **Java 21** (JDK 21 이상)
- **Gradle 8.13+** (포함된 래퍼 사용 가능)
- **MySQL 8.0+** (개발용으로는 H2 사용 가능)
- **Docker** (선택사항, 컨테이너 배포용)

## 🚀 빠른 시작

### 1. 저장소 복제
```bash
git clone https://github.com/your-org/hamalog.git
cd hamalog
```

### 2. 환경 설정
민감한 설정을 위한 환경 변수 생성:

```bash
# 데이터베이스 설정 (MySQL)
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=hamalog
export DB_USERNAME=your_username
export DB_PASSWORD=your_password

# JWT 설정 (운영 환경에서 필수)
export JWT_SECRET=$(openssl rand -base64 32)  # 안전한 256비트 키 생성
export JWT_EXPIRY=3600000  # 밀리초 단위로 1시간

# OAuth2 카카오 연동 (선택사항)
export KAKAO_CLIENT_ID=your_kakao_client_id
export KAKAO_CLIENT_SECRET=your_kakao_client_secret
```

### 3. 데이터베이스 설정

#### 옵션 A: MySQL (운영 환경 권장)
```bash
# 데이터베이스 생성
mysql -u root -p
CREATE DATABASE hamalog CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'hamalog_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON hamalog.* TO 'hamalog_user'@'localhost';
FLUSH PRIVILEGES;
```

#### 옵션 B: H2 (개발용만)
별도 설정 불필요 - H2는 기본적으로 인메모리에서 실행됩니다.

### 4. 빌드 및 실행
```bash
# 클린 및 빌드
./gradlew clean build

# 애플리케이션 실행
./gradlew bootRun
```

애플리케이션은 `http://localhost:8080`에서 시작됩니다

### 5. API 문서 접근
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

## ⚙️ 설정

### 애플리케이션 속성
애플리케이션은 환경 변수를 통한 외부화된 설정을 지원합니다:

| 속성 | 환경 변수 | 기본값 | 설명 |
|----------|---------------------|---------|-------------|
| 데이터베이스 호스트 | `DB_HOST` | `db` | MySQL 데이터베이스 호스트 |
| 데이터베이스 포트 | `DB_PORT` | `3306` | MySQL 데이터베이스 포트 |
| 데이터베이스 이름 | `DB_NAME` | `hamalog` | 데이터베이스 이름 |
| 데이터베이스 사용자명 | `DB_USERNAME` | `user` | 데이터베이스 사용자명 |
| 데이터베이스 비밀번호 | `DB_PASSWORD` | `password` | 데이터베이스 비밀번호 |
| Redis 호스트 | `SPRING_DATA_REDIS_HOST` | `localhost` | Redis 서버 호스트 |
| Redis 포트 | `SPRING_DATA_REDIS_PORT` | `6379` | Redis 서버 포트 |
| JWT 비밀키 | `JWT_SECRET` | (개발용 기본값) | JWT 서명 비밀키 |
| JWT 만료시간 | `JWT_EXPIRY` | `3600000` | 토큰 만료시간 (밀리초) |
| 카카오 클라이언트 ID | `KAKAO_CLIENT_ID` | (더미값) | 카카오 OAuth2 클라이언트 ID |
| 카카오 클라이언트 시크릿 | `KAKAO_CLIENT_SECRET` | (더미값) | 카카오 OAuth2 클라이언트 시크릿 |
| 프론트엔드 URL | `FRONTEND_URL` | `http://localhost:3000` | OAuth2 리디렉션용 프론트엔드 URL |

### 파일 업로드 설정
- **업로드 디렉터리**: `/data/hamalog/images` (`hamalog.upload.image-dir`로 설정 가능)
- **최대 파일 크기**: 50MB (`spring.servlet.multipart.max-file-size`)
- **최대 요청 크기**: 50MB (`spring.servlet.multipart.max-request-size`)
- **지원 형식**: 복약 일정용 이미지
- **DoS 보호**: HTTP 요청 헤더 크기 64KB 제한, 연결 시간 초과 20초

## 📚 API 문서

### 인증 엔드포인트
- `POST /auth/signup` - 사용자 회원가입
- `POST /auth/login` - 사용자 로그인
- `POST /auth/logout` - 토큰 무효화를 통한 사용자 로그아웃

### 복약 관리
- `GET /medication-schedule/list/{member-id}` - 사용자의 복약 일정 목록 조회
- `GET /medication-schedule/{medication-schedule-id}` - 특정 복약 일정 조회
- `POST /medication-schedule` - 새 복약 일정 생성
- `PUT /medication-schedule/{medication-schedule-id}` - 복약 일정 수정
- `DELETE /medication-schedule/{medication-schedule-id}` - 복약 일정 삭제

### 부작용 추적
- `GET /side-effect/recent?userId={userId}` - 사용자의 최근 부작용 조회

### API 기능
- **권한 부여**: 모든 엔드포인트(인증 및 문서 제외)에 유효한 JWT 토큰 필요
- **소유권 검증**: 사용자는 본인 데이터에만 접근 가능
- **페이지네이션 지원**: 목록 엔드포인트에서 Spring Data 페이지네이션 지원
- **파일 업로드**: 복약 이미지용 멀티파트 폼 데이터 지원
- **Swagger 문서**: 예제가 포함된 완전한 API 문서

## 🏗 프로젝트 구조

```
src/
├── main/
│   ├── java/com/Hamalog/
│   │   ├── aop/                    # 관점 지향 프로그래밍 (로깅)
│   │   ├── config/                 # Spring 설정 클래스
│   │   ├── controller/             # REST 컨트롤러
│   │   │   ├── auth/              # 인증 엔드포인트
│   │   │   ├── medication/        # 복약 관리
│   │   │   └── sideEffect/        # 부작용 추적
│   │   ├── domain/                 # 엔티티 클래스
│   │   │   ├── idClass/           # 복합 키 클래스
│   │   │   ├── medication/        # 복약 엔티티
│   │   │   ├── member/            # 사용자 엔티티
│   │   │   └── sideEffect/        # 부작용 엔티티
│   │   ├── dto/                    # 데이터 전송 객체
│   │   ├── exception/              # 사용자 정의 예외
│   │   ├── handler/                # 글로벌 예외 처리
│   │   ├── logging/                # 요청 로깅 인프라
│   │   ├── repository/             # 데이터 접근 계층
│   │   ├── security/               # 보안 설정
│   │   │   ├── jwt/               # JWT 인증
│   │   │   └── oauth2/            # OAuth2 연동
│   │   └── service/                # 비즈니스 로직 계층
│   └── resources/
│       ├── application.properties  # 애플리케이션 설정
│       └── ValidationMessages.properties # 검증 메시지
├── test/                          # 테스트 클래스
├── docs/                          # 프로젝트 문서
├── .github/workflows/             # CI/CD 워크플로우
├── Dockerfile                     # Docker 설정
└── build.gradle                   # 빌드 설정
```

### 주요 아키텍처 패턴
- **Repository 패턴**: 데이터 접근을 위한 Spring Data JPA 저장소
- **DTO 패턴**: API 경계를 위한 별도의 요청/응답 객체
- **Builder 패턴**: 엔티티 생성을 위한 Lombok @Builder
- **AOP**: 횡단 관심사 (로깅, 보안)
- **글로벌 예외 처리**: 사용자 정의 예외를 통한 중앙집중식 오류 처리

## 👩‍💻 개발 가이드

### 로컬 개발 환경 설정

1. **IDE 설정**
   - Lombok 어노테이션 처리 활성화
   - Java 21을 프로젝트 SDK로 설정
   - Spring Boot 개발 도구 설치

2. **개발용 데이터베이스**
   ```bash
   # 빠른 개발을 위한 H2 사용
   # 별도 설정 불필요 - 인메모리에서 실행
   
   # 또는 로컬에서 MySQL 사용
   docker run -d --name hamalog-mysql \
     -e MYSQL_DATABASE=hamalog \
     -e MYSQL_USER=user \
     -e MYSQL_PASSWORD=password \
     -e MYSQL_ROOT_PASSWORD=rootpassword \
     -p 3306:3306 mysql:8.0
   ```

3. **핫 리로드**
   - 자동 재시작을 위한 Spring Boot DevTools 포함
   - Gradle 연속 빌드: `./gradlew build --continuous`

### 코드 스타일 가이드라인

- **Lombok 사용**: `@Builder`, `@RequiredArgsConstructor`, `@Getter` 선호
- **엔티티 설계**: 엔티티에 `@EqualsAndHashCode(of = "id")` 사용
- **Record 사용**: DTO에 Java record 사용 (불변 데이터)
- **검증**: 사용자 정의 메시지와 함께 Bean Validation 사용
- **예외 처리**: 비즈니스 로직 오류에 `CustomException` 확장

### 로깅 설정
- **요청 추적**: 자동 요청 ID 생성 및 MDC 통합
- **콘솔 패턴**: 상관관계를 위한 요청 ID 포함
- **디버그 로깅**: `logging.level.com.Hamalog=debug`로 활성화

## 🧪 테스트

### 테스트 실행

```bash
# 모든 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests "com.Hamalog.service.auth.AuthServiceTest"

# 커버리지와 함께 실행
./gradlew test jacocoTestReport
```

### 테스트 모범 사례

- **단위 테스트**: 서비스 계층 테스트를 위한 JUnit 5와 Mockito
- **통합 테스트**: 컨트롤러 테스트를 위한 Spring Boot Test
- **Builder 패턴**: 테스트 데이터 생성을 위한 엔티티 빌더 사용
- **BDD 스타일**: 모킹을 위한 `given().willReturn()` 사용

### 테스트 구조
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Service Tests")
class ServiceTest {
    
    @Mock
    private Repository repository;
    
    @InjectMocks
    private Service service;
    
    @Test
    @DisplayName("Test description")
    void testMethod() {
        // Given
        given(repository.method()).willReturn(expectedValue);
        
        // When
        Result result = service.method();
        
        // Then
        assertThat(result).isEqualTo(expected);
        verify(repository).method();
    }
}
```

## 🚀 배포

### Docker 배포

#### 1. 애플리케이션 빌드
```bash
./gradlew bootJar
```

#### 2. Docker 이미지 빌드
```bash
docker build -t hamalog .
```

#### 3. Docker Compose로 실행
```bash
# 기존 docker-compose.yml 사용
docker-compose up -d
```

실제 `docker-compose.yml` 구성:
- **hamalog-app**: 메인 애플리케이션 서비스
- **redis**: Redis 7-alpine (캐싱, 토큰 블랙리스트, 속도 제한)
- **mysql-hamalog**: MySQL 8.0 데이터베이스
- **Volumes**: mysql-data, redis-data, hamalog-uploads
- **Health checks**: 자동 상태 확인 및 재시작 정책

### 운영 환경 배포

#### 환경 변수 (필수)
```bash
# 보안 (중요)
export JWT_SECRET=$(openssl rand -base64 32)

# 데이터베이스
export DB_HOST=your-prod-db-host
export DB_USERNAME=your-prod-db-user
export DB_PASSWORD=your-prod-db-password

# OAuth2 (카카오 로그인 사용 시)
export KAKAO_CLIENT_ID=your-production-client-id
export KAKAO_CLIENT_SECRET=your-production-client-secret
```

#### CI/CD 파이프라인
프로젝트에는 포괄적인 자동 배포를 위한 GitHub Actions 워크플로우가 포함되어 있습니다:

**빌드 및 테스트 단계:**
- Java 21 설정 및 Gradle 캐싱
- Gradle wrapper 검증 및 권한 설정
- 전체 애플리케이션 빌드 및 테스트 실행

**Docker 및 레지스트리:**
- Docker Buildx 설정 및 GitHub Container Registry 로그인
- 멀티 태그 Docker 이미지 빌드 (latest, branch, SHA)
- GitHub Actions 캐시를 이용한 효율적인 빌드

**배포 및 검증:**
- 운영 환경 변수 주입 (GitHub Secrets)
- Docker Compose 기반 서비스 배포
- 자동 헬스 체크 및 연결성 테스트 (최대 10회 재시도)
- 배포 후 로그 검증 및 정리

**유지보수:**
- 오래된 Docker 이미지 자동 정리
- 배포 성공/실패 알림

### 헬스 체크
- **애플리케이션**: `GET /actuator/health` (Spring Boot Actuator 활성화 시)
- **데이터베이스**: 시작 시 연결 유효성 검사
- **JWT**: 토큰 검증 미들웨어

## 🔒 보안

### 보안 기능
- **JWT 인증**: 설정 가능한 만료 시간을 갖는 무상태 인증
- **OAuth2 연동**: 사용자 정의 성공 핸들러가 있는 카카오 소셜 로그인
- **CORS 설정**: 프론트엔드 연동을 위한 설정 가능한 허용 도메인
- **보안 헤더**: CSP, 프레임 옵션, 리퍼러 정책
- **비밀번호 암호화**: 사용자 비밀번호를 위한 BCrypt 해싱
- **토큰 블랙리스트**: 토큰 무효화를 통한 안전한 로그아웃
- **권한 부여**: 사용자 소유권 검증이 포함된 엔드포인트 수준 보안

### 보안 모범 사례
- **JWT 비밀키**: 강력하고 무작위로 생성된 비밀키 사용 (최소 256비트)
- **환경 변수**: 비밀키를 버전 관리에 커밋하지 않음
- **HTTPS**: 운영 환경에서 HTTPS 사용 (리버스 프록시 설정)
- **데이터베이스 보안**: 최소 권한을 가진 전용 데이터베이스 사용자 사용
- **정기 업데이트**: 보안 패치를 위한 의존성 정기 업데이트

### 규정 준수 고려사항
- **데이터 보호**: 사용자 데이터 암호화 및 안전한 처리
- **감사 추적**: 요청 로깅 및 활동 추적
- **접근 제어**: 역할 기반 권한 및 데이터 소유권 검증
- **개인정보 보호**: 사용자 데이터 격리 및 안전한 삭제 기능

## 🤝 기여 방법

### 시작하기
1. 저장소 포크
2. 기능 브랜치 생성: `git checkout -b feature/amazing-feature`
3. 변경사항 작성 및 테스트 추가
4. 모든 테스트 통과 확인: `./gradlew test`
5. 변경사항 커밋: `git commit -m 'Add amazing feature'`
6. 브랜치에 푸시: `git push origin feature/amazing-feature`
7. Pull Request 생성

### 개발 가이드라인
- 기존 코드 스타일 및 패턴 준수
- 새로운 기능에 대한 테스트 작성
- API 변경 시 문서 업데이트
- 보안 모범 사례 준수
- 디버깅을 위한 적절한 로깅 추가

### 코드 리뷰 프로세스
- 모든 변경사항은 풀 리퀘스트 리뷰 필요
- 자동화된 CI/CD 검사 통과 필요
- 인증/권한 변경 시 보안 리뷰 필요
- 데이터베이스 쿼리의 성능 고려사항

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 라이선스가 부여됩니다 - 자세한 내용은 [LICENSE](LICENSE) 파일을 참조하세요.

---

## 🔗 링크

- **문서**: [API 문서](http://localhost:8080/swagger-ui.html)
- **이슈**: [GitHub Issues](https://github.com/your-org/hamalog/issues)
- **위키**: [프로젝트 위키](https://github.com/your-org/hamalog/wiki)

## 📞 지원

지원 및 문의사항:
- GitHub에서 이슈 생성
- `/docs`의 기존 문서 검토
- `/swagger-ui.html`에서 API 문서 확인

---

<div align="center">

**Spring Boot와 Java 21로 ❤️와 함께 제작됨**

*기술을 통해 더 나은 복약 관리를 지원합니다*

</div>