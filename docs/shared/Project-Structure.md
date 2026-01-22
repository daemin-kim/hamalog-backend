# Hamalog 프로젝트 구조 명세서

## 개요

Hamalog는 **Spring Boot 3.4.5** 기반의 건강 관리 애플리케이션 백엔드 서버입니다. 복약 스케줄 관리, 마음 일기 작성, 부작용 기록 등의 기능을 제공합니다.

### 기술 스택

| 구분 | 기술 | 버전 |
|------|------|------|
| **Language** | Java | 21 |
| **Framework** | Spring Boot | 3.4.5 |
| **Build Tool** | Gradle | Wrapper |
| **ORM** | Spring Data JPA | - |
| **Database** | MySQL (Production), H2 (Development) | 8.0 |
| **Cache** | Redis | 7 |
| **Security** | Spring Security, JWT (jjwt) | 0.12.6 |
| **API Documentation** | SpringDoc OpenAPI | 2.7.0 |
| **Logging** | Logstash Logback Encoder | 7.4 |
| **Code Quality** | Spotless, JaCoCo | 6.25.0, 0.8.10 |
| **Container** | Docker, Docker Compose | - |
| **Reverse Proxy** | Nginx | Alpine |
| **CDN/Security** | Cloudflare Tunnel | - |

---

## 프로젝트 디렉토리 구조

```
Hamalog/
├── src/
│   ├── main/
│   │   ├── java/com/Hamalog/
│   │   │   ├── HamalogApplication.java      # 애플리케이션 진입점
│   │   │   ├── aop/                         # AOP (Aspect-Oriented Programming)
│   │   │   ├── config/                      # 설정 클래스
│   │   │   ├── controller/                  # REST 컨트롤러
│   │   │   ├── domain/                      # 엔티티 및 도메인 모델
│   │   │   ├── dto/                         # 데이터 전송 객체
│   │   │   ├── exception/                   # 예외 클래스
│   │   │   ├── handler/                     # 전역 예외 핸들러
│   │   │   ├── logging/                     # 로깅 관련 클래스
│   │   │   ├── repository/                  # 데이터 접근 계층
│   │   │   ├── security/                    # 보안 관련 클래스
│   │   │   ├── service/                     # 비즈니스 로직 계층
│   │   │   └── validation/                  # 커스텀 유효성 검증
│   │   ├── kotlin/com/Hamalog/              # Kotlin 소스 (DTO, 유틸리티)
│   │   │   ├── dto/                         # Kotlin DTO (신규 DTO 권장)
│   │   │   │   └── medication/              # 복약 관련 Kotlin DTO (예정)
│   │   │   │       ├── request/             # 요청 DTO
│   │   │   │       └── response/            # 응답 DTO
│   │   │   └── util/                        # Kotlin 확장 함수 및 유틸리티
│   │   │       ├── DateExtensions.kt        # 날짜 확장 함수
│   │   │       └── StringExtensions.kt      # 문자열 확장 함수
│   │   └── resources/
│   │       ├── application.properties       # 기본 설정
│   │       ├── application-prod.properties  # 프로덕션 설정
│   │       ├── db/migration/                # Flyway 마이그레이션 스크립트
│   │       ├── logback-spring.xml           # 로깅 설정
│   │       ├── messages.properties          # 메시지 리소스 (영어)
│   │       ├── messages_ko.properties       # 메시지 리소스 (한국어)
│   │       └── ValidationMessages.properties # 유효성 검사 메시지
│   └── test/
│       └── java/com/Hamalog/
│           ├── architecture/                # ArchUnit 아키텍처 테스트
│           └── ...                          # 기타 테스트 코드
├── .github/
│   ├── workflows/
│   │   └── ci.yml                           # GitHub Actions CI 파이프라인
│   └── copilot-instructions.md              # GitHub Copilot 지시 파일
├── scripts/
│   ├── generate-crud.sh                     # CRUD 스캐폴딩 스크립트
│   └── benchmark/                           # 벤치마크 스크립트
├── docs/
│   ├── README.md                            # 문서 가이드
│   ├── PORTFOLIO.md                         # 메인 포트폴리오
│   ├── PORTFOLIO-SUMMARY.md                 # 포트폴리오 요약
│   ├── GITHUB-PROJECT-CARD.md               # GitHub 프로젝트 카드
│   ├── shared/                              # 공유용 문서 (프론트엔드, 기획자)
│   │   ├── API-specification.md
│   │   ├── Project-Structure.md
│   │   └── FUTURE-IMPROVEMENTS.md
│   ├── internal/                            # 개발용 내부 문서
│   │   ├── API-reference.md
│   │   ├── CODING-CONVENTIONS.md
│   │   ├── KOTLIN-GUIDE.md
│   │   ├── PROJECT-AUDIT-GUIDE.md
│   │   ├── IMPROVEMENT-TASKS.md
│   │   ├── adr/                             # Architecture Decision Records
│   │   │   ├── 0001-adr-template.md
│   │   │   ├── 0002-jwt-csrf-dual-protection.md
│   │   │   ├── 0003-aop-cross-cutting-concerns.md
│   │   │   ├── 0004-domain-event-pattern.md
│   │   │   ├── 0005-redis-cache-strategy.md
│   │   │   ├── 0006-sensitive-data-encryption.md
│   │   │   └── 0007-message-queue-redis-stream.md
│   │   └── patterns/                        # 선언적 패턴 가이드
│   │       ├── ANNOTATION-GUIDE.md
│   │       ├── ERROR-HANDLING.md
│   │       ├── SECURITY-PATTERNS.md
│   │       ├── CACHING-PATTERNS.md
│   │       ├── JPA-PERFORMANCE.md
│   │       └── MESSAGE-QUEUE-PATTERNS.md
│   ├── portfolio/                           # 기술적 도전 심층 분석 (9개)
│   │   ├── README.md
│   │   └── 01~09-*.md
│   └── ai/                                  # AI 협업 가이드
│       └── VIBE-CODING-GUIDE.md
├── .cursorrules                             # Cursor IDE 컨텍스트
├── CHANGELOG.md                             # 변경 이력
├── build.gradle                             # Gradle 빌드 설정
├── docker-compose.yml                       # Docker 컴포즈 (프로덕션)
├── docker-compose-dev.yml                   # Docker 컴포즈 (개발)
├── Dockerfile                               # Docker 이미지 빌드
├── nginx-docker.conf                        # Nginx 설정
└── README.md                                # 프로젝트 소개
```

---

## 패키지 상세 설명

### 1. Controller Layer (`controller/`)

HTTP 요청을 처리하는 REST API 컨트롤러 계층입니다.

| 패키지 | 파일 | 설명 |
|--------|------|------|
| `controller/auth/` | `AuthController.java` | 회원가입, 로그인, 로그아웃, 토큰 갱신, 회원 탈퇴, 로그인 이력, 세션 관리 |
| | `CsrfController.java` | CSRF 토큰 발급 및 상태 확인 |
| | `MemberController.java` | 회원 프로필 조회/수정, 비밀번호 변경 |
| `controller/diary/` | `MoodDiaryController.java` | 마음 일기 CRUD, 통계, 캘린더, 검색 |
| `controller/medication/` | `MedicationScheduleController.java` | 복약 스케줄 관리, 검색, 이미지 관리, 필터링 |
| | `MedicationRecordController.java` | 복약 기록 관리, 배치 작업 |
| | `MedicationStatsController.java` | 복약 통계 (이행률, 요약) |
| | `MedicationTimeController.java` | 복약 알림 시간 CRUD |
| | `MedicationScheduleGroupController.java` | 복약 스케줄 그룹 관리 |
| `controller/oauth2/` | `OAuth2Controller.java` | 카카오 OAuth2 로그인 처리 |
| `controller/sideEffect/` | `SideEffectController.java` | 부작용 기록 CRUD, 목록 조회 |
| `controller/export/` | `ExportController.java` | 데이터 내보내기 (JSON/CSV) |
| `controller/notification/` | `NotificationController.java` | 알림 설정 및 FCM 토큰 관리 |
| `controller/benchmark/` | `BenchmarkController.java` | 성능 벤치마크 전용 (개발/테스트 환경만) |

---

### 2. Service Layer (`service/`)

비즈니스 로직을 담당하는 서비스 계층입니다.

| 패키지 | 파일 | 설명 |
|--------|------|------|
| `service/auth/` | `MemberRegistrationService.java` | 회원가입 비즈니스 로직 |
| | `AuthenticationService.java` | 로그인/로그아웃/토큰 관리 |
| | `MemberDeletionService.java` | 회원 탈퇴 및 관련 데이터 삭제 |
| | `KakaoOAuth2AuthService.java` | 카카오 OAuth2 인증 처리 |
| | `MemberCacheService.java` | 회원 정보 캐싱 서비스 |
| | `MemberProfileService.java` | 회원 프로필 조회/수정, 비밀번호 변경 |
| | `MemberDeletedEventHandler.java` | 회원 탈퇴 이벤트 처리 |
| | `LoginHistoryService.java` | 로그인 이력 및 세션 관리 **(신규)** |
| `service/diary/` | `MoodDiaryService.java` | 마음 일기 비즈니스 로직 |
| | `MoodDiaryStatsService.java` | 마음 일기 통계 및 캘린더 |
| `service/medication/` | `MedicationScheduleService.java` | 복약 스케줄 비즈니스 로직, 이미지 관리 |
| | `MedicationRecordService.java` | 복약 기록 비즈니스 로직, 배치 작업 |
| | `MedicationStatsService.java` | 복약 이행률 및 통계 |
| | `MedicationTimeService.java` | 복약 알림 시간 관리 |
| | `MedicationScheduleGroupService.java` | 복약 스케줄 그룹 관리 |
| | `FileStorageService.java` | 파일 저장/조회/삭제 서비스 |
| | `MedicationScheduleEventHandler.java` | 복약 스케줄 이벤트 처리 |
| `service/sideEffect/` | `SideEffectService.java` | 부작용 비즈니스 로직 |
| | `RecentSideEffectCacheService.java` | 최근 부작용 캐싱 서비스 |
| `service/oauth2/` | `KakaoOAuth2UserService.java` | 카카오 OAuth2 사용자 서비스 |
| | `StatePersistenceService.java` | OAuth2 State 관리 |
| `service/security/` | `RateLimitingService.java` | 요청 제한 서비스 |
| | `RefreshTokenService.java` | 리프레시 토큰 관리 |
| `service/i18n/` | `MessageService.java` | 다국어 메시지 서비스 |
| `service/monitoring/` | `TransactionMetricsService.java` | 트랜잭션 메트릭 서비스 |
| `service/export/` | `ExportService.java` | 데이터 내보내기 서비스 |
| `service/notification/` | `NotificationSettingsService.java` | 알림 설정 및 FCM 토큰 관리 |
| | `FcmPushService.java` | FCM 푸시 알림 발송 |
| | `NotificationSchedulerService.java` | 알림 스케줄러 |
| | `NotificationSettingsEventHandler.java` | 알림 설정 이벤트 핸들러 |
| `service/queue/` | `MessageQueueService.java` | 메시지 발행 (Producer) |
| | `NotificationConsumerService.java` | 메시지 소비 및 FCM 발송 |
| | `QueuedNotificationService.java` | 큐 활성화 여부에 따른 Facade |
| | `DiscordWebhookService.java` | DLQ 알림 발송 |
| `service/queue/message/` | `NotificationMessage.java` | 알림 메시지 DTO |
| | `NotificationType.java` | 알림 유형 상수 |
| `service/alert/` | `DiscordAlertService.java` | Discord Webhook 알림 서비스 |
| `service/benchmark/` | `BenchmarkService.java` | 성능 벤치마크 서비스 (개발/테스트 환경만) |

---

### 3. Domain Layer (`domain/`)

JPA 엔티티 및 도메인 모델을 정의합니다.

| 패키지 | 파일 | 설명 |
|--------|------|------|
| `domain/member/` | `Member.java` | 회원 엔티티 |
| `domain/diary/` | `MoodDiary.java` | 마음 일기 엔티티 |
| | `MoodType.java` | 기분 타입 Enum |
| | `DiaryType.java` | 일기 타입 Enum |
| `domain/medication/` | `MedicationSchedule.java` | 복약 스케줄 엔티티 |
| | `MedicationScheduleGroup.java` | 복약 스케줄 그룹 엔티티 |
| | `MedicationScheduleMedicationScheduleGroup.java` | 스케줄-그룹 연결 테이블 |
| | `MedicationRecord.java` | 복약 기록 엔티티 |
| | `MedicationTime.java` | 복약 시간 엔티티 |
| | `AlarmType.java` | 알람 타입 Enum |
| `domain/sideEffect/` | `SideEffect.java` | 부작용 엔티티 |
| | `SideEffectRecord.java` | 부작용 기록 엔티티 |
| | `SideEffectSideEffectRecord.java` | 부작용-기록 연결 테이블 |
| | `SideEffectDegree.java` | 부작용 정도 Enum |
| `domain/security/` | `RefreshToken.java` | 리프레시 토큰 엔티티 |
| | `LoginHistory.java` | 로그인 이력 엔티티 **(신규)** |
| `domain/notification/` | `NotificationSettings.java` | 알림 설정 엔티티 **(신규)** |
| | `FcmDeviceToken.java` | FCM 디바이스 토큰 엔티티 **(신규)** |
| | `DeviceType.java` | 디바이스 타입 Enum **(신규)** |
| | `NotificationType.java` | 알림 타입 Enum **(신규)** |
| `domain/events/` | `DomainEvent.java` | 도메인 이벤트 인터페이스 |
| | `DomainEventPublisher.java` | 도메인 이벤트 발행자 |
| `domain/idClass/` | `MedicationScheduleMedicationScheduleGroupId.java` | 복합 키 클래스 |
| | `SideEffectSideEffectRecordId.java` | 복합 키 클래스 |

---

### 4. Repository Layer (`repository/`)

데이터베이스 접근을 담당하는 JPA Repository 계층입니다.

| 패키지 | 파일 | 설명 |
|--------|------|------|
| `repository/member/` | `MemberRepository.java` | 회원 데이터 접근 |
| `repository/diary/` | `MoodDiaryRepository.java` | 마음 일기 데이터 접근 |
| `repository/medication/` | `MedicationScheduleRepository.java` | 복약 스케줄 데이터 접근 |
| | `MedicationRecordRepository.java` | 복약 기록 데이터 접근 |
| | `MedicationTimeRepository.java` | 복약 시간 데이터 접근 |
| | `MedicationScheduleGroupRepository.java` | 복약 그룹 데이터 접근 |
| `repository/sideEffect/` | `SideEffectRepository.java` | 부작용 데이터 접근 |
| | `SideEffectRecordRepository.java` | 부작용 기록 데이터 접근 |
| | `SideEffectSideEffectRecordRepository.java` | 부작용-기록 연결 데이터 접근 |
| `repository/security/` | `RefreshTokenRepository.java` | 리프레시 토큰 데이터 접근 |
| | `LoginHistoryRepository.java` | 로그인 이력 데이터 접근 **(신규)** |
| `repository/notification/` | `NotificationSettingsRepository.java` | 알림 설정 데이터 접근 **(신규)** |
| | `FcmDeviceTokenRepository.java` | FCM 토큰 데이터 접근 **(신규)** |

---

### 5. DTO Layer (`dto/`)

요청 및 응답 데이터 전송 객체를 정의합니다.

| 패키지 | 구분 | 파일 | 설명 |
|--------|------|------|------|
| `dto/auth/request/` | Request | `SignupRequest.java` | 회원가입 요청 |
| | | `LoginRequest.java` | 로그인 요청 |
| | | `TokenRefreshRequest.java` | 토큰 갱신 요청 |
| | | `ProfileUpdateRequest.java` | 프로필 수정 요청 **(신규)** |
| | | `PasswordChangeRequest.java` | 비밀번호 변경 요청 **(신규)** |
| `dto/auth/response/` | Response | `LoginResponse.java` | 로그인 응답 |
| | | `TokenRefreshResponse.java` | 토큰 갱신 응답 |
| | | `MemberProfileResponse.java` | 회원 프로필 응답 **(신규)** |
| `dto/diary/request/` | Request | `MoodDiaryCreateRequest.java` | 마음 일기 생성 요청 |
| | | `MoodDiaryUpdateRequest.java` | 마음 일기 수정 요청 **(신규)** |
| `dto/diary/response/` | Response | `MoodDiaryResponse.java` | 마음 일기 응답 |
| | | `MoodDiaryListResponse.java` | 마음 일기 목록 응답 |
| | | `MoodDiaryStatsResponse.java` | 마음 일기 통계 응답 **(신규)** |
| | | `MoodDiaryCalendarResponse.java` | 마음 일기 캘린더 응답 **(신규)** |
| `dto/medication/request/` | Request | `MedicationScheduleCreateRequest.java` | 복약 스케줄 생성 요청 |
| | | `MedicationScheduleUpdateRequest.java` | 복약 스케줄 수정 요청 |
| | | `MedicationRecordCreateRequest.java` | 복약 기록 생성 요청 |
| | | `MedicationRecordUpdateRequest.java` | 복약 기록 수정 요청 |
| | | `MedicationTimeCreateRequest.java` | 알림 시간 생성 요청 **(신규)** |
| | | `MedicationTimeUpdateRequest.java` | 알림 시간 수정 요청 **(신규)** |
| `dto/medication/response/` | Response | `MedicationScheduleResponse.java` | 복약 스케줄 응답 |
| | | `MedicationScheduleListResponse.java` | 복약 스케줄 목록 응답 |
| | | `MedicationRecordResponse.java` | 복약 기록 응답 |
| | | `MedicationAdherenceResponse.java` | 복약 이행률 응답 **(신규)** |
| | | `MedicationSummaryResponse.java` | 복약 요약 응답 **(신규)** |
| | | `MedicationTimeResponse.java` | 알림 시간 응답 **(신규)** |
| `dto/sideEffect/request/` | Request | `SideEffectRecordRequest.java` | 부작용 기록 요청 |
| `dto/sideEffect/response/` | Response | `RecentSideEffectResponse.java` | 최근 부작용 응답 |
| | | `SideEffectRecordResponse.java` | 부작용 기록 상세 응답 **(신규)** |
| | | `SideEffectRecordListResponse.java` | 부작용 기록 목록 응답 **(신규)** |
| `dto/notification/request/` | Request | `FcmTokenRegisterRequest.java` | FCM 토큰 등록 요청 **(신규)** |
| | | `NotificationSettingsUpdateRequest.java` | 알림 설정 수정 요청 **(신규)** |
| `dto/notification/response/` | Response | `NotificationSettingsResponse.java` | 알림 설정 응답 **(신규)** |
| | | `FcmDeviceTokenResponse.java` | FCM 토큰 응답 **(신규)** |
| | | `FcmDeviceTokenListResponse.java` | FCM 토큰 목록 응답 **(신규)** |

---

### 6. Security Layer (`security/`)

보안 관련 기능을 담당합니다.

| 패키지 | 파일 | 설명 |
|--------|------|------|
| `security/` | `CustomUserDetails.java` | 커스텀 UserDetails 구현 |
| | `CustomUserDetailsService.java` | 커스텀 UserDetailsService 구현 |
| `security/jwt/` | `JwtTokenProvider.java` | JWT 토큰 생성 및 검증 |
| | `JwtAuthenticationFilter.java` | JWT 인증 필터 |
| | `TokenBlacklistService.java` | 토큰 블랙리스트 관리 |
| `security/filter/` | `RateLimitingFilter.java` | 요청 제한 필터 |
| | `CsrfValidationFilter.java` | CSRF 검증 필터 |
| | `BotProtectionFilter.java` | 봇 차단 필터 |
| | `RequestSizeMonitoringFilter.java` | 요청 크기 모니터링 필터 |
| | `TrustedProxyService.java` | 신뢰 프록시 서비스 |
| `security/oauth2/` | `OAuth2AuthenticationSuccessHandler.java` | OAuth2 인증 성공 핸들러 |
| `security/csrf/` | `CsrfTokenProvider.java` | CSRF 토큰 제공자 |
| `security/encryption/` | `DataEncryptionUtil.java` | 데이터 암호화 유틸리티 |
| | `EncryptedStringConverter.java` | 암호화된 문자열 변환기 |
| | `EncryptedLocalDateConverter.java` | 암호화된 날짜 변환기 |
| `security/authorization/` | `ResourceOwnershipValidator.java` | 리소스 소유권 검증 |
| `security/validation/` | `InputValidationUtil.java` | 입력 검증 유틸리티 |
| `security/annotation/` | `RequireResourceOwnership.java` | 리소스 소유권 어노테이션 |
| `security/aspect/` | `ResourceOwnershipAspect.java` | 리소스 소유권 AOP |

---

### 7. Configuration Layer (`config/`)

애플리케이션 설정 클래스들입니다.

| 파일 | 설명 |
|------|------|
| `ApiVersion.java` | API 경로 상수 |
| `SecurityConfig.java` | Spring Security 설정 |
| `WebMvcConfig.java` | Web MVC 설정 (CORS 등) |
| `RedisConfig.java` | Redis 연결 설정 |
| `RateLimitProperties.java` | Rate Limit 설정 프로퍼티 |
| `RestTemplateConfig.java` | RestTemplate 설정 |
| `OpenApiConfig.java` | Swagger/OpenAPI 설정 |
| `AopConfiguration.java` | AOP 설정 |
| `TextPlainJsonHttpMessageConverter.java` | 커스텀 HTTP 메시지 변환기 |
| `MessageQueueConfig.java` | 메시지 큐 설정 **(신규)** |
| `MessageQueueProperties.java` | 메시지 큐 프로퍼티 **(신규)** |
| `AlertConfig.java` | Discord 알림 설정 **(신규)** |
| `AlertProperties.java` | 알림 프로퍼티 **(신규)** |
| `FirebaseConfig.java` | Firebase Admin SDK 설정 **(신규)** |
| `AsyncConfig.java` | 비동기 처리 설정 |
| `SchedulingConfig.java` | 스케줄링 설정 |
| `QuerydslConfig.java` | QueryDSL 설정 |

---

### 8. AOP Layer (`aop/`)

횡단 관심사를 처리하는 Aspect 클래스들입니다.

| 파일 | 설명 |
|------|------|
| `ApiLoggingAspect.java` | API 요청/응답 로깅 |
| `PerformanceMonitoringAspect.java` | 성능 모니터링 (실행 시간 측정) |
| `ServiceLoggingAspect.java` | 서비스 계층 일관 로깅 |
| `BusinessAuditAspect.java` | 비즈니스 감사 로깅 |
| `CachingAspect.java` | 캐싱 처리 |
| `RetryAspect.java` | 재시도 메커니즘 |

---

### 9. Logging Layer (`logging/`)

구조화된 로깅 시스템을 제공합니다.

| 패키지 | 파일 | 설명 |
|--------|------|------|
| `logging/` | `StructuredLogger.java` | 구조화된 로거 |
| | `RequestLoggingFilter.java` | 요청 로깅 필터 |
| | `HealthCheckFilter.java` | 헬스체크 필터 |
| | `MDCUtil.java` | MDC 유틸리티 |
| | `LoggingConstants.java` | 로깅 상수 |
| | `SensitiveDataMasker.java` | 민감 데이터 마스킹 |
| | `StatusAwareResponseWrapper.java` | 응답 래퍼 |
| `logging/events/` | `ApiEvent.java` | API 이벤트 |
| | `AuditEvent.java` | 감사 이벤트 |
| | `BusinessEvent.java` | 비즈니스 이벤트 |
| | `PerformanceEvent.java` | 성능 이벤트 |
| | `SecurityEvent.java` | 보안 이벤트 |
| `logging/business/` | `BusinessIntelligenceLogger.java` | 비즈니스 인텔리전스 로거 |
| `logging/metrics/` | `JVMMetricsLogger.java` | JVM 메트릭 로거 |
| `logging/security/` | `SecurityEventMonitor.java` | 보안 이벤트 모니터 |

---

### 10. Exception Layer (`exception/`)

예외 처리 클래스들입니다.

| 패키지 | 파일 | 설명 |
|--------|------|------|
| `exception/` | `CustomException.java` | 커스텀 예외 기본 클래스 |
| | `ErrorCode.java` | 에러 코드 Enum |
| `exception/member/` | `MemberNotFoundException.java` | 회원 없음 예외 |
| `exception/diary/` | `MoodDiaryNotFoundException.java` | 마음 일기 없음 예외 |
| | `DiaryAlreadyExistsException.java` | 일기 중복 예외 |
| | `InvalidDiaryTypeException.java` | 잘못된 일기 타입 예외 |
| `exception/medication/` | `MedicationScheduleNotFoundException.java` | 복약 스케줄 없음 예외 |
| | `MedicationRecordNotFoundException.java` | 복약 기록 없음 예외 |
| | `MedicationTimeNotFoundException.java` | 복약 시간 없음 예외 |
| `exception/sideEffect/` | `SideEffectNotFoundException.java` | 부작용 없음 예외 |
| `exception/token/` | `TokenException.java` | 토큰 예외 |
| | `TokenExpiredException.java` | 토큰 만료 예외 |
| | `RefreshTokenException.java` | 리프레시 토큰 예외 |
| `exception/oauth2/` | `OAuth2Exception.java` | OAuth2 예외 |
| | `OAuth2TokenExchangeException.java` | OAuth2 토큰 교환 예외 |
| `exception/file/` | `FileSaveFailException.java` | 파일 저장 실패 예외 |
| `exception/validation/` | `InvalidInputException.java` | 잘못된 입력 예외 |

---

### 11. Handler Layer (`handler/`)

전역 예외 핸들러 클래스들입니다.

| 파일 | 설명 |
|------|------|
| `GlobalExceptionHandler.java` | 전역 예외 처리기 (보안 기능 통합) |
| `ExceptionHandlerUtils.java` | 예외 처리 유틸리티 |
| `ErrorResponse.java` | 에러 응답 DTO |
| `ErrorSeverity.java` | 에러 심각도 Enum |

---

### 12. Validation Layer (`validation/`)

커스텀 유효성 검증 클래스들입니다.

| 파일 | 설명 |
|------|------|
| `ValidImage.java` | 이미지 유효성 검증 어노테이션 |
| `ImageValidator.java` | 이미지 유효성 검증 구현체 |

---

## 리소스 파일

### `application.properties` 주요 설정

| 설정 영역 | 설명 |
|-----------|------|
| 데이터베이스 | MySQL/H2 연결 설정 |
| JPA | Hibernate 설정 |
| Security | JWT 시크릿, 만료 시간 |
| Redis | 캐시 및 세션 저장소 설정 |
| OAuth2 | 카카오 OAuth2 설정 |
| Logging | 로그 레벨 설정 |
| Actuator | 모니터링 엔드포인트 설정 |

### 로그 파일 구조

| 파일 | 설명 |
|------|------|
| `logs/application.log` | 애플리케이션 일반 로그 |
| `logs/audit.log` | 감사 로그 |
| `logs/performance.log` | 성능 로그 |
| `logs/security.log` | 보안 로그 |

---

## 배포 아키텍처

### 인프라 구성도 (Infrastructure Architecture)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              Internet (사용자)                                   │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          Cloudflare Edge Network                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  • DDoS 방어                    • WAF (웹 방화벽)                       │   │
│  │  • SSL/TLS 종단                 • Bot Management                        │   │
│  │  • CDN 캐싱                     • Rate Limiting (1차)                   │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
                              Cloudflare Tunnel
                           (Zero Trust 보안 터널)
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        On-Premise Server (홈 서버)                               │
│  ┌──────────────────────────────────────────────────────────────────────────┐  │
│  │                     Docker Compose Network                                │  │
│  │                                                                           │  │
│  │   ┌─────────────────┐                                                    │  │
│  │   │ cloudflared     │◄──── Cloudflare Tunnel 연결                        │  │
│  │   │ (tunnel)        │      외부 포트 노출 없음                            │  │
│  │   └────────┬────────┘                                                    │  │
│  │            │                                                              │  │
│  │            ▼                                                              │  │
│  │   ┌─────────────────┐                                                    │  │
│  │   │     Nginx       │◄──── 리버스 프록시                                  │  │
│  │   │  (nginx-hamalog)│      • 봇 차단 (User-Agent 필터링)                 │  │
│  │   │    Port: 80     │      • Rate Limiting (2차)                         │  │
│  │   │   (내부 전용)    │      • 보안 헤더 추가                              │  │
│  │   └────────┬────────┘      • 악성 경로 차단                              │  │
│  │            │                                                              │  │
│  │            ▼                                                              │  │
│  │   ┌─────────────────┐      ┌─────────────────┐   ┌─────────────────┐    │  │
│  │   │  Spring Boot    │      │     Redis       │   │     MySQL       │    │  │
│  │   │  (hamalog-app)  │◄────►│ (hamalog-redis) │   │ (mysql-hamalog) │    │  │
│  │   │   Port: 8080    │      │   Port: 6379    │   │   Port: 3306    │    │  │
│  │   │   (내부 전용)    │      │   (내부 전용)    │   │   (내부 전용)    │    │  │
│  │   └────────┬────────┘      └─────────────────┘   └─────────────────┘    │  │
│  │            │                        │                     │              │  │
│  │            │                        ▼                     ▼              │  │
│  │            │               ┌─────────────────┐   ┌─────────────────┐    │  │
│  │            │               │   redis-data    │   │   mysql-data    │    │  │
│  │            │               │    (Volume)     │   │    (Volume)     │    │  │
│  │            │               └─────────────────┘   └─────────────────┘    │  │
│  │            ▼                                                              │  │
│  │   ┌─────────────────┐                                                    │  │
│  │   │ hamalog-uploads │                                                    │  │
│  │   │    (Volume)     │                                                    │  │
│  │   └─────────────────┘                                                    │  │
│  └──────────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

### CI/CD 파이프라인 (GitHub Actions)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                            GitHub Repository                                  │
│                                                                               │
│   main 브랜치 Push ─────────────────────────────────────────────────────►    │
│                                                                               │
└──────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                     Build Job (ubuntu-latest)                                 │
│  ┌────────────────────────────────────────────────────────────────────────┐  │
│  │  1. Checkout Repository                                                 │  │
│  │  2. Setup Java 21 (Temurin)                                            │  │
│  │  3. Gradle Build (./gradlew clean build -x test)                       │  │
│  │  4. Docker Login (GHCR)                                                 │  │
│  │  5. Docker Build & Push                                                 │  │
│  │     └─► ghcr.io/daemin-kim/hamalog:latest                              │  │
│  └────────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                   Deploy Job (self-hosted runner)                             │
│  ┌────────────────────────────────────────────────────────────────────────┐  │
│  │  1. Generate docker-compose.prod.yml                                    │  │
│  │  2. Inject GitHub Secrets (환경 변수)                                   │  │
│  │  3. Cleanup existing containers                                         │  │
│  │  4. Pull latest Docker image                                            │  │
│  │  5. docker compose up -d                                                │  │
│  │  6. Health Check (20회 재시도, 10초 간격)                               │  │
│  │  7. Deployment verification                                             │  │
│  └────────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────┘
```

#### 배포 파이프라인 상세

| 단계 | Job | Runner | 설명 |
|------|-----|--------|------|
| 1 | Build | `ubuntu-latest` | Gradle 빌드 및 Docker 이미지 생성 |
| 2 | Push | `ubuntu-latest` | GHCR에 Docker 이미지 푸시 |
| 3 | Deploy | `self-hosted` | On-Premise 서버에 컨테이너 배포 |
| 4 | Verify | `self-hosted` | 헬스체크를 통한 배포 검증 |

#### 배포 관련 파일

| 파일 | 위치 | 설명 |
|------|------|------|
| `deploy-main.yml` | `.github/workflows/` | GitHub Actions 워크플로우 정의 |
| `deploy.sh` | `.github/scripts/` | 환경 설정 및 배포 스크립트 |
| `verify-deployment.sh` | `.github/scripts/` | 배포 검증 스크립트 |
| `setup-ssh-and-copy.sh` | `.github/scripts/` | SSH 설정 및 파일 복사 스크립트 |

---

### Docker 컨테이너 상세 구성

#### 컨테이너 정보

| 컨테이너 | 이미지 | 포트 | 역할 | 의존성 |
|----------|--------|------|------|--------|
| `cloudflare-tunnel` | `cloudflare/cloudflared:latest` | 없음 (outbound only) | Cloudflare 터널 연결 | nginx |
| `nginx-hamalog` | `nginx:alpine` | 80 (내부) | 리버스 프록시 | hamalog-app |
| `hamalog-app` | `ghcr.io/daemin-kim/hamalog:latest` | 8080 (내부) | Spring Boot 앱 | mysql, redis |
| `hamalog-redis` | `redis:7-alpine` | 6379 (내부) | 캐시/세션 저장소 | - |
| `mysql-hamalog` | `mysql:8.0` | 3306 (내부) | 데이터베이스 | - |

#### 컨테이너 리소스 제한

| 컨테이너 | CPU 제한 | 메모리 제한 | CPU 예약 | 메모리 예약 |
|----------|----------|-------------|----------|-------------|
| `hamalog-app` | 2.0 cores | 1536MB | 0.5 cores | 512MB |

#### 볼륨 구성

| 볼륨 | 마운트 경로 | 용도 |
|------|-------------|------|
| `mysql-data` | `/var/lib/mysql` | MySQL 데이터 영구 저장 |
| `redis-data` | `/data` | Redis AOF 영구 저장 |
| `hamalog-uploads` | `/data/hamalog/images` | 업로드 파일 저장 |

#### 헬스체크 설정

| 컨테이너 | 엔드포인트 | 간격 | 타임아웃 | 시작 대기 | 재시도 |
|----------|------------|------|----------|-----------|--------|
| `hamalog-app` | `/actuator/health` | 30s | 10s | 60s | 3 |
| `hamalog-redis` | `redis-cli ping` | 30s | 10s | - | 3 |
| `mysql-hamalog` | `mysqladmin ping` | 30s | 10s | - | 3 |

---

### Nginx 보안 설정

#### Rate Limiting 구성

| Zone | 제한 | Burst | 적용 대상 |
|------|------|-------|-----------|
| `api_limit` | 10 req/s | 20 | 일반 API 요청 |
| `auth_limit` | 5 req/min | 3 | 인증 관련 엔드포인트 |
| `conn_limit` | 20 connections | - | 동시 연결 수 |

#### 차단 목록

**악성 봇 User-Agent 차단:**
```
l9scan, leakix, zgrab, nuclei, nikto, sqlmap, masscan, nmap, 
dirbuster, gobuster, wfuzz, burpsuite, acunetix, nessus, 
censys, shodan, Cortex-Xpanse, cypex.ai
```

**악성 경로 차단:**
```
.env, .git, .svn, .DS_Store, wp-admin, wp-login, 
phpmyadmin, *.php, *.asp, graphql, server-status
```

#### 보안 헤더

| 헤더 | 값 | 설명 |
|------|-----|------|
| `X-Frame-Options` | DENY | 클릭재킹 방지 |
| `X-Content-Type-Options` | nosniff | MIME 스니핑 방지 |
| `X-XSS-Protection` | 1; mode=block | XSS 필터링 |
| `Referrer-Policy` | strict-origin-when-cross-origin | 리퍼러 정책 |
| `Permissions-Policy` | geolocation=(), microphone=(), camera=() | 권한 정책 |

---

### Cloudflare Tunnel 구성

#### Zero Trust 보안 모델

```
┌─────────────────────────────────────────────────────────────────┐
│                    보안 레이어 구조                               │
├─────────────────────────────────────────────────────────────────┤
│ Layer 1: Cloudflare Edge                                         │
│   • DDoS 공격 완화                                               │
│   • SSL/TLS 암호화                                               │
│   • WAF (웹 애플리케이션 방화벽)                                  │
├─────────────────────────────────────────────────────────────────┤
│ Layer 2: Cloudflare Tunnel                                       │
│   • 외부 포트 노출 없음 (Zero Inbound Ports)                     │
│   • 아웃바운드 전용 연결                                          │
│   • IP 주소 비공개                                               │
├─────────────────────────────────────────────────────────────────┤
│ Layer 3: Nginx Reverse Proxy                                     │
│   • Rate Limiting                                                │
│   • 봇 차단                                                       │
│   • 보안 헤더 주입                                               │
├─────────────────────────────────────────────────────────────────┤
│ Layer 4: Spring Security                                         │
│   • JWT 인증                                                      │
│   • CSRF 보호                                                    │
│   • 리소스 소유권 검증                                           │
└─────────────────────────────────────────────────────────────────┘
```

#### Real IP 복원 설정

Cloudflare를 통해 들어오는 요청에서 실제 클라이언트 IP를 복원합니다:

| 설정 | 값 | 설명 |
|------|-----|------|
| `real_ip_header` | `CF-Connecting-IP` | Cloudflare 전용 헤더 |
| `set_real_ip_from` | Cloudflare IPv4/IPv6 대역 | 신뢰할 수 있는 프록시 IP |
| `real_ip_recursive` | on | 재귀적 IP 복원 |

---

### 개발 vs 프로덕션 환경 비교

| 구분 | 개발 환경 | 프로덕션 환경 |
|------|-----------|---------------|
| **구성 파일** | `docker-compose-dev.yml` | `docker-compose.yml` |
| **Nginx** | ❌ 미사용 | ✅ 리버스 프록시 |
| **Cloudflare Tunnel** | ❌ 미사용 | ✅ Zero Trust 터널 |
| **포트 노출** | localhost 바인딩 | 내부 네트워크만 |
| **SSL/TLS** | ❌ | ✅ (Cloudflare) |
| **Rate Limiting** | ❌ | ✅ (Nginx + Spring) |
| **봇 차단** | 선택적 비활성화 가능 | ✅ 활성화 |
| **헬스체크** | 기본 | 상세 설정 |

---

### 환경 변수 (GitHub Secrets)

| 시크릿 | 필수 | 설명 | 예시 |
|--------|------|------|------|
| `MYSQL_ROOT_PASSWORD` | ✅ | MySQL root 비밀번호 | 강력한 비밀번호 |
| `DB_NAME` | ✅ | 데이터베이스 이름 | `hamalog` |
| `DB_USERNAME` | ✅ | DB 사용자 이름 | `hamalog_user` |
| `DB_PASSWORD` | ✅ | DB 사용자 비밀번호 | 강력한 비밀번호 |
| `JWT_SECRET` | ✅ | JWT 서명 키 (Base64) | `openssl rand -base64 32` |
| `JWT_EXPIRY` | ✅ | Access Token 만료(ms) | `900000` (15분) |
| `JWT_REFRESH_TOKEN_EXPIRY` | ✅ | Refresh Token 만료(ms) | `604800000` (7일) |
| `HAMALOG_ENCRYPTION_KEY` | ✅ | 데이터 암호화 키 | `openssl rand -base64 32` |
| `KAKAO_CLIENT_ID` | ✅ | 카카오 REST API 키 | 카카오 개발자 콘솔 |
| `KAKAO_CLIENT_SECRET` | ✅ | 카카오 Client Secret | 카카오 개발자 콘솔 |
| `KAKAO_REDIRECT_URI` | ✅ | OAuth2 콜백 URI | `https://api.hamalog.shop/...` |
| `SPRING_DATA_REDIS_PASSWORD` | ✅ | Redis 비밀번호 | 강력한 비밀번호 |
| `CLOUDFLARE_TUNNEL_TOKEN` | ✅ | Cloudflare Tunnel 토큰 | Zero Trust 대시보드 |

---

## 빌드 및 의존성

### Gradle 주요 의존성

| 구분 | 의존성 | 설명 |
|------|--------|------|
| **Core** | `spring-boot-starter-web` | Spring MVC 웹 애플리케이션 |
| | `spring-boot-starter-data-jpa` | JPA/Hibernate |
| | `spring-boot-starter-validation` | Bean Validation |
| **Security** | `spring-boot-starter-security` | Spring Security |
| | `spring-boot-starter-oauth2-client` | OAuth2 클라이언트 |
| | `jjwt-api`, `jjwt-impl`, `jjwt-jackson` | JWT 토큰 처리 |
| **Database** | `mysql-connector-j` | MySQL 드라이버 |
| | `h2` | H2 인메모리 DB (개발용) |
| | `spring-boot-starter-data-redis` | Redis 연동 |
| **Monitoring** | `spring-boot-starter-actuator` | 애플리케이션 모니터링 |
| | `logstash-logback-encoder` | 구조화된 JSON 로깅 |
| **Documentation** | `springdoc-openapi-starter-webmvc-ui` | Swagger UI |
| **Development** | `lombok` | 보일러플레이트 코드 제거 |
| | `spring-boot-devtools` | 개발 편의 도구 |

### Dockerfile 구성

```dockerfile
FROM eclipse-temurin:21-jre-alpine

# JVM 최적화 옵션
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200"
```

| 설정 | 값 | 설명 |
|------|-----|------|
| Base Image | `eclipse-temurin:21-jre-alpine` | 경량 JRE 이미지 |
| GC | G1GC | 짧은 GC 중단 시간 |
| RAM 사용률 | 75% (Max), 50% (Initial) | 컨테이너 메모리 최적화 |
| GC Pause | 200ms | 최대 GC 중단 시간 |

---

## 아키텍처 패턴

### 계층형 아키텍처 (Layered Architecture)

```
┌─────────────────────────────────────────────────────────────┐
│                    Controller Layer                          │
│              (REST API 엔드포인트 처리)                       │
├─────────────────────────────────────────────────────────────┤
│                     Service Layer                            │
│                  (비즈니스 로직 처리)                         │
├─────────────────────────────────────────────────────────────┤
│                   Repository Layer                           │
│                  (데이터 접근 처리)                           │
├─────────────────────────────────────────────────────────────┤
│                     Domain Layer                             │
│               (엔티티 및 도메인 모델)                         │
└─────────────────────────────────────────────────────────────┘
```

### 적용된 디자인 패턴

| 패턴 | 적용 위치 | 설명 |
|------|-----------|------|
| **Repository Pattern** | `repository/` | 데이터 접근 추상화 |
| **DTO Pattern** | `dto/` | 계층 간 데이터 전송 |
| **Event-Driven** | `domain/events/`, `service/*EventHandler` | 도메인 이벤트 기반 처리 |
| **AOP** | `aop/`, `security/aspect/` | 횡단 관심사 분리 |
| **Strategy Pattern** | `security/filter/` | 다양한 필터 전략 |
| **Template Method** | `logging/` | 로깅 템플릿 |

---

## 보안 기능

| 기능 | 구현 위치 | 설명 |
|------|-----------|------|
| **JWT 인증** | `security/jwt/` | 토큰 기반 인증 |
| **CSRF 보호** | `security/csrf/`, `security/filter/` | CSRF 토큰 검증 |
| **Rate Limiting** | `security/filter/RateLimitingFilter.java` | 요청 제한 |
| **봇 차단** | `security/filter/BotProtectionFilter.java` | 악성 봇 차단 |
| **리소스 소유권 검증** | `security/authorization/` | 본인 데이터만 접근 가능 |
| **데이터 암호화** | `security/encryption/` | 민감 데이터 암호화 저장 |
| **SSRF 방지** | `security/ssrf/` | 서버 사이드 요청 위조 방지 |
| **입력 검증** | `security/validation/`, `validation/` | XSS, SQL Injection 방지 |

---

## 테스트 구조

```
src/test/java/com/Hamalog/
├── HamalogApplicationTests.java    # 통합 테스트
├── aop/                            # AOP 테스트
├── config/                         # 설정 테스트
├── controller/                     # 컨트롤러 테스트
├── domain/                         # 도메인 테스트
├── dto/                            # DTO 테스트
├── exception/                      # 예외 테스트
├── handler/                        # 핸들러 테스트
├── logging/                        # 로깅 테스트
├── nplusone/                       # N+1 문제 테스트
├── repository/                     # 리포지토리 테스트
├── security/                       # 보안 테스트
├── service/                        # 서비스 테스트
└── validation/                     # 유효성 검사 테스트
```

---

## 문서 버전

| 버전 | 날짜 | 작성자 | 비고 |
|------|------|--------|------|
| 1.0.0 | 2025-12-16 | - | 최초 작성 |
| 1.1.0 | 2025-12-16 | - | 배포 아키텍처 및 CI/CD 파이프라인 추가 |
| 1.2.0 | 2025-12-17 | - | AuthService 분리 리팩토링 완료 (SRP 적용) |
| 1.3.0 | 2025-12-20 | - | 최종 명세 동기화 및 누락 클래스(MemberCacheService 등) 추가 |
| 1.4.0 | 2026-01-03 | - | Redis Stream 메시지 큐 시스템 추가 |
| 1.5.0 | 2026-01-12 | - | 프로젝트 구조 현행화 (Kotlin 소스, alert 패키지, 신규 Config 반영) |

---

## 변경 이력 (Changelog)

### v1.5.0 (2026-01-12) - 프로젝트 구조 현행화

#### 🔄 주요 변경사항

**신규 패키지 및 클래스 반영**
- `service/alert/` 패키지 및 `DiscordAlertService.java` 추가 (Discord Webhook 알림)
- `service/notification/NotificationSettingsEventHandler.java` 추가 (이벤트 핸들러)
- `config/AlertConfig.java`, `config/AlertProperties.java` 추가 (알림 설정)

**Kotlin 소스 구조 상세화**
- `src/main/kotlin/com/Hamalog/` 디렉토리 구조 문서화
- Kotlin 유틸리티 파일 (`DateExtensions.kt`, `StringExtensions.kt`) 반영
- Kotlin DTO 디렉토리 구조 (`dto/medication/request`, `response`) 명시

---

### v1.4.0 (2026-01-20) - 미구현 컨트롤러 구현 및 보안 설정 정리

#### 🆕 구현 완료

**신규 컨트롤러 구현**
| 컨트롤러 | 기능 | 엔드포인트 |
|----------|------|-----------|
| `ExportController` | 데이터 내보내기 (JSON/CSV) | `/export/*` |
| `MedicationScheduleGroupController` | 복약 스케줄 그룹 관리 | `/medication-group/*` |

**보안 설정 개선**
- CORS 허용 헤더에 `X-CSRF-TOKEN`, `X-FCM-Token` 추가
- 벤치마크 API는 개발/테스트 환경에서만 활성화 (`@Profile`)

#### 📚 문서 정리
- 공개 API 명세서에서 벤치마크 API 제외 (내부 문서만 기록)
- 컨트롤러/서비스 목록 현행화 (**(신규)** 표시 정리)

---

### v1.3.0 (2026-01-12) - 프로젝트 정리 및 현행화

**문서 간 일관성 개선**
- API 명세서, API 참고 문서와 상호 참조 링크 검증
- 중복 정보 정리 및 상세 내역은 CHANGELOG.md 참조 안내 추가

---

### v1.2.0 (2025-12-17) - AuthService 리팩토링

#### 🔄 주요 변경사항

**AuthService 분리 (SRP 원칙 적용)**
- 기존 417줄의 `AuthService.java`를 4개의 독립적인 서비스로 분리
- 각 서비스가 단일 책임 원칙(SRP)을 준수

| 서비스 | 책임 | 비고 |
|--------|------|------|
| `MemberRegistrationService` | 회원가입, 회원 정보 검증 | ~90줄 |
| `AuthenticationService` | 로그인, 로그아웃, 토큰 관리 | ~132줄 |
| `MemberDeletionService` | 회원 탈퇴, 관련 데이터 삭제 | ~102줄 |
| `KakaoOAuth2AuthService` | 카카오 OAuth2 인증 처리 | ~290줄 |

**테스트 추가**
- `MemberRegistrationServiceTest` (208줄)
- `AuthenticationServiceTest` (369줄)
- `MemberDeletionServiceTest` 
- `KakaoOAuth2AuthServiceTest`

**코드 개선**
- deprecated 메서드 완전 제거
- 불필요한 import 문 정리
- AuthService Facade 패턴 제거 완료

---

## 참고 문서

- [API 명세서](./API-specification.md) - 프론트엔드 개발자용 REST API 엔드포인트 명세
- [API 참고 문서](../internal/API-reference.md) - 인프라 구성, DB 스키마, 변경 이력
- [README](../README.md)

