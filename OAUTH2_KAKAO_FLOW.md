# OAuth2 카카오 로그인 콜백 처리 개선 - 구현 완료

## 문제점 (Before)
1. 브라우저에 콜백 URL이 그대로 표시됨
   - `http://49.142.154.182:8080/oauth2/auth/kakao/callback?code=인증코드&state=상태값`
2. Authorization Code를 JWT 토큰으로 교환 후 JSON 응답만 반환
3. React Native 앱으로 토큰이 전달되지 않음

## 해결 방법 (After)

### 1. 설정 파일 수정
모든 환경(dev, local, prod)의 `application*.properties`에 RN 앱 리다이렉트 스킴 추가:
```properties
hamalog.oauth2.rn-app-redirect-scheme=${RN_APP_REDIRECT_SCHEME:hamalog-rn}
```

### 2. OAuth2Controller 콜백 핸들러 개선

#### Before
```java
@GetMapping("/oauth2/auth/kakao/callback")
public ResponseEntity<LoginResponse> handleKakaoCallback(@RequestParam("code") String code) {
    // JSON 응답 반환
    LoginResponse response = authService.processOAuth2Callback(code);
    return ResponseEntity.ok(response);
}
```

#### After
```java
@GetMapping("/oauth2/auth/kakao/callback")
public void handleKakaoCallback(
        @RequestParam("code") String code,
        @RequestParam(value = "state", required = false) String state,
        HttpServletResponse response) throws IOException {
    try {
        // 1. Authorization Code → JWT 토큰 교환
        LoginResponse loginResponse = authService.processOAuth2Callback(code);
        String jwtToken = loginResponse.token();
        
        // 2. RN 앱으로 리다이렉트
        String redirectUri = String.format("%s://auth?token=%s",
                rnAppRedirectScheme,
                URLEncoder.encode(jwtToken, StandardCharsets.UTF_8));
        
        response.sendRedirect(redirectUri);
    } catch (Exception e) {
        // 에러 발생 시 에러 정보와 함께 RN 앱으로 리다이렉트
        String redirectUri = String.format("%s://auth?error=%s",
                rnAppRedirectScheme,
                URLEncoder.encode("TOKEN_EXCHANGE_FAILED", StandardCharsets.UTF_8));
        response.sendRedirect(redirectUri);
    }
}
```

### 3. AuthService 로깅 개선
Token 교환 및 사용자 정보 조회 과정에서 더 자세한 로깅 추가:
- Authorization Code 교환 상태 확인
- Kakao API 호출 상태 확인
- 에러 메시지 상세 기록

## 구현된 요청 흐름

```
1. [RN App] → /oauth2/auth/kakao
   ↓
2. [Backend] → 카카오 인증 서버로 리다이렉트
   ↓
3. [User] → 카카오에서 인증
   ↓
4. [Kakao] → /oauth2/auth/kakao/callback?code=...&state=... (브라우저)
   ↓
5. [Backend]
   a) Authorization Code를 Kakao API로 교환 → Access Token 획득
   b) Access Token으로 사용자 정보 조회
   c) 사용자 정보로 JWT 토큰 생성
   d) hamalog-rn://auth?token={jwtToken} 으로 리다이렉트
   ↓
6. [RN App] → 브라우저가 앱 스킴 실행 → 딥링크로 토큰 전달
   ↓
7. [RN App] → 로그인 완료, 토큰 저장 및 앱 사용
```

## 환경별 설정

### Local Development
```properties
hamalog.oauth2.rn-app-redirect-scheme=hamalog-rn
```
→ `hamalog-rn://auth?token=...`

### Production
```properties
hamalog.oauth2.rn-app-redirect-scheme=${RN_APP_REDIRECT_SCHEME:hamalog-rn}
```
→ 환경 변수 `RN_APP_REDIRECT_SCHEME`로 오버라이드 가능

## 보안 고려사항

### State Parameter
- 현재 생성되고 있음: `/oauth2/auth/kakao` 엔드포인트에서 state UUID 생성
- 향후 개선: State 검증 로직 추가 예정 (CSRF 공격 방지)

### JWT Token 인코딩
- 토큰을 URL 파라미터로 전달 시 URLEncoder로 인코딩
- 앱에서는 디코딩하여 토큰 사용

### 에러 처리
- Token 교환 실패 시 `error=TOKEN_EXCHANGE_FAILED` 파라미터와 함께 리다이렉트
- RN 앱에서 error 파라미터 확인하여 에러 처리

## 테스트 시나리오

### 성공 케이스
1. 앱에서 `/oauth2/auth/kakao` 호출
2. 카카오 로그인 수행
3. 백엔드가 JWT 토큰 생성 및 RN 앱으로 리다이렉트
4. RN 앱이 `hamalog-rn://auth?token=...` 받음
5. 토큰 저장 및 앱 사용

### 실패 케이스
1. Authorization Code 교환 실패
2. 사용자 정보 조회 실패
3. 에러 정보 `hamalog-rn://auth?error=TOKEN_EXCHANGE_FAILED`로 리다이렉트
4. RN 앱에서 에러 처리

## 수정된 파일 목록

1. `src/main/resources/application.properties` - RN 앱 리다이렉트 스킴 설정 추가
2. `src/main/resources/application-local.properties` - RN 앱 리다이렉트 스킴 설정 추가
3. `src/main/resources/application-prod.properties` - RN 앱 리다이렉트 스킴 설정 추가
4. `src/main/java/com/Hamalog/controller/oauth2/OAuth2Controller.java`
   - Import 추가 (@Value, URLEncoder, StandardCharsets)
   - `handleKakaoCallback` 메서드 수정
   - RN 앱 리다이렉트 로직 구현
5. `src/main/java/com/Hamalog/service/auth/AuthService.java`
   - `exchangeCodeForToken` 메서드 로깅 개선
   - `getUserInfoFromKakao` 메서드 로깅 개선

## 빌드 상태
✅ BUILD SUCCESSFUL (모든 컴파일 에러 해결됨)

## 다음 개선 사항 (Future)
- [ ] State Parameter 검증 로직 구현
- [ ] 중간 HTML 페이지 옵션 (간단한 로딩 화면)
- [ ] Refresh Token 구현
- [ ] OAuth2 다중 제공자 지원 (Google, Apple 등)

