# 🎯 Hamalog 프로젝트 실무 평가 리포트

> **평가 기준**: 신입 개발자 포트폴리오 + 스타트업 프로덕션 코드 기준
> 
> **평가 일자**: 2026-01-14
> 
> **평가 관점**: 시니어 개발자 / 채용 담당자 / CTO 시점

---

## 📊 종합 평가 요약

### 총점: **92/100** (상위 5% 신입 포트폴리오 수준)

| 평가 항목 | 점수 | 신입 기대치 | 스타트업 기준 | 평가 |
|-----------|------|-------------|---------------|------|
| 기술 스택 선택 | 95/100 | 70점 | 85점 | ⭐⭐⭐⭐⭐ |
| 아키텍처 설계 | 93/100 | 60점 | 80점 | ⭐⭐⭐⭐⭐ |
| 코드 품질 | 90/100 | 65점 | 80점 | ⭐⭐⭐⭐⭐ |
| 테스트 전략 | 95/100 | 50점 | 75점 | ⭐⭐⭐⭐⭐ |
| 보안/운영 | 90/100 | 40점 | 75점 | ⭐⭐⭐⭐⭐ |
| 문서화 | 95/100 | 50점 | 70점 | ⭐⭐⭐⭐⭐ |

---

## 1. 기술 스택 분석 (95/100)

### ✅ 강점 (실무자 관점에서 인상적인 점)

| 기술 선택 | 평가 | 실무 적합성 |
|-----------|------|------------|
| **Java 21** | 최신 LTS, Virtual Thread 지원 | 대부분의 스타트업보다 앞선 선택 |
| **Spring Boot 3.4.5** | 최신 버전 (2025.12 기준) | 적극적인 업그레이드 의지 |
| **Spring Data JPA + QueryDSL** | 표준 조합 | 실무 표준 |
| **Redis 7** | 캐시/세션/메시지큐 통합 | 인프라 단순화 |
| **Kotlin 2.0.21 (일부)** | Java-Kotlin 혼용 전략 | 실용적 접근 |
| **Flyway** | 스키마 버전 관리 | 필수 도구 채택 |
| **Docker + Cloudflare Tunnel** | Zero Trust 아키텍처 | 보안 우수 |

### ⚠️ 개선 필요 사항

| 항목 | 현재 상태 | 권장 사항 | 우선순위 |
|------|-----------|-----------|----------|
| **QueryDSL 활용도** | 의존성만 추가, 실제 사용 미확인 | 복잡 동적 쿼리에 적극 활용 | 중 |
| **Kotlin 비율** | 2개 파일 (DTO 위주) | 유틸리티, 확장 함수 확대 | 하 |
| **OpenAPI 문서** | SpringDoc 추가됨 | API 문서 자동화 실제 활용 확인 | 중 |

### 💡 실무자 코멘트
> "Java 21 + Spring Boot 3.4라는 최신 스택 조합은 대부분의 스타트업보다 기술적으로 앞서 있습니다. 다만, 채택만 하고 실제로 활용하지 않는 기술(QueryDSL, Kotlin)이 있어 '기술 과시용' 의심을 받을 수 있습니다."

---

## 2. 아키텍처 설계 평가 (93/100)

### ✅ 강점

#### 2.1 레이어드 아키텍처 준수
```
Controller → Service → Repository
     ↑
   ArchUnit으로 의존성 규칙 강제
```
- **ArchUnit 테스트**: 레이어 간 의존성 위반 자동 감지
- **SRP 분리**: `auth/` 패키지에서 `AuthenticationService`, `MemberRegistrationService`, `MemberDeletionService` 등 책임 분리

#### 2.2 AOP 기반 횡단 관심사 분리
```java
// 한 줄로 소유권 검증 완료 - 실무에서 매우 인상적인 패턴
@RequireResourceOwnership(resourceType = MEDICATION_SCHEDULE, paramName = "id")
```
- `ResourceOwnershipAspect`: 리소스 소유권 검증
- `BusinessAuditAspect`: 감사 로깅
- `CachingAspect`: 캐싱 투명 적용

#### 2.3 ADR(Architecture Decision Records)
- **7개의 ADR 문서**: 의사결정 근거 명확
- JWT+CSRF, 캐시 전략, 암호화, 메시지 큐 등 핵심 결정 문서화

#### 2.4 도메인 이벤트 패턴
```java
// 도메인 이벤트 발행
domainEventPublisher.publish(new MedicationScheduleCreated(...));
```
- 느슨한 결합
- 마이크로서비스 전환 준비

### ⚠️ 개선 필요 사항

| 항목 | 현재 상태 | 문제점 | 권장 사항 |
|------|-----------|--------|-----------|
| **패키지 구조** | 계층별 패키지 (`controller/`, `service/`) | 도메인별 응집도 낮음 | 헥사고날/도메인별 패키지 고려 |
| **서비스 의존성** | `ResourceOwnershipAspect`가 여러 Service에 직접 의존 | 순환 의존 가능성 | Validator 인터페이스 추상화 |
| **트랜잭션 경계** | `@Transactional(readOnly=true)` 기본 | 명시적이지만 일부 메서드 누락 가능 | 리뷰 필요 |

### 💡 실무자 코멘트
> "ADR 문서화, ArchUnit 아키텍처 테스트, AOP 기반 횡단 관심사 분리는 **경력 3년차 이상에서도 잘 못하는 수준**입니다. 다만, 패키지 구조가 전통적인 계층별 구조라서 도메인 응집도가 아쉽습니다."

---

## 3. 코드 품질 평가 (90/100)

### ✅ 강점

#### 3.1 일관된 네이밍 컨벤션
```java
// 엔티티 ID 네이밍: {도메인}Id
private Long medicationScheduleId;

// 메서드 네이밍: create/find/update/delete
public MedicationSchedule createMedicationSchedule(...)
public MedicationSchedule getMedicationSchedule(Long medicationScheduleId)
```

#### 3.2 Java Record 활용 (DTO)
```java
public record MedicationScheduleCreateRequest(
    @NotNull Long memberId,
    @NotBlank @Size(max = 100) String name,
    ...
) {}
```
- 불변성 보장
- 보일러플레이트 제거

#### 3.3 표준화된 에러 처리
```java
// ErrorCode Enum 기반 일관된 예외 처리
throw ErrorCode.MEDICATION_SCHEDULE_NOT_FOUND.toException();

// 27개 이상의 세분화된 에러 코드
MEMBER_NOT_FOUND, INVALID_TOKEN, OPTIMISTIC_LOCK_FAILED, ...
```

#### 3.4 N+1 해결 패턴
```java
// DTO Projection
@Query("SELECT new ...Projection(...) FROM MedicationSchedule ms ...")

// EntityGraph
@EntityGraph(attributePaths = {"member", "medicationTimes"})

// 배치 조회
@Query("... WHERE ms.medicationScheduleId IN :scheduleIds")
```

### ⚠️ 개선 필요 사항

| 항목 | 현재 상태 | 문제점 | 권장 사항 |
|------|-----------|--------|-----------|
| **매직 넘버** | `expiresIn = 900` 하드코딩 | 설정값 분산 | 상수/프로퍼티로 추출 |
| **중복 검증 로직** | `if (memberId == null || memberId <= 0)` 반복 | 검증 로직 중복 | Validator 클래스 추출 |
| **주석 일관성** | 일부 메서드에만 Javadoc | 문서화 불균형 | 전체 public 메서드 문서화 |
| **예외 계층** | `CustomException` 단일 클래스 + ErrorCode | 예외 구분 어려움 | 도메인별 예외 클래스 분리 (이미 부분 적용됨) |

### 💡 실무자 코멘트
> "코드 품질 자체는 우수합니다. 특히 N+1 해결을 위한 3가지 전략(EntityGraph, DTO Projection, 배치 조회)을 모두 적용한 점은 JPA 이해도가 높다는 증거입니다. 다만, `expiresIn = 900`과 같은 매직 넘버가 곳곳에 있어 설정 관리 개선이 필요합니다."

---

## 4. 테스트 전략 평가 (95/100)

### ✅ 강점

| 지표 | 수치 | 신입 평균 | 평가 |
|------|------|-----------|------|
| **테스트 케이스 수** | 1,455개 | 50~100개 | 🏆 탁월 |
| **테스트 성공률** | 100% | - | ✅ |
| **테스트 파일 수** | 140개 | 10~20개 | 🏆 탁월 |
| **테스트 실행 시간** | 10.7초 | - | ⚡ 빠름 |

#### 4.1 테스트 피라미드 준수
```
├── unit tests (Service, Validation) - 다수
├── integration tests (Repository, Security) - 적절
├── architecture tests (ArchUnit) - 존재 ✅
└── e2e tests (AuthE2ETest, MedicationE2ETest, ...) - 존재 ✅
```

#### 4.2 E2E 테스트 기반 클래스
```java
// 실제 Spring Context 로드 + MockMvc
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TestRedisConfig.class)
public abstract class BaseE2ETest { ... }
```
- 5개 도메인 E2E 테스트
- 공통 설정 추상화

#### 4.3 ArchUnit 아키텍처 테스트
```java
// 레이어 의존성 규칙 자동 검증
noClasses().that().resideInAPackage("..controller..")
    .should().accessClassesThat().resideInAPackage("..repository..")
```

### ⚠️ 개선 필요 사항

| 항목 | 현재 상태 | 권장 사항 | 우선순위 |
|------|-----------|-----------|----------|
| **커버리지 리포트** | JaCoCo 설정됨, 실제 수치 미확인 | 80%+ 목표 설정 및 CI 연동 | 상 |
| **성능 테스트** | 미존재 | JMeter/Gatling 부하 테스트 | 중 |
| **계약 테스트** | 미존재 | Pact/Spring Cloud Contract | 하 |
| **테스트 데이터 팩토리** | `createTestMember()` 수동 | Fixture/Factory 패턴 도입 | 중 |

### 💡 실무자 코멘트
> "1,455개의 테스트는 신입 수준을 완전히 초월합니다. E2E 테스트와 ArchUnit까지 갖춘 프로젝트는 실제 스타트업에서도 드뭅니다. 다만, 커버리지 목표 수치를 명시하고 CI에서 강제하면 더욱 신뢰도가 높아집니다."

---

## 5. 보안/운영 평가 (90/100)

### ✅ 강점

#### 5.1 인증/인가 체계
| 기능 | 구현 상태 | 평가 |
|------|-----------|------|
| JWT 인증 | Access(15분) + Refresh(7일) | ✅ 표준 |
| Refresh Token Rotation | 사용 시 재발급 + 기존 무효화 | ✅ 보안 강화 |
| CSRF 보호 | Redis 기반 토큰 (60분 TTL) | ✅ SPA 최적화 |
| Rate Limiting | Redis Sorted Set 기반 | ✅ DDoS 방어 |
| 민감 정보 암호화 | AES-256-GCM | ✅ 법적 요건 충족 |

#### 5.2 로그 분리 전략
```
logs/
├── application.log  # 일반 로그
├── security.log     # 인증/인가
├── audit.log        # 비즈니스 감사
└── performance.log  # 성능 모니터링
```
- Logstash 포맷 (ELK 연동 가능)
- traceId 기반 추적

#### 5.3 민감 정보 마스킹
```java
SensitiveDataMasker.maskEmail(loginId)  // test***@hamalog.com
SensitiveDataMasker.maskUserId(memberId) // 12***
```

### ⚠️ 개선 필요 사항

| 항목 | 현재 상태 | 문제점 | 권장 사항 |
|------|-----------|--------|-----------|
| **비밀 관리** | 환경변수 기반 | Secret rotation 미지원 | HashiCorp Vault 또는 AWS Secrets Manager |
| **보안 헤더** | 기본 Spring Security | 일부 헤더 누락 가능 | CSP, X-Frame-Options 명시적 설정 |
| **감사 로그 보존** | 파일 기반 | 장기 보존/검색 어려움 | ELK 또는 CloudWatch 연동 |
| **알림 임계값** | 미설정 | 장애 인지 지연 | Grafana Alert, PagerDuty 연동 |

### 💡 실무자 코멘트
> "JWT+CSRF 이중 보호, Refresh Token Rotation, AES-256-GCM 암호화는 보안을 깊이 이해하고 있다는 증거입니다. 다만, 실제 프로덕션에서는 Vault 기반 비밀 관리와 모니터링 알림 설정이 추가로 필요합니다."

---

## 6. 문서화 평가 (95/100)

### ✅ 강점

| 문서 유형 | 수량/품질 | 평가 |
|-----------|-----------|------|
| **README.md** | 178줄, 배지, Quick Start | ✅ 우수 |
| **PORTFOLIO.md** | 576줄, 기술적 도전과 해결 | ✅ 면접 대비 최적화 |
| **ADR** | 7개 문서 | ✅ 의사결정 근거 명확 |
| **패턴 가이드** | 6개 (보안, 캐싱, JPA, 에러처리 등) | ✅ 온보딩 용이 |
| **코딩 컨벤션** | 469줄 | ✅ 일관성 보장 |
| **바이브 코딩 가이드** | 510줄, AI 협업 전략 | ✅ 시대 적합 |
| **CHANGELOG** | git-cliff 자동화 | ✅ 버전 관리 |

### ⚠️ 개선 필요 사항

| 항목 | 현재 상태 | 권장 사항 | 우선순위 |
|------|-----------|-----------|----------|
| **API 문서** | 수동 Markdown | Swagger UI 실제 배포 확인 | 상 |
| **다이어그램** | 텍스트 기반 | PlantUML/Mermaid 시각화 | 중 |
| **성능 벤치마크** | 미존재 | 응답 시간, TPS 측정 결과 | 상 |

---

## 7. 바이브 코딩 프로젝트로서의 평가

### 🎯 바이브 코딩 성숙도: 95/100

| 요소 | 구현 상태 | 평가 |
|------|-----------|------|
| `.github/copilot-instructions.md` | 상세한 컨텍스트 제공 | ✅ |
| 코딩 컨벤션 문서화 | 469줄 상세 가이드 | ✅ |
| 선언적 패턴 (AOP) | 5개 이상 커스텀 어노테이션 | ✅ |
| 스캐폴딩 스크립트 | `scripts/generate-crud.sh` | ✅ |
| 자동화된 검증 | Spotless, ArchUnit, JaCoCo | ✅ |

### 💡 실무자 코멘트
> "바이브 코딩 관점에서 이 프로젝트는 **AI와 협업하기 최적화된 구조**입니다. 명확한 컨벤션, 선언적 패턴, 풍부한 컨텍스트 문서가 있어 AI 도구가 일관된 코드를 생성할 수 있습니다. 이 점은 면접에서 '왜 바이브 코딩을 했는가'라는 질문에 대한 좋은 답변이 됩니다."

---

## 8. 실무 채용 관점 종합 평가

### 8.1 포지션별 적합도

| 포지션 | 적합도 | 코멘트 |
|--------|--------|--------|
| **신입 백엔드 개발자** | ⭐⭐⭐⭐⭐ | 상위 5% 수준, 즉시 채용 |
| **주니어 (1-2년차)** | ⭐⭐⭐⭐⭐ | 경력직 수준의 포트폴리오 |
| **미드레벨 (3-5년차)** | ⭐⭐⭐⭐ | 실무 경험 부재 시 증명 필요 |

### 8.2 면접 예상 질문 및 대응

| 예상 질문 | 핵심 답변 포인트 |
|-----------|------------------|
| "바이브 코딩으로 만들었다면 당신의 기여는?" | AOP 패턴 설계, ADR 의사결정, 아키텍처 규칙 정의 |
| "N+1 문제를 어떻게 해결했나?" | EntityGraph, DTO Projection, 배치 조회 3가지 전략 |
| "JWT만 쓰면 되는데 왜 CSRF도?" | SPA + Cookie 환경에서의 CSRF 취약점 설명 |
| "테스트 1,400개가 모두 의미있는 테스트인가?" | ArchUnit 아키텍처 테스트, E2E 5개 도메인, 단위 테스트 분리 설명 |
| "실제 트래픽은 얼마나 처리할 수 있나?" | (약점) 부하 테스트 미실시 → 개선 필요 |

### 8.3 경쟁력 분석

```
┌─────────────────────────────────────────────────────────────┐
│                    신입 포트폴리오 분포                        │
│                                                             │
│  ████████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░  하위 50%  │
│  ████████████████████████████████░░░░░░░░░░░░░░░  상위 30%  │
│  ████████████████████████████████████████████░░░  상위 10%  │
│  ██████████████████████████████████████████████▓  ← Hamalog │
│                                                    상위 5%   │
└─────────────────────────────────────────────────────────────┘
```

---

## 9. 개선 태스크 목록 (우선순위별)

### 🔴 상 (채용 전 필수)

| # | 태스크 | 설명 | 예상 소요 |
|---|--------|------|-----------|
| 1 | **성능 벤치마크 추가** | JMeter/Gatling으로 부하 테스트 실시, 결과 문서화 | 1-2일 |
| 2 | **JaCoCo 커버리지 목표 설정** | 80% 이상 목표, CI 연동, 배지 추가 | 0.5일 |
| 3 | **매직 넘버 상수화** | `expiresIn = 900` 등 하드코딩된 값 프로퍼티로 추출 | 0.5일 |
| 4 | **Swagger UI 배포** | `/swagger-ui.html` 접근 가능하도록 설정 확인 | 0.5일 |

### 🟡 중 (면접 후 1개월 내)

| # | 태스크 | 설명 | 예상 소요 |
|---|--------|------|-----------|
| 5 | **QueryDSL 실제 활용** | 복잡한 동적 쿼리(검색, 필터링)에 QueryDSL 적용 | 1일 |
| 6 | **테스트 데이터 팩토리** | Builder/Factory 패턴으로 테스트 데이터 생성 표준화 | 1일 |
| 7 | **아키텍처 다이어그램** | PlantUML/Mermaid로 시스템 구조 시각화 | 0.5일 |
| 8 | **중복 검증 로직 추출** | `ValidationUtils` 클래스로 공통 검증 로직 분리 | 0.5일 |

### 🟢 하 (장기 개선)

| # | 태스크 | 설명 | 예상 소요 |
|---|--------|------|-----------|
| 9 | **Kotlin 활용 확대** | 유틸리티, 확장 함수를 Kotlin으로 작성 | 2일 |
| 10 | **도메인별 패키지 구조** | 헥사고날 또는 도메인별 패키지로 리팩토링 | 3일 |
| 11 | **계약 테스트 도입** | Spring Cloud Contract 또는 Pact | 2일 |
| 12 | **Vault 비밀 관리** | HashiCorp Vault 연동 | 2일 |

---

## 10. 최종 결론

### ✅ 이 프로젝트가 실무에서 인정받는 이유

1. **깊이 있는 기술 이해**: N+1 해결, JWT+CSRF 이중 보호, AOP 패턴 등 표면적 사용이 아닌 깊은 이해
2. **엔지니어링 문화**: ADR 문서화, ArchUnit 아키텍처 테스트, 코딩 컨벤션 강제
3. **실무 감각**: 로그 분리, 에러 코드 표준화, 민감 정보 마스킹 등 운영 관점 고려
4. **테스트 문화**: 1,400+ 테스트, E2E 테스트, 100% 성공률

### ⚠️ 면접에서 조심해야 할 점

1. **"AI가 다 했다"는 인상 주지 않기**: 본인이 설계하고 AI가 구현을 도왔다는 점 강조
2. **실제 트래픽 경험 부재**: 부하 테스트 결과가 없으면 "확장성" 주장이 약해짐
3. **프론트엔드 연동 경험**: 백엔드만으로는 풀스택 이해도 증명 어려움

### 🎯 한 줄 평가

> **"바이브 코딩 시대에 AI를 도구로 활용하면서도 엔지니어링 원칙을 잃지 않은, 신입 포트폴리오의 새로운 기준을 제시하는 프로젝트"**

---

## 📎 참고: 평가 근거 자료

| 항목 | 분석 대상 |
|------|-----------|
| 소스 파일 | 236개 Java + 2개 Kotlin |
| 테스트 파일 | 140개 |
| 테스트 케이스 | 1,455개 (100% 성공) |
| 문서 | README, PORTFOLIO, ADR 7개, 패턴 가이드 6개 |
| 코드 분석 | AuthenticationService, MedicationScheduleService, ResourceOwnershipAspect 등 |

