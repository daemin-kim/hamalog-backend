# 🔍 Hamalog 프로젝트 전체 점검 가이드

> 📌 **목적**: 이 문서는 Hamalog 프로젝트의 코드, 보안, API, 트랜잭션, 문서 등 모든 영역을 체계적으로 점검하기 위한 종합 체크리스트입니다.
> 
> 📌 **대상**: AI 코드 리뷰, 실무 프로젝트 점검, 기술 면접 준비, 포트폴리오 검증
> 
> 📌 **작성일**: 2026-01-20
> 
> 📌 **버전**: 1.0.0

---

## 📋 목차

### 핵심 점검 영역
1. [Part 1: 코드 품질 및 아키텍처 점검](#part-1-코드-품질-및-아키텍처-점검)
2. [Part 2: 인증/인가 및 보안 점검](#part-2-인증인가-및-보안-점검)
3. [Part 3: API 구현 상태 점검](#part-3-api-구현-상태-점검)
4. [Part 4: 데이터베이스 및 JPA 점검](#part-4-데이터베이스-및-jpa-점검)
5. [Part 5: 트랜잭션 관리 점검](#part-5-트랜잭션-관리-점검)

### 성능 및 인프라 점검
6. [Part 6: 캐싱 및 성능 점검](#part-6-캐싱-및-성능-점검)
7. [Part 7: 메시지 큐 및 비동기 처리 점검](#part-7-메시지-큐-및-비동기-처리-점검)

### 품질 보증 및 문서화
8. [Part 8: 테스트 커버리지 점검](#part-8-테스트-커버리지-점검)
9. [Part 9: 문서화 점검](#part-9-문서화-점검)
10. [Part 10: 인프라 및 배포 점검](#part-10-인프라-및-배포-점검)

### 부록
- [Appendix A: 자동화 스크립트 및 명령어](#appendix-a-자동화-스크립트-및-명령어)
- [Appendix B: 점검 결과 템플릿](#appendix-b-점검-결과-템플릿)
- [Appendix C: 우선순위별 점검 순서](#appendix-c-우선순위별-점검-순서)

---

## 🎯 점검 우선순위

| 우선순위 | 영역 | 중요도 | 이유 |
|:--------:|------|:------:|------|
| 🔴 1순위 | **보안 (Part 2)** | Critical | 인증/인가 취약점은 즉시 악용 가능 |
| 🔴 2순위 | **API (Part 3)** | Critical | 사용자 경험 직접 영향 |
| 🟠 3순위 | **트랜잭션 (Part 5)** | High | 데이터 정합성 보장 |
| 🟠 4순위 | **JPA/DB (Part 4)** | High | 성능 및 데이터 무결성 |
| 🟡 5순위 | **테스트 (Part 8)** | Medium | 회귀 방지, 안정성 |
| 🟡 6순위 | **아키텍처 (Part 1)** | Medium | 유지보수성 |
| 🟢 7순위 | **캐싱/성능 (Part 6)** | Low-Med | 응답 속도 개선 |
| 🟢 8순위 | **메시지 큐 (Part 7)** | Low-Med | 비동기 처리 안정성 |
| ⚪ 9순위 | **문서화 (Part 9)** | Low | 팀 협업 효율 |
| ⚪ 10순위 | **인프라 (Part 10)** | Low | 배포 자동화 |

---

# Part 1: 코드 품질 및 아키텍처 점검

> 🎯 **목표**: 프로젝트 구조, 네이밍 컨벤션, 레이어 분리, Entity/DTO 설계가 올바르게 되어 있는지 점검

---

## 1.1 패키지 구조 점검

### 1.1.1 표준 패키지 구조

Hamalog 프로젝트는 **도메인 기반 패키지 구조**를 따릅니다.

```
src/main/java/com/Hamalog/
├── aop/                          # AOP (Aspect)
│   ├── BusinessAuditAspect.java
│   └── CachingAspect.java
├── config/                       # 설정 클래스
│   ├── security/                 # 보안 설정
│   ├── benchmark/               # 벤치마크 설정 (개발용)
│   ├── SecurityConfig.java
│   ├── RedisConfig.java
│   └── ...
├── controller/                   # 컨트롤러 (도메인별)
│   ├── auth/
│   ├── medication/
│   ├── diary/
│   ├── sideEffect/
│   ├── notification/
│   └── export/
├── domain/                       # Entity (도메인별)
│   ├── member/
│   ├── medication/
│   ├── diary/
│   ├── sideEffect/
│   ├── notification/
│   └── events/                  # 도메인 이벤트
├── dto/                          # DTO (도메인별)
│   ├── {도메인}/
│   │   ├── request/
│   │   └── response/
│   └── projection/              # JPA Projection DTO
├── exception/                    # 예외 클래스
│   ├── ErrorCode.java
│   ├── CustomException.java
│   └── {도메인}/                # 도메인별 예외 (선택)
├── handler/                      # 전역 핸들러
│   └── GlobalExceptionHandler.java
├── logging/                      # 로깅 유틸리티
├── repository/                   # Repository (도메인별)
│   ├── {도메인}/
│   └── querydsl/                # QueryDSL 커스텀 구현
├── security/                     # 보안 컴포넌트
│   ├── annotation/              # 커스텀 어노테이션
│   ├── aspect/                  # 보안 AOP
│   ├── filter/                  # Security Filter
│   ├── jwt/                     # JWT 처리
│   ├── oauth2/                  # OAuth2 처리
│   ├── csrf/                    # CSRF 처리
│   └── encryption/              # 암호화 유틸
├── service/                      # 서비스 (도메인별)
│   ├── {도메인}/
│   ├── alert/                   # Discord 알림
│   ├── queue/                   # Redis Stream 메시지 큐
│   └── events/                  # 이벤트 처리
└── validation/                   # 커스텀 Validator
```

### 1.1.2 패키지 구조 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 비고 |
|-----------|:----:|----------|------|
| Controller는 `/controller/{도메인}/` 하위에 위치 | ☐ | 디렉토리 확인 | |
| Service는 `/service/{도메인}/` 하위에 위치 | ☐ | 디렉토리 확인 | |
| Repository는 `/repository/{도메인}/` 하위에 위치 | ☐ | 디렉토리 확인 | |
| Entity는 `/domain/{도메인}/` 하위에 위치 | ☐ | 디렉토리 확인 | Java로 작성 |
| DTO Request는 `/dto/{도메인}/request/` 하위에 위치 | ☐ | 디렉토리 확인 | Java record 또는 Kotlin data class |
| DTO Response는 `/dto/{도메인}/response/` 하위에 위치 | ☐ | 디렉토리 확인 | |
| 도메인별 패키지가 일관되게 분리됨 | ☐ | 전체 구조 확인 | |
| config 패키지에 비즈니스 로직 없음 | ☐ | 코드 리뷰 | |
| util 패키지가 적절히 분리됨 | ☐ | 디렉토리 확인 | Kotlin 권장 |

### 1.1.3 자동 점검 스크립트

```bash
# 패키지 구조 검증
find src/main/java/com/Hamalog -type d | sort

# Controller 위치 확인
find src/main/java -name "*Controller.java" | grep -v "/controller/"

# Service 위치 확인
find src/main/java -name "*Service.java" | grep -v "/service/"

# Repository 위치 확인
find src/main/java -name "*Repository.java" | grep -v "/repository/"
```

---

## 1.2 네이밍 컨벤션 점검

### 1.2.1 클래스 네이밍 규칙

| 유형 | 패턴 | 올바른 예시 | 잘못된 예시 |
|------|------|------------|-------------|
| **Entity** | `{도메인명}` (단수형) | `MedicationSchedule` | `MedicationSchedules`, `MedicationScheduleEntity` |
| **Controller** | `{도메인}Controller` | `MedicationScheduleController` | `MedicationScheduleCtrl`, `MedicationController` |
| **Service** | `{도메인}Service` | `MedicationScheduleService` | `MedicationScheduleSvc`, `MedScheduleService` |
| **Repository** | `{도메인}Repository` | `MedicationScheduleRepository` | `MedicationScheduleRepo`, `MedRepo` |
| **DTO Request** | `{동작}{도메인}Request` | `MedicationScheduleCreateRequest` | `CreateMedicationScheduleRequest`, `MedReq` |
| **DTO Response** | `{도메인}Response` | `MedicationScheduleResponse` | `MedicationScheduleDTO`, `MedRes` |
| **Exception** | `{예외명}Exception` | `ResourceNotFoundException` | `ResourceNotFound`, `NotFoundException` |
| **Enum** | PascalCase (단수형) | `AlarmType`, `MoodType` | `AlarmTypes`, `ALARM_TYPE` |

### 1.2.2 메서드 네이밍 규칙

| 동작 | 접두사 | 올바른 예시 | 잘못된 예시 |
|------|--------|------------|-------------|
| **생성** | `create`, `register` | `createSchedule()` | `makeSchedule()`, `newSchedule()` |
| **단건 조회** | `findById`, `getById` | `findById(Long id)` | `get(Long id)`, `selectById()` |
| **목록 조회** | `findAll`, `findBy*`, `getList` | `findAllByMemberId()` | `selectAll()`, `list()` |
| **수정** | `update`, `modify` | `updateSchedule()` | `editSchedule()`, `changeSchedule()` |
| **삭제** | `delete`, `remove` | `deleteById()` | `removeById()`, `erase()` |
| **검증** | `validate`, `check`, `verify` | `validateOwnership()` | `isOwner()` (boolean 반환 시 허용) |
| **변환** | `from`, `to`, `convert` | `Response.from(entity)` | `toResponse()` (엔티티 내부 시) |
| **존재 확인** | `exists`, `isPresent` | `existsByLoginId()` | `hasLoginId()` |

### 1.2.3 변수 네이밍 규칙

```java
// ✅ Good - 명확한 의미
Long memberId;
String medicationName;
boolean isActive;
boolean hasPermission;
LocalDateTime createdAt;
List<MedicationSchedule> schedules;

// ❌ Bad - 불명확하거나 약어
Long id;           // 어떤 ID인지 불명확
String n;          // 약어
boolean active;    // is 접두사 없음 (boolean)
LocalDateTime dt;  // 약어
List<MedicationSchedule> list;  // 타입만 명시
```

### 1.2.4 네이밍 컨벤션 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 |
|-----------|:----:|----------|
| Entity 클래스명이 단수형 PascalCase | ☐ | `grep -r "public class.*Entity"` |
| Controller 클래스명이 `*Controller`로 끝남 | ☐ | `find -name "*Controller.java"` |
| Service 클래스명이 `*Service`로 끝남 | ☐ | `find -name "*Service.java"` |
| Repository 인터페이스명이 `*Repository`로 끝남 | ☐ | `find -name "*Repository.java"` |
| Request DTO가 `*Request`로 끝남 | ☐ | `find -name "*Request.java"` |
| Response DTO가 `*Response`로 끝남 | ☐ | `find -name "*Response.java"` |
| Boolean 변수/필드가 `is*`, `has*`, `can*`으로 시작 | ☐ | 코드 리뷰 |
| 메서드명이 동작을 명확히 표현 | ☐ | 코드 리뷰 |
| 약어 사용 최소화 | ☐ | 코드 리뷰 |

### 1.2.5 자동 점검 스크립트

```bash
# Controller 네이밍 확인
find src/main/java -name "*.java" -exec grep -l "@RestController" {} \; | \
  xargs -I {} basename {} | grep -v "Controller.java$"

# Service 네이밍 확인  
find src/main/java -name "*.java" -exec grep -l "@Service" {} \; | \
  xargs -I {} basename {} | grep -v "Service.java$"

# Boolean 필드 is 접두사 확인
grep -rn "private boolean [^i]" src/main/java --include="*.java"
```

---

## 1.3 레이어 분리 점검

### 1.3.1 레이어별 책임

```
┌─────────────────────────────────────────────────────────────────┐
│                         Controller Layer                         │
│  - HTTP 요청/응답 처리                                           │
│  - 요청 데이터 검증 (@Valid)                                     │
│  - 응답 HTTP 상태 코드 결정                                      │
│  - ❌ 비즈니스 로직 금지                                         │
└─────────────────────────────────────────────────────────────────┘
                                 ↓
┌─────────────────────────────────────────────────────────────────┐
│                          Service Layer                           │
│  - 비즈니스 로직 처리                                            │
│  - 트랜잭션 관리 (@Transactional)                               │
│  - 도메인 객체 조합 및 조율                                      │
│  - Entity ↔ DTO 변환                                            │
│  - 다른 서비스 호출 허용                                         │
└─────────────────────────────────────────────────────────────────┘
                                 ↓
┌─────────────────────────────────────────────────────────────────┐
│                        Repository Layer                          │
│  - 데이터 접근 (CRUD)                                            │
│  - JPA 쿼리 메서드                                               │
│  - @EntityGraph, @Query                                          │
│  - ❌ 직접 SQL 최소화 (JPA 쿼리 메서드 우선)                     │
└─────────────────────────────────────────────────────────────────┘
                                 ↓
┌─────────────────────────────────────────────────────────────────┐
│                          Domain Layer                            │
│  - Entity 클래스                                                 │
│  - 도메인 로직 (상태 변경 메서드)                                │
│  - 도메인 이벤트                                                 │
│  - ❌ 외부 의존성 금지                                           │
└─────────────────────────────────────────────────────────────────┘
```

### 1.3.2 레이어 분리 위반 패턴

#### ❌ Controller에 비즈니스 로직

```java
// ❌ Bad - Controller에서 비즈니스 로직 처리
@PostMapping
public ResponseEntity<MedicationScheduleResponse> create(
    @RequestBody MedicationScheduleRequest request
) {
    // Controller에서 직접 Repository 호출 ❌
    Member member = memberRepository.findById(request.getMemberId())
        .orElseThrow(() -> new RuntimeException("Member not found"));
    
    // Controller에서 Entity 생성 ❌
    MedicationSchedule schedule = new MedicationSchedule(
        request.getName(),
        member
    );
    
    // Controller에서 직접 저장 ❌
    MedicationSchedule saved = medicationScheduleRepository.save(schedule);
    
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(MedicationScheduleResponse.from(saved));
}

// ✅ Good - Controller는 Service에 위임
@PostMapping
public ResponseEntity<MedicationScheduleResponse> create(
    @Valid @RequestBody MedicationScheduleRequest request
) {
    MedicationScheduleResponse response = medicationScheduleService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

#### ❌ Service에서 HttpServletRequest 의존

```java
// ❌ Bad - Service에서 HTTP 관련 객체 의존
@Service
public class MemberService {
    public void updateProfile(HttpServletRequest request) {  // ❌
        String ipAddress = request.getRemoteAddr();
        // ...
    }
}

// ✅ Good - 필요한 값만 파라미터로 전달
@Service
public class MemberService {
    public void updateProfile(UpdateProfileRequest request, String ipAddress) {
        // ...
    }
}
```

#### ❌ Repository에서 비즈니스 로직

```java
// ❌ Bad - Repository에서 비즈니스 로직 처리
public interface MedicationScheduleRepository extends JpaRepository<...> {
    
    // Repository에서 복잡한 비즈니스 계산 ❌
    @Query("SELECT COUNT(mr) * 100.0 / :totalDays FROM MedicationRecord mr " +
           "WHERE mr.medicationSchedule.member.memberId = :memberId " +
           "AND mr.isTakeMedication = true " +
           "AND mr.realTakeTime BETWEEN :startDate AND :endDate")
    Double calculateAdherenceRate(...);  // 이런 계산은 Service에서
}

// ✅ Good - Repository는 단순 데이터 조회
public interface MedicationScheduleRepository extends JpaRepository<...> {
    
    @Query("SELECT mr FROM MedicationRecord mr WHERE ...")
    List<MedicationRecord> findRecordsByMemberAndDateRange(...);
}

// Service에서 계산
@Service
public class MedicationStatsService {
    public double calculateAdherenceRate(...) {
        List<MedicationRecord> records = repository.findRecordsByMemberAndDateRange(...);
        // 비즈니스 로직: 이행률 계산
        return (double) takenCount / totalCount * 100;
    }
}
```

### 1.3.3 레이어 분리 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| Controller에서 Repository 직접 주입 없음 | ☐ | `grep "@Autowired.*Repository" *Controller.java` | 🔴 High |
| Controller에 `@Transactional` 없음 | ☐ | `grep "@Transactional" *Controller.java` | 🔴 High |
| Controller에서 Entity 직접 반환 안 함 | ☐ | 코드 리뷰 | 🟠 Medium |
| Service에서 HttpServletRequest 의존 없음 | ☐ | `grep "HttpServletRequest" *Service.java` | 🟠 Medium |
| Repository에 비즈니스 로직 없음 | ☐ | 코드 리뷰 | 🟡 Low |
| Entity에 외부 서비스 의존성 없음 | ☐ | Entity 클래스 @Autowired 확인 | 🔴 High |

### 1.3.4 자동 점검 스크립트

```bash
# Controller에서 Repository 직접 사용 확인
grep -rn "Repository" src/main/java/com/Hamalog/controller --include="*.java" | \
  grep -v "import"

# Controller에서 @Transactional 사용 확인
grep -rn "@Transactional" src/main/java/com/Hamalog/controller --include="*.java"

# Service에서 HTTP 관련 객체 의존 확인
grep -rn "HttpServletRequest\|HttpServletResponse\|HttpSession" \
  src/main/java/com/Hamalog/service --include="*.java" | grep -v "import"
```

---

## 1.4 Entity 설계 점검

### 1.4.1 Entity 작성 규칙

Hamalog 프로젝트에서 Entity는 반드시 **Java + Lombok**으로 작성합니다.

```java
@Entity
@Table(name = "medication_schedule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA 프록시용
public class MedicationSchedule {
    
    // 1. ID 필드
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long medicationScheduleId;
    
    // 2. 일반 필드 (Not Null 명시)
    @Column(nullable = false, length = 100)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlarmType alarmType;
    
    // 3. 연관관계 필드 (반드시 LAZY)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
    
    @OneToMany(mappedBy = "medicationSchedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MedicationTime> medicationTimes = new ArrayList<>();
    
    // 4. 낙관적 락 (동시성 제어)
    @Version
    private Long version;
    
    // 5. Audit 필드 (선택)
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    // 6. 비즈니스 생성자 (필수 필드만)
    public MedicationSchedule(String name, Member member, AlarmType alarmType) {
        this.name = name;
        this.member = member;
        this.alarmType = alarmType;
    }
    
    // 7. 도메인 메서드 (상태 변경)
    public void updateName(String name) {
        this.name = name;
    }
    
    public void addMedicationTime(MedicationTime time) {
        this.medicationTimes.add(time);
        time.setMedicationSchedule(this);  // 양방향 연관관계 설정
    }
}
```

### 1.4.2 Entity 금지 사항

| 금지 항목 | 이유 | 대안 |
|-----------|------|------|
| ❌ `@Data` 사용 | equals/hashCode 자동 생성으로 무한 루프 가능 | `@Getter`, `@NoArgsConstructor` |
| ❌ `FetchType.EAGER` | N+1 문제, 불필요한 조인 | `FetchType.LAZY` + `@EntityGraph` |
| ❌ `@Setter` 전체 적용 | 캡슐화 위반, 무분별한 상태 변경 | 도메인 메서드로 상태 변경 |
| ❌ Kotlin으로 작성 | JPA 프록시 호환성 문제 | Java + Lombok 유지 |
| ❌ public 기본 생성자 | 무분별한 객체 생성 방지 | `AccessLevel.PROTECTED` |
| ❌ 비즈니스 로직에 외부 서비스 주입 | 도메인 순수성 유지 | 서비스 레이어에서 처리 |

### 1.4.3 연관관계 설계 규칙

```java
// ✅ Good - 연관관계 설정
@Entity
public class MedicationSchedule {
    
    // 다대일: 항상 LAZY
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;
    
    // 일대다: mappedBy로 연관관계 주인 명시
    @OneToMany(mappedBy = "medicationSchedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MedicationTime> medicationTimes = new ArrayList<>();
    
    // 일대일: 주 테이블에 FK
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_settings_id")
    private NotificationSettings notificationSettings;
}

// ❌ Bad - 양방향 연관관계 무분별 사용
@Entity
public class Member {
    // 불필요한 양방향 - Member에서 모든 Schedule 조회 필요 없음
    @OneToMany(mappedBy = "member")
    private List<MedicationSchedule> schedules;  // 제거 권장
}
```

### 1.4.4 Entity 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| `@Data` 어노테이션 사용 안 함 | ☐ | `grep "@Data" src/main/java/com/Hamalog/domain` |
| `FetchType.EAGER` 사용 안 함 | ☐ | `grep "FetchType.EAGER" src/main/java` |
| 모든 `@ManyToOne`이 `FetchType.LAZY` | ☐ | `grep "@ManyToOne" -A2` | 🔴 High |
| 기본 생성자가 `protected` | ☐ | `grep "NoArgsConstructor" -A1` | 🟠 Medium |
| `@Version` 필드 존재 (동시성 제어) | ☐ | `grep "@Version"` | 🟠 Medium |
| 양방향 연관관계 최소화 | ☐ | 코드 리뷰 | 🟡 Low |
| Entity가 Java로 작성됨 | ☐ | `find domain -name "*.kt"` | 🔴 High |
| `@Column` nullable, length 명시 | ☐ | 코드 리뷰 | 🟡 Low |

### 1.4.5 자동 점검 스크립트

```bash
# @Data 사용 확인 (Entity)
grep -rn "@Data" src/main/java/com/Hamalog/domain --include="*.java"

# FetchType.EAGER 사용 확인
grep -rn "FetchType.EAGER" src/main/java --include="*.java"

# @ManyToOne에서 LAZY 누락 확인
grep -rn "@ManyToOne" src/main/java/com/Hamalog/domain --include="*.java" -A2 | \
  grep -v "FetchType.LAZY"

# Kotlin Entity 확인 (금지)
find src/main/java/com/Hamalog/domain -name "*.kt"

# @Version 필드 확인
for entity in $(find src/main/java/com/Hamalog/domain -name "*.java" -exec grep -l "@Entity" {} \;); do
  if ! grep -q "@Version" "$entity"; then
    echo "Missing @Version: $entity"
  fi
done
```

---

## 1.5 DTO 설계 점검

### 1.5.1 DTO 작성 규칙

Hamalog 프로젝트에서 DTO는 **Java record** 또는 **Kotlin data class**로 작성합니다.

#### Java Record DTO

```java
// Request DTO - Bean Validation 적용
public record MedicationScheduleCreateRequest(
    @NotNull(message = "회원 ID는 필수입니다")
    Long memberId,
    
    @NotBlank(message = "약 이름은 필수입니다")
    @Size(max = 100, message = "약 이름은 100자 이하여야 합니다")
    String name,
    
    @Size(max = 100)
    String hospitalName,
    
    LocalDate prescriptionDate,
    
    @Size(max = 500)
    String memo,
    
    @NotNull
    LocalDate startOfAd,
    
    @Min(value = 1, message = "처방 일수는 1일 이상이어야 합니다")
    Integer prescriptionDays,
    
    @Min(value = 1, message = "1일 복용 횟수는 1회 이상이어야 합니다")
    Integer perDay,
    
    @NotNull
    AlarmType alarmType
) {}

// Response DTO - 정적 팩토리 메서드
public record MedicationScheduleResponse(
    Long medicationScheduleId,
    Long memberId,
    String name,
    String hospitalName,
    LocalDate prescriptionDate,
    String memo,
    LocalDate startOfAd,
    Integer prescriptionDays,
    Integer perDay,
    AlarmType alarmType,
    Boolean isActive,
    LocalDateTime createdAt
) {
    // Entity → DTO 변환
    public static MedicationScheduleResponse from(MedicationSchedule entity) {
        return new MedicationScheduleResponse(
            entity.getMedicationScheduleId(),
            entity.getMember().getMemberId(),
            entity.getName(),
            entity.getHospitalName(),
            entity.getPrescriptionDate(),
            entity.getMemo(),
            entity.getStartOfAd(),
            entity.getPrescriptionDays(),
            entity.getPerDay(),
            entity.getAlarmType(),
            entity.getIsActive(),
            entity.getCreatedAt()
        );
    }
}
```

#### Kotlin Data Class DTO (권장)

```kotlin
// Request DTO
data class MedicationScheduleCreateRequest(
    @field:NotNull(message = "회원 ID는 필수입니다")
    val memberId: Long,
    
    @field:NotBlank(message = "약 이름은 필수입니다")
    @field:Size(max = 100, message = "약 이름은 100자 이하여야 합니다")
    val name: String,
    
    @field:Size(max = 100, message = "병원명은 100자 이하여야 합니다")
    val hospitalName: String? = null,
    
    val prescriptionDate: LocalDate? = null,
    
    @field:Size(max = 500)
    val memo: String? = null,
    
    @field:NotNull
    val startOfAd: LocalDate,
    
    @field:Min(value = 1, message = "처방 일수는 1일 이상이어야 합니다")
    val prescriptionDays: Int,
    
    @field:Min(value = 1, message = "1일 복용 횟수는 1회 이상이어야 합니다")
    val perDay: Int,
    
    @field:NotNull
    val alarmType: AlarmType
)

// Response DTO
data class MedicationScheduleResponse(
    val medicationScheduleId: Long,
    val memberId: Long,
    val name: String,
    val hospitalName: String?,
    val prescriptionDate: LocalDate?,
    val memo: String?,
    val startOfAd: LocalDate,
    val prescriptionDays: Int,
    val perDay: Int,
    val alarmType: AlarmType,
    val isActive: Boolean,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(entity: MedicationSchedule) = MedicationScheduleResponse(
            medicationScheduleId = entity.medicationScheduleId,
            memberId = entity.member.memberId,
            name = entity.name,
            hospitalName = entity.hospitalName,
            memo = entity.memo,
            startOfAd = entity.startOfAd,
            prescriptionDays = entity.prescriptionDays,
            perDay = entity.perDay,
            alarmType = entity.alarmType,
            isActive = entity.isActive,
            createdAt = entity.createdAt
        )
    }
}
```

### 1.5.2 DTO 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| Request DTO에 `@Valid` 어노테이션 사용 | ☐ | Controller 파라미터 확인 | 🔴 High |
| Request DTO에 Bean Validation 적용 | ☐ | `@NotNull`, `@NotBlank` 등 확인 | 🔴 High |
| Response DTO에 `from()` 정적 팩토리 메서드 존재 | ☐ | 코드 리뷰 | 🟠 Medium |
| DTO가 record 또는 data class로 작성됨 | ☐ | 클래스 선언 확인 | 🟡 Low |
| DTO에 비즈니스 로직 없음 | ☐ | 메서드 확인 | 🟠 Medium |
| 민감 정보가 Response에 노출되지 않음 | ☐ | 비밀번호, 토큰 등 확인 | 🔴 High |
| DTO 필드명이 API 명세와 일치 | ☐ | API 명세서 대조 | 🟠 Medium |

### 1.5.3 자동 점검 스크립트

```bash
# Request DTO에 @Valid 적용 확인 (Controller)
grep -rn "@RequestBody" src/main/java/com/Hamalog/controller --include="*.java" | \
  grep -v "@Valid"

# Response DTO에 from() 메서드 확인
for dto in $(find src/main/java/com/Hamalog/dto -name "*Response.java"); do
  if ! grep -q "public static.*from" "$dto"; then
    echo "Missing from() method: $dto"
  fi
done

# 민감 정보 노출 확인
grep -rn "password\|secret\|token" src/main/java/com/Hamalog/dto/**/response \
  --include="*.java" --include="*.kt"
```

---

## 1.6 코드 포맷팅 및 스타일

### 1.6.1 Spotless 설정 확인

```groovy
// build.gradle
spotless {
    java {
        target 'src/main/java/**/*.java', 'src/test/java/**/*.java'
        googleJavaFormat('1.18.1')
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlin {
        target 'src/main/kotlin/**/*.kt', 'src/test/kotlin/**/*.kt'
        ktlint('1.0.1')
        trimTrailingWhitespace()
        endWithNewline()
    }
}
```

### 1.6.2 코드 스타일 점검

```bash
# Spotless 검사 실행
./gradlew spotlessCheck

# Spotless 자동 포맷팅
./gradlew spotlessApply

# 사용하지 않는 import 확인
./gradlew checkstyleMain  # (checkstyle 설정 시)
```

### 1.6.3 코드 스타일 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 |
|-----------|:----:|----------|
| Spotless 검사 통과 | ☐ | `./gradlew spotlessCheck` |
| 사용하지 않는 import 없음 | ☐ | IDE 경고 확인 |
| 일관된 들여쓰기 (4 spaces) | ☐ | 설정 확인 |
| 한 줄 최대 120자 | ☐ | 설정 확인 |
| 파일 끝 빈 줄 | ☐ | Spotless 자동 처리 |

---

## 1.7 Part 1 종합 점검 체크리스트

### 1.7.1 Quick Check (5분 점검)

```bash
#!/bin/bash
# Part 1 Quick Check Script

echo "=== Part 1: 코드 품질 및 아키텍처 Quick Check ==="

echo -e "\n[1] @Data 사용 확인 (Entity)"
grep -rn "@Data" src/main/java/com/Hamalog/domain --include="*.java" && echo "❌ @Data 발견" || echo "✅ @Data 없음"

echo -e "\n[2] FetchType.EAGER 사용 확인"
grep -rn "FetchType.EAGER" src/main/java --include="*.java" && echo "❌ EAGER 발견" || echo "✅ EAGER 없음"

echo -e "\n[3] Controller에서 Repository 사용 확인"
grep -rn "Repository" src/main/java/com/Hamalog/controller --include="*.java" | grep -v "import" && echo "❌ Controller에서 Repository 사용" || echo "✅ 정상"

echo -e "\n[4] @Valid 누락 확인"
grep -rn "@RequestBody" src/main/java/com/Hamalog/controller --include="*.java" | grep -v "@Valid" && echo "⚠️ @Valid 누락 가능성" || echo "✅ 정상"

echo -e "\n[5] Spotless 검사"
./gradlew spotlessCheck && echo "✅ 포맷팅 통과" || echo "❌ 포맷팅 오류"

echo -e "\n=== Quick Check 완료 ==="
```

### 1.7.2 전체 점검 요약표

| 영역 | 점검 항목 수 | Critical | High | Medium | Low |
|------|:-----------:|:--------:|:----:|:------:|:---:|
| 패키지 구조 | 9 | 0 | 0 | 3 | 6 |
| 네이밍 컨벤션 | 9 | 0 | 2 | 4 | 3 |
| 레이어 분리 | 6 | 3 | 2 | 1 | 0 |
| Entity 설계 | 8 | 4 | 2 | 1 | 1 |
| DTO 설계 | 7 | 2 | 3 | 2 | 0 |
| 코드 스타일 | 5 | 0 | 0 | 2 | 3 |
| **총계** | **44** | **9** | **9** | **13** | **13** |

---

> 📌 **다음**: [Part 2: 인증/인가 및 보안 점검](#part-2-인증인가-및-보안-점검)

---

# Part 2: 인증/인가 및 보안 점검

> 🎯 **목표**: JWT 인증, CSRF 보호, 리소스 소유권 검증, Rate Limiting, 민감 데이터 암호화 등 보안 메커니즘이 올바르게 구현되어 있는지 점검
> 
> ⚠️ **중요도**: 🔴 Critical - 보안 취약점은 즉시 악용 가능하므로 가장 먼저 점검해야 함

---

## 2.1 인증 아키텍처 점검

### 2.1.1 인증 흐름 다이어그램

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client (SPA)                             │
│  1. 로그인 요청 (loginId, password)                              │
│  2. Access Token + Refresh Token + CSRF Token 수신               │
│  3. API 요청 시 Authorization + X-CSRF-TOKEN 헤더 포함           │
└─────────────────────────────────────────────────────────────────┘
                                 ↓
┌─────────────────────────────────────────────────────────────────┐
│                      Spring Security Filter Chain                │
│  ┌───────────┐ ┌─────────────┐ ┌────────────┐ ┌──────────────┐  │
│  │ CORS      │→│ Rate Limit  │→│ CSRF       │→│ JWT Auth     │  │
│  │ Filter    │ │ Filter      │ │ Filter     │ │ Filter       │  │
│  └───────────┘ └─────────────┘ └────────────┘ └──────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                                 ↓
┌─────────────────────────────────────────────────────────────────┐
│                    Resource Ownership AOP                        │
│  @RequireResourceOwnership → 리소스 소유자 검증                  │
└─────────────────────────────────────────────────────────────────┘
                                 ↓
┌─────────────────────────────────────────────────────────────────┐
│                         Controller                               │
└─────────────────────────────────────────────────────────────────┘
```

### 2.1.2 Security Filter Chain 순서

| 순서 | Filter | 역할 | 파일 위치 |
|:----:|--------|------|-----------|
| 1 | CORS Filter | Cross-Origin 요청 허용 | `SecurityConfig.java` |
| 2 | `RateLimitingFilter` | API 호출 빈도 제한 | `security/filter/` |
| 3 | `BotProtectionFilter` | 봇 탐지 및 차단 | `security/filter/` |
| 4 | `CsrfValidationFilter` | CSRF 토큰 검증 | `security/filter/` |
| 5 | `JwtAuthenticationFilter` | JWT 토큰 인증 | `security/jwt/` |
| 6 | `RequestSizeMonitoringFilter` | 요청 크기 모니터링 | `security/filter/` |

### 2.1.3 인증 제외 경로 점검

```java
// SecurityConfig.java에서 확인해야 할 항목
.authorizeHttpRequests(auth -> auth
    // 인증 없이 접근 가능 (Public)
    .requestMatchers("/api/auth/login", "/api/auth/signup").permitAll()
    .requestMatchers("/api/auth/refresh").permitAll()
    .requestMatchers("/api/oauth2/**").permitAll()
    .requestMatchers("/actuator/health").permitAll()
    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
    
    // 그 외 모든 요청은 인증 필요
    .anyRequest().authenticated()
)
```

### 2.1.4 인증 아키텍처 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| Security Filter Chain 순서 올바름 | ☐ | `SecurityConfig.java` 확인 | 🔴 Critical |
| 인증 제외 경로가 최소화됨 | ☐ | `permitAll()` 경로 검토 | 🔴 Critical |
| 민감 엔드포인트가 인증 필요 | ☐ | `/member/**`, `/medication/**` 등 | 🔴 Critical |
| Actuator 엔드포인트 보호 | ☐ | `/actuator/**` 중 health만 public | 🟠 High |
| Swagger 운영 환경 비활성화 | ☐ | 프로필별 설정 확인 | 🟠 High |
| OPTIONS 요청 CORS 허용 | ☐ | preflight 요청 처리 | 🟠 High |

### 2.1.5 자동 점검 스크립트

```bash
# 인증 제외 경로 확인
grep -A20 "authorizeHttpRequests" src/main/java/com/Hamalog/config/SecurityConfig.java | \
  grep "permitAll()"

# Security Filter 등록 확인
grep -rn "extends OncePerRequestFilter\|implements Filter" \
  src/main/java/com/Hamalog/security --include="*.java"

# actuator 설정 확인
grep -rn "actuator" src/main/resources/application*.yml
```

---

## 2.2 JWT 토큰 관리 점검

### 2.2.1 JWT 토큰 구조

#### Access Token
```
Header: { "alg": "HS512", "typ": "JWT" }
Payload: {
    "sub": "loginId",
    "memberId": 123,
    "iat": 1703404800,
    "exp": 1703408400  // 1시간 (또는 15분)
}
Signature: HMACSHA512(header + payload, secret)
```

#### Refresh Token
```
- Redis 저장 (key: "refresh:{memberId}:{tokenId}")
- TTL: 7일
- Rotation: 사용 시 새 토큰 발급, 기존 토큰 무효화
```

### 2.2.2 JWT 설정 점검

```yaml
# application.yml에서 확인해야 할 항목
jwt:
  secret: ${JWT_SECRET}  # 환경변수로 관리 (256비트 이상)
  access-token-validity: 3600000   # 1시간 (밀리초)
  refresh-token-validity: 604800000  # 7일 (밀리초)
```

### 2.2.3 토큰 발급 및 검증 로직

```java
// JwtTokenProvider.java 필수 구현 사항
public class JwtTokenProvider {
    
    // 토큰 생성
    public String createAccessToken(Authentication authentication) {
        // - Claims에 memberId 포함
        // - 만료 시간 설정
        // - HS512 알고리즘으로 서명
    }
    
    // 토큰 검증
    public boolean validateToken(String token) {
        // - 서명 검증
        // - 만료 시간 확인
        // - 블랙리스트 확인
    }
    
    // 토큰에서 인증 정보 추출
    public Authentication getAuthentication(String token) {
        // - Claims 파싱
        // - UserDetails 로드
        // - UsernamePasswordAuthenticationToken 생성
    }
}
```

### 2.2.4 토큰 블랙리스트

```java
// TokenBlacklistService.java 필수 구현 사항
@Service
public class TokenBlacklistService {
    
    // 로그아웃 시 토큰 블랙리스트 등록
    public void blacklistToken(String token) {
        // Redis에 저장: key = "blacklist:{token}", TTL = 토큰 남은 만료시간
    }
    
    // 블랙리스트 확인
    public boolean isBlacklisted(String token) {
        // Redis 조회
    }
}
```

### 2.2.5 Refresh Token Rotation

```java
// 토큰 갱신 시 Rotation 구현
@Transactional
public AuthTokens refresh(String refreshToken) {
    // 1. Refresh Token 유효성 검증
    // 2. Redis에서 저장된 토큰과 비교
    // 3. 새 Access Token 발급
    // 4. 새 Refresh Token 발급 (Rotation)
    // 5. 기존 Refresh Token 무효화
    // 6. 새 CSRF Token 발급
}
```

### 2.2.6 JWT 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| JWT Secret이 환경변수로 관리됨 | ☐ | `application.yml` 확인 | 🔴 Critical |
| JWT Secret이 256비트 이상 | ☐ | Secret 길이 확인 (32바이트 이상) | 🔴 Critical |
| Access Token 만료 시간 적절 (15분~1시간) | ☐ | 설정 확인 | 🟠 High |
| Refresh Token 만료 시간 적절 (7일 이하) | ☐ | 설정 확인 | 🟠 High |
| 토큰 블랙리스트 구현됨 | ☐ | `TokenBlacklistService` 확인 | 🔴 Critical |
| Refresh Token Rotation 구현됨 | ☐ | 갱신 로직 확인 | 🟠 High |
| 로그아웃 시 토큰 무효화 | ☐ | 로그아웃 API 확인 | 🔴 Critical |
| 토큰에 민감 정보 미포함 | ☐ | Claims 내용 확인 | 🟠 High |

### 2.2.7 자동 점검 스크립트

```bash
# JWT Secret 환경변수 확인
grep -rn "jwt.*secret" src/main/resources/application*.yml

# 토큰 만료 시간 확인
grep -rn "token.*validity\|expires" src/main/resources/application*.yml

# 블랙리스트 서비스 확인
find src/main/java -name "*Blacklist*" -o -name "*TokenService*"

# 토큰 생성 시 Claims 내용 확인
grep -A10 "createAccessToken\|createToken" \
  src/main/java/com/Hamalog/security/jwt/JwtTokenProvider.java
```

---

## 2.3 CSRF 보호 점검

### 2.3.1 CSRF 토큰 흐름

```
1. 로그인 성공 시 CSRF 토큰 발급
2. 클라이언트가 GET /auth/csrf-token 호출하여 토큰 획득
3. POST/PUT/DELETE 요청 시 X-CSRF-TOKEN 헤더에 포함
4. 서버에서 Redis에 저장된 토큰과 비교 검증
```

### 2.3.2 CSRF 토큰 관리

```java
// CSRF 토큰 발급
public String generateCsrfToken(Long memberId) {
    String token = UUID.randomUUID().toString();
    String key = "csrf:" + memberId;
    redisTemplate.opsForValue().set(key, token, Duration.ofHours(1));
    return token;
}

// CSRF 토큰 검증
public boolean validateCsrfToken(Long memberId, String token) {
    String key = "csrf:" + memberId;
    String storedToken = redisTemplate.opsForValue().get(key);
    return token != null && token.equals(storedToken);
}
```

### 2.3.3 CSRF 검증 예외 경로

```java
// CsrfValidationFilter.java
private static final List<String> CSRF_EXEMPT_PATHS = List.of(
    "/api/auth/login",
    "/api/auth/signup",
    "/api/auth/refresh",
    "/api/oauth2/**"
);

// GET, HEAD, OPTIONS, TRACE는 CSRF 검증 제외
private boolean isMethodExempt(String method) {
    return "GET".equals(method) || 
           "HEAD".equals(method) || 
           "OPTIONS".equals(method) || 
           "TRACE".equals(method);
}
```

### 2.3.4 CSRF 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| CSRF 필터가 Filter Chain에 등록됨 | ☐ | `SecurityConfig.java` 확인 | 🔴 Critical |
| POST/PUT/DELETE에 CSRF 검증 적용 | ☐ | Filter 로직 확인 | 🔴 Critical |
| CSRF 토큰 TTL 적절 (1시간) | ☐ | Redis 저장 로직 확인 | 🟠 High |
| 로그인/회원가입은 CSRF 검증 제외 | ☐ | 예외 경로 확인 | 🟠 High |
| CORS 설정에 X-CSRF-TOKEN 헤더 허용 | ☐ | `allowedHeaders` 확인 | 🟠 High |
| CSRF 토큰 갱신 API 존재 | ☐ | `/auth/csrf-token` 확인 | 🟡 Medium |

### 2.3.5 자동 점검 스크립트

```bash
# CSRF Filter 확인
grep -rn "CsrfValidationFilter\|CsrfFilter" \
  src/main/java/com/Hamalog/config --include="*.java"

# CSRF 토큰 저장 로직 확인
grep -rn "csrf:" src/main/java --include="*.java"

# CORS 설정에서 CSRF 헤더 확인
grep -A5 "allowedHeaders" src/main/java/com/Hamalog/config/SecurityConfig.java
```

---

## 2.4 리소스 소유권 검증 점검

### 2.4.1 @RequireResourceOwnership 어노테이션

```java
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireResourceOwnership {
    ResourceType resourceType();          // 리소스 타입
    String paramName();                    // 리소스 ID 파라미터명
    ParameterSource source() default ParameterSource.PATH_VARIABLE;  // 파라미터 추출 전략
    String bodyField() default "";         // REQUEST_BODY 시 필드명
}
```

### 2.4.2 리소스 타입별 검증 전략

| ResourceType | 검증 방식 | 쿼리 경로 |
|--------------|-----------|-----------|
| `MEDICATION_SCHEDULE` | 스케줄 → 회원 | `schedule.getMember().getMemberId()` |
| `MEDICATION_RECORD` | 기록 → 스케줄 → 회원 | `record.getSchedule().getMember().getMemberId()` |
| `MEDICATION_SCHEDULE_BY_MEMBER` | 회원 ID 직접 비교 | `memberId == currentMemberId` |
| `MEMBER` | 회원 ID 직접 비교 | `memberId == currentMemberId` |
| `MOOD_DIARY` | 일기 → 회원 | `diary.getMember().getMemberId()` |
| `MOOD_DIARY_BY_MEMBER` | 회원 ID 직접 비교 | `memberId == currentMemberId` |
| `SIDE_EFFECT_RECORD` | 기록 → 회원 | `record.getMember().getMemberId()` |

### 2.4.3 적용 예시

```java
// 리소스 ID로 소유권 검증
@GetMapping("/{id}")
@RequireResourceOwnership(
    resourceType = ResourceType.MEDICATION_SCHEDULE,
    paramName = "id"
)
public ResponseEntity<MedicationScheduleResponse> getById(@PathVariable Long id) {
    return ResponseEntity.ok(service.findById(id));
}

// 회원 ID로 소유권 검증
@GetMapping("/list/{memberId}")
@RequireResourceOwnership(
    resourceType = ResourceType.MEDICATION_SCHEDULE_BY_MEMBER,
    paramName = "memberId"
)
public ResponseEntity<List<MedicationScheduleResponse>> getByMemberId(
    @PathVariable Long memberId
) {
    return ResponseEntity.ok(service.findByMemberId(memberId));
}

// Request Body에서 파라미터 추출
@PostMapping
@RequireResourceOwnership(
    resourceType = ResourceType.MEMBER,
    paramName = "memberId",
    source = ParameterSource.REQUEST_BODY,
    bodyField = "memberId"
)
public ResponseEntity<MedicationScheduleResponse> create(
    @Valid @RequestBody MedicationScheduleCreateRequest request
) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.create(request));
}
```

### 2.4.4 소유권 검증 누락 위험 API

| API 유형 | 위험도 | 점검 필요 |
|----------|:------:|----------|
| 단건 조회 `GET /{id}` | 🔴 High | 반드시 `@RequireResourceOwnership` 적용 |
| 수정 `PUT /{id}` | 🔴 High | 반드시 `@RequireResourceOwnership` 적용 |
| 삭제 `DELETE /{id}` | 🔴 High | 반드시 `@RequireResourceOwnership` 적용 |
| 목록 조회 `GET /list/{memberId}` | 🟠 Medium | 회원 ID 비교 필요 |
| 생성 `POST` | 🟡 Low | Request Body의 memberId 검증 |

### 2.4.5 리소스 소유권 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| 모든 단건 조회 API에 소유권 검증 적용 | ☐ | Controller `@GetMapping("/{id}")` 확인 | 🔴 Critical |
| 모든 수정 API에 소유권 검증 적용 | ☐ | Controller `@PutMapping` 확인 | 🔴 Critical |
| 모든 삭제 API에 소유권 검증 적용 | ☐ | Controller `@DeleteMapping` 확인 | 🔴 Critical |
| 목록 조회에 회원 ID 검증 적용 | ☐ | Controller 확인 | 🟠 High |
| 생성 API에서 Request Body memberId 검증 | ☐ | Controller 확인 | 🟠 High |
| ResourceOwnershipAspect 정상 동작 | ☐ | 테스트 코드 확인 | 🔴 Critical |

### 2.4.6 자동 점검 스크립트

```bash
# @RequireResourceOwnership 없는 API 찾기 (위험)
for controller in $(find src/main/java/com/Hamalog/controller -name "*Controller.java"); do
  echo "=== $controller ==="
  # GetMapping/{id} 패턴에서 @RequireResourceOwnership 없는 경우
  grep -B5 '@GetMapping.*{.*id\|@PutMapping.*{.*id\|@DeleteMapping.*{.*id' "$controller" | \
    grep -v "@RequireResourceOwnership"
done

# ResourceOwnershipAspect 존재 확인
find src/main/java -name "*OwnershipAspect*" -o -name "*ResourceOwnership*"

# 테스트 코드에서 403 Forbidden 테스트 확인
grep -rn "Forbidden\|isForbidden\|403" src/test/java --include="*.java"
```

---

## 2.5 Rate Limiting 점검

### 2.5.1 Rate Limit 설정

```yaml
# application.yml
rate-limit:
  enabled: true
  # 인증 API 제한
  auth:
    requests-per-minute: 5
    requests-per-hour: 20
  # 일반 API 제한
  api:
    requests-per-minute: 60
    requests-per-hour: 1000
```

### 2.5.2 Rate Limiting 구현 방식

```java
// Sliding Window 알고리즘 (Redis ZADD 사용)
public boolean isRateLimited(String clientIp, String path) {
    String key = "ratelimit:" + clientIp + ":" + path;
    long now = System.currentTimeMillis();
    long windowStart = now - WINDOW_SIZE_MS;
    
    // 1. 윈도우 이전 데이터 삭제
    redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
    
    // 2. 현재 윈도우 내 요청 수 확인
    Long count = redisTemplate.opsForZSet().zCard(key);
    
    // 3. 제한 초과 여부 판단
    if (count >= MAX_REQUESTS) {
        return true;  // Rate Limited
    }
    
    // 4. 현재 요청 추가
    redisTemplate.opsForZSet().add(key, UUID.randomUUID().toString(), now);
    redisTemplate.expire(key, Duration.ofMinutes(1));
    
    return false;
}
```

### 2.5.3 응답 헤더

```
# Rate Limit 정보 헤더
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 55
X-RateLimit-Reset: 1703404860  # Unix Timestamp

# 제한 초과 시
HTTP/1.1 429 Too Many Requests
Retry-After: 30
```

### 2.5.4 Brute Force 방지

```java
// 로그인 실패 추적
public void recordLoginFailure(String clientIp) {
    String key = "login_failures:" + clientIp;
    Long failures = redisTemplate.opsForValue().increment(key);
    redisTemplate.expire(key, Duration.ofMinutes(15));
    
    if (failures >= BRUTE_FORCE_THRESHOLD) {
        blockIp(clientIp, Duration.ofHours(1));
    }
}

// IP 차단 확인
public boolean isIpBlocked(String clientIp) {
    return redisTemplate.hasKey("blocked_ip:" + clientIp);
}
```

### 2.5.5 Rate Limiting 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| Rate Limiting 필터 등록됨 | ☐ | `SecurityConfig.java` 확인 | 🟠 High |
| 인증 API에 더 엄격한 제한 적용 | ☐ | 설정 확인 (5회/분) | 🟠 High |
| Redis 기반 분산 환경 대응 | ☐ | Redis 사용 확인 | 🟠 High |
| 응답에 Rate Limit 헤더 포함 | ☐ | 헤더 확인 | 🟡 Medium |
| 429 응답 시 Retry-After 헤더 포함 | ☐ | 응답 확인 | 🟡 Medium |
| Brute Force IP 차단 구현 | ☐ | `SecurityEventMonitor` 확인 | 🟠 High |
| IP 차단 해제 메커니즘 존재 | ☐ | 해제 로직 확인 | 🟡 Medium |

### 2.5.6 자동 점검 스크립트

```bash
# Rate Limiting 설정 확인
grep -rn "rate-limit\|ratelimit" src/main/resources/application*.yml

# RateLimitingFilter 확인
find src/main/java -name "*RateLimit*"

# Brute Force 방지 로직 확인
grep -rn "blocked_ip\|login_failure\|brute" src/main/java --include="*.java"
```

---

## 2.6 민감 데이터 암호화 점검

### 2.6.1 암호화 대상 데이터

| 데이터 유형 | 암호화 방식 | 저장 위치 | 복호화 필요 |
|-------------|-------------|-----------|:-----------:|
| 비밀번호 | BCrypt (단방향 해시) | MySQL | ❌ |
| Refresh Token | UUID (Redis TTL) | Redis | ❌ |
| JWT Secret | 환경변수 | 없음 (메모리) | ❌ |
| 전화번호 | AES-256-GCM | MySQL | ✅ |
| 마음 일기 내용 | AES-256-GCM | MySQL | ✅ |
| 부작용 상세 내용 | AES-256-GCM | MySQL | ✅ |

### 2.6.2 비밀번호 해싱

```java
// BCryptPasswordEncoder 사용
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);  // strength: 12
}

// 비밀번호 저장 시
String encodedPassword = passwordEncoder.encode(rawPassword);

// 비밀번호 검증 시
boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
```

### 2.6.3 AES-256 암호화

```java
@Service
public class EncryptionService {
    
    @Value("${app.encryption.key}")
    private String encryptionKey;  // 256비트 (32바이트)
    
    public String encrypt(String plainText) {
        // AES-256-GCM 암호화
        // - IV (Initialization Vector): 12바이트 랜덤 생성
        // - Tag: 128비트 인증 태그
        // - 결과: Base64(IV + CipherText + Tag)
    }
    
    public String decrypt(String cipherText) {
        // AES-256-GCM 복호화
        // - Base64 디코딩
        // - IV, CipherText, Tag 분리
        // - 복호화 및 인증
    }
}
```

### 2.6.4 JPA Converter 적용

```java
// 암호화 컨버터
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    
    @Autowired
    private EncryptionService encryptionService;
    
    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute != null ? encryptionService.encrypt(attribute) : null;
    }
    
    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData != null ? encryptionService.decrypt(dbData) : null;
    }
}

// Entity 적용
@Entity
public class MoodDiary {
    @Column(columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String content;  // 자동 암/복호화
}
```

### 2.6.5 민감 데이터 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| 비밀번호가 BCrypt로 해싱됨 | ☐ | `PasswordEncoder` 빈 확인 | 🔴 Critical |
| BCrypt strength 10 이상 | ☐ | 설정 확인 | 🟠 High |
| 암호화 키가 환경변수로 관리됨 | ☐ | `application.yml` 확인 | 🔴 Critical |
| AES 키가 256비트 이상 | ☐ | 키 길이 확인 | 🔴 Critical |
| 전화번호가 암호화됨 | ☐ | Entity 컨버터 확인 | 🟠 High |
| 민감 내용(일기, 부작용)이 암호화됨 | ☐ | Entity 컨버터 확인 | 🟠 High |
| 암호화된 데이터 검색 불가능함을 인지 | ☐ | 검색 기능 확인 | 🟡 Medium |
| 로그에 민감 정보 미출력 | ☐ | 로깅 설정 확인 | 🔴 Critical |

### 2.6.6 자동 점검 스크립트

```bash
# PasswordEncoder 설정 확인
grep -rn "BCryptPasswordEncoder\|PasswordEncoder" src/main/java --include="*.java"

# 암호화 키 환경변수 확인
grep -rn "encryption.*key\|ENCRYPTION_KEY" src/main/resources/application*.yml

# 암호화 컨버터 적용 Entity 확인
grep -rn "@Convert.*Encrypted" src/main/java/com/Hamalog/domain --include="*.java"

# 로그에 민감 정보 출력 확인
grep -rn "log.*password\|log.*token\|log.*secret" src/main/java --include="*.java"
```

---

## 2.7 보안 헤더 점검

### 2.7.1 필수 보안 헤더

| 헤더 | 값 | 목적 |
|------|-----|------|
| `X-Content-Type-Options` | `nosniff` | MIME 타입 스니핑 방지 |
| `X-Frame-Options` | `DENY` | Clickjacking 방지 |
| `X-XSS-Protection` | `1; mode=block` | XSS 필터 활성화 |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | Referrer 정보 제한 |
| `Content-Security-Policy` | `default-src 'self'` | XSS/인젝션 방지 |
| `Strict-Transport-Security` | `max-age=31536000` | HTTPS 강제 (운영) |

### 2.7.2 SecurityConfig 설정

```java
.headers(headers -> headers
    .contentSecurityPolicy(csp -> csp
        .policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'"))
    .frameOptions(frame -> frame.deny())
    .xssProtection(xss -> xss.enable())
    .contentTypeOptions(Customizer.withDefaults())
    .referrerPolicy(referrer -> referrer
        .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
    .httpStrictTransportSecurity(hsts -> hsts
        .includeSubDomains(true)
        .maxAgeInSeconds(31536000))  // 1년
)
```

### 2.7.3 CORS 설정

```java
.cors(cors -> cors.configurationSource(request -> {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of(
        "https://hamalog.shop",
        "http://localhost:3000"  // 개발용
    ));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of(
        "Authorization",
        "Content-Type",
        "X-CSRF-TOKEN",
        "X-Requested-With"
    ));
    config.setExposedHeaders(List.of(
        "X-CSRF-TOKEN",
        "X-RateLimit-Limit",
        "X-RateLimit-Remaining"
    ));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);
    return config;
}))
```

### 2.7.4 보안 헤더 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| X-Content-Type-Options 설정됨 | ☐ | 응답 헤더 확인 | 🟠 High |
| X-Frame-Options: DENY 설정됨 | ☐ | 응답 헤더 확인 | 🟠 High |
| XSS Protection 활성화됨 | ☐ | 응답 헤더 확인 | 🟠 High |
| CSP 헤더 설정됨 | ☐ | 응답 헤더 확인 | 🟠 High |
| HSTS 운영 환경에서 활성화 | ☐ | 프로필별 설정 확인 | 🟠 High |
| CORS 허용 Origin 최소화 | ☐ | 설정 확인 | 🟠 High |
| CORS 자격 증명 허용 시 와일드카드 금지 | ☐ | `*` 사용 여부 확인 | 🔴 Critical |

### 2.7.5 자동 점검 스크립트

```bash
# 보안 헤더 설정 확인
grep -A20 ".headers(" src/main/java/com/Hamalog/config/SecurityConfig.java

# CORS 설정 확인
grep -A20 ".cors(" src/main/java/com/Hamalog/config/SecurityConfig.java

# 실제 응답 헤더 확인 (서버 실행 중)
curl -I https://api.hamalog.shop/actuator/health
```

---

## 2.8 OAuth2 보안 점검

### 2.8.1 OAuth2 흐름 (카카오)

```
1. GET /oauth2/auth/kakao → 카카오 로그인 페이지로 리다이렉트
2. 사용자 카카오 로그인
3. 카카오 → GET /oauth2/auth/kakao/callback?code=xxx
4. 서버에서 카카오 Access Token 획득
5. 카카오 사용자 정보 조회
6. 회원 등록/조회 후 자체 JWT 발급
```

### 2.8.2 OAuth2 보안 체크포인트

```java
// 1. State 파라미터 검증 (CSRF 방지)
String state = generateState();
session.setAttribute("oauth2_state", state);
String authUrl = kakaoAuthUrl + "&state=" + state;

// 2. 콜백에서 State 검증
String savedState = session.getAttribute("oauth2_state");
if (!savedState.equals(request.getParameter("state"))) {
    throw new OAuth2AuthenticationException("Invalid state");
}

// 3. 카카오 토큰은 서버에만 저장 (클라이언트 노출 금지)
// 4. 카카오 사용자 정보 필수 항목만 저장
```

### 2.8.3 OAuth2 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| State 파라미터 사용 (CSRF 방지) | ☐ | OAuth2 코드 확인 | 🔴 Critical |
| 콜백에서 State 검증 | ☐ | 콜백 핸들러 확인 | 🔴 Critical |
| 카카오 토큰 클라이언트 노출 안 함 | ☐ | 응답 확인 | 🔴 Critical |
| Client Secret 환경변수 관리 | ☐ | 설정 확인 | 🔴 Critical |
| 리다이렉트 URI 화이트리스트 | ☐ | 카카오 개발자 설정 확인 | 🟠 High |
| 필요한 사용자 정보만 수집 | ☐ | 스코프 확인 | 🟡 Medium |

---

## 2.9 로깅 및 감사 추적

### 2.9.1 보안 이벤트 로깅

```java
// 로그인 성공/실패
log.info("Login success - memberId: {}, ip: {}", memberId, clientIp);
log.warn("Login failed - loginId: {}, ip: {}, reason: {}", loginId, clientIp, reason);

// 권한 없는 접근 시도
log.warn("Unauthorized access attempt - memberId: {}, resource: {}, action: {}", 
    memberId, resourceId, action);

// Rate Limit 초과
log.warn("Rate limit exceeded - ip: {}, path: {}", clientIp, path);

// Brute Force 감지
log.error("Brute force detected - ip: {}, blocked for: {}", clientIp, duration);
```

### 2.9.2 민감 정보 마스킹

```java
// 로그에 민감 정보 출력 금지
log.info("User registered - loginId: {}", maskEmail(loginId));  // u***@example.com
log.debug("Token issued - tokenPrefix: {}", token.substring(0, 20) + "...");

// @NoLogging 어노테이션 사용
@NoLogging
public TokenResponse login(LoginRequest request) {
    // 이 메서드의 파라미터/반환값은 AOP 로깅에서 제외
}
```

### 2.9.3 감사 추적 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| 로그인 성공/실패 로깅 | ☐ | 로그 출력 확인 | 🟠 High |
| 권한 없는 접근 로깅 | ☐ | 로그 출력 확인 | 🟠 High |
| 중요 데이터 변경 로깅 | ☐ | 로그 출력 확인 | 🟡 Medium |
| 비밀번호 로그 미출력 | ☐ | 코드 검색 | 🔴 Critical |
| 토큰 로그 미출력 | ☐ | 코드 검색 | 🔴 Critical |
| IP 주소 기록 | ☐ | 로그 확인 | 🟠 High |
| 로그 파일 권한 제한 | ☐ | 서버 설정 확인 | 🟠 High |

---

## 2.10 Part 2 종합 점검 체크리스트

### 2.10.1 Quick Check (10분 점검)

```bash
#!/bin/bash
# Part 2 Security Quick Check Script

echo "=== Part 2: 보안 Quick Check ==="

echo -e "\n[1] JWT Secret 환경변수 확인"
grep -rn "jwt.*secret.*\${" src/main/resources/application*.yml && \
  echo "✅ 환경변수 사용" || echo "❌ 하드코딩 위험"

echo -e "\n[2] FetchType.EAGER (N+1 + 정보노출 위험)"
grep -rn "FetchType.EAGER" src/main/java && echo "❌ EAGER 발견" || echo "✅ 없음"

echo -e "\n[3] 비밀번호 로그 출력 확인"
grep -rn "log.*password" src/main/java --include="*.java" && \
  echo "⚠️ 비밀번호 로깅 가능성" || echo "✅ 없음"

echo -e "\n[4] @RequireResourceOwnership 사용 현황"
grep -rn "@RequireResourceOwnership" src/main/java/com/Hamalog/controller --include="*.java" | wc -l

echo -e "\n[5] Rate Limiting 설정"
grep -rn "rate-limit" src/main/resources/application*.yml && echo "✅ 설정됨" || echo "⚠️ 미설정"

echo -e "\n[6] CORS 와일드카드 확인"
grep -rn 'allowedOrigins.*"\*"' src/main/java && echo "❌ 와일드카드 발견" || echo "✅ 정상"

echo -e "\n=== Quick Check 완료 ==="
```

### 2.10.2 전체 점검 요약표

| 영역 | 점검 항목 수 | Critical | High | Medium | Low |
|------|:-----------:|:--------:|:----:|:------:|:---:|
| 인증 아키텍처 | 6 | 3 | 3 | 0 | 0 |
| JWT 토큰 관리 | 8 | 4 | 4 | 0 | 0 |
| CSRF 보호 | 6 | 2 | 3 | 1 | 0 |
| 리소스 소유권 | 6 | 4 | 2 | 0 | 0 |
| Rate Limiting | 7 | 0 | 4 | 3 | 0 |
| 민감 데이터 암호화 | 8 | 4 | 3 | 1 | 0 |
| 보안 헤더 | 7 | 1 | 6 | 0 | 0 |
| OAuth2 | 6 | 4 | 1 | 1 | 0 |
| 로깅/감사 | 7 | 2 | 4 | 1 | 0 |
| **총계** | **61** | **24** | **30** | **7** | **0** |

---

# Part 3: API 구현 상태 점검

> 🎯 **목표**: API 명세서와의 일치 여부, HTTP 상태 코드 적절성, 응답 시간, 예외 처리 일관성 점검
> 
> ⚠️ **중요도**: 🟡 Medium - 사용자 경험 및 시스템 안정성에 영향

---

## 3.1 API 명세서 일치 여부

### 3.1.1 필수 점검 항목

| 항목 | 설명 |
|------|------|
| API 경로 | 명세서와 일치해야 함 |
| HTTP 메서드 | GET, POST, PUT, DELETE 중 하나여야 함 |
| 요청/응답 형식 | JSON, XML 등 명세서와 일치해야 함 |
| 인증/인가 | 필요한 경우 Bearer Token 등 명세서와 일치해야 함 |

### 3.1.2 자동 점검 스크립트

```bash
# API 경로 및 메서드 점검
curl -X GET https://api.hamalog.shop/api/auth/login
curl -X POST https://api.hamalog.shop/api/auth/signup
curl -X GET https://api.hamalog.shop/api/member/1
curl -X PUT https://api.hamalog.shop/api/member/1
curl -X DELETE https://api.hamalog.shop/api/member/1

# 요청/응답 형식 점검 (예시)
curl -X POST https://api.hamalog.shop/api/auth/login -H "Content-Type: application/json" -d '{"loginId":"test", "password":"test"}'
```

---

## 3.2 HTTP 상태 코드 적절성

### 3.2.1 필수 상태 코드

| 상태 코드 | 의미 |
|-----------|------|
| 200 OK | 성공 |
| 201 Created | 리소스 생성 성공 |
| 204 No Content | 성공, 반환할 데이터 없음 |
| 400 Bad Request | 잘못된 요청 |
| 401 Unauthorized | 인증 실패 |
| 403 Forbidden | 권한 없음 |
| 404 Not Found | 리소스 없음 |
| 409 Conflict | 요청 충돌 (예: 중복 데이터) |
| 500 Internal Server Error | 서버 오류 |

### 3.2.2 상태 코드 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| 성공 시 200 또는 201 상태 코드 | ☐ | API 명세서 대조 | 🟢 Low |
| 잘못된 요청 시 400 상태 코드 | ☐ | 유효성 검사 실패 시나리오 테스트 | 🔴 Critical |
| 인증 실패 시 401 상태 코드 | ☐ | 잘못된 토큰, 만료 토큰 테스트 | 🔴 Critical |
| 권한 없음 시 403 상태 코드 | ☐ | 인가 실패 테스트 | 🔴 Critical |
| 리소스 없음 시 404 상태 코드 | ☐ | 존재하지 않는 ID 조회 테스트 | 🔴 Critical |
| 요청 충돌 시 409 상태 코드 | ☐ | 중복 데이터 입력 테스트 | 🟠 High |
| 서버 오류 시 500 상태 코드 | ☐ | 의도적인 서버 오류 유발 테스트 | 🔴 Critical |

---

## 3.3 응답 시간

### 3.3.1 응답 시간 기준

| 환경 | 기준 |
|------|------|
| 개발 | 500ms 이하 |
| QA | 300ms 이하 |
| 운영 | 200ms 이하 |

### 3.3.2 응답 시간 측정 방법

```java
// AOP를 이용한 응답 시간 측정
@Aspect
@Component
public class ResponseTimeAspect {
    
    @Around("execution(* com.Hamalog..controller..*(..))")
    public Object logResponseTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        
        Object result = joinPoint.proceed();
        
        long elapsedTime = System.currentTimeMillis() - start;
        log.info("Response time: {} ms", elapsedTime);
        
        return result;
    }
}
```

### 3.3.3 응답 시간 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| 개발 환경에서 500ms 이하 | ☐ | AOP 로그 확인 | 🟡 Medium |
| QA 환경에서 300ms 이하 | ☐ | AOP 로그 확인 | 🟡 Medium |
| 운영 환경에서 200ms 이하 | ☐ | AOP 로그 확인 | 🔴 Critical |

---

## 3.4 예외 처리 일관성

### 3.4.1 전역 예외 처리

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException ex) {
        ErrorResponse response = new ErrorResponse(ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus()).body(response);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        ErrorResponse response = new ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
```

### 3.4.2 예외 처리 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| 전역 예외 처리기 등록 | ☐ | `@ControllerAdvice` 확인 | 🔴 Critical |
| 사용자 정의 예외 처리 | ☐ | `@ExceptionHandler` 확인 | 🟠 High |
| 예외 발생 시 일관된 응답 구조 | ☐ | 에러 응답 JSON 확인 | 🟡 Medium |

---

## 3.5 Part 3 종합 점검 체크리스트

### 3.5.1 Quick Check (5분 점검)

```bash
#!/bin/bash
# Part 3 API Quick Check Script

echo "=== Part 3: API 구현 상태 Quick Check ==="

echo -e "\n[1] API 경로 및 메서드 점검"
curl -X GET https://api.hamalog.shop/api/auth/login
curl -X POST https://api.hamalog.shop/api/auth/signup
curl -X GET https://api.hamalog.shop/api/member/1
curl -X PUT https://api.hamalog.shop/api/member/1
curl -X DELETE https://api.hamalog.shop/api/member/1

echo -e "\n[2] 요청/응답 형식 점검 (예시)"
curl -X POST https://api.hamalog.shop/api/auth/login -H "Content-Type: application/json" -d '{"loginId":"test", "password":"test"}'

echo -e "\n[3] HTTP 상태 코드 점검"
curl -X POST https://api.hamalog.shop/api/auth/login -d '{"loginId":"test"}' -i | grep HTTP/
curl -X GET https://api.hamalog.shop/api/member/999 -i | grep HTTP/

echo -e "\n[4] 응답 시간 측정"
curl -X GET https://api.hamalog.shop/api/member/1 -w "응답 시간: %{time_total}s\n"

echo -e "\n=== Quick Check 완료 ==="
```

### 3.5.2 전체 점검 요약표

| 영역 | 점검 항목 수 | Critical | High | Medium | Low |
|------|:-----------:|:--------:|:----:|:------:|:---:|
| API 명세 일치 | 4 | 0 | 2 | 2 | 0 |
| HTTP 상태 코드 | 7 | 3 | 3 | 1 | 0 |
| 응답 시간 | 3 | 0 | 0 | 3 | 0 |
| 예외 처리 | 2 | 0 | 2 | 0 | 0 |
| **총계** | **16** | **3** | **7** | **6** | **0** |

---

> 📌 **다음**: [Part 4: 데이터베이스 및 JPA 점검](#part-4-데이터베이스-및-jpa-점검)

---

# Part 4: 데이터베이스 및 JPA 점검

> 🎯 **목표**: N+1 문제 해결, DTO Projection, QueryDSL 활용, 커넥션 풀 설정, Flyway 마이그레이션, 낙관적 락 구현 점검
> 
> ⚠️ **중요도**: 🟠 High - 성능 및 데이터 무결성에 직접 영향

---

## 4.1 N+1 문제 해결 점검

### 4.1.1 N+1 문제란?

```
문제 상황:
1. MedicationSchedule 10개 조회 (1개 쿼리)
2. 각 Schedule의 Member 조회 (10개 쿼리)
→ 총 11개 쿼리 (1 + N)

해결 후:
1. MedicationSchedule + Member JOIN FETCH (1개 쿼리)
→ 총 1개 쿼리
```

### 4.1.2 해결 방법

#### @EntityGraph 사용

```java
public interface MedicationScheduleRepository extends JpaRepository<MedicationSchedule, Long> {
    
    // ✅ Good - 연관 엔티티 함께 조회
    @EntityGraph(attributePaths = {"member"})
    List<MedicationSchedule> findAllByMember_MemberId(Long memberId);
    
    // ✅ Good - 여러 연관관계 함께 조회
    @EntityGraph(attributePaths = {"member", "medicationTimes"})
    Optional<MedicationSchedule> findWithTimesById(Long id);
}
```

#### JOIN FETCH 사용

```java
public interface MedicationScheduleRepository extends JpaRepository<MedicationSchedule, Long> {
    
    // ✅ Good - JPQL JOIN FETCH
    @Query("SELECT ms FROM MedicationSchedule ms " +
           "JOIN FETCH ms.member " +
           "WHERE ms.member.memberId = :memberId")
    List<MedicationSchedule> findAllByMemberIdWithMember(@Param("memberId") Long memberId);
}
```

#### 배치 조회

```java
// ✅ Good - IN 절로 한 번에 조회
@Query("SELECT mr FROM MedicationRecord mr " +
       "JOIN FETCH mr.medicationSchedule ms " +
       "JOIN FETCH mr.medicationTime mt " +
       "WHERE ms.medicationScheduleId IN :scheduleIds")
List<MedicationRecord> findAllByScheduleIds(@Param("scheduleIds") List<Long> scheduleIds);
```

### 4.1.3 N+1 문제 탐지

```java
// 테스트에서 N+1 탐지
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NPlusOneTest {
    
    @Autowired
    private EntityManager em;
    
    @Test
    @DisplayName("N+1 문제 없음 확인")
    void noNPlusOne() {
        // given
        em.createQuery("SELECT ms FROM MedicationSchedule ms", MedicationSchedule.class)
            .getResultList();
        
        // then
        // Hibernate 통계로 쿼리 수 확인
        Statistics stats = em.unwrap(Session.class).getSessionFactory().getStatistics();
        assertThat(stats.getQueryExecutionCount()).isEqualTo(1);
    }
}
```

### 4.1.4 N+1 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| `FetchType.EAGER` 사용 안 함 | ☐ | `grep "FetchType.EAGER"` | 🔴 Critical |
| 목록 조회 시 `@EntityGraph` 사용 | ☐ | Repository 확인 | 🟠 High |
| 연관 엔티티 필요 시 `JOIN FETCH` 사용 | ☐ | JPQL 확인 | 🟠 High |
| 배치 조회 (IN 절) 활용 | ☐ | 서비스 코드 확인 | 🟠 High |
| N+1 테스트 코드 존재 | ☐ | `/test/nplusone/` 확인 | 🟡 Medium |
| `default_batch_fetch_size` 설정 | ☐ | application.yml 확인 | 🟡 Medium |

### 4.1.5 자동 점검 스크립트

```bash
# FetchType.EAGER 확인
grep -rn "FetchType.EAGER" src/main/java --include="*.java"

# @EntityGraph 사용 현황
grep -rn "@EntityGraph" src/main/java/com/Hamalog/repository --include="*.java"

# JOIN FETCH 사용 현황
grep -rn "JOIN FETCH" src/main/java --include="*.java"

# batch_fetch_size 설정 확인
grep -rn "batch_fetch_size\|default_batch_fetch_size" src/main/resources/application*.yml
```

---

## 4.2 DTO Projection 점검

### 4.2.1 언제 사용하는가?

| 상황 | 권장 방식 | 이유 |
|------|-----------|------|
| 엔티티 전체 필요 | Entity 조회 | 수정/삭제 용도 |
| 조회만 필요 (일부 필드) | **DTO Projection** | 메모리/네트워크 절약 |
| 복잡한 통계/집계 | **DTO Projection** | 계산 결과 직접 매핑 |
| API 응답용 | **DTO Projection** | 불필요한 필드 제외 |

### 4.2.2 Projection 방법

#### JPQL Constructor Expression

```java
// Projection DTO
public record MedicationScheduleProjection(
    Long medicationScheduleId,
    Long memberId,
    String name,
    AlarmType alarmType,
    Boolean isActive
) {}

// Repository
@Query("SELECT new com.Hamalog.dto.medication.projection.MedicationScheduleProjection(" +
       "ms.medicationScheduleId, ms.member.memberId, ms.name, ms.alarmType, ms.isActive) " +
       "FROM MedicationSchedule ms WHERE ms.member.memberId = :memberId")
List<MedicationScheduleProjection> findProjectionsByMemberId(@Param("memberId") Long memberId);
```

#### Interface Projection

```java
// Projection 인터페이스
public interface MedicationScheduleSummary {
    Long getMedicationScheduleId();
    String getName();
    Boolean getIsActive();
}

// Repository
List<MedicationScheduleSummary> findSummaryByMember_MemberId(Long memberId);
```

#### QueryDSL Projection

```java
// QueryDSL로 Projection
public List<MedicationScheduleProjection> findProjections(Long memberId) {
    return queryFactory
        .select(Projections.constructor(MedicationScheduleProjection.class,
            medicationSchedule.medicationScheduleId,
            medicationSchedule.member.memberId,
            medicationSchedule.name,
            medicationSchedule.alarmType,
            medicationSchedule.isActive
        ))
        .from(medicationSchedule)
        .where(medicationSchedule.member.memberId.eq(memberId))
        .fetch();
}
```

### 4.2.3 DTO Projection 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| 통계/집계 쿼리에 Projection 사용 | ☐ | 코드 리뷰 | 🟠 High |
| 대량 조회 시 필요 필드만 조회 | ☐ | 코드 리뷰 | 🟠 High |
| Projection DTO가 `/dto/projection/`에 위치 | ☐ | 디렉토리 확인 | 🟡 Medium |
| 불필요한 연관관계 조회 안 함 | ☐ | 쿼리 로그 확인 | 🟠 High |

---

## 4.3 QueryDSL 활용 점검

### 4.3.1 QueryDSL 설정

```groovy
dependencies {
    implementation 'com.querydsl:querydsl-jpa:5.0.0:jakarta'
    annotationProcessor 'com.querydsl:querydsl-apt:5.0.0:jakarta'
}
```

### 4.3.2 Custom Repository 패턴

```java
// Custom Repository 인터페이스
public interface MedicationScheduleRepositoryCustom {
    Page<MedicationScheduleProjection> searchSchedules(
        Long memberId,
        String keyword,
        Boolean isActive,
        Pageable pageable
    );
}

// Custom Repository 구현
@Repository
@RequiredArgsConstructor
public class MedicationScheduleRepositoryImpl implements MedicationScheduleRepositoryCustom {
    
    private final JPAQueryFactory queryFactory;
    
    @Override
    public Page<MedicationScheduleProjection> searchSchedules(
        Long memberId,
        String keyword,
        Boolean isActive,
        Pageable pageable
    ) {
        BooleanBuilder where = new BooleanBuilder();
        where.and(medicationSchedule.member.memberId.eq(memberId));
        
        if (keyword != null && !keyword.isBlank()) {
            where.and(medicationSchedule.name.containsIgnoreCase(keyword));
        }
        
        if (isActive != null) {
            where.and(medicationSchedule.isActive.eq(isActive));
        }
        
        List<MedicationScheduleProjection> content = queryFactory
            .select(Projections.constructor(MedicationScheduleProjection.class, ...))
            .from(medicationSchedule)
            .where(where)
            .orderBy(medicationSchedule.createdAt.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();
        
        Long total = queryFactory
            .select(medicationSchedule.count())
            .from(medicationSchedule)
            .where(where)
            .fetchOne();
        
        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }
}

// 메인 Repository에서 상속
public interface MedicationScheduleRepository 
    extends JpaRepository<MedicationSchedule, Long>, MedicationScheduleRepositoryCustom {
}
```

### 4.3.3 QueryDSL 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| Q클래스 생성됨 | ☐ | `build/generated/querydsl` 확인 | 🟠 High |
| Custom Repository 구현체가 `*Impl`로 끝남 | ☐ | 클래스명 확인 | 🟠 High |
| 동적 쿼리에 `BooleanBuilder` 사용 | ☐ | 코드 리뷰 | 🟡 Medium |
| 페이징 시 count 쿼리 최적화 | ☐ | 코드 리뷰 | 🟡 Medium |

---

## 4.4 HikariCP 커넥션 풀 설정

### 4.4.1 권장 설정

```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10       # 최대 커넥션 수
      minimum-idle: 5             # 최소 유휴 커넥션
      idle-timeout: 300000        # 유휴 커넥션 타임아웃 (5분)
      max-lifetime: 1800000       # 커넥션 최대 수명 (30분)
      connection-timeout: 30000   # 커넥션 획득 타임아웃 (30초)
      leak-detection-threshold: 60000  # 커넥션 누수 탐지 (60초)
```

### 4.4.2 커넥션 풀 사이즈 계산

```
최적 커넥션 수 = (CPU 코어 수 * 2) + 유효 스핀들 수

예시:
- 4코어 CPU, SSD 사용
- 최적 = (4 * 2) + 1 = 9~10개
```

### 4.4.3 커넥션 풀 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| `maximum-pool-size` 설정됨 | ☐ | application.yml 확인 | 🟠 High |
| `leak-detection-threshold` 설정됨 | ☐ | application.yml 확인 | 🟠 High |
| 커넥션 풀 모니터링 활성화 | ☐ | Actuator 확인 | 🟡 Medium |
| 환경별 풀 사이즈 분리 | ☐ | 프로필별 설정 확인 | 🟡 Medium |

---

## 4.5 Flyway 마이그레이션 점검

### 4.5.1 마이그레이션 파일 구조

```
src/main/resources/db/migration/
├── V1__Create_member_table.sql
├── V2__Create_medication_tables.sql
├── V3__Create_diary_table.sql
├── V4__Add_notification_settings.sql
└── V5__Add_indexes.sql
```

### 4.5.2 네이밍 규칙

```
V{버전}__{설명}.sql

예시:
V1__Create_member_table.sql
V1.1__Add_member_phone.sql
V2__Create_medication_tables.sql
```

### 4.5.3 마이그레이션 작성 규칙

```sql
-- V5__Add_indexes.sql

-- 인덱스 추가 (성능 최적화)
CREATE INDEX idx_medication_schedule_member_id 
ON medication_schedule(member_id);

CREATE INDEX idx_medication_record_schedule_id 
ON medication_record(medication_schedule_id);

CREATE INDEX idx_mood_diary_member_date 
ON mood_diary(member_id, diary_date);

-- 롤백 불가능한 DDL은 주석으로 롤백 방법 명시
-- 롤백: DROP INDEX idx_medication_schedule_member_id ON medication_schedule;
```

### 4.5.4 Flyway 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| 마이그레이션 파일 버전 순차적 | ☐ | 파일명 확인 | 🔴 Critical |
| 파일명 네이밍 규칙 준수 (V{n}__{desc}.sql) | ☐ | 파일명 확인 | 🟠 High |
| 운영 DB 스키마와 마이그레이션 일치 | ☐ | flyway_schema_history 확인 | 🔴 Critical |
| 롤백 방법 주석으로 명시 | ☐ | SQL 파일 확인 | 🟡 Medium |
| 개발/운영 마이그레이션 분리 (필요 시) | ☐ | 설정 확인 | 🟡 Medium |

### 4.5.5 자동 점검 스크립트

```bash
# Flyway 마이그레이션 파일 확인
ls -la src/main/resources/db/migration/

# 마이그레이션 파일 네이밍 규칙 검사
for f in src/main/resources/db/migration/*.sql; do
  if [[ ! $(basename "$f") =~ ^V[0-9]+(\.[0-9]+)?__.*\.sql$ ]]; then
    echo "잘못된 네이밍: $f"
  fi
done

# Flyway 상태 확인 (서버 실행 중)
./gradlew flywayInfo
```

---

## 4.6 낙관적 락 (Optimistic Lock) 점검

### 4.6.1 @Version 적용

```java
@Entity
public class MedicationSchedule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long medicationScheduleId;
    
    // 낙관적 락용 버전 필드
    @Version
    private Long version;
    
    // ...
}
```

### 4.6.2 낙관적 락 예외 처리

```java
// Service에서 처리
@Transactional
public MedicationScheduleResponse update(Long id, UpdateRequest request) {
    try {
        MedicationSchedule schedule = repository.findById(id)
            .orElseThrow(ErrorCode.SCHEDULE_NOT_FOUND::toException);
        
        schedule.update(request);
        repository.save(schedule);  // 버전 충돌 시 예외 발생
        
        return MedicationScheduleResponse.from(schedule);
    } catch (OptimisticLockException | ObjectOptimisticLockingFailureException e) {
        throw ErrorCode.OPTIMISTIC_LOCK_FAILED.toException();
    }
}

// GlobalExceptionHandler에서 처리
@ExceptionHandler({OptimisticLockException.class, ObjectOptimisticLockingFailureException.class})
public ResponseEntity<ErrorResponse> handleOptimisticLock(Exception ex, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ErrorResponse.of(ErrorCode.OPTIMISTIC_LOCK_FAILED, request.getRequestURI(), traceId));
}
```

### 4.6.3 낙관적 락 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| 주요 Entity에 `@Version` 필드 존재 | ☐ | Entity 확인 | 🟠 High |
| `OptimisticLockException` 처리됨 | ☐ | GlobalExceptionHandler 확인 | 🟠 High |
| 409 Conflict 응답 반환 | ☐ | 에러 코드 확인 | 🟠 High |
| 프론트엔드에서 재시도 로직 구현 | ☐ | API 명세서 확인 | 🟡 Medium |

### 4.6.4 자동 점검 스크립트

```bash
# @Version 필드 확인
for entity in $(find src/main/java/com/Hamalog/domain -name "*.java" -exec grep -l "@Entity" {} \;); do
  if ! grep -q "@Version" "$entity"; then
    echo "Missing @Version: $entity"
  fi
done

# OptimisticLockException 처리 확인
grep -rn "OptimisticLockException\|ObjectOptimisticLockingFailureException" \
  src/main/java --include="*.java"
```

---

## 5.1 @Transactional 기본 규칙

### 5.1.1 위치 규칙

```java
// ✅ Good - Service 레이어에서 트랜잭션 관리
@Service
@Transactional(readOnly = true)  // 클래스 레벨: 기본 읽기 전용
public class MedicationScheduleService {
    
    @Transactional  // 메서드 레벨: 쓰기 작업
    public MedicationScheduleResponse create(CreateRequest request) {
        // 생성 로직
    }
    
    // 읽기 전용 (클래스 레벨 상속)
    public MedicationScheduleResponse findById(Long id) {
        // 조회 로직
    }
    
    @Transactional  // 쓰기 작업
    public void delete(Long id) {
        // 삭제 로직
    }
}

// ❌ Bad - Controller에서 트랜잭션 금지
@RestController
public class BadController {
    
    @Transactional  // ❌ Controller에 트랜잭션 금지
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Request request) {
        // ...
    }
}

// ❌ Bad - Repository에서 트랜잭션 금지
public interface BadRepository extends JpaRepository<...> {
    
    @Transactional  // ❌ Repository에 트랜잭션 금지 (Spring Data JPA가 자동 관리)
    void deleteByMemberId(Long memberId);
}
```

### 5.1.2 readOnly 설정

```java
// ✅ Good - 읽기 전용 트랜잭션
@Transactional(readOnly = true)
public MedicationScheduleResponse findById(Long id) {
    MedicationSchedule schedule = repository.findById(id)
        .orElseThrow(ErrorCode.SCHEDULE_NOT_FOUND::toException);
    return MedicationScheduleResponse.from(schedule);
}

// readOnly = true 장점:
// 1. Hibernate 더티 체킹 비활성화 → 성능 향상
// 2. 읽기 전용 DB 복제본 사용 가능 (DB Replication)
// 3. 실수로 데이터 변경 방지
```

### 5.1.3 트랜잭션 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| Controller에 `@Transactional` 없음 | ☐ | `grep "@Transactional" *Controller.java` | 🔴 High |
| Service 클래스에 `@Transactional(readOnly = true)` | ☐ | 클래스 레벨 확인 | 🟠 High |
| 쓰기 메서드에 `@Transactional` | ☐ | 메서드 레벨 확인 | 🔴 Critical |
| 읽기 메서드에 `readOnly = true` | ☐ | 메서드 레벨 또는 클래스 레벨 | 🟠 High |
| Repository에 `@Transactional` 없음 | ☐ | Repository 확인 | 🟡 Medium |

---

## 5.2 트랜잭션 전파 전략

### 5.2.1 전파 옵션

| Propagation | 설명 | 사용 시점 |
|-------------|------|-----------|
| `REQUIRED` (기본값) | 기존 트랜잭션 있으면 참여, 없으면 생성 | 일반적인 경우 |
| `REQUIRES_NEW` | 항상 새 트랜잭션 생성 (기존 보류) | 독립적 커밋 필요 시 |
| `NESTED` | 중첩 트랜잭션 (savepoint) | 부분 롤백 필요 시 |
| `SUPPORTS` | 트랜잭션 있으면 참여, 없어도 OK | 선택적 트랜잭션 |
| `NOT_SUPPORTED` | 트랜잭션 없이 실행 | 트랜잭션 불필요 작업 |
| `MANDATORY` | 기존 트랜잭션 필수 | 반드시 트랜잭션 내 실행 |
| `NEVER` | 트랜잭션 있으면 예외 | 트랜잭션 금지 작업 |

### 5.2.2 REQUIRES_NEW 사용 예시

```java
@Service
@RequiredArgsConstructor
public class MedicationRecordService {
    
    private final AuditLogService auditLogService;
    
    @Transactional
    public void createRecord(CreateRequest request) {
        // 1. 메인 비즈니스 로직
        MedicationRecord record = createMedicationRecord(request);
        
        // 2. 감사 로그 저장 (독립 트랜잭션)
        // 메인 트랜잭션 실패해도 감사 로그는 저장됨
        auditLogService.logRecordCreation(record);
    }
}

@Service
public class AuditLogService {
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logRecordCreation(MedicationRecord record) {
        // 독립적인 트랜잭션에서 실행
        // 이 트랜잭션이 실패해도 메인 트랜잭션에 영향 없음
        auditLogRepository.save(new AuditLog(...));
    }
}
```

### 5.2.3 전파 전략 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| 대부분 `REQUIRED` (기본값) 사용 | ☐ | 코드 리뷰 | 🟡 Medium |
| `REQUIRES_NEW`는 독립 커밋 필요 시만 | ☐ | 사용 이유 확인 | 🟠 High |
| 같은 클래스 내 메서드 호출 시 전파 동작 이해 | ☐ | self-invocation 문제 | 🔴 Critical |

---

## 5.3 트랜잭션 롤백 규칙

### 5.3.1 기본 롤백 규칙

```java
// 기본: RuntimeException 및 Error → 롤백
// 기본: Checked Exception → 커밋 (롤백 안 함!)

@Transactional
public void process() {
    try {
        // 비즈니스 로직
    } catch (IOException e) {
        // Checked Exception: 기본적으로 롤백 안 함!
        throw new RuntimeException(e);  // RuntimeException으로 감싸서 롤백
    }
}
```

### 5.3.2 rollbackFor 명시적 설정

```java
// ✅ Good - Checked Exception도 롤백
@Transactional(rollbackFor = Exception.class)
public void importData() throws IOException {
    // IOException 발생 시에도 롤백
}

// ✅ Good - 특정 예외만 롤백
@Transactional(rollbackFor = {CustomException.class, DataIntegrityViolationException.class})
public void process() {
    // 지정된 예외 발생 시 롤백
}

// noRollbackFor - 특정 예외에서 롤백 안 함
@Transactional(noRollbackFor = {NotificationFailedException.class})
public void createWithNotification() {
    createRecord();
    sendNotification();  // 실패해도 레코드는 저장됨
}
```

### 5.3.3 롤백 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| Checked Exception 롤백 처리 확인 | ☐ | `rollbackFor` 확인 | 🟠 High |
| 예외 catch 후 재throw 확인 | ☐ | catch 블록 확인 | 🔴 Critical |
| 부분 실패 시 롤백 범위 적절 | ☐ | 비즈니스 로직 확인 | 🟠 High |

---

## 5.4 트랜잭션 경계와 지연 로딩

### 5.4.1 LazyInitializationException 방지

```java
// ❌ Bad - 트랜잭션 밖에서 지연 로딩
@Service
public class BadService {
    
    @Transactional(readOnly = true)
    public MedicationSchedule findById(Long id) {
        return repository.findById(id).orElseThrow();
    }
}

@RestController
public class BadController {
    
    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        MedicationSchedule schedule = service.findById(id);
        // LazyInitializationException 발생!
        return ResponseEntity.ok(schedule.getMember().getName());  
    }
}

// ✅ Good - 트랜잭션 내에서 필요한 데이터 모두 로드
@Service
public class GoodService {
    
    @Transactional(readOnly = true)
    public MedicationScheduleResponse findById(Long id) {
        MedicationSchedule schedule = repository.findByIdWithMember(id)  // JOIN FETCH
            .orElseThrow(ErrorCode.SCHEDULE_NOT_FOUND::toException);
        return MedicationScheduleResponse.from(schedule);  // 트랜잭션 내에서 DTO 변환
    }
}
```

### 5.4.2 OSIV (Open Session In View) 설정

```yaml
# application.yml
spring:
  jpa:
    open-in-view: false  # 운영 환경에서는 false 권장
```

| OSIV | 장점 | 단점 |
|:----:|------|------|
| **true** (기본값) | View에서 지연 로딩 가능 | DB 커넥션 점유 시간 증가 |
| **false** (권장) | 커넥션 효율적 사용 | 트랜잭션 밖 지연 로딩 불가 |

### 5.4.3 지연 로딩 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| Service에서 DTO 변환 후 반환 | ☐ | 반환 타입 확인 | 🟠 High |
| `open-in-view: false` 설정 | ☐ | application.yml 확인 | 🟠 High |
| 필요한 연관 엔티티 `JOIN FETCH` | ☐ | Repository 확인 | 🟠 High |
| `LazyInitializationException` 테스트 | ☐ | 테스트 코드 확인 | 🟡 Medium |

---

## 5.5 비동기 처리와 트랜잭션

### 5.5.1 @Async와 트랜잭션 분리

```java
// ❌ Bad - @Async와 @Transactional 같이 사용
@Service
public class BadService {
    
    @Async
    @Transactional  // 기대대로 동작 안 함!
    public void processAsync() {
        // 새 스레드에서 실행되어 트랜잭션 전파 안 됨
    }
}

// ✅ Good - 비동기와 트랜잭션 분리
@Service
@RequiredArgsConstructor
public class GoodService {
    
    private final TransactionalService transactionalService;
    
    @Async
    public void processAsync(Long id) {
        // 비동기 스레드에서 트랜잭션 서비스 호출
        transactionalService.processInTransaction(id);
    }
}

@Service
public class TransactionalService {
    
    @Transactional
    public void processInTransaction(Long id) {
        // 트랜잭션 내에서 처리
    }
}
```

### 5.5.2 이벤트 기반 비동기 처리

```java
// 이벤트 발행 (트랜잭션 커밋 후)
@Service
public class MedicationRecordService {
    
    private final ApplicationEventPublisher eventPublisher;
    
    @Transactional
    public MedicationRecordResponse create(CreateRequest request) {
        MedicationRecord record = createMedicationRecord(request);
        
        // 트랜잭션 커밋 후 이벤트 발행
        eventPublisher.publishEvent(new MedicationRecordCreatedEvent(record.getId()));
        
        return MedicationRecordResponse.from(record);
    }
}

// 이벤트 리스너 (비동기)
@Component
public class MedicationRecordEventListener {
    
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRecordCreated(MedicationRecordCreatedEvent event) {
        // 트랜잭션 커밋 후 비동기로 실행
        // 알림 발송, 통계 업데이트 등
    }
}
```

### 5.5.3 비동기 트랜잭션 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| `@Async`와 `@Transactional` 분리 | ☐ | 같은 메서드에 없는지 확인 | 🔴 Critical |
| 비동기 메서드에서 트랜잭션 서비스 호출 | ☐ | 코드 리뷰 | 🟠 High |
| 이벤트 리스너 `TransactionPhase` 확인 | ☐ | `@TransactionalEventListener` 확인 | 🟠 High |
| 비동기 예외 처리 구현 | ☐ | `AsyncUncaughtExceptionHandler` 확인 | 🟠 High |

---

## 5.6 트랜잭션 Self-Invocation 문제

### 5.6.1 문제 상황

```java
// ❌ Bad - Self-Invocation으로 트랜잭션 미적용
@Service
public class BadService {
    
    public void process() {
        // 같은 클래스 내 메서드 호출
        this.save();  // @Transactional이 적용되지 않음!
    }
    
    @Transactional
    public void save() {
        // 트랜잭션 없이 실행됨
    }
}
```

### 5.6.2 해결 방법

```java
// 해결 1: 별도 서비스로 분리
@Service
@RequiredArgsConstructor
public class ProcessService {
    
    private final SaveService saveService;
    
    public void process() {
        saveService.save();  // 프록시를 통해 호출 → 트랜잭션 적용
    }
}

@Service
public class SaveService {
    
    @Transactional
    public void save() {
        // 트랜잭션 정상 적용
    }
}

// 해결 2: self 주입 (권장하지 않음)
@Service
public class SelfInjectService {
    
    @Autowired
    private SelfInjectService self;  // 프록시 주입
    
    public void process() {
        self.save();  // 프록시를 통해 호출
    }
    
    @Transactional
    public void save() { }
}
```

### 5.6.3 Self-Invocation 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| 같은 클래스 내 `@Transactional` 메서드 호출 확인 | ☐ | 코드 리뷰 | 🔴 Critical |
| 필요 시 별도 서비스로 분리 | ☐ | 클래스 구조 확인 | 🟠 High |
| self-invocation 테스트 코드 존재 | ☐ | 테스트 확인 | 🟡 Medium |

---

## 5.7 Part 5 종합 점검 체크리스트

### 5.7.1 Quick Check (5분 점검)

```bash
#!/bin/bash
# Part 5 Quick Check Script

echo "=== Part 5: 트랜잭션 관리 Quick Check ==="

echo -e "\n[1] Controller에서 @Transactional 사용"
grep -rn "@Transactional" src/main/java/com/Hamalog/controller --include="*.java" && \
  echo "❌ 발견" || echo "✅ 정상"

echo -e "\n[2] Service 클래스 레벨 @Transactional(readOnly = true)"
grep -rn "@Transactional(readOnly = true)" src/main/java/com/Hamalog/service --include="*.java" | \
  grep "class" | wc -l

echo -e "\n[3] @Async와 @Transactional 같이 사용"
grep -rn "@Async" src/main/java --include="*.java" -A1 | grep "@Transactional" && \
  echo "⚠️ 주의 필요" || echo "✅ 정상"

echo -e "\n[4] open-in-view 설정"
grep -rn "open-in-view" src/main/resources/application*.yml || echo "⚠️ 기본값 (true) 사용 중"

echo -e "\n[5] rollbackFor 사용 현황"
grep -rn "rollbackFor" src/main/java --include="*.java" | wc -l

echo -e "\n=== Quick Check 완료 ==="
```

### 5.7.2 전체 점검 요약표

| 영역 | 점검 항목 수 | Critical | High | Medium | Low |
|------|:-----------:|:--------:|:----:|:------:|:---:|
| @Transactional 기본 규칙 | 5 | 2 | 3 | 0 | 0 |
| 전파 전략 | 3 | 1 | 1 | 1 | 0 |
| 롤백 규칙 | 3 | 1 | 2 | 0 | 0 |
| 지연 로딩 | 4 | 0 | 3 | 1 | 0 |
| 비동기 처리 | 4 | 1 | 3 | 0 | 0 |
| Self-Invocation | 3 | 1 | 1 | 1 | 0 |
| **총계** | **22** | **6** | **13** | **3** | **0** |

---

# Part 6: 캐싱 및 성능 점검

> 🎯 **목표**: Redis 캐시 설정, TTL 관리, 캐시 무효화 전략, 벤치마크 결과 분석 점검
> 
> ⚠️ **중요도**: 🟢 Low-Med - 응답 속도 개선 및 DB 부하 감소

---

## 6.1 Redis 캐시 설정 점검

### 6.1.1 캐시 설정

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
            "memberCache", defaultConfig.entryTtl(Duration.ofMinutes(5)),
            "memberProfile", defaultConfig.entryTtl(Duration.ofHours(1)),
            "medicationStats", defaultConfig.entryTtl(Duration.ofMinutes(30)),
            "diaryStats", defaultConfig.entryTtl(Duration.ofMinutes(30)),
            "scheduleList", defaultConfig.entryTtl(Duration.ofMinutes(10))
        );

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .build();
    }
}
```

### 6.1.2 캐시 목록 및 TTL

| 캐시명 | TTL | 용도 | 키 패턴 |
|--------|-----|------|---------|
| `memberCache` | 5분 | 인증용 회원 정보 | `loginId:{loginId}` |
| `memberProfile` | 1시간 | 회원 프로필 | `{memberId}` |
| `medicationStats` | 30분 | 복약 통계 | `member:{memberId}:year:{year}` |
| `diaryStats` | 30분 | 일기 통계 | `member:{memberId}:month:{month}` |
| `scheduleList` | 10분 | 복약 스케줄 목록 | `member:{memberId}` |

### 6.1.3 캐시 설정 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| `@EnableCaching` 설정됨 | ☐ | CacheConfig 확인 | 🟠 High |
| Redis 연결 설정됨 | ☐ | application.yml 확인 | 🟠 High |
| 캐시별 TTL 설정됨 | ☐ | CacheConfig 확인 | 🟡 Medium |
| JSON 직렬화 설정됨 | ☐ | Serializer 확인 | 🟡 Medium |

---

## 6.2 @Cacheable / @CacheEvict 사용

### 6.2.1 캐싱 어노테이션 패턴

```java
@Service
@Transactional(readOnly = true)
public class MemberService {

    // 조회 시 캐싱
    @Cacheable(value = "memberProfile", key = "#memberId")
    public MemberProfileResponse getProfile(Long memberId) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(ErrorCode.MEMBER_NOT_FOUND::toException);
        return MemberProfileResponse.from(member);
    }

    // 수정 시 캐시 무효화
    @Transactional
    @CacheEvict(value = "memberProfile", key = "#memberId")
    public MemberProfileResponse updateProfile(Long memberId, UpdateRequest request) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(ErrorCode.MEMBER_NOT_FOUND::toException);
        member.updateProfile(request);
        return MemberProfileResponse.from(member);
    }

    // 삭제 시 캐시 무효화
    @Transactional
    @CacheEvict(value = "memberProfile", key = "#memberId")
    public void delete(Long memberId) {
        memberRepository.deleteById(memberId);
    }
}
```

### 6.2.2 복합 캐시 무효화

```java
// 여러 캐시 동시 무효화
@Caching(evict = {
    @CacheEvict(value = "scheduleList", key = "#memberId"),
    @CacheEvict(value = "medicationStats", key = "'member:' + #memberId + ':year:' + T(java.time.Year).now().getValue()")
})
@Transactional
public MedicationScheduleResponse create(Long memberId, CreateRequest request) {
    // 스케줄 생성 → 목록 캐시 + 통계 캐시 무효화
}
```

### 6.2.3 캐시 어노테이션 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| 조회 메서드에 `@Cacheable` 적용 | ☐ | Service 확인 | 🟡 Medium |
| 수정/삭제 메서드에 `@CacheEvict` 적용 | ☐ | Service 확인 | 🟠 High |
| 캐시 키 패턴 일관성 | ☐ | 키 패턴 확인 | 🟡 Medium |
| 연관 캐시 동시 무효화 | ☐ | `@Caching` 확인 | 🟡 Medium |

---

## 6.3 캐시 무효화 전략

### 6.3.1 무효화 시점

| 이벤트 | 무효화 대상 캐시 | 방법 |
|--------|-----------------|------|
| 스케줄 생성/수정/삭제 | `scheduleList`, `medicationStats` | `@CacheEvict` |
| 복약 기록 생성 | `medicationStats` | `@CacheEvict` |
| 일기 생성/수정/삭제 | `diaryStats` | `@CacheEvict` |
| 회원 정보 수정 | `memberProfile`, `memberCache` | `@CacheEvict` |

### 6.3.2 수동 캐시 관리

```java
@Service
@RequiredArgsConstructor
public class CacheManagementService {

    private final CacheManager cacheManager;

    // 특정 캐시 키 삭제
    public void evictCache(String cacheName, String key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }

    // 캐시 전체 삭제 (주의: 운영 환경에서는 조심)
    public void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
```

---

## 6.4 성능 벤치마크

### 6.4.1 벤치마크 도구

```bash
# Gatling 부하 테스트 실행
./gradlew gatlingRun

# 벤치마크 결과 확인
cat benchmark-results/BENCHMARK-REPORT-*.md
```

### 6.4.2 성능 목표

| 지표 | 목표 | 측정 방법 |
|------|------|----------|
| 평균 응답 시간 | < 200ms | Gatling |
| P95 응답 시간 | < 500ms | Gatling |
| P99 응답 시간 | < 1000ms | Gatling |
| 처리량 (TPS) | > 100 | Gatling |
| 에러율 | < 1% | Gatling |

### 6.4.3 캐시 적용 전후 비교

```
캐시 미적용:
- 평균 응답 시간: 150ms
- DB 쿼리 수: 5개/요청

캐시 적용 후:
- 평균 응답 시간: 30ms (80% 감소)
- DB 쿼리 수: 0개/요청 (캐시 히트 시)
```

---

## 6.5 Part 6 종합 점검

### 6.5.1 Quick Check

```bash
#!/bin/bash
echo "=== Part 6: 캐싱 및 성능 Quick Check ==="

echo -e "\n[1] @EnableCaching 설정"
grep -rn "@EnableCaching" src/main/java --include="*.java" && echo "✅ 설정됨" || echo "❌ 미설정"

echo -e "\n[2] @Cacheable 사용 현황"
grep -rn "@Cacheable" src/main/java/com/Hamalog/service --include="*.java" | wc -l

echo -e "\n[3] @CacheEvict 사용 현황"
grep -rn "@CacheEvict" src/main/java/com/Hamalog/service --include="*.java" | wc -l

echo -e "\n[4] Redis 설정"
grep -rn "redis:" src/main/resources/application*.yml && echo "✅ 설정됨" || echo "❌ 미설정"

echo -e "\n=== Quick Check 완료 ==="
```

### 6.5.2 점검 요약표

| 영역 | 점검 항목 수 | Critical | High | Medium | Low |
|------|:-----------:|:--------:|:----:|:------:|:---:|
| Redis 캐시 설정 | 4 | 0 | 2 | 2 | 0 |
| 캐시 어노테이션 | 4 | 0 | 1 | 3 | 0 |
| 캐시 무효화 | 3 | 0 | 1 | 2 | 0 |
| 성능 벤치마크 | 3 | 0 | 1 | 2 | 0 |
| **총계** | **14** | **0** | **5** | **9** | **0** |

---

> 📌 **다음**: [Part 7: 메시지 큐 및 비동기 처리 점검](#part-7-메시지-큐-및-비동기-처리-점검)

---

# Part 7: 메시지 큐 및 비동기 처리 점검

> 🎯 **목표**: Redis Stream 메시지 큐, Consumer Group, 재시도 정책, DLQ, Discord 알림 점검
> 
> ⚠️ **중요도**: 🟢 Low-Med - 비동기 처리 안정성

---

## 7.1 Redis Stream 설정

### 7.1.1 메시지 큐 구조

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│    Service      │────▶│  Redis Stream   │────▶│   Consumer      │
│   (Producer)    │     │  (Message Queue)│     │   (FCM 발송)    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                               │
                               ▼ (실패 시)
                        ┌─────────────────┐     ┌─────────────────┐
                        │  Dead Letter    │────▶│  Discord Alert  │
                        │  Queue (DLQ)    │     │  (Webhook)      │
                        └─────────────────┘     └─────────────────┘
```

### 7.1.2 설정 확인

```yaml
# application.yml
message-queue:
  enabled: true
  stream-key: hamalog:notifications
  consumer-group: notification-processors
  batch-size: 10
  poll-timeout: 5000
  retry:
    max-attempts: 3
    delay-ms: 1000
    multiplier: 2.0
  dlq:
    enabled: true
    stream-key: hamalog:notifications:dlq
```

### 7.1.3 메시지 큐 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| Redis Stream 설정됨 | ☐ | application.yml 확인 | 🟠 High |
| Consumer Group 생성됨 | ☐ | Redis CLI 확인 | 🟠 High |
| 재시도 정책 설정됨 | ☐ | retry 설정 확인 | 🟡 Medium |
| DLQ 활성화됨 | ☐ | dlq.enabled 확인 | 🟡 Medium |

---

## 7.2 Producer / Consumer 구현

### 7.2.1 Producer 패턴

```java
@Service
@RequiredArgsConstructor
public class MessageQueueService {

    private final StringRedisTemplate redisTemplate;
    private final MessageQueueProperties properties;

    public String publish(NotificationMessage message) {
        Map<String, String> messageMap = Map.of(
            "memberId", String.valueOf(message.getMemberId()),
            "title", message.getTitle(),
            "body", message.getBody(),
            "type", message.getType().name(),
            "timestamp", Instant.now().toString()
        );

        RecordId recordId = redisTemplate.opsForStream()
            .add(properties.getStreamKey(), messageMap);

        return recordId.getValue();
    }
}
```

### 7.2.2 Consumer 패턴

```java
@Component
@RequiredArgsConstructor
public class NotificationConsumerService {

    private final FcmPushService fcmService;
    private final MessageQueueProperties properties;

    @Scheduled(fixedDelay = 1000)
    public void consume() {
        List<MapRecord<String, String, String>> messages = redisTemplate.opsForStream()
            .read(
                Consumer.from(properties.getConsumerGroup(), consumerId),
                StreamReadOptions.empty()
                    .count(properties.getBatchSize())
                    .block(Duration.ofMillis(properties.getPollTimeout())),
                StreamOffset.create(properties.getStreamKey(), ReadOffset.lastConsumed())
            );

        for (MapRecord<String, String, String> message : messages) {
            try {
                processMessage(message);
                acknowledge(message.getId());
            } catch (Exception e) {
                handleFailure(message, e);
            }
        }
    }
}
```

### 7.2.3 Producer/Consumer 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| Producer 서비스 존재 | ☐ | MessageQueueService 확인 | 🟠 High |
| Consumer 서비스 존재 | ☐ | ConsumerService 확인 | 🟠 High |
| 메시지 ACK 처리 구현 | ☐ | acknowledge() 확인 | 🟡 Medium |
| 배치 처리 설정 | ☐ | batch-size 확인 | 🟡 Medium |

---

## 7.3 재시도 및 DLQ 처리

### 7.3.1 재시도 로직

```java
private void handleFailure(MapRecord<String, String, String> message, Exception e) {
    int retryCount = getRetryCount(message);

    if (retryCount < properties.getRetry().getMaxAttempts()) {
        // 재시도
        long delay = calculateBackoff(retryCount);
        scheduleRetry(message, delay);
    } else {
        // DLQ로 이동
        moveToDeadLetterQueue(message, e);
        discordAlertService.sendDlqAlert(message, e);
    }
}

private long calculateBackoff(int retryCount) {
    return (long) (properties.getRetry().getDelayMs() * 
           Math.pow(properties.getRetry().getMultiplier(), retryCount));
}
```

### 7.3.2 Discord 알림

```java
@Service
@RequiredArgsConstructor
public class DiscordWebhookService {

    @Value("${alert.discord.webhook-url}")
    private String webhookUrl;

    public void sendDlqAlert(MapRecord<String, String, String> message, Exception e) {
        String content = String.format(
            "🚨 **DLQ Alert**\n" +
            "Message ID: %s\n" +
            "Error: %s\n" +
            "Timestamp: %s",
            message.getId(),
            e.getMessage(),
            Instant.now()
        );

        restTemplate.postForEntity(webhookUrl, 
            Map.of("content", content), String.class);
    }
}
```

### 7.3.3 재시도/DLQ 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| Exponential Backoff 구현 | ☐ | calculateBackoff 확인 | 🟡 Medium |
| DLQ 이동 로직 구현 | ☐ | moveToDeadLetterQueue 확인 | 🟡 Medium |
| Discord 알림 구현 | ☐ | DiscordWebhookService 확인 | 🟠 High |
| 최대 재시도 횟수 설정 | ☐ | max-attempts 확인 | 🟡 Medium |

---

## 7.4 Part 7 종합 점검

### 7.4.1 Quick Check

```bash
#!/bin/bash
echo "=== Part 7: 메시지 큐 Quick Check ==="

echo -e "\n[1] message-queue 설정"
grep -rn "message-queue:" src/main/resources/application*.yml && echo "✅ 설정됨" || echo "⚠️ 미설정"

echo -e "\n[2] Producer 구현"
find src/main/java -name "*QueueService*" -o -name "*Producer*" | head -3

echo -e "\n[3] Consumer 구현"
find src/main/java -name "*Consumer*" | head -3

echo -e "\n[4] Discord Webhook 설정"
grep -rn "discord.*webhook" src/main/resources/application*.yml && echo "✅ 설정됨" || echo "⚠️ 미설정"

echo -e "\n=== Quick Check 완료 ==="
```

### 7.4.2 점검 요약표

| 영역 | 점검 항목 수 | Critical | High | Medium | Low |
|------|:-----------:|:--------:|:----:|:------:|:---:|
| Redis Stream 설정 | 4 | 0 | 2 | 2 | 0 |
| Producer/Consumer | 4 | 0 | 2 | 2 | 0 |
| 재시도/DLQ | 4 | 0 | 1 | 3 | 0 |
| **총계** | **12** | **0** | **5** | **7** | **0** |

---

> 📌 **다음**: [Part 8: 테스트 커버리지 점검](#part-8-테스트-커버리지-점검)

---

# Part 8: 테스트 커버리지 점검

> 🎯 **목표**: 단위 테스트, 통합 테스트, E2E 테스트, 아키텍처 테스트, 테스트 커버리지 점검
> 
> ⚠️ **중요도**: 🟡 Medium - 회귀 방지, 안정성 보장

---

## 8.1 테스트 구조

### 8.1.1 테스트 디렉토리 구조

```
src/test/java/com/Hamalog/
├── architecture/              # ArchUnit 아키텍처 테스트
├── config/                    # 테스트 설정
├── controller/                # Controller 통합 테스트
│   ├── auth/
│   ├── medication/
│   └── diary/
├── service/                   # Service 단위 테스트
│   ├── auth/
│   ├── medication/
│   └── diary/
├── repository/                # Repository 테스트
├── security/                  # 보안 테스트
├── e2e/                       # E2E 테스트
├── nplusone/                  # N+1 문제 테스트
└── validation/                # 유효성 검증 테스트
```

### 8.1.2 테스트 명명 규칙

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("복약 스케줄 서비스 테스트")
class MedicationScheduleServiceTest {
    
    @Nested
    @DisplayName("생성")
    class Create {
        
        @Test
        @DisplayName("성공: 유효한 요청")
        void success() { }
        
        @Test
        @DisplayName("실패: 회원 없음")
        void fail_memberNotFound() { }
    }
    
    @Nested
    @DisplayName("조회")
    class FindById {
        
        @Test
        @DisplayName("성공: 존재하는 ID")
        void success() { }
        
        @Test
        @DisplayName("실패: 존재하지 않는 ID")
        void fail_notFound() { }
    }
}
```

---

## 8.2 테스트 유형별 점검

### 8.2.1 단위 테스트 (Service)

```java
@ExtendWith(MockitoExtension.class)
class MedicationScheduleServiceTest {

    @Mock
    private MedicationScheduleRepository scheduleRepository;
    
    @Mock
    private MemberRepository memberRepository;
    
    @InjectMocks
    private MedicationScheduleService scheduleService;
    
    @Test
    @DisplayName("스케줄 생성 성공")
    void createSchedule_success() {
        // given
        CreateRequest request = new CreateRequest(1L, "약 이름", ...);
        Member member = createTestMember();
        MedicationSchedule schedule = createTestSchedule(member);
        
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(scheduleRepository.save(any())).thenReturn(schedule);
        
        // when
        MedicationScheduleResponse result = scheduleService.create(request);
        
        // then
        assertThat(result.getName()).isEqualTo("약 이름");
        verify(scheduleRepository).save(any());
    }
}
```

### 8.2.2 통합 테스트 (Controller)

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MedicationScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @DisplayName("스케줄 생성 API - 성공")
    @WithMockUser(username = "testUser")
    void createSchedule_success() throws Exception {
        CreateRequest request = new CreateRequest(...);
        
        mockMvc.perform(post("/api/medication-schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("약 이름"));
    }
    
    @Test
    @DisplayName("스케줄 생성 API - 검증 실패")
    @WithMockUser
    void createSchedule_validationFail() throws Exception {
        CreateRequest request = new CreateRequest(null, "", ...);  // 필수값 누락
        
        mockMvc.perform(post("/api/medication-schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
```

### 8.2.3 아키텍처 테스트 (ArchUnit)

```java
@AnalyzeClasses(packages = "com.Hamalog")
class ArchitectureTest {

    @ArchTest
    static final ArchRule controllers_should_not_access_repositories =
        noClasses()
            .that().resideInAPackage("..controller..")
            .should().accessClassesThat().resideInAPackage("..repository..");

    @ArchTest
    static final ArchRule services_should_only_be_accessed_by_controllers =
        classes()
            .that().resideInAPackage("..service..")
            .should().onlyBeAccessed().byAnyPackage("..controller..", "..service..");

    @ArchTest
    static final ArchRule entities_should_not_use_lombok_data =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().beAnnotatedWith(Data.class);
}
```

---

## 8.3 테스트 커버리지 목표

### 8.3.1 커버리지 기준

| 레이어 | 목표 커버리지 | 우선순위 |
|--------|:------------:|:--------:|
| Service | 80% 이상 | 🔴 High |
| Controller | 70% 이상 | 🟠 Medium |
| Repository | 60% 이상 | 🟡 Low |
| Security | 80% 이상 | 🔴 High |

### 8.3.2 커버리지 측정

```bash
# JaCoCo 리포트 생성
./gradlew test jacocoTestReport

# 리포트 확인
open build/jacocoHtml/index.html
```

---

## 8.4 Part 8 종합 점검

### 8.4.1 Quick Check

```bash
#!/bin/bash
echo "=== Part 8: 테스트 커버리지 Quick Check ==="

echo -e "\n[1] 테스트 파일 수"
find src/test/java -name "*Test.java" | wc -l

echo -e "\n[2] Service 테스트 수"
find src/test/java/com/Hamalog/service -name "*Test.java" 2>/dev/null | wc -l

echo -e "\n[3] Controller 테스트 수"
find src/test/java/com/Hamalog/controller -name "*Test.java" 2>/dev/null | wc -l

echo -e "\n[4] 테스트 실행"
./gradlew test --info | tail -20

echo -e "\n=== Quick Check 완료 ==="
```

### 8.4.2 점검 요약표

| 영역 | 점검 항목 수 | Critical | High | Medium | Low |
|------|:-----------:|:--------:|:----:|:------:|:---:|
| 단위 테스트 | 4 | 1 | 2 | 1 | 0 |
| 통합 테스트 | 4 | 1 | 2 | 1 | 0 |
| 아키텍처 테스트 | 3 | 0 | 2 | 1 | 0 |
| 커버리지 | 4 | 1 | 2 | 1 | 0 |
| **총계** | **15** | **3** | **8** | **4** | **0** |

---

> 📌 **다음**: [Part 9: 문서화 점검](#part-9-문서화-점검)

---

# Part 9: 문서화 점검

> 🎯 **목표**: 패턴 문서 현행화, API 명세서 최신화, ADR 관리, CHANGELOG 관리 점검
> 
> ⚠️ **중요도**: ⚪ Low - 팀 협업 효율

---

## 9.1 패턴 문서 점검

### 9.1.1 필수 패턴 문서

| 문서 | 위치 | 상태 |
|------|------|:----:|
| 어노테이션 가이드 | `docs/internal/patterns/ANNOTATION-GUIDE.md` | ☐ |
| 에러 처리 패턴 | `docs/internal/patterns/ERROR-HANDLING.md` | ☐ |
| 보안 패턴 | `docs/internal/patterns/SECURITY-PATTERNS.md` | ☐ |
| 캐싱 패턴 | `docs/internal/patterns/CACHING-PATTERNS.md` | ☐ |
| JPA 성능 | `docs/internal/patterns/JPA-PERFORMANCE.md` | ☐ |
| 메시지 큐 패턴 | `docs/internal/patterns/MESSAGE-QUEUE-PATTERNS.md` | ☐ |

### 9.1.2 문서 현행화 점검

```bash
# 최근 수정된 패턴 문서 확인
ls -lt docs/internal/patterns/*.md | head -10

# 문서와 코드 동기화 확인
# 예: @RequireResourceOwnership 문서화 vs 실제 구현
grep -rn "ResourceType" src/main/java/com/Hamalog/security/annotation --include="*.java"
```

---

## 9.2 API 명세서 점검

### 9.2.1 API 명세서 위치

| 문서 | 위치 | 상태 |
|------|------|:----:|
| API Reference | `docs/internal/API-reference.md` | ☐ |
| Swagger UI | `/swagger-ui/index.html` | ☐ |
| OpenAPI JSON | `/v3/api-docs` | ☐ |

### 9.2.2 명세서 vs 구현 검증

```bash
# Controller 엔드포인트 추출
grep -rn "@GetMapping\|@PostMapping\|@PutMapping\|@DeleteMapping" \
  src/main/java/com/Hamalog/controller --include="*.java" | \
  sed 's/.*@//' | sort > /tmp/impl_endpoints.txt

# 명세서와 비교 (수동)
cat docs/internal/API-reference.md | grep "^\| " | grep -E "GET|POST|PUT|DELETE"
```

---

## 9.3 ADR (Architecture Decision Record)

### 9.3.1 ADR 목록

| ADR | 제목 | 상태 |
|-----|------|:----:|
| ADR-0001 | 프로젝트 구조 결정 | ☐ |
| ADR-0002 | JWT + CSRF 이중 보호 | ☐ |
| ADR-0003 | Redis 캐싱 전략 | ☐ |
| ADR-0007 | Redis Stream 메시지 큐 선택 | ☐ |

### 9.3.2 ADR 작성 템플릿

```markdown
# ADR-XXXX: 제목

## 상태
Accepted / Proposed / Deprecated

## 컨텍스트
결정이 필요한 배경

## 결정
선택한 솔루션

## 결과
예상되는 결과 및 트레이드오프
```

---

## 9.4 CHANGELOG 관리

### 9.4.1 CHANGELOG 생성

```bash
# git-cliff로 CHANGELOG 자동 생성
git-cliff -o CHANGELOG.md

# 최신 변경사항 확인
head -50 CHANGELOG.md
```

### 9.4.2 Conventional Commits 형식

```
feat: 새 기능 추가
fix: 버그 수정
docs: 문서 변경
style: 코드 스타일 변경
refactor: 리팩토링
perf: 성능 개선
test: 테스트 추가/수정
chore: 빌드/도구 변경
```

---

## 9.5 Part 9 종합 점검 체크리스트

### 9.5.1 Quick Check

```bash
#!/bin/bash
echo "=== Part 9: 문서화 Quick Check ==="

echo -e "\n[1] 패턴 문서 수"
ls docs/internal/patterns/*.md 2>/dev/null | wc -l

echo -e "\n[2] ADR 문서 수"
ls docs/internal/adr/*.md 2>/dev/null | wc -l

echo -e "\n[3] CHANGELOG 존재"
[ -f CHANGELOG.md ] && echo "✅ 존재" || echo "❌ 없음"

echo -e "\n[4] 최근 문서 수정일"
ls -lt docs/internal/*.md | head -3

echo -e "\n=== Quick Check 완료 ==="
```

### 9.5.2 점검 요약표

| 영역 | 점검 항목 수 | Critical | High | Medium | Low |
|------|:-----------:|:--------:|:----:|:------:|:---:|
| 패턴 문서 | 6 | 0 | 2 | 4 | 0 |
| API 명세서 | 3 | 0 | 2 | 1 | 0 |
| ADR | 4 | 0 | 1 | 3 | 0 |
| CHANGELOG | 2 | 0 | 0 | 2 | 0 |
| **총계** | **15** | **0** | **5** | **10** | **0** |

---

> 📌 **다음**: [Part 10: 인프라 및 배포 점검](#part-10-인프라-및-배포-점검)

---

# Part 10: 인프라 및 배포 점검

> 🎯 **목표**: Docker Compose, 환경별 설정, Cloudflare Tunnel, 로깅 설정, Gradle 빌드 점검
> 
> ⚠️ **중요도**: ⚪ Low - 배포 자동화 및 인프라 관리

---

## 10.1 Docker 설정 점검

### 10.1.1 Docker Compose 구성

```yaml
# docker-compose.yml
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - JWT_SECRET=${JWT_SECRET}
      - DB_PASSWORD=${DB_PASSWORD}
    depends_on:
      - mysql
      - redis

  mysql:
    image: mysql:8.0
    volumes:
      - mysql-data:/var/lib/mysql
    environment:
      - MYSQL_ROOT_PASSWORD=${DB_ROOT_PASSWORD}
      - MYSQL_DATABASE=hamalog

  redis:
    image: redis:7-alpine
    volumes:
      - redis-data:/data
```

### 10.1.2 Docker 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| Dockerfile 존재 | ☐ | 파일 확인 | 🟠 High |
| docker-compose.yml 존재 | ☐ | 파일 확인 | 🟠 High |
| 환경변수 외부 주입 | ☐ | ${} 사용 확인 | 🔴 Critical |
| 볼륨 마운트 설정 | ☐ | volumes 확인 | 🟡 Medium |

---

## 10.2 환경별 설정 분리

### 10.2.1 프로필별 설정 파일

```
src/main/resources/
├── application.yml           # 공통 설정
├── application-local.yml     # 로컬 개발
├── application-dev.yml       # 개발 서버
├── application-prod.yml      # 운영 서버
└── application-test.yml      # 테스트
```

### 10.2.2 민감 정보 관리

```yaml
# application-prod.yml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

jwt:
  secret: ${JWT_SECRET}

app:
  encryption:
    key: ${ENCRYPTION_KEY}
```

### 10.2.3 환경 설정 점검 체크리스트

| 점검 항목 | 상태 | 점검 방법 | 심각도 |
|-----------|:----:|----------|:------:|
| 프로필별 설정 파일 존재 | ☐ | 파일 확인 | 🟠 High |
| 민감 정보 환경변수 사용 | ☐ | ${} 사용 확인 | 🔴 Critical |
| 운영 설정에 개발 정보 없음 | ☐ | prod 설정 확인 | 🔴 Critical |
| 로깅 레벨 환경별 분리 | ☐ | logging 설정 확인 | 🟡 Medium |

---

## 10.3 로깅 설정 점검

### 10.3.1 Logback 설정

```xml
<!-- logback-spring.xml -->
<configuration>
    <springProfile name="prod">
        <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>logs/application.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>logs/application.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
                <maxFileSize>100MB</maxFileSize>
                <maxHistory>30</maxHistory>
            </rollingPolicy>
        </appender>
    </springProfile>
</configuration>
```

### 10.3.2 로그 파일 구조

```
logs/
├── application.log      # 일반 로그
├── security.log        # 보안 이벤트
├── audit.log           # 감사 로그
└── performance.log     # 성능 로그
```

---

## 10.4 Part 10 종합 점검

### 10.4.1 Quick Check

```bash
#!/bin/bash
echo "=== Part 10: 인프라 및 배포 Quick Check ==="

echo -e "\n[1] Docker 파일"
[ -f Dockerfile ] && echo "✅ Dockerfile 존재" || echo "❌ Dockerfile 없음"
[ -f docker-compose.yml ] && echo "✅ docker-compose.yml 존재" || echo "❌ 없음"

echo -e "\n[2] 환경별 설정 파일"
ls src/main/resources/application*.yml 2>/dev/null

echo -e "\n[3] 환경변수 사용"
grep -rn '\${' src/main/resources/application*.yml | wc -l | xargs -I {} echo "환경변수 사용 수: {}"

echo -e "\n[4] 로그 설정"
[ -f src/main/resources/logback-spring.xml ] && echo "✅ logback 설정 존재" || echo "⚠️ 없음"

echo -e "\n=== Quick Check 완료 ==="
```

### 10.4.2 점검 요약표

| 영역 | 점검 항목 수 | Critical | High | Medium | Low |
|------|:-----------:|:--------:|:----:|:------:|:---:|
| Docker 설정 | 4 | 1 | 2 | 1 | 0 |
| 환경별 설정 | 4 | 2 | 1 | 1 | 0 |
| 로깅 설정 | 4 | 0 | 2 | 2 | 0 |
| **총계** | **12** | **3** | **5** | **4** | **0** |

---

# Appendix A: 자동화 스크립트 및 명령어

## A.1 전체 Quick Check 스크립트

```bash
#!/bin/bash
# scripts/audit-quick-check.sh
# 전체 프로젝트 Quick Check 스크립트

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║        Hamalog 프로젝트 전체 점검 Quick Check                ║"
echo "╚══════════════════════════════════════════════════════════════╝"

# Part 1: 코드 품질
echo -e "\n━━━ Part 1: 코드 품질 ━━━"
grep -rn "@Data" src/main/java/com/Hamalog/domain --include="*.java" 2>/dev/null && echo "❌ @Data 발견" || echo "✅ @Data 없음"
grep -rn "FetchType.EAGER" src/main/java --include="*.java" 2>/dev/null && echo "❌ EAGER 발견" || echo "✅ EAGER 없음"

# Part 2: 보안
echo -e "\n━━━ Part 2: 보안 ━━━"
grep -rn "jwt.*secret.*\${" src/main/resources/application*.yml && echo "✅ JWT Secret 환경변수" || echo "❌ 하드코딩 위험"
echo "@RequireResourceOwnership 사용: $(grep -rn "@RequireResourceOwnership" src/main/java/com/Hamalog/controller 2>/dev/null | wc -l) 개"

# Part 4: JPA
echo -e "\n━━━ Part 4: JPA ━━━"
echo "@EntityGraph 사용: $(grep -rn "@EntityGraph" src/main/java/com/Hamalog/repository 2>/dev/null | wc -l) 개"
echo "@Version 적용: $(grep -rl "@Version" src/main/java/com/Hamalog/domain 2>/dev/null | wc -l) Entity"

# Part 5: 트랜잭션
echo -e "\n━━━ Part 5: 트랜잭션 ━━━"
grep -rn "@Transactional" src/main/java/com/Hamalog/controller --include="*.java" 2>/dev/null && echo "❌ Controller에 @Transactional" || echo "✅ 정상"

# Part 8: 테스트
echo -e "\n━━━ Part 8: 테스트 ━━━"
echo "테스트 파일 수: $(find src/test/java -name "*Test.java" 2>/dev/null | wc -l) 개"

echo -e "\n╔══════════════════════════════════════════════════════════════╗"
echo "║                    Quick Check 완료                           ║"
echo "╚══════════════════════════════════════════════════════════════╝"
```

## A.2 자주 사용하는 명령어

```bash
# 테스트 실행
./gradlew test

# 커버리지 리포트
./gradlew test jacocoTestReport
open build/jacocoHtml/index.html

# 코드 포맷팅
./gradlew spotlessApply

# Flyway 상태
./gradlew flywayInfo

# Docker 빌드 및 실행
docker-compose up --build -d

# 로그 확인
tail -f logs/application.log

# CHANGELOG 생성
git-cliff -o CHANGELOG.md
```

---

# Appendix B: 점검 결과 템플릿

## B.1 점검 결과 보고서 템플릿

```markdown
# Hamalog 프로젝트 점검 결과 보고서

**점검일**: 2026-01-XX
**점검자**: 
**버전**: 

## 요약

| Part | 영역 | Critical | High | Medium | 통과율 |
|:----:|------|:--------:|:----:|:------:|:------:|
| 1 | 코드 품질 | ☐/☐ | ☐/☐ | ☐/☐ | -% |
| 2 | 보안 | ☐/☐ | ☐/☐ | ☐/☐ | -% |
| 3 | API | ☐/☐ | ☐/☐ | ☐/☐ | -% |
| 4 | JPA/DB | ☐/☐ | ☐/☐ | ☐/☐ | -% |
| 5 | 트랜잭션 | ☐/☐ | ☐/☐ | ☐/☐ | -% |
| 6 | 캐싱 | ☐/☐ | ☐/☐ | ☐/☐ | -% |
| 7 | 메시지 큐 | ☐/☐ | ☐/☐ | ☐/☐ | -% |
| 8 | 테스트 | ☐/☐ | ☐/☐ | ☐/☐ | -% |
| 9 | 문서화 | ☐/☐ | ☐/☐ | ☐/☐ | -% |
| 10 | 인프라 | ☐/☐ | ☐/☐ | ☐/☐ | -% |

## 발견된 이슈

### Critical
1. 

### High
1. 

### Medium
1. 

## 개선 권고사항

1. 
2. 
3. 

## 다음 점검 예정일

2026-XX-XX
```

---

# Appendix C: 우선순위별 점검 순서

## C.1 긴급 점검 (30분)

보안 관련 Critical 항목만 점검

1. JWT Secret 환경변수 확인
2. `@RequireResourceOwnership` 적용 확인
3. 비밀번호 로깅 없음 확인
4. CORS 와일드카드 없음 확인

## C.2 일반 점검 (2시간)

Part 1~5 핵심 항목 점검

1. Part 2: 보안 전체
2. Part 3: API 검증
3. Part 5: 트랜잭션
4. Part 4: JPA N+1
5. Part 1: Entity 설계

## C.3 전체 점검 (1일)

모든 Part 상세 점검

1. Part 2: 보안 (2시간)
2. Part 3: API (1시간)
3. Part 5: 트랜잭션 (1시간)
4. Part 4: JPA/DB (1시간)
5. Part 1: 아키텍처 (1시간)
6. Part 8: 테스트 (1시간)
7. Part 6-7: 캐싱/메시지큐 (30분)
8. Part 9-10: 문서/인프라 (30분)

---

> 📝 **문서 정보**
> - 작성일: 2026-01-20
> - 버전: 1.0.0
> - 총 점검 항목: 약 250개
> - Critical: 약 70개 | High: 약 100개 | Medium: 약 60개 | Low: 약 20개

