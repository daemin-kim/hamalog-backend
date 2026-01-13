# 🔐 Hamalog 보안 패턴

> 이 문서는 Hamalog 프로젝트의 보안 패턴과 구현 방법을 설명합니다.
> 모든 API는 이 보안 패턴을 따라 구현되어야 합니다.

---

## 📋 목차

1. [인증 아키텍처](#1-인증-아키텍처)
2. [JWT + CSRF 이중 보호](#2-jwt--csrf-이중-보호)
3. [리소스 소유권 검증](#3-리소스-소유권-검증)
4. [Rate Limiting](#4-rate-limiting)
5. [민감 데이터 암호화](#5-민감-데이터-암호화)
6. [보안 헤더](#6-보안-헤더)
7. [API 보안 체크리스트](#7-api-보안-체크리스트)

---

## 1. 인증 아키텍처

### 1.1 전체 흐름

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
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │ CORS Filter │→ │ CSRF Filter │→ │ JWT Authentication      │  │
│  │             │  │ (Redis)     │  │ Filter                  │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
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

### 1.2 인증 제외 경로

```java
// SecurityConfig.java
.authorizeHttpRequests(auth -> auth
    // 인증 없이 접근 가능
    .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
    .requestMatchers("/api/auth/refresh").permitAll()
    .requestMatchers("/api/oauth2/**").permitAll()
    .requestMatchers("/actuator/health").permitAll()
    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
    
    // 그 외 모든 요청은 인증 필요
    .anyRequest().authenticated()
)
```

---

## 2. JWT + CSRF 이중 보호

### 2.1 왜 이중 보호인가?

| 공격 유형 | JWT만 | JWT + CSRF |
|-----------|-------|------------|
| XSS (스크립트 삽입) | ⚠️ 토큰 탈취 가능 | ⚠️ 토큰 탈취 가능 |
| CSRF (교차 사이트 요청) | ✅ 헤더 필요 | ✅ 헤더 필요 |
| 세션 고정 | ✅ Stateless | ✅ Stateless |
| 토큰 재사용 | ⚠️ 가능 | ✅ CSRF 토큰 만료 |

### 2.2 토큰 구조

#### Access Token (JWT)
```
Header: { "alg": "HS512", "typ": "JWT" }
Payload: {
    "sub": "loginId",
    "memberId": 123,
    "iat": 1703404800,
    "exp": 1703408400  // 1시간
}
Signature: HMACSHA512(header + payload, secret)
```

#### Refresh Token
```
- Redis 저장 (key: "refresh:memberId:tokenId")
- TTL: 7일
- Rotation: 사용 시 새 토큰 발급
```

#### CSRF Token
```
- Redis 저장 (key: "csrf:memberId")
- TTL: 1시간 (Access Token과 동일)
- 요청마다 X-CSRF-TOKEN 헤더로 전송
```

### 2.3 구현 코드

```java
// 로그인 응답
@PostMapping("/login")
public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    AuthTokens tokens = authService.login(request);
    
    return ResponseEntity.ok()
        .header("X-CSRF-TOKEN", tokens.csrfToken())
        .body(new LoginResponse(
            tokens.accessToken(),
            tokens.refreshToken(),
            tokens.expiresIn()
        ));
}

// API 요청 시 클라이언트
fetch('/api/medication-schedule', {
    method: 'POST',
    headers: {
        'Authorization': 'Bearer ' + accessToken,
        'X-CSRF-TOKEN': csrfToken,
        'Content-Type': 'application/json'
    },
    body: JSON.stringify(data)
});
```

### 2.4 토큰 갱신

```java
@PostMapping("/refresh")
public ResponseEntity<TokenRefreshResponse> refresh(
    @RequestHeader("X-Refresh-Token") String refreshToken
) {
    // 1. Refresh Token 검증 (Redis 확인)
    // 2. 새 Access Token 발급
    // 3. Refresh Token Rotation (선택)
    // 4. 새 CSRF Token 발급
    
    AuthTokens newTokens = authService.refresh(refreshToken);
    
    return ResponseEntity.ok()
        .header("X-CSRF-TOKEN", newTokens.csrfToken())
        .body(new TokenRefreshResponse(newTokens.accessToken()));
}
```

---

## 3. 리소스 소유권 검증

### 3.1 AOP 기반 선언적 검증

```java
@GetMapping("/{id}")
@RequireResourceOwnership(
    resourceType = ResourceType.MEDICATION_SCHEDULE,
    paramName = "id"
)
public ResponseEntity<MedicationScheduleResponse> getById(@PathVariable Long id) {
    // AOP에서 이미 소유권 검증 완료
    return ResponseEntity.ok(scheduleService.findById(id));
}
```

### 3.2 검증 흐름

```
1. @RequireResourceOwnership 어노테이션 감지
2. SecurityContext에서 현재 사용자 ID 추출
3. paramName으로 리소스 ID 추출
4. resourceType에 맞는 Repository로 리소스 조회
5. 리소스.getMemberId() == 현재사용자ID 비교
6. 불일치 시 ErrorCode.FORBIDDEN 예외
```

### 3.3 리소스 타입별 검증 전략

| ResourceType | 검증 쿼리 |
|--------------|-----------|
| MEDICATION_SCHEDULE | `scheduleRepository.findById(id)` → `getMember().getMemberId()` |
| MEDICATION_RECORD | `recordRepository.findById(id)` → `getSchedule().getMember().getMemberId()` |
| MOOD_DIARY | `diaryRepository.findById(id)` → `getMember().getMemberId()` |
| MEMBER | `id == currentMemberId` (직접 비교) |

### 3.4 수동 검증 (fallback)

AOP를 사용하지 못하는 경우:

```java
private void validateOwnership(Long resourceMemberId, Long currentMemberId) {
    if (!resourceMemberId.equals(currentMemberId)) {
        throw ErrorCode.FORBIDDEN.toException();
    }
}

public MedicationScheduleResponse update(Long id, UpdateRequest request) {
    MedicationSchedule schedule = scheduleRepository.findById(id)
        .orElseThrow(ErrorCode.MEDICATION_SCHEDULE_NOT_FOUND::toException);
    
    Long currentMemberId = SecurityUtil.getCurrentMemberId();
    validateOwnership(schedule.getMember().getMemberId(), currentMemberId);
    
    // 업데이트 로직...
}
```

---

## 4. Rate Limiting

> ✅ **구현 완료** - Redis 기반 Rate Limiting 및 Brute Force IP 차단 기능이 구현되어 있습니다.

### 4.1 Rate Limiting 구현

`RateLimitingService`와 `RateLimitingFilter`를 통해 Redis 기반 Sliding Window 알고리즘으로 구현되어 있습니다.

| 엔드포인트 | 제한 | 기준 |
|------------|------|------|
| `/api/auth/**` | 5회/분, 20회/시간 | IP |
| 일반 API | 60회/분, 1000회/시간 | IP |

### 4.2 Brute Force IP 차단

`SecurityEventMonitor`에서 인증 실패 횟수를 추적하고, 임계값 초과 시 자동으로 IP를 차단합니다.

```java
// SecurityEventMonitor.java
private static final int MAX_LOGIN_FAILURES = 5;      // 경고 시작
private static final int BRUTE_FORCE_THRESHOLD = 10;  // IP 차단
private static final Duration FAILURE_WINDOW = Duration.ofMinutes(15);

// Brute Force 감지 시 1시간 동안 IP 차단
private void blockIpAddress(String clientIp, Duration duration) {
    String blockKey = "blocked_ip:" + clientIp;
    redisTemplate.opsForValue().set(blockKey, "BRUTE_FORCE", duration);
    log.info("IP {} blocked for {} due to brute force attack", clientIp, duration);
}

// IP 차단 여부 확인
public boolean isIpBlocked(String clientIp) {
    String blockKey = "blocked_ip:" + clientIp;
    return Boolean.TRUE.equals(redisTemplate.hasKey(blockKey));
}
```

### 4.3 리스크 레벨 판단

| 실패 횟수 | 리스크 레벨 | 조치 |
|-----------|-------------|------|
| 1-2회 | LOW | 로그만 기록 |
| 3-4회 | MEDIUM | 경고 로그 |
| 5-9회 | HIGH | 에러 로그, 모니터링 알림 |
| 10회 이상 | CRITICAL | IP 1시간 차단, 보안 이벤트 발행 |

### 4.4 응답 헤더

```
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 55
X-RateLimit-Reset: 1703404860
```

---

## 5. 민감 데이터 암호화

### 5.1 암호화 대상

| 데이터 | 암호화 방식 | 저장 위치 |
|--------|-------------|-----------|
| 비밀번호 | BCrypt (단방향) | DB |
| Refresh Token | UUID (Redis TTL) | Redis |
| 마음 일기 내용 | AES-256 (양방향) | DB |
| 부작용 상세 | AES-256 (양방향) | DB |

### 5.2 AES 암호화 서비스

```java
@Service
public class EncryptionService {

    @Value("${app.encryption.key}")
    private String encryptionKey;
    
    public String encrypt(String plainText) {
        // AES-256-GCM 암호화
    }
    
    public String decrypt(String cipherText) {
        // AES-256-GCM 복호화
    }
}
```

### 5.3 Entity 적용

```java
@Entity
public class MoodDiary {
    
    @Column(columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String content;  // 자동 암/복호화
}
```

---

## 6. 보안 헤더

### 6.1 SecurityConfig 설정

```java
.headers(headers -> headers
    .contentSecurityPolicy(csp -> csp
        .policyDirectives("default-src 'self'; script-src 'self'"))
    .frameOptions(frame -> frame.deny())
    .xssProtection(xss -> xss.enable())
    .contentTypeOptions(Customizer.withDefaults())
    .referrerPolicy(referrer -> referrer
        .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
)
```

### 6.2 응답 헤더

```
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Referrer-Policy: strict-origin-when-cross-origin
Content-Security-Policy: default-src 'self'
```

---

## 7. API 보안 체크리스트

### 7.1 새 API 추가 시 체크리스트

- [ ] 인증 필요 여부 확인 (`SecurityConfig`에 제외 경로 추가?)
- [ ] `@RequireResourceOwnership` 적용 (리소스 접근 API)
- [ ] `@Valid` 적용 (요청 DTO 검증)
- [ ] 민감 데이터 로깅 제외 (`@NoLogging` 또는 마스킹)
- [ ] 에러 메시지에 민감 정보 미포함

### 7.2 코드 리뷰 체크리스트

```java
// ❌ 잘못된 예
@GetMapping("/{id}")
public ResponseEntity<?> get(@PathVariable Long id) {
    return ResponseEntity.ok(service.findById(id));
    // 소유권 검증 누락!
}

// ✅ 올바른 예
@GetMapping("/{id}")
@RequireResourceOwnership(resourceType = ResourceType.MEDICATION_SCHEDULE, paramName = "id")
public ResponseEntity<?> get(@PathVariable Long id) {
    return ResponseEntity.ok(service.findById(id));
}
```

### 7.3 보안 테스트

```java
@Test
@DisplayName("다른 사용자의 리소스 접근 시 403 반환")
void accessOtherUserResource_forbidden() {
    // given
    Long otherUserResourceId = createResourceForOtherUser();
    
    // when & then
    mockMvc.perform(get("/api/medication-schedule/{id}", otherUserResourceId)
            .header("Authorization", "Bearer " + myAccessToken)
            .header("X-CSRF-TOKEN", myCsrfToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
}
```

---

## 📚 관련 문서

- [ADR-0002: JWT + CSRF 이중 보호](../adr/0002-jwt-csrf-dual-protection.md)
- [커스텀 어노테이션 가이드](./ANNOTATION-GUIDE.md)
- [에러 처리 패턴](./ERROR-HANDLING.md)

---

> 📝 최종 업데이트: 2026년 1월 13일

