# 🏥 Hamalog (하마로그)

Hamalog는 복약 일정, 복약 기록, 부작용, 마음 일기를 한 번에 관리하는 헬스케어 백엔드 시스템입니다.  
보안과 성능, 그리고 안정적인 운영을 최우선으로 설계되었으며, 최신 Spring Boot 기술 스택을 활용하여 구축되었습니다.

[![Java 21](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.4.5](https://img.shields.io/badge/Spring%20Boot-3.4.5-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![MySQL 8.0](https://img.shields.io/badge/MySQL-8.0-blue?logo=mysql)](https://www.mysql.com/)
[![Redis 7](https://img.shields.io/badge/Redis-7-red?logo=redis)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-ready-blue?logo=docker)](https://www.docker.com/)

---

## 🚀 주요 기능

### 1. 인증 및 보안 (Security First)
- **JWT + CSRF 더블 가드**: JWT 기반 인증과 더불어 SPA 환경에 최적화된 CSRF 검증 필터 적용.
- **Refresh Token Rotation**: 보안 강화를 위한 리프레시 토큰 로테이션 및 블랙리스트 관리.
- **리소스 소유권 검증**: AOP(`@RequireResourceOwnership`)를 활용한 선언적 데이터 접근 제어.
- **Rate Limiting**: Redis 기반의 엔드포인트별 요청 제한 (Auth/API 분리).
- **데이터 암호화**: AES 기반의 민감 데이터 필드 단위 암호화 저장.

### 2. 건강 관리 (Healthcare Logic)
- **복약 스케줄 및 기록**: 반복 일정 설정, 이미지 업로드(Multipart), 페이지네이션 지원.
- **부작용 추적**: Redis 캐시를 활용한 최근 부작용 목록 조회 및 Batch 저장 최적화.
- **마음 일기**: 하루 1회 작성 제한 로직 및 템플릿/자유 서식 지원.

### 3. 운영 및 가시성 (Observability)
- **구조화된 로깅**: Logstash 기반 JSON 포맷팅으로 앱, 보안, 감사, 성능 로그 분리 저장.
- **성능 모니터링**: AOP 기반 메서드 실행 시간 추적 및 Prometheus 메트릭 수집.
- **에러 추적**: 모든 에러 응답에 `traceId`와 `timestamp`를 포함하여 상관관계 분석 지원.

---

## 🛠 기술 스택

| 구분 | 기술 사양 |
|------|-----------|
| **Language** | Java 21 (OpenJDK) |
| **Framework** | Spring Boot 3.4.5 |
| **Persistence** | Spring Data JPA, Hibernate, MySQL 8.0, Flyway |
| **Cache/Security** | Redis 7, Spring Security, JWT (jjwt) |
| **Build/DevOps** | Gradle, Docker, Cloudflare Tunnel (Zero Trust) |
| **Monitoring** | Spring Boot Actuator, Micrometer Prometheus, Grafana |
| **Testing** | JUnit 5, Mockito, AssertJ, ArchUnit, JaCoCo |

---

## 🏗 프로젝트 아키텍처

### 레이어 구조
- **Controller**: REST API 엔드포인트 및 요청 유효성 검증
- **Service**: 비즈니스 로직 및 트랜잭션 관리 (SRP 준수를 위한 서비스 분리 완료)
- **Domain**: JPA 엔티티 및 도메인 모델 (이벤트 기반 처리 포함)
- **Repository**: 데이터 접근 추상화 (JPA)
- **AOP**: 횡단 관심사(로깅, 보안, 성능, 재시도) 처리

### 배포 아키텍처 (Zero Trust)
```
사용자 -> Cloudflare (WAF/DDoS) -> Cloudflare Tunnel -> Nginx (Reverse Proxy) -> Spring Boot App
```
*외부 포트 노출 없이 보안 터널을 통해서만 내부 서비스에 접근하는 안전한 구조를 채택하고 있습니다.*

---

## 🏁 빠른 시작 (Quick Start)

### 필수 요구사항
- JDK 21
- Docker & Docker Compose
- (선택) Redis & MySQL (로컬 실행 시)

### 빌드 및 실행
```bash
# 저장소 복제
git clone https://github.com/daemin-kim/Hamalog.git
cd Hamalog

# 프로젝트 빌드
./gradlew clean build

# Docker Compose를 이용한 로컬 환경 기동 (Redis, MySQL 포함)
docker-compose -f docker-compose-dev.yml up -d

# 애플리케이션 실행
./gradlew bootRun
```

---

## 🧪 테스트 (Testing)
Hamalog는 높은 코드 품질을 위해 1,300개 이상의 테스트 케이스를 보유하고 있습니다.

```bash
# 전체 테스트 실행
./gradlew test

# 테스트 리포트 생성 (JaCoCo)
./gradlew jacocoTestReport
```

---

## ⚙️ 주요 설정 (Environment Variables)

주요 설정은 환경변수로 관리하며 `.env.example`를 참고하세요. 프로덕션에서는 **실제 비밀값을 환경변수/Secret Manager로만 주입**하고, 리포지토리에는 dummy 값만 남깁니다.

| 변수명 | 기본값 | 설명 |
|--------|--------|------|
| `SPRING_PROFILES_ACTIVE` | `prod` (예시) | 활성 프로파일 지정 |
| `SPRING_DATASOURCE_URL` | - | DB JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | - | DB 계정 |
| `SPRING_DATASOURCE_PASSWORD` | - | DB 비밀번호 |
| `JWT_SECRET` | (필수) | JWT 서명용 Base64 256비트 키 |
| `JWT_EXPIRY` | `900000` | 액세스 토큰 만료(ms) |
| `JWT_REFRESH_TOKEN_EXPIRY` | `604800000` | 리프레시 토큰 만료(ms) |
| `HAMALOG_ENCRYPTION_KEY` | (필수) | AES-256 암호화 키 (Base64) |
| `KAKAO_CLIENT_ID` / `KAKAO_CLIENT_SECRET` | (필수) | 카카오 OAuth2 앱 크레덴셜 |
| `KAKAO_REDIRECT_URI` | - | 카카오 OAuth2 리다이렉트 URI |
| `FRONTEND_URL` | - | 프런트엔드 베이스 URL |
| `ALLOWED_ORIGINS` | `https://api.hamalog.shop,http://localhost:3000` | CORS 허용 Origin |
| `SPRING_DATA_REDIS_HOST` / `SPRING_DATA_REDIS_PORT` | `localhost` / `6379` | Redis 호스트/포트 |
| `SPRING_DATA_REDIS_PASSWORD` | (선택) | Redis 비밀번호 |
| `LOG_DIR` | `./logs` | 로그 출력 경로 |
| `BOT_PROTECTION_ENABLED` | `true` | Bot 차단 기능 on/off |
| `TRUSTED_PROXIES` | `127.0.0.1/32,::1/128,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16` | 신뢰 프록시 대역 |
| `RATE_LIMIT_API_PER_MINUTE` / `RATE_LIMIT_AUTH_PER_MINUTE` | `60` / `5` | 레이트리밋 설정 |

- 샘플 값은 `.env.example`에 `dummy-...` 혹은 `<PLACEHOLDER>` 형태로 제공합니다.
- 프로덕션 배포 시 `.env.prod`(또는 CI/CD 시크릿)로 실제 값을 주입하세요.
- 민감정보는 절대 커밋하지 말고, 로그/프로파일 덤프에 포함되지 않도록 유의하세요.

---

## 📂 문서화 (Documentation)

| 문서 | 설명 |
|------|------|
| [🎨 **포트폴리오**](./docs/PORTFOLIO.md) | 프로젝트 핵심 역량 및 기술적 도전 |
| [📋 포트폴리오 요약](./docs/PORTFOLIO-SUMMARY.md) | 1~2페이지 축약 버전 |
| [📖 API 명세서](./docs/shared/API-specification.md) | REST API 엔드포인트 상세 가이드 |
| [📁 프로젝트 구조](./docs/shared/Project-Structure.md) | 상세 디렉토리 구조 및 인프라 명세 |
| [📝 API 참고 문서](./docs/internal/API-reference.md) | DB 스키마, 인프라 구성, 변경 이력 |
| [📜 CHANGELOG](./CHANGELOG.md) | 버전별 변경 이력 |
| [🎯 바이브 코딩 가이드](./docs/ai/VIBE-CODING-GUIDE.md) | AI 협업 개발 전략 및 가이드 |
| [📐 코딩 컨벤션](./docs/internal/CODING-CONVENTIONS.md) | 코드 스타일 및 규칙 |
| [🏛️ ADR 문서](./docs/internal/adr/) | 아키텍처 결정 기록 |
| [📈 개선 보고서](./docs/IMPROVEMENT-REPORT.md) | 최신 리팩토링 및 성능 개선 내역 |

---

## 🎯 바이브 코딩 (Vibe Coding)

Hamalog는 **AI와 협업하기 최적화된 "바이브 코딩" 프로젝트**입니다.

### AI 컨텍스트 파일
- `.cursorrules` - Cursor IDE용 프로젝트 컨텍스트
- `.github/copilot-instructions.md` - GitHub Copilot 지시사항

### 선언적 패턴
```java
// 한 줄로 리소스 소유권 검증
@RequireResourceOwnership(resourceType = "MEDICATION_SCHEDULE", idParam = "id")

// 한 줄로 재시도 로직 적용
@Retryable(maxAttempts = 3, delay = 1000)
```

### ADR (Architecture Decision Records)
주요 설계 결정을 문서화하여 AI가 "왜 이렇게 구현했는지" 이해할 수 있습니다.
- [JWT + CSRF 이중 보호](./docs/internal/adr/0002-jwt-csrf-dual-protection.md)
- [AOP 기반 횡단 관심사](./docs/internal/adr/0003-aop-cross-cutting-concerns.md)

---

## 📜 라이선스
사내 프로젝트용으로 별도의 외부 라이선스는 제공되지 않습니다.
