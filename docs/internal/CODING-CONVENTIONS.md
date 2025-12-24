# Hamalog 코딩 컨벤션

> 이 문서는 Hamalog 프로젝트의 코딩 규칙을 정의합니다.
> AI 코드 생성 시 이 규칙을 따라야 합니다.

---

## 1. 네이밍 규칙

### 1.1 패키지
- 소문자만 사용
- 도메인 기반 구조
```
com.Hamalog.domain.medication
com.Hamalog.service.medication
com.Hamalog.dto.medication.request
com.Hamalog.dto.medication.response
```

### 1.2 클래스
| 유형 | 패턴 | 예시 |
|------|------|------|
| Entity | `{도메인명}` | `MedicationSchedule`, `MoodDiary` |
| DTO Request | `{동작}{도메인}Request` | `MedicationScheduleCreateRequest` |
| DTO Response | `{도메인}Response` | `MedicationScheduleResponse` |
| Service | `{도메인}Service` | `MedicationScheduleService` |
| Controller | `{도메인}Controller` | `MedicationScheduleController` |
| Repository | `{도메인}Repository` | `MedicationScheduleRepository` |
| Exception | `{예외명}Exception` | `BusinessException`, `ResourceNotFoundException` |

### 1.3 메서드
| 동작 | 접두사 | 예시 |
|------|--------|------|
| 생성 | `create`, `register` | `createSchedule()` |
| 조회 (단건) | `findById`, `getById` | `findById(Long id)` |
| 조회 (목록) | `findAll`, `getList` | `findAllByMemberId()` |
| 수정 | `update`, `modify` | `updateSchedule()` |
| 삭제 | `delete`, `remove` | `deleteById()` |
| 검증 | `validate`, `check` | `validateOwnership()` |
| 변환 | `from`, `to` | `Response.from(entity)` |

### 1.4 변수
- camelCase 사용
- 의미 있는 이름 사용 (약어 지양)
- Boolean은 `is`, `has`, `can` 접두사

```java
// Good
Long memberId;
String medicationName;
boolean isActive;
boolean hasPermission;

// Bad
Long id;  // 불명확
String n;  // 약어
boolean active;  // 접두사 없음
```

---

## 2. 클래스 구조

### 2.1 Entity

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
    
    // 2. 일반 필드
    @Column(nullable = false, length = 20)
    private String name;
    
    @Enumerated(EnumType.STRING)
    private AlarmType alarmType;
    
    // 3. 연관관계 필드
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;
    
    @OneToMany(mappedBy = "medicationSchedule", cascade = CascadeType.ALL)
    private List<MedicationTime> medicationTimes = new ArrayList<>();
    
    // 4. 낙관적 락
    @Version
    private Long version;
    
    // 5. 생성자 (필수 필드만)
    public MedicationSchedule(String name, Member member) {
        this.name = name;
        this.member = member;
    }
    
    // 6. 비즈니스 메서드
    public void updateName(String name) {
        this.name = name;
    }
    
    public void addMedicationTime(MedicationTime time) {
        this.medicationTimes.add(time);
        time.setMedicationSchedule(this);
    }
}
```

### 2.2 DTO (Java Record)

```java
// Request DTO
public record MedicationScheduleCreateRequest(
    @NotNull(message = "회원 ID는 필수입니다")
    Long memberId,
    
    @NotBlank(message = "약 이름은 필수입니다")
    @Size(max = 20, message = "약 이름은 20자 이내여야 합니다")
    String name,
    
    @NotNull(message = "알람 타입은 필수입니다")
    AlarmType alarmType
) {}

// Response DTO
public record MedicationScheduleResponse(
    Long medicationScheduleId,
    Long memberId,
    String name,
    AlarmType alarmType,
    LocalDateTime createdAt
) {
    // Entity → DTO 변환 팩토리 메서드
    public static MedicationScheduleResponse from(MedicationSchedule entity) {
        return new MedicationScheduleResponse(
            entity.getMedicationScheduleId(),
            entity.getMember().getMemberId(),
            entity.getName(),
            entity.getAlarmType(),
            entity.getCreatedAt()
        );
    }
}
```

### 2.3 Service

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // 기본 읽기 전용
@Slf4j
public class MedicationScheduleService {
    
    // 1. 의존성 주입 (생성자 주입)
    private final MedicationScheduleRepository scheduleRepository;
    private final MemberRepository memberRepository;
    
    // 2. 조회 메서드 (읽기 전용 트랜잭션)
    public MedicationScheduleResponse findById(Long id) {
        MedicationSchedule schedule = scheduleRepository.findById(id)
            .orElseThrow(ErrorCode.SCHEDULE_NOT_FOUND::toException);
        return MedicationScheduleResponse.from(schedule);
    }
    
    // 3. 변경 메서드 (@Transactional 명시)
    @Transactional
    public MedicationScheduleResponse create(MedicationScheduleCreateRequest request) {
        Member member = memberRepository.findById(request.memberId())
            .orElseThrow(ErrorCode.MEMBER_NOT_FOUND::toException);
        
        MedicationSchedule schedule = new MedicationSchedule(
            request.name(),
            member
        );
        
        MedicationSchedule saved = scheduleRepository.save(schedule);
        return MedicationScheduleResponse.from(saved);
    }
    
    // 4. 삭제 메서드
    @Transactional
    public void delete(Long id) {
        MedicationSchedule schedule = scheduleRepository.findById(id)
            .orElseThrow(ErrorCode.SCHEDULE_NOT_FOUND::toException);
        scheduleRepository.delete(schedule);
    }
}
```

### 2.4 Controller

```java
@RestController
@RequestMapping("/medication-schedule")
@RequiredArgsConstructor
@Tag(name = "복약 스케줄", description = "복약 스케줄 관리 API")
public class MedicationScheduleController {
    
    private final MedicationScheduleService scheduleService;
    
    @Operation(summary = "복약 스케줄 생성")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "생성 성공"),
        @ApiResponse(responseCode = "400", description = "유효성 검증 실패"),
        @ApiResponse(responseCode = "404", description = "회원 없음")
    })
    @PostMapping
    public ResponseEntity<MedicationScheduleResponse> create(
            @Valid @RequestBody MedicationScheduleCreateRequest request) {
        MedicationScheduleResponse response = scheduleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @Operation(summary = "복약 스케줄 상세 조회")
    @GetMapping("/{id}")
    @RequireResourceOwnership(resourceType = "MEDICATION_SCHEDULE", idParam = "id")
    public ResponseEntity<MedicationScheduleResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(scheduleService.findById(id));
    }
    
    @Operation(summary = "복약 스케줄 삭제")
    @DeleteMapping("/{id}")
    @RequireResourceOwnership(resourceType = "MEDICATION_SCHEDULE", idParam = "id")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        scheduleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

---

## 3. 에러 처리

### 3.1 ErrorCode Enum 사용

```java
public enum ErrorCode {
    // 공통
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C001", "서버 오류가 발생했습니다"),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C002", "잘못된 입력입니다"),
    
    // 인증
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증이 필요합니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "A002", "접근 권한이 없습니다"),
    
    // 회원
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "회원을 찾을 수 없습니다"),
    DUPLICATE_MEMBER(HttpStatus.CONFLICT, "M002", "이미 존재하는 회원입니다"),
    
    // 복약
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "복약 스케줄을 찾을 수 없습니다");
    
    private final HttpStatus status;
    private final String code;
    private final String message;
    
    public BusinessException toException() {
        return new BusinessException(this);
    }
}
```

### 3.2 예외 발생

```java
// 조회 실패 시
Member member = memberRepository.findById(id)
    .orElseThrow(ErrorCode.MEMBER_NOT_FOUND::toException);

// 조건 검증 실패 시
if (!member.isActive()) {
    throw ErrorCode.MEMBER_DEACTIVATED.toException();
}
```

---

## 4. 테스트 규칙

### 4.1 테스트 클래스 구조

```java
@DisplayName("복약 스케줄 서비스 테스트")
@ExtendWith(MockitoExtension.class)
class MedicationScheduleServiceTest {
    
    @Mock
    private MedicationScheduleRepository scheduleRepository;
    
    @Mock
    private MemberRepository memberRepository;
    
    @InjectMocks
    private MedicationScheduleService scheduleService;
    
    // 공통 테스트 데이터
    private Member testMember;
    private MedicationSchedule testSchedule;
    
    @BeforeEach
    void setUp() {
        testMember = createTestMember();
        testSchedule = createTestSchedule(testMember);
    }
    
    @Nested
    @DisplayName("스케줄 생성")
    class CreateSchedule {
        
        @Test
        @DisplayName("성공: 유효한 요청으로 스케줄 생성")
        void success_withValidRequest() {
            // given
            var request = new MedicationScheduleCreateRequest(1L, "비타민", AlarmType.SOUND);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
            when(scheduleRepository.save(any())).thenReturn(testSchedule);
            
            // when
            var result = scheduleService.create(request);
            
            // then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("비타민");
            verify(scheduleRepository).save(any());
        }
        
        @Test
        @DisplayName("실패: 존재하지 않는 회원")
        void fail_memberNotFound() {
            // given
            var request = new MedicationScheduleCreateRequest(999L, "비타민", AlarmType.SOUND);
            when(memberRepository.findById(999L)).thenReturn(Optional.empty());
            
            // when & then
            assertThatThrownBy(() -> scheduleService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
        }
    }
}
```

### 4.2 테스트 메서드 네이밍

```java
// 패턴: {결과}_{조건}
void success_withValidRequest()
void success_whenMemberExists()
void fail_memberNotFound()
void fail_whenDuplicateEntry()
```

---

## 5. 주석 규칙

### 5.1 클래스 주석

```java
/**
 * 복약 스케줄 서비스
 * 
 * 복약 스케줄의 CRUD 및 관련 비즈니스 로직을 처리합니다.
 * 
 * @see MedicationSchedule
 * @see MedicationScheduleController
 */
@Service
public class MedicationScheduleService { }
```

### 5.2 메서드 주석 (필요 시)

```java
/**
 * 복약 스케줄 생성
 *
 * @param request 생성 요청 DTO
 * @return 생성된 스케줄 응답 DTO
 * @throws BusinessException 회원이 존재하지 않는 경우 (MEMBER_NOT_FOUND)
 */
@Transactional
public MedicationScheduleResponse create(MedicationScheduleCreateRequest request) { }
```

### 5.3 인라인 주석

```java
// 복약 시작일이 처방일보다 이전인지 검증
if (request.startOfAd().isBefore(request.prescriptionDate())) {
    throw ErrorCode.INVALID_DATE_RANGE.toException();
}
```

---

## 6. Git 커밋 규칙

### 6.1 Conventional Commits

```
<type>(<scope>): <subject>

<body>

<footer>
```

### 6.2 타입

| Type | 설명 |
|------|------|
| `feat` | 새로운 기능 |
| `fix` | 버그 수정 |
| `docs` | 문서 변경 |
| `style` | 코드 포맷팅 (기능 변경 없음) |
| `refactor` | 리팩토링 |
| `perf` | 성능 개선 |
| `test` | 테스트 추가/수정 |
| `chore` | 빌드, 설정 변경 |

### 6.3 예시

```bash
feat(auth): 로그인 이력 조회 API 추가

- GET /auth/login-history 엔드포인트 구현
- LoginHistory 엔티티 및 Repository 추가
- 페이지네이션 지원

Closes #123
```

---

> 📅 최종 업데이트: 2025년 12월 23일

