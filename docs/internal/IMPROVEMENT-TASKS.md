# 🔧 Hamalog 개선 태스크 체크리스트

> **목적**: 실무 평가에서 발견된 약점을 보완하여 포트폴리오 경쟁력 강화
> 
> **생성일**: 2026-01-14
> 
> **목표**: 채용 면접 전까지 🔴 상 우선순위 태스크 완료

---

## 🔴 상 우선순위 (채용 면접 전 필수)

### 1. 성능 벤치마크 추가
- [x] ~~JMeter 또는~~ Gatling 설치 및 테스트 스크립트 작성
- [x] 주요 API 엔드포인트 부하 테스트 (로그인, 복약 스케줄 조회, 일기 생성)
- [x] 결과 문서화: `docs/internal/PERFORMANCE-BENCHMARK.md`
  - 동시 사용자 수, 평균 응답 시간, TPS, 에러율
  - 병목 지점 분석 및 개선 방안
- [ ] README.md에 성능 지표 배지 추가

**구현 완료 (2026-01-15)**:
- `build.gradle`에 Gatling 3.11.5 플러그인 추가
- `src/gatling/kotlin/com/Hamalog/simulation/` 시뮬레이션 작성
  - `MedicationScheduleSimulation.kt`: N+1 Before/After 비교
  - `AuthenticationSimulation.kt`: 인증 플로우 부하 테스트
- `scripts/benchmark/run-benchmark.sh`: 벤치마크 실행 스크립트
- `scripts/benchmark/load-test-data.sql`: 테스트 데이터 생성 SQL
- Benchmark 전용 API 추가 (`/api/v1/benchmark/*`)

**실행 방법**:
```bash
./scripts/benchmark/run-benchmark.sh
```

**예상 소요**: ~~1-2일~~ 완료

---

### 2. JaCoCo 커버리지 목표 설정 및 CI 연동
- [ ] `build.gradle`에 커버리지 실패 조건 추가
```groovy
jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = 0.80  // 80% 이상
            }
        }
    }
}
```
- [ ] GitHub Actions CI에 커버리지 체크 단계 추가
- [ ] README.md에 커버리지 배지 추가 (Codecov 또는 JaCoCo 배지)
- [ ] 현재 커버리지 수치 확인 및 문서화

**예상 소요**: 0.5일

---

### 3. 매직 넘버 상수화
- [x] 하드코딩된 값 검색 및 목록화
```java
// 개선 완료
// Before: expiresIn = 900;  // 하드코딩
// After: ACCESS_TOKEN_EXPIRY_SECONDS = 900L;  // 상수화
```
- [ ] `application.properties`로 추출할 값 식별
- [x] 설정 클래스 또는 상수 클래스 생성
```java
// AuthenticationService.java에 상수 추가 완료
private static final long ACCESS_TOKEN_EXPIRY_SECONDS = 900L;
```
- [x] 관련 코드 리팩토링
- [ ] 테스트 통과 확인

**완료 항목 (2026-01-15)**:
- `AuthenticationService.java`: `expiresIn = 900` → `ACCESS_TOKEN_EXPIRY_SECONDS` 상수화

**대상 파일**:
- ~~`AuthenticationService.java`: `expiresIn = 900`~~ ✅ 완료
- 기타 검색: `grep -r "= [0-9]\{2,\}" src/main/java`

**예상 소요**: ~~0.5일~~ 진행 중

---

### 4. Swagger UI 배포 확인
- [ ] 로컬에서 `/swagger-ui/index.html` 접근 테스트
- [ ] OpenAPI 문서 자동 생성 확인 (`/v3/api-docs`)
- [ ] 인증 필요 엔드포인트에 대한 Swagger 설정
```java
@SecurityRequirement(name = "bearerAuth")
@Operation(summary = "복약 스케줄 조회")
```
- [ ] README.md에 API 문서 링크 추가
- [ ] 프로덕션 배포 시 Swagger 접근 설정 검토 (보안)

**예상 소요**: 0.5일

---

## 🟡 중 우선순위 (면접 후 1개월 내)

### 5. QueryDSL 실제 활용
- [ ] 복잡한 검색/필터링이 필요한 API 식별
  - 복약 스케줄 검색 (이름, 기간, 상태 필터)
  - 마음 일기 검색 (기분 타입, 날짜 범위)
- [ ] QueryDSL Repository 구현
```java
public interface MedicationScheduleRepositoryCustom {
    Page<MedicationSchedule> searchByConditions(SearchCondition condition, Pageable pageable);
}
```
- [ ] Q클래스 생성 확인 (`./gradlew compileJava`)
- [ ] 기존 JPQL 쿼리 중 복잡한 것 QueryDSL로 마이그레이션
- [ ] 테스트 코드 작성

**예상 소요**: 1일

---

### 6. 테스트 데이터 팩토리 패턴 도입
- [ ] Fixture/Factory 패턴 선택
```java
// 권장: Builder 패턴 기반 Factory
public class MemberFixture {
    public static Member.MemberBuilder defaultMember() {
        return Member.builder()
            .loginId("test@hamalog.com")
            .password("encoded-password")
            .name("테스트유저");
    }
    
    public static Member withLoginId(String loginId) {
        return defaultMember().loginId(loginId).build();
    }
}
```
- [ ] 주요 도메인별 Fixture 클래스 생성
  - `MemberFixture`
  - `MedicationScheduleFixture`
  - `MoodDiaryFixture`
- [ ] 기존 테스트 코드 리팩토링
- [ ] `BaseE2ETest.createTestMember()` 개선

**예상 소요**: 1일

---

### 7. 아키텍처 다이어그램 시각화
- [ ] Mermaid 문법으로 시스템 아키텍처 작성
```markdown
## 시스템 아키텍처

​```mermaid
graph TB
    Client[SPA Client] --> CF[Cloudflare WAF]
    CF --> Tunnel[Cloudflare Tunnel]
    Tunnel --> Nginx
    Nginx --> App[Spring Boot]
    App --> MySQL[(MySQL 8.0)]
    App --> Redis[(Redis 7)]
​```
```
- [ ] 레이어드 아키텍처 다이어그램
- [ ] 인증 플로우 시퀀스 다이어그램
- [ ] `docs/internal/ARCHITECTURE-DIAGRAMS.md` 생성
- [ ] README.md에 다이어그램 추가

**예상 소요**: 0.5일

---

### 8. 중복 검증 로직 추출
- [ ] 반복되는 검증 패턴 식별
```java
// 중복 패턴 예시
if (memberId == null || memberId <= 0) {
    throw new InvalidInputException(ErrorCode.INVALID_PARAMETER);
}
```
- [ ] `ValidationUtils` 유틸리티 클래스 생성
```java
public final class ValidationUtils {
    public static void requirePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new InvalidInputException(ErrorCode.INVALID_PARAMETER, fieldName);
        }
    }
}
```
- [ ] 기존 서비스 코드 리팩토링
- [ ] 테스트 코드 작성

**예상 소요**: 0.5일

---

## 🟢 하 우선순위 (장기 개선)

### 9. Kotlin 활용 확대
- [ ] 유틸리티 함수를 Kotlin 확장 함수로 작성
```kotlin
// src/main/kotlin/com/Hamalog/util/StringExtensions.kt
fun String.maskEmail(): String = 
    this.replace(Regex("(?<=.{3}).(?=.*@)"), "*")
```
- [ ] 새로운 DTO는 Kotlin data class로 작성
- [ ] 기존 Java 유틸리티 클래스 마이그레이션 검토

**예상 소요**: 2일

---

### 10. 도메인별 패키지 구조 검토
- [ ] 현재 구조 vs 도메인별 구조 장단점 분석
```
# 현재: 레이어별
src/main/java/com/Hamalog/
├── controller/medication/
├── service/medication/
├── repository/medication/
└── domain/medication/

# 대안: 도메인별
src/main/java/com/Hamalog/
└── medication/
    ├── controller/
    ├── service/
    ├── repository/
    └── domain/
```
- [ ] 마이그레이션 계획 수립 (점진적 전환)
- [ ] ADR 문서 작성: "패키지 구조 결정"

**예상 소요**: 3일 (계획 + 마이그레이션)

---

### 11. 계약 테스트 도입
- [ ] Spring Cloud Contract 또는 Pact 선택
- [ ] Consumer/Provider 계약 정의
- [ ] CI 파이프라인 연동

**예상 소요**: 2일

---

### 12. Vault 비밀 관리 연동
- [ ] HashiCorp Vault 또는 AWS Secrets Manager 선택
- [ ] Spring Cloud Vault 의존성 추가
- [ ] 기존 환경변수 기반 비밀을 Vault로 마이그레이션
- [ ] 로컬/개발/프로덕션 환경 분리

**예상 소요**: 2일

---

## 📊 진행 상황 트래킹

| 우선순위 | 전체 | 완료 | 진행률 |
|----------|------|------|--------|
| 🔴 상 | 4 | 1 | 25% |
| 🟡 중 | 4 | 0 | 0% |
| 🟢 하 | 4 | 0 | 0% |
| **합계** | **12** | **1** | **8%** |

---

## 📝 완료 기록

| 날짜 | 태스크 # | 태스크명 | 비고 |
|------|----------|----------|------|
| 2026-01-15 | 1 | 성능 벤치마크 추가 | Gatling 3.11.5, Before/After 비교 구현 |
| 2026-01-15 | 3 | 매직 넘버 상수화 | AuthenticationService 상수화 (진행 중) |

---

## 🔗 관련 문서

- [실무 평가 리포트](./PRACTICAL-EVALUATION-REPORT.md)
- [코딩 컨벤션](./CODING-CONVENTIONS.md)
- [바이브 코딩 가이드](../ai/VIBE-CODING-GUIDE.md)

