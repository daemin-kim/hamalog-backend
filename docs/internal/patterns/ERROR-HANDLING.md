# 🚨 Hamalog 에러 처리 패턴

> 이 문서는 Hamalog 프로젝트의 표준화된 에러 처리 패턴을 설명합니다.
> 모든 예외는 이 패턴을 따라 일관성 있게 처리되어야 합니다.

---

## 📋 목차

1. [에러 처리 아키텍처](#1-에러-처리-아키텍처)
2. [ErrorCode Enum](#2-errorcode-enum)
3. [CustomException](#3-customexception)
4. [GlobalExceptionHandler](#4-globalexceptionhandler)
5. [ErrorResponse 형식](#5-errorresponse-형식)
6. [사용 예제](#6-사용-예제)
7. [새 에러 코드 추가](#7-새-에러-코드-추가)

---

## 1. 에러 처리 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                        Controller                            │
│  throw ErrorCode.XXX.toException()                          │
└─────────────────────────┬───────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│                     Service Layer                            │
│  .orElseThrow(ErrorCode.NOT_FOUND::toException)             │
└─────────────────────────┬───────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│               GlobalExceptionHandler                         │
│  @ExceptionHandler(CustomException.class)                    │
│  → ErrorResponse 생성 + HTTP 상태 코드 매핑                  │
└─────────────────────────┬───────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│                    ErrorResponse (JSON)                      │
│  { code, message, traceId, timestamp, details }             │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. ErrorCode Enum

### 2.1 위치

```
src/main/java/com/Hamalog/exception/ErrorCode.java
```

### 2.2 구조

```java
@Getter
public enum ErrorCode {
    // Member 관련
    MEMBER_NOT_FOUND("MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다."),
    DUPLICATE_MEMBER("DUPLICATE_MEMBER", "이미 존재하는 회원입니다."),
    
    // Medication 관련
    MEDICATION_SCHEDULE_NOT_FOUND("SCHEDULE_NOT_FOUND", "복약 스케줄을 찾을 수 없습니다."),
    MEDICATION_RECORD_NOT_FOUND("RECORD_NOT_FOUND", "복약 기록을 찾을 수 없습니다."),
    
    // 인증/보안
    UNAUTHORIZED("UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN("FORBIDDEN", "접근 권한이 없습니다."),
    
    // 시스템
    INTERNAL_SERVER_ERROR("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다.");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public CustomException toException() {
        return new CustomException(this);
    }
}
```

### 2.3 에러 코드 분류

| 카테고리 | 코드 패턴 | 예시 |
|----------|-----------|------|
| Member | `MEMBER_*` | MEMBER_NOT_FOUND, DUPLICATE_MEMBER |
| Medication | `SCHEDULE_*`, `RECORD_*`, `TIME_*` | SCHEDULE_NOT_FOUND |
| Diary | `MOOD_DIARY_*`, `DIARY_*` | MOOD_DIARY_NOT_FOUND |
| SideEffect | `SIDE_EFFECT_*` | SIDE_EFFECT_NOT_FOUND |
| Auth | `UNAUTHORIZED`, `FORBIDDEN`, `*_TOKEN` | TOKEN_EXPIRED |
| Validation | `INVALID_*`, `BAD_REQUEST` | INVALID_INPUT |
| File | `FILE_*` | FILE_NOT_FOUND, FILE_SAVE_FAIL |
| System | `INTERNAL_*`, `DATABASE_*` | INTERNAL_SERVER_ERROR |

### 2.4 HTTP 상태 코드 매핑

GlobalExceptionHandler에서 ErrorCode를 HTTP 상태 코드로 매핑:

| ErrorCode 패턴 | HTTP Status |
|----------------|-------------|
| `*_NOT_FOUND` | 404 Not Found |
| `UNAUTHORIZED`, `*_TOKEN` | 401 Unauthorized |
| `FORBIDDEN` | 403 Forbidden |
| `DUPLICATE_*`, `*_CONFLICT` | 409 Conflict |
| `INVALID_*`, `BAD_REQUEST` | 400 Bad Request |
| `INTERNAL_*` | 500 Internal Server Error |

---

## 3. CustomException

### 3.1 위치

```
src/main/java/com/Hamalog/exception/CustomException.java
```

### 3.2 구조

```java
@Getter
public class CustomException extends RuntimeException {
    
    private final ErrorCode errorCode;
    
    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

### 3.3 사용법

```java
// 기본 사용
throw ErrorCode.MEMBER_NOT_FOUND.toException();

// 직접 생성
throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);

// 메서드 레퍼런스
Member member = memberRepository.findById(id)
    .orElseThrow(ErrorCode.MEMBER_NOT_FOUND::toException);
```

---

## 4. GlobalExceptionHandler

### 4.1 위치

```
src/main/java/com/Hamalog/handler/GlobalExceptionHandler.java
```

### 4.2 처리하는 예외 유형

| 예외 타입 | 처리 방식 |
|-----------|-----------|
| `CustomException` | ErrorCode 기반 응답 생성 |
| `MethodArgumentNotValidException` | Validation 오류 상세 정보 |
| `ConstraintViolationException` | Bean Validation 오류 |
| `HttpMessageNotReadableException` | JSON 파싱 오류 |
| `AccessDeniedException` | 403 Forbidden |
| `Exception` | 500 Internal Server Error (로깅) |

### 4.3 예외 처리 우선순위

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        // 1순위: 비즈니스 예외
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(...) {
        // 2순위: Validation 예외
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        // 마지막: 알 수 없는 예외 (로깅 필수)
    }
}
```

---

## 5. ErrorResponse 형식

### 5.1 표준 응답 구조

```json
{
    "code": "MEMBER_NOT_FOUND",
    "message": "회원을 찾을 수 없습니다.",
    "traceId": "abc123def456",
    "timestamp": "2025-12-24T10:30:00",
    "path": "/api/members/999",
    "details": null
}
```

### 5.2 Validation 오류 응답

```json
{
    "code": "INVALID_INPUT",
    "message": "입력값이 유효하지 않습니다.",
    "traceId": "xyz789",
    "timestamp": "2025-12-24T10:30:00",
    "path": "/api/medication-schedule",
    "details": {
        "fieldErrors": [
            {
                "field": "name",
                "message": "복약명은 필수입니다.",
                "rejectedValue": null
            },
            {
                "field": "prescriptionDays",
                "message": "처방 일수는 1 이상이어야 합니다.",
                "rejectedValue": 0
            }
        ]
    }
}
```

### 5.3 DTO 구조

```java
public record ErrorResponse(
    String code,
    String message,
    String traceId,
    LocalDateTime timestamp,
    String path,
    Object details
) {
    public static ErrorResponse of(ErrorCode errorCode, String traceId, String path) {
        return new ErrorResponse(
            errorCode.getCode(),
            errorCode.getMessage(),
            traceId,
            LocalDateTime.now(),
            path,
            null
        );
    }
}
```

---

## 6. 사용 예제

### 6.1 Service에서 예외 발생

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicationScheduleService {

    private final MedicationScheduleRepository scheduleRepository;
    private final MemberRepository memberRepository;

    @Transactional(rollbackFor = {Exception.class})
    public MedicationScheduleResponse create(MedicationScheduleCreateRequest request) {
        // 1. 회원 존재 확인
        Member member = memberRepository.findById(request.memberId())
            .orElseThrow(ErrorCode.MEMBER_NOT_FOUND::toException);
        
        // 2. 비즈니스 규칙 검증
        if (request.prescriptionDays() < 1) {
            throw ErrorCode.INVALID_PRESCRIPTION_DAYS.toException();
        }
        
        // 3. 엔티티 생성 및 저장
        MedicationSchedule schedule = new MedicationSchedule(
            request.name(),
            member,
            request.prescriptionDays()
        );
        
        return MedicationScheduleResponse.from(scheduleRepository.save(schedule));
    }

    public MedicationScheduleResponse findById(Long id) {
        return scheduleRepository.findById(id)
            .map(MedicationScheduleResponse::from)
            .orElseThrow(ErrorCode.MEDICATION_SCHEDULE_NOT_FOUND::toException);
    }
}
```

### 6.2 조건부 예외 발생

```java
public void updateProfile(Long memberId, ProfileUpdateRequest request) {
    Member member = memberRepository.findById(memberId)
        .orElseThrow(ErrorCode.MEMBER_NOT_FOUND::toException);
    
    // 변경할 내용이 없는 경우
    if (request.isEmpty()) {
        throw ErrorCode.NO_PROFILE_UPDATE_DATA.toException();
    }
    
    // 현재 비밀번호 확인
    if (request.hasPasswordChange() && 
        !passwordEncoder.matches(request.currentPassword(), member.getPassword())) {
        throw ErrorCode.INVALID_CURRENT_PASSWORD.toException();
    }
    
    member.updateProfile(request);
}
```

### 6.3 도메인별 예외 클래스 활용

특정 도메인에서 반복적으로 사용되는 예외는 전용 클래스로 정의하여 사용합니다:

```java
// 도메인별 예외 클래스 정의
public class MoodDiaryNotFoundException extends CustomException {
    public MoodDiaryNotFoundException() {
        super(ErrorCode.MOOD_DIARY_NOT_FOUND);
    }
}

// 사용 예시
public MoodDiaryResponse getMoodDiary(Long diaryId, Long memberId) {
    return moodDiaryRepository.findByIdAndMemberId(diaryId, memberId)
        .map(MoodDiaryResponse::from)
        .orElseThrow(MoodDiaryNotFoundException::new);
}
```

---

## 7. 새 에러 코드 추가

### 7.1 절차

1. **ErrorCode.java**에 새 코드 추가
2. 적절한 카테고리 주석 아래에 배치
3. 의미 있는 코드명과 한글 메시지 작성
4. 필요시 GlobalExceptionHandler에 HTTP 상태 매핑 추가

### 7.2 예시: 새 도메인 "알림" 추가

```java
public enum ErrorCode {
    // ... 기존 코드들 ...

    // Notification 관련 (새로 추가)
    NOTIFICATION_NOT_FOUND("NOTIFICATION_NOT_FOUND", "알림을 찾을 수 없습니다."),
    NOTIFICATION_ALREADY_READ("NOTIFICATION_ALREADY_READ", "이미 읽은 알림입니다."),
    FCM_TOKEN_INVALID("FCM_TOKEN_INVALID", "유효하지 않은 FCM 토큰입니다."),
    PUSH_SEND_FAILED("PUSH_SEND_FAILED", "푸시 알림 전송에 실패했습니다.");
    
    // ...
}
```

### 7.3 네이밍 규칙

| 패턴 | 용도 | 예시 |
|------|------|------|
| `*_NOT_FOUND` | 리소스 없음 | MEMBER_NOT_FOUND |
| `*_ALREADY_EXISTS` | 중복 | DIARY_ALREADY_EXISTS |
| `INVALID_*` | 유효성 검증 실패 | INVALID_INPUT |
| `*_FAILED` | 작업 실패 | FILE_SAVE_FAIL |
| `*_EXPIRED` | 만료 | TOKEN_EXPIRED |
| `*_MISMATCH` | 불일치 | PASSWORD_CONFIRM_MISMATCH |

---

## 🔗 관련 문서

- [커스텀 어노테이션 가이드](./ANNOTATION-GUIDE.md)
- [보안 패턴](./SECURITY-PATTERNS.md)
- [코딩 컨벤션](../CODING-CONVENTIONS.md)

---

> 📝 최종 업데이트: 2026년 1월 5일

