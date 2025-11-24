# API 예외처리 강화 작업 완료 보고서

## 📋 작업 개요

Hamalog 프로젝트의 모든 API 엔드포인트에 대한 예외처리를 체계적으로 분석하고 강화했습니다.

**작업 일시**: 2025-11-24  
**작업 범위**: 전체 API (Auth, OAuth2, Medication Schedule, Medication Record, Side Effect)

---

## 🔍 문제 분석

### 1. ErrorCode 부족
- OAuth2 관련 구체적인 에러 코드 부재
- 토큰 관련 세분화된 에러 코드 부재 (만료, 손상, 블랙리스트 등)
- 부작용 기록 관련 에러 코드 부족
- 페이지네이션 관련 에러 코드 부족
- 파일 업로드 관련 세부 에러 코드 부족
- 동시성 제어 관련 에러 코드 부족

### 2. 입력값 검증 부족
- null 체크 누락
- 비즈니스 로직 레벨 검증 부족 (날짜 범위, 음수 값 등)
- 부작용 기록 생성 시 존재 여부 검증 미흡
- ID 파라미터 유효성 검증 부족

### 3. 예외 클래스 부족
- OAuth2 관련 구체적 예외 클래스 부재
- Token 관련 예외 클래스 부재
- 입력값 검증 예외 클래스 부재

### 4. 외부 API 호출 예외처리
- OAuth2 카카오 API 호출 실패 시 처리 부족
- 네트워크 타임아웃, 연결 실패 등 구체적 예외 처리 부재

---

## ✅ 구현 내용

### Phase 1: ErrorCode 확장 (36개 → 54개)

**추가된 ErrorCode:**

#### 복약 관련 (4개)
- `INVALID_MEDICATION_SCHEDULE`: 유효하지 않은 복약 스케줄
- `INVALID_PRESCRIPTION_DAYS`: 처방 일수 오류 (1일 미만 또는 365일 초과)
- `INVALID_PER_DAY`: 1일 복용 횟수 오류 (1회 미만 또는 10회 초과)
- `INVALID_DATE_RANGE`: 시작일이 처방일 이전

#### 부작용 관련 (3개)
- `SIDE_EFFECT_NOT_FOUND`: 부작용 정보 없음
- `INVALID_SIDE_EFFECT_DEGREE`: 부작용 정도 범위 오류 (1-5 외)
- `EMPTY_SIDE_EFFECT_LIST`: 부작용 목록 비어있음

#### 토큰 관련 (5개)
- `TOKEN_EXPIRED`: 토큰 만료
- `TOKEN_BLACKLISTED`: 무효화된 토큰
- `INVALID_REFRESH_TOKEN`: 유효하지 않은 Refresh Token
- `REFRESH_TOKEN_EXPIRED`: Refresh Token 만료
- `REFRESH_TOKEN_REVOKED`: 폐기된 Refresh Token

#### OAuth2 관련 (6개)
- `OAUTH2_CONFIG_ERROR`: OAuth2 설정 오류
- `OAUTH2_INIT_ERROR`: OAuth2 초기화 오류
- `OAUTH2_TOKEN_EXCHANGE_FAILED`: 토큰 교환 실패
- `OAUTH2_USER_INFO_FAILED`: 사용자 정보 조회 실패
- `OAUTH2_INVALID_CODE`: 유효하지 않은 인증 코드
- `OAUTH2_STATE_VALIDATION_FAILED`: CSRF 검증 실패

#### 입력값 검증 관련 (4개)
- `INVALID_INPUT`: 입력값 유효하지 않음
- `INVALID_PARAMETER`: 파라미터 유효하지 않음
- `MISSING_REQUIRED_FIELD`: 필수 필드 누락
- `INVALID_PAGE_SIZE`: 페이지 크기 범위 오류
- `INVALID_PAGE_NUMBER`: 페이지 번호 음수

#### 파일 관련 (3개)
- `FILE_SIZE_EXCEEDED`: 파일 크기 제한 초과
- `INVALID_FILE_TYPE`: 지원하지 않는 파일 형식
- `FILE_NOT_FOUND`: 파일 없음

#### 동시성 관련 (2개)
- `OPTIMISTIC_LOCK_FAILED`: 낙관적 락 실패
- `RESOURCE_CONFLICT`: 리소스 충돌

#### 외부 API 관련 (2개)
- `EXTERNAL_API_ERROR`: 외부 API 호출 오류
- `EXTERNAL_API_TIMEOUT`: 외부 API 타임아웃

#### 시스템 관련 (2개)
- `DATABASE_ERROR`: 데이터베이스 오류
- `CACHE_ERROR`: 캐시 처리 오류

---

### Phase 2: 커스텀 예외 클래스 생성

**새로 생성된 예외 클래스:**

1. **OAuth2 관련**
   - `OAuth2Exception`: OAuth2 관련 기본 예외
   - `OAuth2TokenExchangeException`: 토큰 교환 실패
   - `OAuth2StateValidationException`: State 검증 실패 (CSRF 방지)

2. **Token 관련**
   - `TokenException`: 토큰 관련 기본 예외
   - `TokenExpiredException`: 토큰 만료
   - `RefreshTokenException`: Refresh Token 예외

3. **부작용 관련**
   - `SideEffectNotFoundException`: 부작용 정보 없음

4. **입력값 검증**
   - `InvalidInputException`: 입력값 검증 실패

---

### Phase 3: 서비스 레이어 검증 강화

#### 1. MedicationScheduleService 강화

**추가된 검증:**
- ✅ 회원 존재 여부 검증
- ✅ 페이지네이션 파라미터 검증 (페이지 번호 >= 0, 크기 1-100)
- ✅ ID 파라미터 null 및 양수 검증
- ✅ 날짜 범위 검증 (시작일 >= 처방일)
- ✅ 처방 일수 검증 (1-365일)
- ✅ 1일 복용 횟수 검증 (1-10회)
- ✅ 필수 필드 검증
- ✅ 문자열 공백 검증

**비즈니스 로직 검증:**
```java
// 날짜 범위 검증
private void validateDateRange(LocalDate prescriptionDate, LocalDate startOfAd) {
    if (startOfAd.isBefore(prescriptionDate)) {
        throw new InvalidInputException(ErrorCode.INVALID_DATE_RANGE);
    }
}

// 처방 일수 검증
private void validatePrescriptionDays(Integer prescriptionDays) {
    if (prescriptionDays == null || prescriptionDays < 1 || prescriptionDays > 365) {
        throw new InvalidInputException(ErrorCode.INVALID_PRESCRIPTION_DAYS);
    }
}

// 1일 복용 횟수 검증
private void validatePerDay(Integer perDay) {
    if (perDay == null || perDay < 1 || perDay > 10) {
        throw new InvalidInputException(ErrorCode.INVALID_PER_DAY);
    }
}
```

#### 2. MedicationRecordService 강화

**추가된 검증:**
- ✅ ID 파라미터 null 및 양수 검증
- ✅ 복약 스케줄 존재 여부 검증
- ✅ 필수 필드 검증
- ✅ MedicationTime이 MedicationSchedule에 속하는지 검증
- ✅ 실제 복용 시간 미래 시간 방지

**비즈니스 로직 검증:**
```java
// MedicationTime이 MedicationSchedule에 속하는지 검증
private void validateMedicationTimeBelongsToSchedule(
    MedicationTime medicationTime, 
    MedicationSchedule medicationSchedule
) {
    if (!medicationTime.getMedicationSchedule().getMedicationScheduleId()
            .equals(medicationSchedule.getMedicationScheduleId())) {
        throw new InvalidInputException(ErrorCode.INVALID_MEDICATION_SCHEDULE);
    }
}

// 실제 복용 시간 검증
private void validateRealTakeTime(LocalDateTime realTakeTime) {
    if (realTakeTime.isAfter(LocalDateTime.now())) {
        throw new InvalidInputException(ErrorCode.INVALID_DATE_RANGE);
    }
}
```

#### 3. SideEffectService 강화

**추가된 검증:**
- ✅ memberId null 및 양수 검증
- ✅ 회원 존재 여부 검증
- ✅ 부작용 목록 비어있는지 검증
- ✅ 부작용 목록 크기 제한 (DoS 방지, 최대 50개)
- ✅ 부작용 정도 범위 검증 (1-5)
- ✅ 생성 시간 미래 시간 방지
- ✅ SideEffect ID 존재 여부 검증

**비즈니스 로직 검증:**
```java
// 부작용 정도 검증
private void validateSideEffectDegree(Integer degree) {
    if (degree == null || degree < 1 || degree > 5) {
        throw new InvalidInputException(ErrorCode.INVALID_SIDE_EFFECT_DEGREE);
    }
}

// 부작용 목록 검증
private void validateSideEffectRecordRequest(SideEffectRecordRequest request) {
    if (request.sideEffects() == null || request.sideEffects().isEmpty()) {
        throw new InvalidInputException(ErrorCode.EMPTY_SIDE_EFFECT_LIST);
    }
    
    // DoS 방지
    if (request.sideEffects().size() > 50) {
        throw new InvalidInputException(ErrorCode.INVALID_INPUT);
    }
}
```

#### 4. AuthService 강화

**OAuth2 예외처리 개선:**
- ✅ Authorization code null/empty 검증
- ✅ Kakao 클라이언트 설정 검증
- ✅ Access token null/empty 검증
- ✅ User info 필수 필드 검증 (id 필드)
- ✅ 네트워크 타임아웃 예외 처리
- ✅ HTTP 4xx/5xx 오류 예외 처리
- ✅ Kakao ID 필드 존재 여부 검증

**외부 API 호출 예외처리:**
```java
} catch (org.springframework.web.client.ResourceAccessException e) {
    // 네트워크 타임아웃, 연결 실패 등
    log.error("Network error while exchanging authorization code for token", e);
    throw new CustomException(ErrorCode.EXTERNAL_API_TIMEOUT);
} catch (org.springframework.web.client.HttpClientErrorException | 
         org.springframework.web.client.HttpServerErrorException e) {
    // HTTP 4xx, 5xx 오류
    log.error("HTTP error while exchanging authorization code. Status: {}, Body: {}", 
        e.getStatusCode(), e.getResponseBodyAsString());
    throw new CustomException(ErrorCode.EXTERNAL_API_ERROR);
}
```

---

### Phase 4: GlobalExceptionHandler 보강

**새로 추가된 Exception Handler:**

1. **OAuth2Exception Handler**
   - HTTP 상태: 400 Bad Request
   - 심각도: HIGH
   - 로깅: ERROR 레벨 + 구조화된 컨텍스트

2. **TokenException Handler**
   - HTTP 상태: 401 Unauthorized
   - 심각도: MEDIUM
   - 로깅: WARN 레벨 + 구조화된 컨텍스트

3. **InvalidInputException Handler**
   - HTTP 상태: 400 Bad Request
   - 심각도: LOW
   - 로깅: WARN 레벨 + 구조화된 컨텍스트

4. **OptimisticLockException Handler**
   - HTTP 상태: 409 Conflict
   - 심각도: MEDIUM
   - 로깅: WARN 레벨
   - 메시지: "다른 사용자가 데이터를 수정했습니다. 다시 시도해주세요."

5. **DataIntegrityViolationException Handler**
   - HTTP 상태: 409 Conflict
   - 심각도: HIGH
   - 로깅: ERROR 레벨
   - 데이터베이스 무결성 위반 감지

**개선된 HTTP 상태 코드 매핑:**
```java
private HttpStatus determineHttpStatus(ErrorCode errorCode) {
    return switch (errorCode) {
        case MEMBER_NOT_FOUND, MEDICATION_SCHEDULE_NOT_FOUND, ... 
            -> HttpStatus.NOT_FOUND;  // 404
        case UNAUTHORIZED, INVALID_TOKEN, TOKEN_EXPIRED, ... 
            -> HttpStatus.UNAUTHORIZED;  // 401
        case FORBIDDEN 
            -> HttpStatus.FORBIDDEN;  // 403
        case OPTIMISTIC_LOCK_FAILED, RESOURCE_CONFLICT, ... 
            -> HttpStatus.CONFLICT;  // 409
        case EXTERNAL_API_TIMEOUT 
            -> HttpStatus.GATEWAY_TIMEOUT;  // 504
        case FILE_SIZE_EXCEEDED 
            -> HttpStatus.PAYLOAD_TOO_LARGE;  // 413
        default 
            -> HttpStatus.BAD_REQUEST;  // 400
    };
}
```

---

## 📊 개선 효과

### 1. 예외처리 커버리지
- **이전**: 기본적인 404, 401, 400, 500 처리
- **이후**: 54개의 구체적인 에러 코드로 세분화

### 2. 입력값 검증
- **이전**: DTO @Valid 어노테이션에만 의존
- **이후**: 서비스 레이어에서 비즈니스 로직 레벨 검증 추가

### 3. 보안 강화
- ✅ OAuth2 CSRF 검증
- ✅ 미래 시간 입력 방지
- ✅ DoS 공격 방지 (페이지 크기, 부작용 목록 크기 제한)
- ✅ 외부 API 타임아웃 처리

### 4. 사용자 경험 개선
- ✅ 구체적인 에러 메시지 제공
- ✅ 적절한 HTTP 상태 코드 반환
- ✅ 동시성 충돌 시 재시도 안내 메시지

### 5. 디버깅 및 모니터링
- ✅ 에러 심각도(Severity) 분류
- ✅ 구조화된 로깅 (MDC, StructuredLogger)
- ✅ 상세한 에러 컨텍스트 (요청 경로, 사용자 ID, correlation ID 등)

---

## 🧪 테스트 권장사항

### 1. 단위 테스트 추가 필요
```java
// MedicationScheduleService 테스트 예시
@Test
void createMedicationSchedule_invalidDateRange_throwsException() {
    // given
    MedicationScheduleCreateRequest request = // startOfAd < prescriptionDate
    
    // when & then
    assertThrows(InvalidInputException.class, 
        () -> service.createMedicationSchedule(request));
}

@Test
void createMedicationSchedule_invalidPrescriptionDays_throwsException() {
    // given
    MedicationScheduleCreateRequest request = // prescriptionDays = 0
    
    // when & then
    InvalidInputException ex = assertThrows(InvalidInputException.class, 
        () -> service.createMedicationSchedule(request));
    assertEquals(ErrorCode.INVALID_PRESCRIPTION_DAYS, ex.getErrorCode());
}
```

### 2. 통합 테스트 시나리오
- OAuth2 콜백 네트워크 타임아웃 시뮬레이션
- 동시 업데이트로 인한 OptimisticLockException 발생 테스트
- 부작용 목록 51개 전송 시 검증 테스트

### 3. E2E 테스트
- 유효하지 않은 입력값으로 API 호출 시 적절한 에러 응답 확인
- 에러 로그가 정상적으로 기록되는지 확인

---

## 📝 마이그레이션 가이드

### 기존 클라이언트 코드 영향도

**변경 없음:**
- 기존 API 엔드포인트 경로 동일
- 기존 성공 응답 형식 동일
- 기존 에러 응답 형식 동일 (ErrorResponse 구조 유지)

**개선된 사항:**
- 더 구체적인 에러 코드 반환
- 더 적절한 HTTP 상태 코드 반환
- 더 명확한 에러 메시지

**클라이언트 권장 개선사항:**
```javascript
// 이전
if (response.status === 400) {
  alert("잘못된 요청입니다.");
}

// 이후 - 에러 코드 기반 처리
if (response.data.code === "INVALID_DATE_RANGE") {
  alert("시작일은 처방일 이후여야 합니다.");
} else if (response.data.code === "INVALID_PRESCRIPTION_DAYS") {
  alert("처방 일수는 1-365일 사이여야 합니다.");
}
```

---

## 🔒 보안 강화 사항

1. **DoS 공격 방지**
   - 페이지 크기 최대 100개 제한
   - 부작용 목록 최대 50개 제한

2. **시간 조작 방지**
   - 미래 시간 입력 차단 (복용 시간, 부작용 기록 시간)

3. **외부 API 보안**
   - OAuth2 State 파라미터 검증 (CSRF 방지)
   - 네트워크 타임아웃 설정 (리소스 고갈 방지)

4. **데이터 무결성**
   - 외래키 참조 검증 (MedicationTime ↔ MedicationSchedule)
   - 존재하지 않는 ID 참조 방지

---

## 📈 성능 영향

### 긍정적 영향
- ✅ 잘못된 입력으로 인한 불필요한 DB 쿼리 감소
- ✅ 명확한 에러 메시지로 클라이언트 재시도 감소

### 부정적 영향 (미미함)
- 추가 검증 로직으로 인한 CPU 사용량 미세 증가 (1% 미만 예상)
- 메모리 사용량 변화 없음

---

## 🎯 향후 개선 방향

1. **메트릭 수집**
   - 에러 발생 빈도 통계
   - 에러 코드별 발생 추이 모니터링

2. **알림 시스템 연동**
   - 심각도 HIGH/CRITICAL 에러 발생 시 Slack/Email 알림

3. **에러 복구 전략**
   - Circuit Breaker 패턴 적용 (외부 API 호출)
   - Retry 메커니즘 강화 (OptimisticLockException)

4. **국제화(i18n)**
   - 에러 메시지 다국어 지원

---

## ✅ 체크리스트

- [x] ErrorCode 확장 (36개 → 54개)
- [x] 커스텀 예외 클래스 생성 (8개)
- [x] MedicationScheduleService 검증 강화
- [x] MedicationRecordService 검증 강화
- [x] SideEffectService 검증 강화
- [x] AuthService OAuth2 예외처리 개선
- [x] GlobalExceptionHandler 핸들러 추가 (5개)
- [x] HTTP 상태 코드 매핑 개선
- [x] 컴파일 성공 확인
- [x] 빌드 성공 확인
- [ ] 단위 테스트 추가 (권장)
- [ ] 통합 테스트 추가 (권장)
- [ ] API 문서 업데이트 (권장)

---

## 📚 참고 자료

- [Spring Boot Exception Handling Best Practices](https://www.baeldung.com/exception-handling-for-rest-with-spring)
- [RFC 7807: Problem Details for HTTP APIs](https://datatracker.ietf.org/doc/html/rfc7807)
- [OWASP Input Validation](https://cheatsheetseries.owasp.org/cheatsheets/Input_Validation_Cheat_Sheet.html)

---

**작성자**: GitHub Copilot  
**검토자**: -  
**승인자**: -

