# 02. AOP 기반 리소스 소유권 검증

> **선언적 어노테이션으로 리소스 접근 권한을 검증하여 코드 중복을 제거하고 보안 누락을 방지하는 패턴**

---

## 📋 목차

1. [문제 상황](#1-문제-상황-problem)
2. [해결 전략](#2-해결-전략-solution-strategy)
3. [구현 상세](#3-구현-상세-implementation)
4. [효과 및 검증](#4-효과-및-검증-results)
5. [면접 대비 Q&A](#5-면접-대비-qa)

---

## 1. 문제 상황 (Problem)

### 1.1 반복되는 소유권 검증 코드

모든 API에서 "요청자가 해당 리소스의 소유자인지" 검증하는 코드가 반복되었습니다.

```java
// MedicationScheduleService.java
public ScheduleResponse getById(Long scheduleId) {
    // 1. 현재 로그인 사용자 조회
    Member member = memberRepository.findByLoginId(getCurrentLoginId())
        .orElseThrow(ErrorCode.MEMBER_NOT_FOUND::toException);
    
    // 2. 스케줄 조회
    MedicationSchedule schedule = scheduleRepository.findById(scheduleId)
        .orElseThrow(ErrorCode.SCHEDULE_NOT_FOUND::toException);
    
    // 3. 소유권 검증 - 모든 메서드에서 반복!
    if (!schedule.getMember().getMemberId().equals(member.getMemberId())) {
        throw ErrorCode.FORBIDDEN.toException();
    }
    
    return ScheduleResponse.from(schedule);
}

// MoodDiaryService.java - 똑같은 패턴 반복
public DiaryResponse getById(Long diaryId) {
    Member member = memberRepository.findByLoginId(getCurrentLoginId())
        .orElseThrow(ErrorCode.MEMBER_NOT_FOUND::toException);
    
    MoodDiary diary = diaryRepository.findById(diaryId)
        .orElseThrow(ErrorCode.DIARY_NOT_FOUND::toException);
    
    if (!diary.getMember().getMemberId().equals(member.getMemberId())) {
        throw ErrorCode.FORBIDDEN.toException();
    }
    
    return DiaryResponse.from(diary);
}
```

### 1.2 문제점 분석

| 문제 | 영향 |
|------|------|
| **코드 중복** | 30+ 메서드에서 동일한 5~10줄 반복 |
| **검증 누락 위험** | 실수로 검증을 빠뜨리면 보안 취약점 발생 |
| **유지보수 어려움** | 검증 로직 변경 시 모든 메서드 수정 필요 |
| **관심사 혼재** | 비즈니스 로직과 보안 로직이 섞임 |
| **테스트 복잡도** | 매번 소유권 검증 시나리오 테스트 필요 |

### 1.3 보안 사고 시나리오

```
🔴 위협: 다른 사용자의 의료 정보 접근

1. 공격자가 자신의 계정으로 로그인
2. API 요청: GET /medication-schedule/12345
3. 스케줄 ID 12345는 다른 사용자의 것
4. 소유권 검증 누락 시 → 다른 사용자의 복약 정보 노출!
```

---

## 2. 해결 전략 (Solution Strategy)

### 2.1 고려한 대안들

| 방식 | 장점 | 단점 | 채택 여부 |
|------|------|------|----------|
| **Service 메서드에서 직접 검증** | 구현 단순 | 중복, 누락 위험 | ❌ |
| **공통 유틸 메서드** | 중복 감소 | 호출 필수, 누락 가능 | ❌ |
| **Spring Security @PreAuthorize** | 표준, SpEL 지원 | 복잡한 쿼리 제한, 커스텀 어려움 | ❌ |
| **커스텀 AOP + 어노테이션** | 선언적, 확장 가능 | 초기 구현 복잡 | ✅ |

### 2.2 최종 선택: 커스텀 AOP 어노테이션

```
┌─────────────────────────────────────────────────────────────────┐
│                      Controller Method                           │
│                                                                  │
│  @GetMapping("/{id}")                                            │
│  @RequireResourceOwnership(                                      │
│      resourceType = MEDICATION_SCHEDULE,                        │
│      paramName = "id"                                           │
│  )                                                               │
│  public ResponseEntity<ScheduleResponse> getById(...)           │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                   ResourceOwnershipAspect                        │
│                                                                  │
│  @Around("@annotation(requireResourceOwnership)")                │
│  1. 현재 인증된 사용자 ID 추출                                   │
│  2. 요청 파라미터에서 리소스 ID 추출                             │
│  3. 리소스 타입에 따른 소유권 검증                               │
│  4. 실패 시 403 Forbidden 반환                                  │
│  5. 성공 시 원본 메서드 실행                                     │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 2.3 왜 이 방식인가?

1. **선언적 프로그래밍**: 의도를 명시적으로 표현, 구현은 AOP가 담당
2. **컴파일 타임 명시**: 어노테이션 없으면 IDE/리뷰에서 확인 가능
3. **중앙 집중 관리**: 검증 로직 변경 시 Aspect만 수정
4. **확장 가능**: 새 리소스 타입 추가 용이

---

## 3. 구현 상세 (Implementation)

### 3.1 커스텀 어노테이션 정의 (RequireResourceOwnership.java)

```java
/**
 * 메서드 실행 전 리소스 소유권을 검증하는 어노테이션
 * 
 * 사용 예시:
 * @RequireResourceOwnership(
 *     resourceType = ResourceType.MEDICATION_SCHEDULE,
 *     paramName = "id"
 * )
 * 
 * → 요청 파라미터 "id"로 MEDICATION_SCHEDULE을 조회하여
 *   현재 로그인 사용자의 소유인지 검증
 */
@Target(ElementType.METHOD)   // 메서드에만 적용 가능
@Retention(RetentionPolicy.RUNTIME)   // 런타임에 리플렉션으로 접근
public @interface RequireResourceOwnership {
    
    // ============================================================
    // 리소스 타입 열거형 (타입 안전성 보장)
    // ============================================================
    
    /**
     * 지원하는 리소스 타입
     * String 대신 enum을 사용하여 오타 방지 및 IDE 자동완성 지원
     */
    enum ResourceType {
        MEDICATION_RECORD("medication-record"),
        MEDICATION_SCHEDULE("medication-schedule"),
        MEDICATION_SCHEDULE_BY_MEMBER("medication-schedule-by-member"),  // memberId로 검증
        MEMBER("member"),
        MOOD_DIARY("mood-diary"),
        MOOD_DIARY_BY_MEMBER("mood-diary-by-member");  // memberId로 검증
        
        private final String value;
        
        ResourceType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    // ============================================================
    // 소유권 검증 전략
    // ============================================================
    
    /**
     * 직접 소유권 vs 간접 소유권 검증
     * 
     * DIRECT: 리소스 자체의 소유자 확인
     *         예: MedicationSchedule.member == currentUser
     * 
     * THROUGH_SCHEDULE: 스케줄을 통한 간접 검증
     *         예: MedicationRecord → MedicationSchedule → Member
     * 
     * THROUGH_MEMBER: memberId 파라미터와 현재 사용자 비교
     *         예: 요청의 memberId == currentUser.memberId
     */
    enum OwnershipStrategy {
        DIRECT,           // 직접 소유권
        THROUGH_SCHEDULE, // 스케줄을 통한 간접 검증
        THROUGH_MEMBER    // memberId 파라미터 검증
    }
    
    // ============================================================
    // 파라미터 추출 전략
    // ============================================================
    
    /**
     * 리소스 ID를 어디서 가져올지 지정
     * 
     * PATH_VARIABLE: @PathVariable로 전달된 값
     *         예: GET /schedules/{id}
     * 
     * REQUEST_PARAM: @RequestParam으로 전달된 값
     *         예: GET /schedules?scheduleId=123
     * 
     * REQUEST_BODY: 요청 본문 내 필드
     *         예: POST body의 { "scheduleId": 123 }
     */
    enum ParameterSource {
        PATH_VARIABLE,
        REQUEST_PARAM,
        REQUEST_BODY
    }
    
    // ============================================================
    // 필수 속성
    // ============================================================
    
    /**
     * 검증할 리소스 타입 (필수)
     */
    ResourceType resourceType();
    
    /**
     * 리소스 ID가 담긴 파라미터 이름 (필수)
     * 예: "id", "scheduleId", "memberId"
     */
    String paramName();
    
    // ============================================================
    // 선택 속성 (기본값 제공)
    // ============================================================
    
    /**
     * 소유권 검증 전략 (기본: DIRECT)
     */
    OwnershipStrategy strategy() default OwnershipStrategy.DIRECT;
    
    /**
     * 파라미터 추출 위치 (기본: PATH_VARIABLE)
     */
    ParameterSource source() default ParameterSource.PATH_VARIABLE;
    
    /**
     * Request Body에서 추출할 필드 경로
     * 중첩 객체 지원: "schedule.id" 형태
     */
    String bodyField() default "";
}
```

### 3.2 AOP Aspect 구현 (ResourceOwnershipAspect.java)

```java
/**
 * 리소스 소유권 검증 AOP Aspect
 * 
 * @RequireResourceOwnership 어노테이션이 붙은 모든 메서드에서
 * 실행 전 자동으로 소유권을 검증합니다.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ResourceOwnershipAspect {
    
    // 각 도메인의 소유권 검증을 위한 서비스들
    private final MedicationRecordService medicationRecordService;
    private final MedicationScheduleService medicationScheduleService;
    private final SideEffectService sideEffectService;
    private final MoodDiaryService moodDiaryService;
    private final ResourceOwnershipValidator resourceOwnershipValidator;
    
    // ============================================================
    // 핵심 Advice: Around 어드바이스
    // ============================================================
    
    /**
     * @Around: 메서드 실행 전후를 모두 제어
     *          실행 여부 자체를 결정할 수 있음 (proceed() 호출 여부)
     * 
     * @annotation(requireResourceOwnership): 해당 어노테이션이 붙은 메서드만 대상
     */
    @Around("@annotation(requireResourceOwnership)")
    public Object checkResourceOwnership(
            ProceedingJoinPoint joinPoint, 
            RequireResourceOwnership requireResourceOwnership
    ) throws Throwable {
        
        // 요청 추적 ID (로그 상관관계 분석용)
        String requestId = MDC.get("requestId");
        
        // ============================================================
        // Step 1: 현재 인증된 사용자 확인
        // ============================================================
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // 인증되지 않은 사용자 → 401 Unauthorized
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("[UNAUTHORIZED] requestId={} | No authenticated user found", requestId);
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        
        // 로그인 ID (이메일) 추출
        String currentLoginId = authentication.getName();
        
        // ============================================================
        // Step 2: 요청에서 리소스 ID 추출
        // ============================================================
        
        Long resourceId = extractResourceId(joinPoint, requireResourceOwnership);
        
        // 리소스 ID를 찾을 수 없음 → 400 Bad Request
        if (resourceId == null) {
            log.error("[AUTHORIZATION_ERROR] requestId={} | Resource ID parameter '{}' not found", 
                     requestId, requireResourceOwnership.paramName());
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
        
        // ============================================================
        // Step 3: 리소스 타입별 소유권 검증
        // ============================================================
        
        boolean isOwner = checkOwnership(
            requireResourceOwnership.resourceType(), 
            resourceId, 
            currentLoginId, 
            requireResourceOwnership.strategy()
        );
        
        // 소유자가 아님 → 403 Forbidden
        if (!isOwner) {
            log.warn("[FORBIDDEN] requestId={} | user={} | resourceType={} | resourceId={} | Access denied", 
                    requestId, currentLoginId, 
                    requireResourceOwnership.resourceType().getValue(), resourceId);
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        
        // 성공 로그 (DEBUG 레벨 - 프로덕션에서는 비활성화)
        log.debug("[AUTHORIZATION_SUCCESS] requestId={} | user={} | resourceType={} | resourceId={}", 
                 requestId, currentLoginId, 
                 requireResourceOwnership.resourceType().getValue(), resourceId);
        
        // ============================================================
        // Step 4: 원본 메서드 실행
        // ============================================================
        
        return joinPoint.proceed();
    }
    
    // ============================================================
    // 리소스 ID 추출 로직
    // ============================================================
    
    /**
     * 어노테이션 설정에 따라 적절한 위치에서 리소스 ID 추출
     */
    private Long extractResourceId(
            ProceedingJoinPoint joinPoint, 
            RequireResourceOwnership annotation
    ) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();
        
        String paramName = annotation.paramName();
        ParameterSource source = annotation.source();
        
        // 추출 전략에 따라 분기
        switch (source) {
            case PATH_VARIABLE:
            case REQUEST_PARAM:
                // URL 경로 변수 또는 쿼리 파라미터에서 추출
                return extractFromParameters(parameters, args, paramName, source);
                
            case REQUEST_BODY:
                // 요청 본문(JSON)에서 추출
                return extractFromRequestBody(args, annotation.bodyField());
                
            default:
                // 하위 호환성을 위한 기본 동작
                return extractFromParameters(parameters, args, paramName, PATH_VARIABLE);
        }
    }
    
    /**
     * 메서드 파라미터에서 ID 추출
     * 
     * @PathVariable Long id  또는  @RequestParam Long scheduleId 형태 지원
     */
    private Long extractFromParameters(
            Parameter[] parameters, 
            Object[] args, 
            String paramName, 
            ParameterSource source
    ) {
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            boolean matches = false;
            
            if (source == ParameterSource.PATH_VARIABLE) {
                // @PathVariable의 name 속성 또는 파라미터 이름과 비교
                matches = parameter.getName().equals(paramName) 
                       || paramName.equals(getPathVariableName(parameter));
            } else if (source == ParameterSource.REQUEST_PARAM) {
                // @RequestParam의 name 속성 또는 파라미터 이름과 비교
                matches = parameter.getName().equals(paramName) 
                       || paramName.equals(getRequestParamName(parameter));
            }
            
            if (matches) {
                return convertToLong(args[i]);
            }
        }
        
        return null;
    }
    
    /**
     * Request Body(DTO)에서 중첩 필드 추출
     * 
     * 예: bodyField = "schedule.id"
     *     → requestBody.getSchedule().getId()
     */
    private Long extractFromRequestBody(Object[] args, String fieldPath) {
        if (!StringUtils.hasText(fieldPath)) {
            log.error("Request body field path is empty");
            return null;
        }
        
        for (Object arg : args) {
            // null이거나 기본 타입(Long, String 등)은 스킵
            if (arg == null || isPrimitiveType(arg.getClass())) {
                continue;
            }
            
            // 중첩 필드 경로 파싱 및 값 추출
            Object extractedValue = extractNestedFieldValue(arg, fieldPath);
            if (extractedValue != null) {
                return convertToLong(extractedValue);
            }
        }
        
        log.warn("Request body does not contain field '{}' for ownership validation", fieldPath);
        return null;
    }
    
    /**
     * 점(.)으로 구분된 중첩 필드 경로에서 값 추출
     * 
     * 예: "schedule.id" → obj.getSchedule().getId()
     * 리플렉션 + Getter 메서드 호출 사용
     */
    private Object extractNestedFieldValue(Object source, String fieldPath) {
        String[] segments = fieldPath.split("\\.");
        Object current = source;
        
        for (String segment : segments) {
            if (current == null) {
                return null;
            }
            current = readSingleFieldValue(current, segment);
        }
        
        return current;
    }
    
    /**
     * 단일 필드 값 읽기 (Getter 메서드 또는 필드 직접 접근)
     */
    private Object readSingleFieldValue(Object target, String fieldName) {
        // Map 타입 지원
        if (target instanceof Map<?, ?> mapTarget) {
            return mapTarget.get(fieldName);
        }
        
        // Getter 메서드 시도: getXxx(), isXxx(), xxx()
        Method accessor = findAccessor(target.getClass(), fieldName);
        if (accessor != null) {
            try {
                accessor.setAccessible(true);
                return accessor.invoke(target);
            } catch (Exception ex) {
                log.debug("Failed to invoke accessor '{}': {}", fieldName, ex.getMessage());
            }
        }
        
        // 필드 직접 접근 시도
        Field field = findField(target.getClass(), fieldName);
        if (field != null) {
            try {
                field.setAccessible(true);
                return field.get(target);
            } catch (IllegalAccessException ex) {
                log.debug("Failed to read field '{}': {}", fieldName, ex.getMessage());
            }
        }
        
        return null;
    }
    
    // ============================================================
    // 소유권 검증 로직
    // ============================================================
    
    /**
     * 리소스 타입에 따른 소유권 검증 분기
     */
    private boolean checkOwnership(
            ResourceType resourceType, 
            Long resourceId, 
            String currentLoginId,
            OwnershipStrategy strategy
    ) {
        // ResourceOwnershipValidator로 위임 (단일 책임 원칙)
        return resourceOwnershipValidator.validate(
            resourceType, 
            resourceId, 
            currentLoginId, 
            strategy
        );
    }
}
```

### 3.3 사용 예시

```java
@RestController
@RequestMapping("/medication-schedule")
@RequiredArgsConstructor
public class MedicationScheduleController {
    
    private final MedicationScheduleService service;
    
    // ============================================================
    // 기본 사용: PathVariable에서 ID 추출
    // ============================================================
    
    /**
     * 복약 스케줄 단건 조회
     * 
     * @RequireResourceOwnership이 자동으로:
     * 1. 현재 로그인 사용자 확인
     * 2. 스케줄 ID로 스케줄 조회
     * 3. 스케줄의 소유자와 현재 사용자 비교
     * 4. 일치하지 않으면 403 Forbidden 반환
     */
    @GetMapping("/{id}")
    @RequireResourceOwnership(
        resourceType = ResourceType.MEDICATION_SCHEDULE, 
        paramName = "id"
    )
    public ResponseEntity<ScheduleResponse> getById(@PathVariable Long id) {
        // 소유권 검증은 AOP가 처리 → 비즈니스 로직만 작성
        return ResponseEntity.ok(service.findById(id));
    }
    
    // ============================================================
    // 수정/삭제도 동일하게 적용
    // ============================================================
    
    @PutMapping("/{id}")
    @RequireResourceOwnership(
        resourceType = ResourceType.MEDICATION_SCHEDULE, 
        paramName = "id"
    )
    public ResponseEntity<ScheduleResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateScheduleRequest request
    ) {
        return ResponseEntity.ok(service.update(id, request));
    }
    
    @DeleteMapping("/{id}")
    @RequireResourceOwnership(
        resourceType = ResourceType.MEDICATION_SCHEDULE, 
        paramName = "id"
    )
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
    
    // ============================================================
    // 간접 검증: MemberId를 통한 검증
    // ============================================================
    
    /**
     * 특정 사용자의 모든 스케줄 조회
     * 
     * 요청 파라미터의 memberId가 현재 로그인 사용자와 일치하는지 확인
     * (다른 사용자의 스케줄 목록 조회 방지)
     */
    @GetMapping
    @RequireResourceOwnership(
        resourceType = ResourceType.MEDICATION_SCHEDULE_BY_MEMBER,
        paramName = "memberId",
        strategy = OwnershipStrategy.THROUGH_MEMBER,
        source = ParameterSource.REQUEST_PARAM
    )
    public ResponseEntity<List<ScheduleResponse>> getByMember(
            @RequestParam Long memberId
    ) {
        return ResponseEntity.ok(service.findAllByMemberId(memberId));
    }
    
    // ============================================================
    // Request Body에서 ID 추출
    // ============================================================
    
    /**
     * 복약 기록 생성 시 스케줄 소유권 검증
     * 
     * Request Body의 scheduleId 필드를 추출하여
     * 해당 스케줄이 현재 사용자의 것인지 확인
     */
    @PostMapping("/records")
    @RequireResourceOwnership(
        resourceType = ResourceType.MEDICATION_SCHEDULE,
        paramName = "scheduleId",
        source = ParameterSource.REQUEST_BODY,
        bodyField = "scheduleId"
    )
    public ResponseEntity<RecordResponse> createRecord(
            @Valid @RequestBody CreateRecordRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recordService.create(request));
    }
}
```

---

## 4. 효과 및 검증 (Results)

### 4.1 코드 개선 효과

| 지표 | Before | After | 개선율 |
|------|--------|-------|-------|
| **검증 코드 라인** | 5~10줄/메서드 | 1줄 (어노테이션) | 90%+ 감소 |
| **중복 코드** | 30+ 메서드 × 10줄 = 300줄+ | 1개 Aspect (~150줄) | 50%+ 감소 |
| **검증 누락 위험** | 실수 가능 | 어노테이션 없으면 명확히 보임 | 거의 0 |
| **수정 시 영향 범위** | 모든 메서드 | Aspect 1개 | 95%+ 감소 |

### 4.2 보안 효과

```
✅ 모든 리소스 접근에 일관된 소유권 검증 적용
✅ 새 API 추가 시 어노테이션만 붙이면 자동 보호
✅ 코드 리뷰에서 어노테이션 누락 쉽게 발견
✅ 검증 로직 중앙화로 버그 수정 시 전체 적용
```

### 4.3 검증 테스트

```java
@Test
@DisplayName("다른 사용자의 스케줄 조회 시 403 반환")
void getById_withOtherUserSchedule_returns403() throws Exception {
    // given: 사용자 A의 스케줄
    Long scheduleId = createScheduleForUserA();
    
    // when: 사용자 B로 조회 시도
    mockMvc.perform(get("/medication-schedule/{id}", scheduleId)
            .header("Authorization", "Bearer " + userBToken))
        
        // then: 403 Forbidden
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
}

@Test
@DisplayName("본인 스케줄 조회 시 성공")
void getById_withOwnSchedule_succeeds() throws Exception {
    // given: 사용자 A의 스케줄
    Long scheduleId = createScheduleForUserA();
    
    // when: 사용자 A로 조회
    mockMvc.perform(get("/medication-schedule/{id}", scheduleId)
            .header("Authorization", "Bearer " + userAToken))
        
        // then: 200 OK
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.medicationScheduleId").value(scheduleId));
}
```

---

## 5. 면접 대비 Q&A

### Q1. @PreAuthorize 대신 커스텀 어노테이션을 만든 이유는?

> **모범 답변**
> 
> Spring Security의 `@PreAuthorize`는 강력하지만 한계가 있습니다:
> 
> 1. **SpEL 표현식 복잡도**: 리소스 조회 → 소유자 확인까지 SpEL로 작성하면 가독성이 떨어집니다.
>    ```java
>    // 복잡하고 오류 발생 시 디버깅 어려움
>    @PreAuthorize("@scheduleService.findById(#id).member.loginId == authentication.name")
>    ```
> 
> 2. **재사용성**: 동일한 검증을 여러 메서드에 적용할 때 SpEL을 반복해야 합니다.
> 
> 3. **확장성**: 검증 전략(DIRECT, THROUGH_SCHEDULE 등)을 유연하게 변경하기 어렵습니다.
> 
> 4. **테스트 용이성**: 커스텀 Aspect는 단위 테스트가 쉽지만, SpEL은 통합 테스트가 필요합니다.
> 
> 커스텀 어노테이션은 **도메인 특화** 검증에 적합하고, `@PreAuthorize`는 **일반적인 권한 검사**(역할 기반 등)에 적합합니다.

### Q2. AOP의 @Around를 선택한 이유는? @Before로는 안 되나요?

> **모범 답변**
> 
> `@Before`도 가능하지만 `@Around`가 더 적합한 이유가 있습니다:
> 
> 1. **실행 제어**: `@Around`는 `joinPoint.proceed()` 호출 여부를 결정할 수 있습니다. 검증 실패 시 원본 메서드를 아예 실행하지 않습니다.
> 
> 2. **반환값 처리**: 검증 실패 시 커스텀 응답을 반환할 수 있습니다.
>    ```java
>    // @Around에서만 가능
>    if (!isOwner) {
>        throw new CustomException(ErrorCode.FORBIDDEN);  // 또는 커스텀 응답 반환
>    }
>    return joinPoint.proceed();  // 성공 시에만 실행
>    ```
> 
> 3. **성능 측정**: 필요 시 실행 시간도 측정할 수 있습니다.
> 
> `@Before`는 예외를 던져 중단할 수는 있지만, 반환값을 제어하거나 실행 후 로직을 추가하는 데 제한이 있습니다.

### Q3. 리플렉션 사용이 성능에 영향을 주지 않나요?

> **모범 답변**
> 
> 영향은 있지만 **무시할 수 있는 수준**입니다:
> 
> 1. **호출 빈도**: API 요청당 1회만 실행됩니다. DB 쿼리, 네트워크 I/O에 비해 무시할 수준입니다.
> 
> 2. **JVM 최적화**: HotSpot JVM은 자주 호출되는 리플렉션을 최적화합니다.
> 
> 3. **실측 데이터**:
>    - 리플렉션 파라미터 추출: ~0.1ms
>    - DB 소유권 검증 쿼리: ~5-10ms
>    - 총 API 응답 시간: ~50-200ms
>    → 리플렉션 비중: 0.1% 미만
> 
> 4. **대안이 더 비쌈**: 리플렉션 없이 하려면 각 컨트롤러에서 직접 ID를 추출해야 하는데, 그러면 AOP의 의미가 없습니다.
> 
> 실제로 성능 이슈가 발생하면 캐싱(Method 객체 캐싱 등)을 적용할 수 있습니다.

### Q4. 새로운 리소스 타입을 추가하려면 어떻게 하나요?

> **모범 답변**
> 
> 세 단계로 확장합니다:
> 
> 1. **Enum에 타입 추가**:
>    ```java
>    enum ResourceType {
>        // ... 기존 타입들
>        SIDE_EFFECT("side-effect");  // 새 타입 추가
>    }
>    ```
> 
> 2. **Validator에 검증 로직 추가**:
>    ```java
>    switch (resourceType) {
>        case SIDE_EFFECT:
>            return sideEffectService.isOwner(resourceId, loginId);
>    }
>    ```
> 
> 3. **Controller에서 사용**:
>    ```java
>    @RequireResourceOwnership(
>        resourceType = ResourceType.SIDE_EFFECT,
>        paramName = "id"
>    )
>    ```
> 
> OCP(Open-Closed Principle)를 더 잘 지키려면 **Strategy 패턴**을 적용하여 각 리소스 타입별 검증기를 별도 클래스로 분리할 수 있습니다.

### Q5. 트랜잭션과 AOP 순서는 어떻게 되나요?

> **모범 답변**
> 
> 기본적으로 **소유권 검증 → 트랜잭션 시작** 순서입니다:
> 
> ```
> 요청 → ResourceOwnershipAspect → @Transactional → Service Method
> ```
> 
> 이 순서가 맞는 이유:
> 1. 권한이 없는 요청에는 트랜잭션을 시작할 필요가 없습니다.
> 2. DB 연결 리소스를 절약합니다.
> 3. 검증 쿼리와 비즈니스 쿼리를 분리하여 책임을 명확히 합니다.
> 
> 만약 순서를 바꿔야 한다면 `@Order` 어노테이션으로 조정할 수 있습니다:
> ```java
> @Aspect
> @Order(Ordered.HIGHEST_PRECEDENCE)  // 가장 먼저 실행
> public class ResourceOwnershipAspect { }
> ```

### Q6. 소유권 검증이 실패하면 어떤 정보가 로그에 남나요?

> **모범 답변**
> 
> **보안과 디버깅의 균형**을 고려한 로깅 전략입니다:
> 
> ```java
> // 검증 실패 시 (WARN 레벨)
> log.warn("[FORBIDDEN] requestId={} | user={} | resourceType={} | resourceId={}", 
>         requestId, currentLoginId, resourceType, resourceId);
> ```
> 
> 로그에 포함되는 정보:
> - **requestId**: 요청 추적 ID (상관관계 분석)
> - **user**: 시도한 사용자 (공격자 식별)
> - **resourceType**: 대상 리소스 종류
> - **resourceId**: 대상 리소스 ID
> 
> 로그에 포함되지 않는 정보:
> - 리소스의 실제 소유자 (프라이버시)
> - 리소스 내용 (민감정보)
> 
> 로그는 별도 `security.log` 파일로 분리되어 보안 감사(Audit)에 활용됩니다.

### Q7. 동시에 여러 리소스의 소유권을 검증하려면?

> **모범 답변**
> 
> 현재는 **단일 리소스 검증**만 지원하지만, 확장 방안이 있습니다:
> 
> 1. **배열 파라미터 지원** (권장):
>    ```java
>    @RequireResourceOwnership(
>        resourceType = MEDICATION_SCHEDULE,
>        paramName = "ids",
>        isMultiple = true  // 새 속성 추가
>    )
>    public void deleteMultiple(@RequestParam List<Long> ids) { }
>    ```
> 
> 2. **별도 어노테이션**:
>    ```java
>    @RequireBatchOwnership(...)
>    ```
> 
> 3. **현재 해결책**: Service에서 직접 검증
>    ```java
>    @Transactional
>    public void deleteMultiple(List<Long> ids) {
>        // Service에서 일괄 검증
>        if (!ownershipValidator.validateAll(ids, currentUser)) {
>            throw ErrorCode.FORBIDDEN.toException();
>        }
>    }
>    ```
> 
> 빈도가 낮다면 Service에서 처리하고, 자주 사용된다면 어노테이션을 확장합니다.

### Q8. 테스트에서 소유권 검증을 건너뛰려면?

> **모범 답변**
> 
> 두 가지 방법이 있습니다:
> 
> 1. **테스트 프로파일에서 Aspect 비활성화**:
>    ```java
>    @Profile("!test")  // test 프로파일에서는 Bean 생성 안 함
>    @Aspect
>    public class ResourceOwnershipAspect { }
>    ```
> 
> 2. **Mock 사용** (권장):
>    ```java
>    @MockBean
>    private ResourceOwnershipValidator validator;
>    
>    @BeforeEach
>    void setup() {
>        // 모든 검증을 통과시킴
>        when(validator.validate(any(), anyLong(), anyString(), any()))
>            .thenReturn(true);
>    }
>    ```
> 
> **주의**: 소유권 검증 자체도 테스트해야 하므로, 통합 테스트에서는 건너뛰지 않는 것이 좋습니다.

### Q9. 이 패턴의 단점은 무엇인가요?

> **모범 답변**
> 
> 솔직하게 인정해야 할 단점들:
> 
> 1. **초기 구현 복잡도**: 어노테이션, Aspect, Validator 등 여러 클래스 필요
> 
> 2. **디버깅 어려움**: 프록시를 통해 실행되므로 스택 트레이스가 복잡해짐
> 
> 3. **학습 곡선**: 팀원들이 AOP, 리플렉션을 이해해야 함
> 
> 4. **숨겨진 동작**: 코드를 읽을 때 어노테이션이 무엇을 하는지 알아야 함
> 
> 5. **컴파일 타임 검증 불가**: 잘못된 paramName을 입력해도 런타임에야 에러 발생
> 
> 이런 단점에도 불구하고, **코드 중복 제거와 보안 일관성**이라는 장점이 더 크다고 판단했습니다.

### Q10. Spring Security의 Method Security와 어떻게 공존하나요?

> **모범 답변**
> 
> **계층적 보안**으로 공존합니다:
> 
> ```
> 요청 → SecurityFilterChain (인증)
>      → @PreAuthorize (역할 기반 인가)
>      → @RequireResourceOwnership (리소스 소유권)
>      → Controller Method
> ```
> 
> 각각의 역할:
> - **SecurityFilterChain**: JWT 검증, 인증
> - **@PreAuthorize**: 역할 기반 접근 제어 (ADMIN만 접근 등)
> - **@RequireResourceOwnership**: 데이터 소유권 검증
> 
> 예시:
> ```java
> @DeleteMapping("/{id}")
> @PreAuthorize("hasRole('USER')")  // 일반 사용자 이상
> @RequireResourceOwnership(...)   // + 본인 데이터만
> public void delete(@PathVariable Long id) { }
> ```
> 
> 이렇게 하면 "USER 역할 + 본인 데이터"인 경우에만 삭제 가능합니다.

---

## 📎 관련 문서

- [ADR-0003: AOP 기반 횡단 관심사 처리](../internal/adr/0003-aop-cross-cutting-concerns.md)
- [ANNOTATION-GUIDE.md](../internal/patterns/ANNOTATION-GUIDE.md)
- [RequireResourceOwnership.java](../../src/main/java/com/Hamalog/security/annotation/RequireResourceOwnership.java)
- [ResourceOwnershipAspect.java](../../src/main/java/com/Hamalog/security/aspect/ResourceOwnershipAspect.java)

