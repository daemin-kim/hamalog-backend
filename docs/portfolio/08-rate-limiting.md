# 08. Redis Sorted Set 기반 Rate Limiting

> **Sliding Window 알고리즘과 Fail-Open 패턴으로 DDoS 공격을 방어하면서도 가용성을 보장하는 Rate Limiting 구현**

---

## 📋 목차

1. [문제 상황](#1-문제-상황-problem)
2. [해결 전략](#2-해결-전략-solution-strategy)
3. [구현 상세](#3-구현-상세-implementation)
4. [효과 및 검증](#4-효과-및-검증-results)
5. [면접 대비 Q&A](#5-면접-대비-qa)

---

## 1. 문제 상황 (Problem)

### 1.1 Rate Limiting의 필요성

API 서버는 다양한 위협에 노출됩니다:

| 위협 | 설명 | 영향 |
|------|------|------|
| **DDoS 공격** | 대량의 요청으로 서비스 마비 | 서비스 중단 |
| **Brute Force** | 로그인/비밀번호 무차별 대입 | 계정 탈취 |
| **API 남용** | 과도한 API 호출로 리소스 독점 | 다른 사용자 피해 |
| **크롤링/스크래핑** | 자동화된 대량 데이터 수집 | 데이터 유출, 부하 |

```
📊 Rate Limiting 없이 발생 가능한 시나리오

공격자: 로그인 요청 10,000회/분 (Brute Force)
  → 계정 비밀번호 무차별 대입 성공 가능
  → DB 부하 증가, 정상 사용자 로그인 지연

Rate Limiting 적용 후:
공격자: 로그인 요청 10,000회/분 → 5회만 허용, 나머지 차단
  → Brute Force 공격 무력화
  → 정상 서비스 유지
```

### 1.2 Hamalog에서의 특수 요구사항

| 요구사항 | 이유 |
|----------|------|
| **인증 엔드포인트 엄격한 제한** | Brute Force 공격 방어 (5회/분, 20회/시) |
| **API 엔드포인트 유연한 제한** | 정상 사용 방해 방지 (60회/분, 1000회/시) |
| **분산 환경 지원** | 여러 서버에서 동일한 제한 적용 |
| **Redis 장애 시 가용성 우선** | 장애 시에도 서비스 중단 방지 |
| **실시간 메트릭** | 공격 탐지 및 모니터링 |

### 1.3 기존 Rate Limiting 알고리즘의 한계

| 알고리즘 | 문제점 |
|----------|--------|
| **Fixed Window** | 경계 시점에 버스트 허용 (2배 요청 가능) |
| **Token Bucket** | 구현 복잡, 상태 관리 어려움 |
| **Leaky Bucket** | 트래픽 급증 허용 불가 |

```
📊 Fixed Window의 문제 (경계 버스트)

제한: 100회/분

시간 ──────────────────────────────────────────────
       11:59                 12:00                 12:01
         │                     │                     │
         └── 100회 요청 ──────┘ 100회 요청 ───────┘
                               ↑
                     경계에서 200회 허용됨!
```

---

## 2. 해결 전략 (Solution Strategy)

### 2.1 Sliding Window Log 알고리즘 선택

**Redis Sorted Set**을 활용한 Sliding Window Log 알고리즘을 선택했습니다.

```
┌────────────────────────────────────────────────────────────────┐
│                   Sliding Window Log 알고리즘                   │
│                                                                 │
│  시간 축 ───────────────────────────────────────────────────▶  │
│                                                                 │
│       │◄──────────── 1분 윈도우 ────────────▶│                 │
│       ▼                                     ▼                  │
│  ┌────────────────────────────────────────────┐                │
│  │ t1 │ t2 │ t3 │ ... │ t58 │ t59 │ t60 │ 현재                │
│  └────────────────────────────────────────────┘                │
│       ↑                                     ↑                  │
│   윈도우 시작                           윈도우 끝              │
│   (현재 - 60초)                          (현재)                │
│                                                                 │
│  Redis Sorted Set:                                             │
│    Key: "rate_limit:ip:1.2.3.4:minutes"                        │
│    Score: timestamp (밀리초)                                   │
│    Member: timestamp 문자열                                    │
│                                                                 │
│  매 요청마다:                                                   │
│  1. 윈도우 밖 데이터 삭제 (ZREMRANGEBYSCORE)                    │
│  2. 현재 윈도우 내 요청 수 카운트 (ZCOUNT)                      │
│  3. 제한 초과 시 거부, 아니면 요청 추가 (ZADD)                  │
│                                                                 │
└────────────────────────────────────────────────────────────────┘
```

### 2.2 왜 Sliding Window Log인가?

| 특성 | Fixed Window | Sliding Window Log |
|------|-------------|-------------------|
| **정확성** | 경계 버스트 가능 | 정확한 윈도우 내 카운트 |
| **구현** | 단순 | Redis Sorted Set으로 간편 |
| **메모리** | O(1) | O(요청 수) |
| **분산 환경** | 별도 동기화 필요 | Redis가 자동 동기화 |

### 2.3 Fail-Open 전략

Redis 장애 시 **서비스 가용성을 우선**합니다:

```
┌────────────────────────────────────────────────────────────────┐
│                     Fail-Open 패턴                              │
│                                                                 │
│  [정상 상태]                                                    │
│  요청 → Redis 조회 → 제한 체크 → 허용/거부                     │
│                                                                 │
│  [Redis 장애 시]                                                │
│  요청 → Redis 조회 실패 → Degraded Mode 진입 → 요청 허용       │
│           │                     │                               │
│           └── 예외 로깅 ────────┘                               │
│                                 │                               │
│                                 ▼                               │
│                     5분 후 자동 복구 시도                        │
│                                                                 │
│  왜 Fail-Open?                                                  │
│  - 보안 < 가용성 (Rate Limiting 실패해도 서비스는 유지)         │
│  - 5분 간격으로 Redis 복구 확인                                 │
│  - 장애 중에도 로그로 요청 추적                                 │
│                                                                 │
└────────────────────────────────────────────────────────────────┘
```

---

## 3. 구현 상세 (Implementation)

### 3.1 RateLimitingService 핵심 구현

```java
/**
 * Redis Sorted Set 기반 Rate Limiting 서비스
 * 
 * Sliding Window Log 알고리즘으로 정확한 요청 제한을 구현하고,
 * Redis 장애 시 Fail-Open 패턴으로 가용성을 보장합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RateLimitingService {

    private final RedisTemplate<String, Object> redisTemplate;

    // ============================================================
    // 엔드포인트별 요청 제한 설정
    // ============================================================
    
    // 인증 엔드포인트: 엄격한 제한 (Brute Force 방어)
    private static final int AUTH_REQUESTS_PER_MINUTE = 5;
    private static final int AUTH_REQUESTS_PER_HOUR = 20;
    
    // API 엔드포인트: 유연한 제한 (정상 사용 허용)
    private static final int API_REQUESTS_PER_MINUTE = 60;
    private static final int API_REQUESTS_PER_HOUR = 1000;
    
    // Fail-Open 관련 설정
    private static final Duration DEFAULT_DEGRADE_DURATION = Duration.ofMinutes(5);
    private static final long MIN_DEGRADED_LOG_INTERVAL_MS = Duration.ofSeconds(30).toMillis();

    // Degraded Mode 상태 관리 (원자적 연산)
    private final AtomicLong degradedUntilEpochMs = new AtomicLong(0);
    private final AtomicLong lastDegradedLogEpochMs = new AtomicLong(0);

    // ============================================================
    // 요청 처리 메서드
    // ============================================================

    /**
     * 인증 엔드포인트 요청 처리
     * 
     * @param key 클라이언트 식별자 (IP 또는 사용자 ID)
     * @return true=허용, false=거부
     */
    public boolean tryConsumeAuthRequest(String key) {
        // 분당 제한 + 시간당 제한 모두 통과해야 허용
        boolean allowed = checkRateLimit(key, AUTH_REQUESTS_PER_MINUTE, 1, TimeUnit.MINUTES)
                && checkRateLimit(key, AUTH_REQUESTS_PER_HOUR, 1, TimeUnit.HOURS);
        recordMetric("auth", allowed);
        return allowed;
    }

    /**
     * API 엔드포인트 요청 처리
     */
    public boolean tryConsumeApiRequest(String key) {
        boolean allowed = checkRateLimit(key, API_REQUESTS_PER_MINUTE, 1, TimeUnit.MINUTES)
                && checkRateLimit(key, API_REQUESTS_PER_HOUR, 1, TimeUnit.HOURS);
        recordMetric("api", allowed);
        return allowed;
    }

    // ============================================================
    // 핵심 Rate Limiting 로직 (Sliding Window Log)
    // ============================================================

    /**
     * Sliding Window Log 알고리즘으로 요청 제한 체크
     * 
     * Redis Sorted Set을 활용:
     * - Score: 요청 타임스탬프 (밀리초)
     * - Member: 타임스탬프 문자열 (고유값)
     * 
     * 동작 순서:
     * 1. 윈도우 밖 요청 삭제 (ZREMRANGEBYSCORE)
     * 2. 현재 윈도우 내 요청 수 카운트 (ZCOUNT)
     * 3. 제한 초과 시 거부
     * 4. 허용 시 현재 요청 추가 (ZADD)
     */
    private boolean checkRateLimit(String key, int maxRequests, long windowSize, TimeUnit timeUnit) {
        // ============================================================
        // Step 1: Degraded Mode 체크
        // ============================================================
        if (isDegradedModeActive()) {
            // Redis 장애 중 - 요청 허용 (Fail-Open)
            logDegradedRequest(key);
            return true;
        }

        try {
            // ============================================================
            // Step 2: Redis 키 및 윈도우 설정
            // ============================================================
            String redisKey = "rate_limit:" + key + ":" + timeUnit.name().toLowerCase();
            long currentTime = System.currentTimeMillis();
            long windowSizeMs = timeUnit.toMillis(windowSize);
            long windowStart = currentTime - windowSizeMs;

            var zSetOps = redisTemplate.opsForZSet();

            // ============================================================
            // Step 3: 윈도우 밖 오래된 요청 삭제
            // ============================================================
            // ZREMRANGEBYSCORE: score가 0 ~ windowStart인 멤버 삭제
            zSetOps.removeRangeByScore(redisKey, 0, windowStart);

            // ============================================================
            // Step 4: 현재 윈도우 내 요청 수 카운트
            // ============================================================
            // ZCOUNT: score가 windowStart ~ currentTime인 멤버 수
            Long currentCount = zSetOps.count(redisKey, windowStart, currentTime);

            // ============================================================
            // Step 5: 제한 초과 체크
            // ============================================================
            if (currentCount != null && currentCount >= maxRequests) {
                log.warn("[RATE_LIMIT] Rate limit exceeded for key: {}, current count: {}, max: {}",
                    key, currentCount, maxRequests);
                return false;  // 거부
            }

            // ============================================================
            // Step 6: 허용 - 현재 요청 추가
            // ============================================================
            // ZADD: 현재 타임스탬프를 score와 member로 추가
            zSetOps.add(redisKey, String.valueOf(currentTime), currentTime);
            
            // TTL 설정 (윈도우 크기 + 1초 여유)
            redisTemplate.expire(redisKey, windowSizeMs + 1000, TimeUnit.MILLISECONDS);

            return true;  // 허용

        } catch (Exception e) {
            // ============================================================
            // Redis 장애 - Degraded Mode 진입
            // ============================================================
            enterDegradedMode(e);
            return true;  // Fail-Open: 장애 시 허용
        }
    }

    // ============================================================
    // Degraded Mode 관리 (Fail-Open 패턴)
    // ============================================================

    /**
     * Degraded Mode 활성화 상태 확인
     * 
     * 원자적 연산(CAS)으로 동시성 문제 방지
     */
    private boolean isDegradedModeActive() {
        long until = degradedUntilEpochMs.get();
        if (until == 0L) {
            return false;
        }

        long now = System.currentTimeMillis();
        // 만료되었으면 해제 (CAS로 원자적 업데이트)
        if (now >= until && degradedUntilEpochMs.compareAndSet(until, 0L)) {
            log.info("[RATE_LIMIT] Fail-open window expired. Resuming normal rate limiting.");
            return false;
        }

        return true;
    }

    /**
     * Degraded Mode 진입
     * 
     * Redis 예외 발생 시 호출
     * 설정된 기간 동안 Rate Limiting 비활성화
     */
    private void enterDegradedMode(Exception e) {
        long now = System.currentTimeMillis();
        Duration degradeDuration = Duration.ofMinutes(5);
        degradedUntilEpochMs.set(now + degradeDuration.toMillis());
        
        // 로그 폭주 방지 (30초마다 한 번만 로깅)
        long lastLog = lastDegradedLogEpochMs.get();
        if (now - lastLog >= MIN_DEGRADED_LOG_INTERVAL_MS &&
                lastDegradedLogEpochMs.compareAndSet(lastLog, now)) {
            log.error("[RATE_LIMIT] Redis unavailable. Entering fail-open mode for {} seconds.",
                degradeDuration.toSeconds(), e);
        }
    }

    // ============================================================
    // 키 생성 유틸리티
    // ============================================================

    /**
     * IP 기반 Rate Limit 키 생성
     */
    public static String createIpKey(String ipAddress) {
        return "ip:" + ipAddress;
    }

    /**
     * 사용자 ID 기반 Rate Limit 키 생성
     */
    public static String createUserKey(String userId) {
        return "user:" + userId;
    }

    // ============================================================
    // Rate Limit 정보 조회 (헤더용)
    // ============================================================

    public record RateLimitInfo(
        int maxRequestsPerMinute,
        int maxRequestsPerHour,
        long remainingRequestsThisMinute
    ) {}

    public RateLimitInfo getRateLimitInfo(String key, boolean isAuthEndpoint) {
        int maxPerMinute = isAuthEndpoint ? AUTH_REQUESTS_PER_MINUTE : API_REQUESTS_PER_MINUTE;
        int maxPerHour = isAuthEndpoint ? AUTH_REQUESTS_PER_HOUR : API_REQUESTS_PER_HOUR;
        long remainingMinute = getRemainingRequests(key, isAuthEndpoint);

        return new RateLimitInfo(maxPerMinute, maxPerHour, remainingMinute);
    }
}
```

### 3.2 RateLimitingFilter 구현

```java
/**
 * Rate Limiting 필터
 * 
 * Spring Security 필터 체인에 추가되어 모든 요청을 검사합니다.
 * 엔드포인트 유형에 따라 다른 제한을 적용합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitingService rateLimitingService;
    private final ObjectMapper objectMapper;
    private final TrustedProxyService trustedProxyService;

    // ============================================================
    // 엔드포인트 분류
    // ============================================================

    // 인증 엔드포인트: 엄격한 제한 (5회/분)
    private static final Set<String> AUTH_ENDPOINTS = Set.of(
        "/auth/login",
        "/auth/signup",
        "/auth/logout"
    );

    // 보호 API 엔드포인트: 일반 제한 (60회/분)
    private static final Set<String> PROTECTED_ENDPOINTS = Set.of(
        "/medication-record",
        "/medication-schedule",
        "/side-effect"
    );

    // ============================================================
    // 필터 처리
    // ============================================================

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        String clientIp = getClientIpAddress(request);
        
        // 엔드포인트 유형 판별
        boolean isAuthEndpoint = AUTH_ENDPOINTS.contains(requestURI);
        boolean isProtectedEndpoint = PROTECTED_ENDPOINTS.stream()
            .anyMatch(requestURI::startsWith);
        
        // Rate Limiting 대상이 아닌 경우 통과
        if (!isAuthEndpoint && !isProtectedEndpoint) {
            filterChain.doFilter(request, response);
            return;
        }

        // Rate Limit 키 생성 (IP 기반)
        String rateLimitKey = RateLimitingService.createIpKey(clientIp);
        
        // 엔드포인트 유형에 따른 제한 체크
        boolean allowed;
        if (isAuthEndpoint) {
            allowed = rateLimitingService.tryConsumeAuthRequest(rateLimitKey);
        } else {
            allowed = rateLimitingService.tryConsumeApiRequest(rateLimitKey);
        }
        
        // ============================================================
        // 제한 초과 시 429 응답
        // ============================================================
        if (!allowed) {
            handleRateLimitExceeded(response, clientIp, requestURI, isAuthEndpoint);
            return;
        }
        
        // ============================================================
        // 허용 시 Rate Limit 헤더 추가 후 통과
        // ============================================================
        addRateLimitHeaders(response, rateLimitKey, isAuthEndpoint);
        filterChain.doFilter(request, response);
    }

    /**
     * Rate Limit 초과 시 429 응답 생성
     */
    private void handleRateLimitExceeded(HttpServletResponse response, String clientIp, 
                                       String requestURI, boolean isAuthEndpoint) throws IOException {
        
        log.warn("Rate limit exceeded - IP: {}, URI: {}, Auth endpoint: {}", 
                clientIp, requestURI, isAuthEndpoint);
        
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> errorResponse = Map.of(
            "error", "Too Many Requests",
            "message", isAuthEndpoint ? 
                "인증 요청이 너무 많습니다. 잠시 후 다시 시도해주세요." : 
                "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.",
            "status", HttpStatus.TOO_MANY_REQUESTS.value(),
            "timestamp", System.currentTimeMillis()
        );
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * Rate Limit 정보 헤더 추가
     * 
     * 클라이언트에게 현재 제한 상태를 알려줍니다:
     * - X-RateLimit-Limit-Minute: 분당 최대 요청 수
     * - X-RateLimit-Limit-Hour: 시간당 최대 요청 수
     * - X-RateLimit-Remaining: 남은 요청 수
     */
    private void addRateLimitHeaders(HttpServletResponse response, String rateLimitKey, 
                                   boolean isAuthEndpoint) {
        try {
            RateLimitingService.RateLimitInfo rateLimitInfo = 
                rateLimitingService.getRateLimitInfo(rateLimitKey, isAuthEndpoint);
            
            response.setHeader("X-RateLimit-Limit-Minute", 
                String.valueOf(rateLimitInfo.maxRequestsPerMinute()));
            response.setHeader("X-RateLimit-Limit-Hour", 
                String.valueOf(rateLimitInfo.maxRequestsPerHour()));
            response.setHeader("X-RateLimit-Remaining", 
                String.valueOf(rateLimitInfo.remainingRequestsThisMinute()));
            
        } catch (Exception e) {
            log.warn("Failed to add rate limit headers", e);
        }
    }

    /**
     * 클라이언트 IP 추출 (프록시 환경 대응)
     * 
     * Cloudflare, Nginx 등 프록시 뒤에 있을 때
     * X-Forwarded-For 헤더에서 실제 클라이언트 IP 추출
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        
        // 신뢰할 수 있는 프록시가 아니면 직접 IP 사용
        if (!trustedProxyService.isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }

        // X-Forwarded-For에서 실제 클라이언트 IP 추출
        return trustedProxyService.extractClientIp(request.getHeader("X-Forwarded-For"))
                .orElse(remoteAddr);
    }

    /**
     * Rate Limiting 제외 대상
     * 
     * 정적 리소스, 헬스 체크 등은 제한하지 않음
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        return requestURI.startsWith("/static/") || 
               requestURI.startsWith("/css/") || 
               requestURI.startsWith("/js/") || 
               requestURI.startsWith("/images/") ||
               requestURI.startsWith("/favicon.ico") ||
               requestURI.startsWith("/actuator/health");
    }
}
```

### 3.3 설정 및 메트릭

```yaml
# application.yml
app:
  rate-limit:
    auth:
      per-minute: 5
      per-hour: 20
    api:
      per-minute: 60
      per-hour: 1000
    degrade-seconds: 300  # Fail-Open 지속 시간
    metrics-enabled: true
```

```java
/**
 * Rate Limit 설정 Properties
 */
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
@Getter @Setter
public class RateLimitProperties {
    private Limit auth = new Limit(5, 20);
    private Limit api = new Limit(60, 1000);
    private int degradeSeconds = 300;
    private boolean metricsEnabled = true;

    @Getter @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Limit {
        private int perMinute;
        private int perHour;
    }
}
```

---

## 4. 효과 및 검증 (Results)

### 4.1 보안 효과

| 공격 유형 | Before | After |
|----------|--------|-------|
| **Brute Force 로그인** | 무제한 시도 가능 | 분당 5회, 시간당 20회 제한 |
| **API 남용** | 리소스 독점 가능 | 분당 60회, 시간당 1000회 제한 |
| **DDoS 초기 단계** | 서버 과부하 | 조기 차단, 서버 보호 |

### 4.2 가용성 보장

```
✅ Fail-Open 패턴으로 Redis 장애 시에도 서비스 유지
✅ 5분 간격 자동 복구 시도
✅ 로그 폭주 방지 (30초마다 1회 로깅)
✅ 정적 리소스, 헬스 체크 제외
```

### 4.3 클라이언트 친화적

```
HTTP/1.1 200 OK
X-RateLimit-Limit-Minute: 60
X-RateLimit-Limit-Hour: 1000
X-RateLimit-Remaining: 45

---

HTTP/1.1 429 Too Many Requests
Content-Type: application/json

{
  "error": "Too Many Requests",
  "message": "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.",
  "status": 429,
  "timestamp": 1736505600000
}
```

### 4.4 모니터링

```
# Prometheus 메트릭
rate_limit_requests_total{endpoint_type="auth", outcome="allowed"} 1234
rate_limit_requests_total{endpoint_type="auth", outcome="blocked"} 56
rate_limit_requests_total{endpoint_type="api", outcome="allowed"} 45678
rate_limit_requests_total{endpoint_type="api", outcome="blocked"} 123

# 로그
[RATE_LIMIT] Rate limit exceeded for key: ip:1.2.3.4, current count: 5, max: 5
Rate limit exceeded - IP: 1.2.3.4, URI: /auth/login, Auth endpoint: true
```

---

## 5. 면접 대비 Q&A

### Q1. Sliding Window Log 알고리즘을 선택한 이유는?

> **모범 답변**
> 
> 세 가지 알고리즘을 비교 검토했습니다:
> 
> 1. **Fixed Window**: 구현 간단하지만 경계에서 2배 트래픽 허용 문제
> 2. **Token Bucket**: 버스트 허용 가능하지만 구현 복잡
> 3. **Sliding Window Log**: 정확한 윈도우 내 카운트, Redis Sorted Set으로 간편 구현
> 
> Sliding Window Log를 선택한 이유:
> - **정확성**: 경계 버스트 문제 없음
> - **분산 환경**: Redis가 자동으로 여러 서버 간 상태 동기화
> - **구현 용이**: Redis Sorted Set의 ZADD, ZREMRANGEBYSCORE, ZCOUNT로 간단 구현
> 
> 단점인 메모리 사용량은 TTL 설정으로 관리합니다.

### Q2. Fail-Open 패턴을 선택한 이유는? Fail-Close가 더 안전하지 않나요?

> **모범 답변**
> 
> 상황에 따라 다릅니다:
> 
> **Fail-Close** (장애 시 모든 요청 차단):
> - 보안이 최우선인 경우 적합 (금융, 결제)
> - 단점: 가용성 희생
> 
> **Fail-Open** (장애 시 모든 요청 허용):
> - 가용성이 중요한 경우 적합
> - 단점: 일시적으로 보안 약화
> 
> Hamalog에서 Fail-Open을 선택한 이유:
> 1. **Rate Limiting은 부가 기능**: 핵심 보안(인증/인가)은 별도로 동작
> 2. **Redis 장애는 일시적**: 5분 간격으로 복구 시도
> 3. **서비스 연속성 중요**: 환자의 복약 기록이 중단되면 안 됨
> 4. **다중 방어**: WAF(Cloudflare), 인증 필터 등 다른 보안 계층 존재
> 
> 핵심: Rate Limiting 실패가 서비스 중단으로 이어지면 안 됩니다.

### Q3. IP 기반 Rate Limiting의 한계는 무엇인가요?

> **모범 답변**
> 
> **한계점**:
> 1. **NAT/프록시 문제**: 같은 IP 뒤에 여러 사용자 (회사, 학교)
> 2. **IP 위조**: X-Forwarded-For 헤더 조작
> 3. **동적 IP**: 공격자가 IP 변경하며 우회
> 
> **해결책**:
> 1. **다중 식별자**: IP + User-Agent + Fingerprint 조합
> 2. **인증 후 사용자 ID 기반**: `RateLimitingService.createUserKey(userId)`
> 3. **신뢰할 수 있는 프록시만 허용**: `TrustedProxyService`
> 
> ```java
> // Hamalog 구현
> private String getClientIpAddress(HttpServletRequest request) {
>     String remoteAddr = request.getRemoteAddr();
>     
>     // 신뢰할 수 있는 프록시(Cloudflare)만 X-Forwarded-For 신뢰
>     if (!trustedProxyService.isTrustedProxy(remoteAddr)) {
>         return remoteAddr;
>     }
>     
>     return trustedProxyService.extractClientIp(
>         request.getHeader("X-Forwarded-For")
>     ).orElse(remoteAddr);
> }
> ```

### Q4. 분산 환경에서 Rate Limiting 동기화는 어떻게 하나요?

> **모범 답변**
> 
> Redis를 **중앙 집중식 저장소**로 사용합니다:
> 
> ```
> 서버 A ──┐
>          ├──→ Redis ←──→ 동일한 Rate Limit 상태
> 서버 B ──┘
> ```
> 
> 동작 방식:
> 1. 모든 서버가 같은 Redis 키 사용: `rate_limit:ip:1.2.3.4:minutes`
> 2. Redis Sorted Set 연산은 원자적
> 3. 별도 동기화 로직 불필요
> 
> 주의점:
> - Redis 지연 시간 고려 (약간의 over-counting 가능)
> - 완벽한 정확성보다 일관성 우선

### Q5. 인증 엔드포인트와 API 엔드포인트의 제한이 다른 이유는?

> **모범 답변**
> 
> **위험도와 사용 패턴**이 다르기 때문입니다:
> 
> | 엔드포인트 | 제한 | 이유 |
> |----------|------|------|
> | **인증 (5회/분)** | 엄격 | Brute Force 공격 대상, 정상 사용 빈도 낮음 |
> | **API (60회/분)** | 유연 | 정상적인 앱 사용에 필요, 공격 가치 낮음 |
> 
> 인증 5회/분의 근거:
> - 정상 사용자: 로그인 실패 후 재시도해도 5회면 충분
> - 공격자: 5회/분으로는 Brute Force 불가능
> 
> API 60회/분의 근거:
> - 앱에서 페이지 로드 시 여러 API 호출
> - 너무 낮으면 정상 사용 방해
> - 시간당 1000회로 장기적 남용 방지

### Q6. Redis Sorted Set을 사용한 이유는?

> **모범 답변**
> 
> Sorted Set의 특성이 Sliding Window Log에 최적입니다:
> 
> 1. **Score 기반 정렬**: 타임스탬프를 score로 사용
> 2. **범위 연산**: ZREMRANGEBYSCORE, ZCOUNT로 윈도우 관리
> 3. **자동 정렬**: 시간 순서 자동 유지
> 4. **고성능**: O(log N) 시간 복잡도
> 
> ```
> ZADD rate_limit:ip:1.2.3.4:minutes 1736505600000 "1736505600000"
> ZREMRANGEBYSCORE rate_limit:ip:1.2.3.4:minutes 0 1736505540000
> ZCOUNT rate_limit:ip:1.2.3.4:minutes 1736505540000 1736505600000
> ```
> 
> 대안으로 String(INCR)도 가능하지만, 정확한 Sliding Window 구현 불가.

### Q7. TTL 설정은 어떻게 하나요?

> **모범 답변**
> 
> 윈도우 크기 + 여유 시간으로 설정합니다:
> 
> ```java
> // 윈도우 1분 → TTL 61초
> redisTemplate.expire(redisKey, windowSizeMs + 1000, TimeUnit.MILLISECONDS);
> ```
> 
> 이유:
> 1. 윈도우 밖 데이터는 어차피 삭제됨 (ZREMRANGEBYSCORE)
> 2. 1초 여유로 경계 조건 안전하게 처리
> 3. 메모리 누수 방지 (오래된 키 자동 삭제)

### Q8. Rate Limit 헤더를 제공하는 이유는?

> **모범 답변**
> 
> **클라이언트 친화적 설계**입니다:
> 
> ```
> X-RateLimit-Limit-Minute: 60
> X-RateLimit-Limit-Hour: 1000
> X-RateLimit-Remaining: 45
> ```
> 
> 장점:
> 1. **클라이언트 자체 조절**: 남은 요청 수 보고 속도 조절
> 2. **UX 개선**: 사용자에게 제한 상태 표시 가능
> 3. **디버깅 용이**: 문제 발생 시 원인 파악
> 4. **API 표준 준수**: IETF draft-ietf-httpapi-ratelimit-headers
> 
> 보안 고려:
> - 공격자에게 정보 노출 우려 → 하지만 제한 자체가 공개이므로 큰 문제 아님

### Q9. 동시성 문제는 어떻게 처리하나요?

> **모범 답변**
> 
> 두 가지 수준에서 처리합니다:
> 
> 1. **Redis 연산 원자성**:
>    - Redis 단일 명령은 원자적
>    - ZADD, ZCOUNT 등 개별 명령은 동시성 안전
> 
> 2. **Java 동시성 제어**:
>    ```java
>    // AtomicLong으로 Degraded Mode 상태 관리
>    private final AtomicLong degradedUntilEpochMs = new AtomicLong(0);
>    
>    // CAS 연산으로 경쟁 조건 방지
>    if (degradedUntilEpochMs.compareAndSet(until, 0L)) {
>        // 한 스레드만 상태 변경
>    }
>    ```
> 
> 완벽한 정확성은 없지만, Rate Limiting 목적상 약간의 오차는 허용 가능합니다.

### Q10. 테스트는 어떻게 작성하나요?

> **모범 답변**
> 
> 여러 시나리오를 커버합니다:
> 
> ```java
> @Test
> @DisplayName("분당 제한 초과 시 요청 거부")
> void shouldRejectWhenExceedingMinuteLimit() {
>     String key = "ip:test-ip";
>     
>     // 5회까지 허용
>     for (int i = 0; i < 5; i++) {
>         assertThat(rateLimitingService.tryConsumeAuthRequest(key)).isTrue();
>     }
>     
>     // 6회째 거부
>     assertThat(rateLimitingService.tryConsumeAuthRequest(key)).isFalse();
> }
> 
> @Test
> @DisplayName("Redis 장애 시 Fail-Open")
> void shouldAllowWhenRedisUnavailable() {
>     // Redis 연결 끊김 시뮬레이션
>     when(redisTemplate.opsForZSet()).thenThrow(RedisConnectionException.class);
>     
>     // 요청 허용 (Fail-Open)
>     assertThat(rateLimitingService.tryConsumeAuthRequest("ip:test")).isTrue();
>     assertThat(rateLimitingService.isDegraded()).isTrue();
> }
> ```
> 
> 통합 테스트:
> - Testcontainers로 실제 Redis 사용
> - 필터 체인 전체 테스트

---

## 📎 관련 문서

- [RateLimitingService.java](../../src/main/java/com/Hamalog/service/security/RateLimitingService.java)
- [RateLimitingFilter.java](../../src/main/java/com/Hamalog/security/filter/RateLimitingFilter.java)
- [SECURITY-PATTERNS.md](../internal/patterns/SECURITY-PATTERNS.md)

