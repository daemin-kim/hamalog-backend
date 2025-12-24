# ADR-0002: JWT + CSRF 이중 보호 채택

## 상태
Accepted

## 컨텍스트

Hamalog는 SPA(Single Page Application) 프론트엔드와 REST API 백엔드로 구성됩니다.
이 아키텍처에서 다음 보안 요구사항을 충족해야 합니다:

1. **인증(Authentication)**: 사용자 신원 확인
2. **CSRF 방어**: Cross-Site Request Forgery 공격 방지
3. **확장성**: 수평 확장이 가능한 Stateless 구조
4. **토큰 탈취 대응**: 토큰 유출 시 피해 최소화

### 고려한 대안들

| 방식 | 장점 | 단점 |
|------|------|------|
| Session + Cookie | 구현 단순 | 서버 상태 유지 필요, 수평 확장 어려움 |
| JWT Only | Stateless, 확장성 | CSRF 취약 (Cookie 사용 시) |
| JWT + CSRF 토큰 | Stateless + CSRF 방어 | 구현 복잡도 증가 |
| OAuth2 + JWT | 표준화, 확장성 | 오버엔지니어링 가능성 |

## 결정

**JWT + CSRF 이중 보호** 방식을 채택합니다.

### 구현 상세

```
[클라이언트]                           [서버]
    |                                    |
    |--- GET /auth/csrf-token ---------->|
    |<-- CSRF Token (Redis 저장, 60분 TTL)
    |                                    |
    |--- POST /auth/login -------------->|
    |    (X-CSRF-TOKEN 헤더 포함)         |
    |<-- Access Token + Refresh Token ---|
    |                                    |
    |--- GET /api/... ------------------>|
    |    Authorization: Bearer {JWT}     |
    |    X-CSRF-TOKEN: {csrf}            |
    |                                    |
```

### 핵심 구성요소

1. **Access Token (JWT)**
   - 만료: 15분 (900,000ms)
   - 서명: HS256 (HMAC-SHA256)
   - Payload: memberId, loginId, roles

2. **Refresh Token (JWT)**
   - 만료: 7일 (604,800,000ms)
   - **Rotation 적용**: 사용 시 새 토큰 발급 + 기존 토큰 무효화
   - DB 저장 (블랙리스트 관리)

3. **CSRF Token**
   - 저장: Redis (60분 TTL)
   - 검증: 안전하지 않은 메서드 (POST, PUT, DELETE) 에만 적용
   - 헤더: `X-CSRF-TOKEN`

### 코드 예시

```java
// CSRF 필터 (CsrfValidationFilter.java)
@Component
public class CsrfValidationFilter extends OncePerRequestFilter {
    
    private final CsrfTokenProvider csrfTokenProvider;
    
    @Override
    protected void doFilterInternal(...) {
        if (isUnsafeMethod(request) && !isExcludedPath(request)) {
            String csrfToken = request.getHeader("X-CSRF-TOKEN");
            String userId = jwtTokenProvider.getUserId(token);
            if (!csrfTokenProvider.validateToken(userId, csrfToken)) {
                throw new CsrfException("Invalid CSRF token");
            }
        }
    }
}

// Refresh Token Rotation (RefreshTokenService.java)
@Transactional
public TokenRefreshResponse rotate(String refreshToken) {
    // 1. 기존 토큰 검증
    // 2. 기존 토큰 무효화 (블랙리스트)
    // 3. 새 Access + Refresh 토큰 발급
    // 4. 새 Refresh 토큰 저장
}
```

## 결과

### 장점
- ✅ **Stateless 확장성**: JWT 기반으로 서버 세션 불필요
- ✅ **CSRF 완전 방어**: 모든 상태 변경 요청에 CSRF 토큰 필수
- ✅ **토큰 탈취 대응**: Refresh Token Rotation으로 피해 최소화
- ✅ **Redis 활용**: CSRF 토큰 분산 저장으로 수평 확장 가능

### 단점
- ⚠️ **구현 복잡도**: 일반 JWT 대비 복잡
- ⚠️ **Redis 의존성**: CSRF 토큰 저장용 Redis 필수
- ⚠️ **클라이언트 복잡도**: 모든 요청에 두 토큰 포함 필요

### 성능 영향
- CSRF 토큰 검증: Redis 조회 1회 (~1ms)
- JWT 검증: 서명 검증 (~0.5ms)
- 전체 인증 오버헤드: 요청당 ~2ms 미만

## 참고

- [OWASP CSRF Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html)
- [JWT Best Practices (RFC 8725)](https://datatracker.ietf.org/doc/html/rfc8725)
- [Refresh Token Rotation - Auth0](https://auth0.com/docs/secure/tokens/refresh-tokens/refresh-token-rotation)

---

> 작성일: 2025-12-23

