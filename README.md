# Hamalog - 복약 관리 시스템

개인의 복약 스케줄을 체계적으로 관리하고 복약 기록, 부작용 추적을 지원하는 Spring Boot 기반 웹 애플리케이션입니다.

## 🧭 빠른 시작

```bash
# 개발 모드 실행 (H2 인메모리 DB)
./gradlew bootRun

# 테스트 실행
./gradlew test

# 커버리지 리포트 생성
./gradlew test jacocoTestReport

# 로컬 인프라 실행 (Redis, MySQL)
docker-compose -f docker-compose.local.yml up -d
```

## 🚀 주요 기능

### 인증 및 보안
- **JWT 기반 인증**: 보안이 강화된 토큰 기반 인증
- **카카오 OAuth2 로그인**: 소셜 로그인 지원
- **토큰 블랙리스트**: Redis 기반 실시간 토큰 무효화
- **개인정보 암호화**: AES 필드 암호화
- **리소스 접근 제어**: AOP 기반 소유권 검증

### 복약 관리
- 복약 스케줄 관리 및 추적
- 복약 기록 및 누락 관리
- 부작용 모니터링
- 이미지 업로드 지원

### 시스템 특징
- **AOP 횡단 관심사**: 로깅, 감사, 캐싱, 성능 모니터링
- **구조화된 로깅**: MDC 기반 추적, JSON 형식 로그
- **성능 최적화**: Redis 캐싱, N+1 쿼리 최적화
- **API 문서화**: Swagger UI 기반

## 🛠 기술 스택

| 구분 | 기술 |
|------|------|
| **Backend** | Spring Boot 3.4.5, Spring Security, Spring Data JPA |
| **Database** | MySQL (prod), H2 (dev) |
| **Cache** | Redis |
| **Authentication** | JWT (JJWT 0.12.6), OAuth2 (Kakao) |
| **Logging** | Logback, Logstash Encoder |
| **API Doc** | SpringDoc OpenAPI 2.7.0 |
| **Build** | Gradle 8.13, JaCoCo |
| **DevOps** | Docker, Docker Compose |

## 📋 요구사항

- **Java 21+**
- **Gradle 8.13+** (또는 `./gradlew` 사용)
- **Docker & Docker Compose** (선택사항)

## 🔧 환경 설정

### 개발 환경 (기본)
```bash
./gradlew bootRun
# H2 인메모리 DB로 자동 실행
```

### 프로덕션 환경
```bash
export SPRING_PROFILES_ACTIVE=prod
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/hamalog
export SPRING_DATASOURCE_USERNAME=your_user
export SPRING_DATASOURCE_PASSWORD=your_password
export SPRING_REDIS_HOST=localhost
export JWT_SECRET=your_base64_256bit_secret
```

### Docker로 전체 실행
```bash
docker-compose up -d
```

## 📊 테스트 및 품질

```bash
# 테스트 실행
./gradlew test

# 커버리지 리포트 (build/reports/jacoco/test/html/index.html)
./gradlew jacocoTestReport

# 프로덕션 빌드
./gradlew build -x test
```

## 📚 API 문서

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **상세 명세**: [API-specification.md](API-specification.md)
- **주요 엔드포인트**:
  - 인증: `/auth/*`, `/oauth2/*`
  - 복약 스케줄: `/medication-schedule/*`
  - 복약 기록: `/medication-record/*`
  - 부작용: `/side-effect/*`

## 🔍 모니터링

### 로그 파일 위치
```
./logs/
├── hamalog.log       # 애플리케이션 전체 로그
├── audit.log         # 비즈니스 감사 로그
├── security.log      # 보안 관련 로그
└── performance.log   # 성능 모니터링 로그
```

### 성능 메트릭
- AOP 기반 메서드 실행 시간 추적
- Redis 캐시 히트율 모니터링
- 데이터베이스 쿼리 성능 추적

### 헬스체크
```
http://localhost:8080/actuator/health
```

## 🏗 프로젝트 구조

```
src/main/java/com/Hamalog/
├── controller/          # REST API 컨트롤러
├── service/             # 비즈니스 로직
├── domain/              # 엔티티
├── repository/          # 데이터 접근
├── security/            # 보안 관련
├── config/              # 설정
├── aop/                 # 횡단 관심사
├── exception/           # 커스텀 예외
├── dto/                 # 요청/응답 DTO
└── logging/             # 로깅 관련
```

## 📖 추가 문서

- [OAuth2 구현 가이드](OAUTH2_IMPLEMENTATION_COMPLETE.md)
- [카카오 OAuth2 플로우](OAUTH2_KAKAO_FLOW.md)
- [구현 요약](IMPLEMENTATION_SUMMARY.md)

## 🤝 기여

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 라이선스

이 프로젝트는 비공개 프로젝트입니다.

