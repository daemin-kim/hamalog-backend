# 01. JWT + CSRF 이중 보호

> **SPA 환경에서 Stateless 아키텍처를 유지하면서 CSRF 공격을 완벽히 차단하는 이중 보호 패턴**

---

## 📋 목차

1. [문제 상황](#1-문제-상황-problem)
2. [해결 전략](#2-해결-전략-solution-strategy)
3. [구현 상세](#3-구현-상세-implementation)
4. [효과 및 검증](#4-효과-및-검증-results)
5. [면접 대비 Q&A](#5-면접-대비-qa)

---

## 1. 문제 상황 (Problem)

### 1.1 CSRF 공격이란?

**CSRF(Cross-Site Request Forgery)**는 사용자가 인증된 상태에서 악의적인 웹사이트가 사용자 대신 요청을 보내는 공격입니다.

```
┌─────────────────────────────────────────────────────────────────┐
│  공격 시나리오                                                   │
│                                                                  │
│  1. 사용자가 hamalog.com에 로그인 (쿠키에 토큰 저장)             │
│  2. 사용자가 악성 사이트 evil.com 방문                           │
│  3. evil.com에서 hamalog.com/api/transfer?to=hacker 요청 전송   │
│  4. 브라우저가 자동으로 쿠키 첨부 → 인증 성공 → 공격 성공        │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 JWT만 사용할 때의 문제점

| 저장 방식 | CSRF 취약성 | XSS 취약성 | 비고 |
|----------|-------------|-----------|------|
| **Cookie** | ⚠️ 취약 | ✅ HttpOnly로 방어 | 브라우저가 자동 첨부 |
| **LocalStorage** | ✅ 안전 | ⚠️ 취약 | JS로 접근 가능 |
| **SessionStorage** | ✅ 안전 | ⚠️ 취약 | JS로 접근 가능 |

**딜레마**: Cookie는 CSRF에 취약하고, Storage는 XSS에 취약합니다.

### 1.3 Hamalog에서의 실제 위협

의료 정보를 다루는 Hamalog에서 CSRF 공격이 성공하면:
- 사용자의 복약 기록 무단 삭제/수정
- 민감한 건강 정보 유출
- 알림 설정 변경으로 복약 누락 유도

---

## 2. 해결 전략 (Solution Strategy)

### 2.1 고려한 대안들

| 방식 | 장점 | 단점 | 채택 여부 |
|------|------|------|----------|
| **Session 기반** | 구현 단순 | Stateful, 수평 확장 어려움 | ❌ |
| **JWT Only (Header)** | Stateless | CSRF 완전 방어 불가 (쿠키 혼용 시) | ❌ |
| **SameSite Cookie** | 구현 단순 | 구형 브라우저 미지원, 완전하지 않음 | ❌ |
| **JWT + CSRF 이중 보호** | Stateless + CSRF 완전 방어 | 구현 복잡도 증가 | ✅ |

### 2.2 최종 선택: JWT + CSRF 이중 보호

```
┌─────────────────────────────────────────────────────────────────┐
│                    인증 흐름                                     │
│                                                                  │
│  1. 로그인 전 CSRF 토큰 요청 (GET /auth/csrf-token)              │
│     → Redis에 저장 (Key: csrf:{sessionId}, TTL: 60분)           │
│                                                                  │
│  2. 로그인 요청 (POST /auth/login)                               │
│     → Header: X-CSRF-TOKEN 포함                                 │
│     → Response: Access Token (15분) + Refresh Token (7일)       │
│                                                                  │
│  3. API 요청                                                     │
│     → Header: Authorization: Bearer {JWT}                       │
│     → Header: X-CSRF-TOKEN (상태 변경 요청만)                    │
│                                                                  │
│  4. 토큰 갱신 (POST /auth/refresh)                              │
│     → Refresh Token Rotation 적용                               │
└─────────────────────────────────────────────────────────────────┘
```

### 2.3 왜 이 방식인가?

1. **Stateless 유지**: JWT로 서버 세션 불필요, 수평 확장 가능
2. **CSRF 완전 방어**: 악성 사이트는 CSRF 토큰을 알 수 없음
3. **토큰 탈취 대응**: Refresh Token Rotation으로 피해 시간 최소화
4. **Redis 활용**: 이미 캐시용으로 사용 중인 Redis 재활용

---

## 3. 구현 상세 (Implementation)

### 3.1 CSRF 토큰 관리 (CsrfTokenProvider.java)

```java
@Component
public class CsrfTokenProvider {
    
    // ============================================================
    // 상수 정의
    // ============================================================
    
    private static final int TOKEN_LENGTH = 32;        // 256비트 토큰
    private static final Duration TOKEN_TTL = Duration.ofMinutes(60);  // 60분 유효
    private static final String REDIS_KEY_PREFIX = "csrf:";  // Redis 키 접두사
    
    // SecureRandom: 암호학적으로 안전한 난수 생성기
    // Math.random()이나 Random과 달리 예측 불가능
    private final SecureRandom secureRandom = new SecureRandom();
    
    // Redis 연결 (Optional: Redis 장애 시 fallback 지원)
    private final RedisTemplate<String, Object> redisTemplate;
    
    // Redis 장애 시 메모리 기반 fallback 저장소
    // ConcurrentHashMap: 멀티스레드 환경에서 안전한 HashMap
    private final ConcurrentMap<String, TokenRecord> fallbackStore = new ConcurrentHashMap<>();
    
    // ============================================================
    // 토큰 생성
    // ============================================================
    
    public String generateToken(String sessionId) {
        // 세션 ID 유효성 검증 - 빈 값 방지
        if (!StringUtils.hasText(sessionId)) {
            throw new IllegalArgumentException("Session ID는 필수입니다");
        }
        
        // 1. 암호학적으로 안전한 무작위 토큰 생성
        String token = createRandomToken();
        
        // 2. Redis에 저장 시도
        if (storeInRedis(sessionId, token)) {
            // Redis 저장 성공 시 fallback 정리
            fallbackStore.remove(sessionId);
        } else {
            // Redis 장애 시 메모리 fallback 사용
            storeInFallback(sessionId, token);
        }
        
        return token;
    }
    
    // ============================================================
    // 토큰 검증
    // ============================================================
    
    public boolean validateToken(String sessionId, String token) {
        // 입력값 null/빈값 체크
        if (!StringUtils.hasText(sessionId) || !StringUtils.hasText(token)) {
            return false;
        }
        
        // Redis에서 검증 시도
        if (validateWithRedis(sessionId, token)) {
            return true;
        }
        
        // Redis 장애 시 fallback에서 검증
        return readFromFallback(sessionId)
                .map(storedToken -> constantTimeEquals(storedToken, token))
                .orElse(false);
    }
    
    // ============================================================
    // 보안 핵심: 상수 시간 비교
    // ============================================================
    
    /**
     * Timing Attack 방어를 위한 상수 시간 문자열 비교
     * 
     * 일반 equals()는 첫 번째 다른 문자에서 즉시 반환하므로
     * 실행 시간 차이로 문자열을 한 글자씩 추측할 수 있음.
     * 
     * 이 메서드는 항상 전체 문자열을 비교하여 실행 시간이 일정함.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        
        int result = 0;
        // XOR 연산: 같으면 0, 다르면 0이 아닌 값
        // OR로 누적: 하나라도 다르면 result != 0
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
    
    // ============================================================
    // 토큰 생성 (내부 메서드)
    // ============================================================
    
    private String createRandomToken() {
        // 32바이트 = 256비트 무작위 바이트 배열
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        
        // URL-safe Base64 인코딩 (패딩 제거)
        // 예: "xV7k9mP2qR5tW8yB3nM6vC1sE4uH0jL2"
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
    
    // ============================================================
    // Redis 저장/조회 (장애 대응 포함)
    // ============================================================
    
    private boolean storeInRedis(String sessionId, String token) {
        if (redisTemplate == null) {
            return false;
        }
        
        try {
            // Redis에 토큰 저장 (TTL 60분)
            // Key: "csrf:sessionId123", Value: "토큰값"
            redisTemplate.opsForValue().set(redisKey(sessionId), token, TOKEN_TTL);
            return true;
        } catch (RuntimeException ex) {
            // Redis 장애 시 로그 남기고 fallback 전환
            log.warn("[CSRF] Redis write 실패 – fallback 전환 (sessionId={})", sessionId, ex);
            return false;
        }
    }
    
    private boolean validateWithRedis(String sessionId, String token) {
        if (redisTemplate == null) {
            return false;
        }
        
        try {
            Object storedValue = redisTemplate.opsForValue().get(redisKey(sessionId));
            
            // Java 21 Pattern Matching: instanceof + 타입 캐스팅 한 번에
            if (storedValue instanceof String storedToken) {
                return constantTimeEquals(storedToken, token);
            }
            return false;
        } catch (RuntimeException ex) {
            log.warn("[CSRF] Redis read 실패 – fallback 검증 사용 (sessionId={})", sessionId, ex);
            return false;
        }
    }
    
    private String redisKey(String sessionId) {
        return REDIS_KEY_PREFIX + sessionId;  // "csrf:sessionId123"
    }
}
```

### 3.2 JWT 토큰 생성/검증 (JwtTokenProvider.java)

```java
@Component
public class JwtTokenProvider {
    
    private SecretKey secretKey;
    private final long validityInMilliseconds;  // 토큰 유효 시간
    private final TokenBlacklistService tokenBlacklistService;  // 로그아웃된 토큰 관리
    private final String issuer;  // 토큰 발급자 (hamalog)
    
    // ============================================================
    // 초기화: 프로덕션 환경에서 필수 검증
    // ============================================================
    
    @PostConstruct
    protected void init() {
        boolean isProduction = Arrays.asList(environment.getActiveProfiles())
                                     .contains("prod");
        
        // 환경변수 우선순위: JWT_SECRET 환경변수 > application.properties
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.trim().isEmpty()) {
            secret = fallbackSecret;
        }
        
        // 프로덕션에서 비밀키 누락 시 애플리케이션 시작 차단
        if ((secret == null || secret.trim().isEmpty()) && isProduction) {
            throw new IllegalStateException(
                "⛔ JWT 비밀키가 설정되지 않았습니다. 프로덕션 환경에서는 반드시 설정해야 합니다.\n" +
                "해결 방법: export JWT_SECRET=$(openssl rand -base64 32)"
            );
        }
        
        // 키 길이 검증: 최소 256비트(32바이트) 필요
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                "JWT 비밀키는 최소 256비트(32바이트)여야 합니다. 현재: " + 
                (keyBytes.length * 8) + "비트"
            );
        }
        
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }
    
    // ============================================================
    // 토큰 생성
    // ============================================================
    
    public String createToken(String loginId, Long memberId, Map<String, Object> extraClaims) {
        // memberId 필수 검증
        if (memberId == null) {
            throw new IllegalArgumentException("memberId must not be null");
        }
        
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityInMilliseconds);
        
        JwtBuilder builder = Jwts.builder()
                .setSubject(loginId)          // 사용자 식별자 (이메일)
                .setIssuedAt(now)             // 발급 시간
                .setExpiration(expiry)        // 만료 시간
                .setIssuer(issuer)            // 발급자 ("hamalog")
                .claim("memberId", memberId); // 커스텀 클레임
        
        // 추가 클레임이 있으면 포함 (memberId 중복 방지)
        if (extraClaims != null && !extraClaims.isEmpty()) {
            Map<String, Object> sanitizedClaims = new HashMap<>(extraClaims);
            sanitizedClaims.remove("memberId");  // 중복 제거
            builder.addClaims(sanitizedClaims);
        }
        
        // HMAC-SHA256으로 서명
        return builder.signWith(secretKey).compact();
    }
    
    // ============================================================
    // 토큰 검증
    // ============================================================
    
    public boolean validateToken(String token) {
        // 1. 블랙리스트 확인 (로그아웃된 토큰)
        if (tokenBlacklistService.isTokenBlacklisted(token)) {
            log.info("JWT 토큰이 블랙리스트에 있습니다");
            return false;
        }
        
        try {
            // 2. 서명 검증 + 만료 시간 검증 + 발급자 검증
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.info("JWT 만료됨");
        } catch (UnsupportedJwtException e) {
            log.warn("JWT 지원하지 않는 형식");
        } catch (MalformedJwtException e) {
            log.warn("JWT 위조 또는 변조 가능");
        } catch (SignatureException | IllegalArgumentException e) {
            log.warn("JWT 서명 오류 및 잘못된 토큰");
        }
        return false;
    }
    
    private Claims parseClaims(String token) {
        Claims claims = Jwts.parser()
                .clockSkewSeconds(60)        // 시계 오차 허용 (60초)
                .verifyWith(secretKey)       // 서명 검증
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        // 발급자 검증: 다른 서비스에서 발급한 토큰 거부
        if (!issuer.equals(claims.getIssuer())) {
            throw new IllegalStateException("Unexpected token issuer: " + claims.getIssuer());
        }
        
        return claims;
    }
}
```

### 3.3 Security Filter Chain 구성

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF: Spring Security 기본 비활성화 (커스텀 구현 사용)
            .csrf(AbstractHttpConfigurer::disable)
            
            // Stateless 세션 (JWT 사용)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 필터 순서: CORS → CSRF 검증 → JWT 인증
            .addFilterBefore(corsFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(csrfValidationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // 경로별 인가 규칙
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/login", "/auth/register", "/auth/csrf-token").permitAll()
                .requestMatchers("/oauth2/**").permitAll()
                .anyRequest().authenticated()
            );
        
        return http.build();
    }
}
```

---

## 4. 효과 및 검증 (Results)

### 4.1 보안 효과

| 공격 유형 | 방어 여부 | 방어 메커니즘 |
|----------|----------|--------------|
| **CSRF** | ✅ 완전 방어 | X-CSRF-TOKEN 헤더 필수 |
| **Session Hijacking** | ✅ 방어 | Stateless JWT, 세션 없음 |
| **Token Replay** | ✅ 방어 | 블랙리스트 + Rotation |
| **Timing Attack** | ✅ 방어 | 상수 시간 비교 |

### 4.2 아키텍처 효과

| 지표 | 개선 효과 |
|------|----------|
| **수평 확장** | 서버 간 세션 동기화 불필요 |
| **Redis 장애 대응** | Fallback 저장소로 서비스 지속 |
| **토큰 탈취 시 피해 시간** | 최대 15분 (Access Token 만료) |

### 4.3 검증 방법

```java
@Test
@DisplayName("CSRF 토큰 없이 POST 요청 시 403 반환")
void csrfProtection_withoutToken_returns403() throws Exception {
    mockMvc.perform(post("/medication-schedule")
            .header("Authorization", "Bearer " + validJwt)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{...}"))
        .andExpect(status().isForbidden());
}

@Test
@DisplayName("유효한 CSRF 토큰으로 POST 요청 시 성공")
void csrfProtection_withValidToken_succeeds() throws Exception {
    String csrfToken = csrfTokenProvider.generateToken(sessionId);
    
    mockMvc.perform(post("/medication-schedule")
            .header("Authorization", "Bearer " + validJwt)
            .header("X-CSRF-TOKEN", csrfToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{...}"))
        .andExpect(status().isCreated());
}
```

---

## 5. 면접 대비 Q&A

### Q1. JWT만 사용하지 않고 CSRF 토큰을 추가한 이유는?

> **모범 답변**
> 
> JWT를 Authorization 헤더로만 전송하면 CSRF 공격에 안전합니다. 하지만 일부 시나리오(쿠키 기반 인증, 레거시 지원)에서는 여전히 CSRF 취약점이 존재할 수 있습니다.
> 
> Hamalog는 의료 정보를 다루므로 **Defense in Depth(심층 방어)** 원칙을 적용했습니다. CSRF 토큰을 추가하면:
> 1. 쿠키와 헤더 혼용 시에도 안전
> 2. 악성 스크립트가 토큰을 훔쳐도 CSRF 토큰 없이는 상태 변경 불가
> 3. 보안 감사(Audit) 요구사항 충족

### Q2. CSRF 토큰을 Redis에 저장한 이유는?

> **모범 답변**
> 
> 세 가지 이유가 있습니다:
> 
> 1. **Stateless 유지**: 서버 메모리에 저장하면 특정 서버에 종속됩니다. Redis는 분산 저장소이므로 어떤 서버에서도 검증 가능합니다.
> 
> 2. **인프라 재활용**: 이미 캐싱/Rate Limiting 용으로 Redis를 사용 중이므로 추가 인프라 비용이 없습니다.
> 
> 3. **TTL 자동 관리**: Redis의 TTL 기능으로 만료된 토큰을 자동 삭제합니다. 별도의 정리 작업이 불필요합니다.

### Q3. constantTimeEquals 메서드가 필요한 이유는?

> **모범 답변**
> 
> **Timing Attack**을 방어하기 위해서입니다.
> 
> 일반적인 `String.equals()`는 첫 번째로 다른 문자를 발견하면 즉시 `false`를 반환합니다. 공격자는 이 시간 차이를 측정하여 한 글자씩 토큰을 추측할 수 있습니다.
> 
> 예: "ABCDEF" vs "AXXXXX"는 두 번째 문자에서 반환 (빠름)
>     "ABCDEF" vs "ABXXXX"는 세 번째 문자에서 반환 (조금 느림)
> 
> `constantTimeEquals`는 항상 전체 문자열을 비교하므로 실행 시간이 일정합니다. XOR 연산 결과를 OR로 누적하여 한 번에 결과를 반환합니다.

### Q4. Redis 장애 시 어떻게 대응하나요?

> **모범 답변**
> 
> **Fallback 패턴**을 구현했습니다.
> 
> ```java
> private final ConcurrentMap<String, TokenRecord> fallbackStore = new ConcurrentHashMap<>();
> ```
> 
> Redis 연결 실패 시:
> 1. 로그 레벨 WARN으로 알림
> 2. 메모리 기반 ConcurrentHashMap에 저장
> 3. TTL은 TokenRecord 내부에서 Instant로 관리
> 4. 정기적으로 만료된 엔트리 정리
> 
> 이렇게 하면 Redis 장애가 서비스 전체 장애로 이어지지 않습니다. 다만, 분산 환경에서는 서버별로 토큰이 분리되므로 로드밸런서의 Sticky Session이 필요할 수 있습니다.

### Q5. Refresh Token Rotation이란 무엇이고 왜 필요한가요?

> **모범 답변**
> 
> Refresh Token Rotation은 Refresh Token 사용 시마다 새 토큰을 발급하고 기존 토큰을 무효화하는 방식입니다.
> 
> **필요한 이유**:
> 1. **탈취 감지**: 공격자가 Refresh Token을 탈취해 사용하면, 실제 사용자가 다음에 사용할 때 "이미 사용된 토큰"으로 감지됩니다.
> 
> 2. **피해 시간 최소화**: 탈취된 토큰은 한 번만 사용 가능하므로, 공격자의 세션 유지 시간이 제한됩니다.
> 
> 3. **Revocation 없이 보안 강화**: 모든 토큰을 블랙리스트에 등록하지 않아도 됩니다.

### Q6. Access Token 만료 시간을 15분으로 설정한 이유는?

> **모범 답변**
> 
> **보안과 UX의 균형**입니다.
> 
> - **너무 짧으면 (1~5분)**: 빈번한 토큰 갱신으로 UX 저하, 서버 부하 증가
> - **너무 길면 (1시간+)**: 토큰 탈취 시 피해 시간 증가
> 
> 15분은 일반적인 사용자 세션에서 자연스럽게 갱신되면서도, 탈취 시 피해를 제한할 수 있는 적절한 값입니다. 의료 시스템의 보안 요구사항을 고려하면 더 짧게 설정할 수도 있지만, 현재는 Refresh Token Rotation과 함께 사용하여 충분한 보안을 제공합니다.

### Q7. JWT 서명 알고리즘으로 HS256을 선택한 이유는?

> **모범 답변**
> 
> **단일 서비스 환경에서의 최적 선택**입니다.
> 
> | 알고리즘 | 키 방식 | 장점 | 단점 |
> |---------|--------|------|------|
> | HS256 | 대칭키 | 빠름, 구현 단순 | 키 공유 필요 |
> | RS256 | 비대칭키 | 키 분리 가능 | 느림, 구현 복잡 |
> 
> Hamalog는 단일 백엔드 서비스이므로 토큰 발급과 검증이 같은 서버에서 이루어집니다. 대칭키 방식의 HS256이 빠르고 충분히 안전합니다.
> 
> 마이크로서비스로 전환 시, 인증 서버만 개인키를 보관하고 다른 서비스는 공개키로 검증하는 RS256을 고려할 것입니다.

### Q8. Token Blacklist는 Stateless 원칙에 위배되지 않나요?

> **모범 답변**
> 
> 기술적으로는 상태를 저장하지만, **실용적 트레이드오프**입니다.
> 
> 순수 Stateless에서는 로그아웃이 불가능합니다. 토큰이 만료될 때까지 유효합니다. 의료 시스템에서는 즉시 로그아웃이 필수입니다.
> 
> **최소한의 상태**만 유지합니다:
> - 블랙리스트에는 토큰의 해시값만 저장 (전체 토큰 X)
> - TTL을 토큰 만료 시간과 동일하게 설정 (자동 정리)
> - Redis 사용으로 분산 환경에서도 일관성 유지
> 
> 이것은 "Stateless 원칙 위배"라기보다 "필요한 보안 요구사항을 위한 최소한의 예외"입니다.

### Q9. SameSite 쿠키 속성만으로는 CSRF를 막을 수 없나요?

> **모범 답변**
> 
> SameSite는 **부분적 방어**만 제공합니다.
> 
> | SameSite 값 | 동작 | 한계 |
> |-------------|------|------|
> | Strict | 모든 크로스사이트 요청에서 쿠키 제외 | UX 저하 (외부 링크 클릭 시 로그인 풀림) |
> | Lax | GET은 허용, POST 등은 차단 | GET 기반 CSRF 가능, 구형 브라우저 미지원 |
> | None | 모두 허용 (Secure 필수) | CSRF 방어 없음 |
> 
> 또한 **서브도메인 공격**, **Cookie Tossing** 등 SameSite로 막을 수 없는 공격도 있습니다. 따라서 SameSite와 CSRF 토큰을 함께 사용하는 것이 모범 사례입니다.

### Q10. CSRF 토큰은 로그인마다 새로 발급하나요, 세션 동안 유지하나요?

> **모범 답변**
> 
> **세션(사용자) 단위로 유지**하되, **상태 변경 요청마다 검증**합니다.
> 
> - **발급 시점**: 첫 CSRF 토큰 요청 시 (로그인 전)
> - **유효 기간**: 60분 TTL (이후 재발급 필요)
> - **갱신 시점**: 만료 임박 시 클라이언트가 재요청
> 
> 요청마다 새 토큰을 발급하는 **Double Submit Cookie** 방식도 있지만, 네트워크 오버헤드가 증가합니다. 현재 방식은 Redis 조회 1회로 검증이 완료되어 효율적입니다.
> 
> 보안을 더 강화하려면 **민감한 작업(비밀번호 변경, 회원 탈퇴)** 시에만 새 토큰을 발급하는 방식을 추가할 수 있습니다.

---

## 📎 관련 문서

- [ADR-0002: JWT + CSRF 이중 보호](../internal/adr/0002-jwt-csrf-dual-protection.md)
- [SECURITY-PATTERNS.md](../internal/patterns/SECURITY-PATTERNS.md)
- [JwtTokenProvider.java](../../src/main/java/com/Hamalog/security/jwt/JwtTokenProvider.java)
- [CsrfTokenProvider.java](../../src/main/java/com/Hamalog/security/csrf/CsrfTokenProvider.java)

