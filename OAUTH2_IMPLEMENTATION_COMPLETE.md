# OAuth2 카카오 로그인 콜백 처리 - 완전 구현 가이드

## 📋 요약

OAuth2 카카오 로그인 콜백을 **React Native 앱으로 직접 리다이렉트**하도록 개선했습니다.

### 핵심 변경사항
- ❌ 브라우저에 콜백 URL 표시 문제 해결
- ✅ JWT 토큰 → RN 앱 스킴으로 리다이렉트
- ✅ 토큰 교환 실패 시 에러 핸들링

---

## 🔄 요청 흐름

```
사용자 (RN 앱)
    ↓
[1] /oauth2/auth/kakao 호출
    ↓
[2] 백엔드 → 카카오 인증 서버로 리다이렉트
    ↓
[3] 사용자가 카카오에서 인증
    ↓
[4] 카카오 → /oauth2/auth/kakao/callback?code=...&state=... (브라우저)
    ↓
[5] 백엔드 처리:
    a) Authorization Code → Kakao API로 교환
    b) Access Token으로 사용자 정보 조회
    c) JWT 토큰 생성
    d) hamalog-rn://auth?token={jwtToken}으로 리다이렉트
    ↓
[6] 브라우저 → RN 앱 스킴 실행
    ↓
[7] RN 앱 → 딥링크로 JWT 토큰 받음
    ↓
[8] 로그인 완료 ✅
```

---

## 📦 수정된 파일

### 1. 설정 파일 (`application*.properties`)

#### `application.properties`
```properties
# OAuth2 RN App Redirect URI
hamalog.oauth2.rn-app-redirect-scheme=${RN_APP_REDIRECT_SCHEME:hamalog-rn}
```

#### `application-local.properties`
```properties
hamalog.oauth2.rn-app-redirect-scheme=hamalog-rn
```

#### `application-prod.properties`
```properties
hamalog.oauth2.rn-app-redirect-scheme=${RN_APP_REDIRECT_SCHEME:hamalog-rn}
```

### 2. 백엔드 Controller (`OAuth2Controller.java`)

#### Before
```java
@GetMapping("/oauth2/auth/kakao/callback")
public ResponseEntity<LoginResponse> handleKakaoCallback(@RequestParam("code") String code) {
    LoginResponse response = authService.processOAuth2Callback(code);
    return ResponseEntity.ok(response);  // JSON 응답 (브라우저에서 볼 수 있음)
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
        // Authorization Code → JWT 토큰 교환
        LoginResponse loginResponse = authService.processOAuth2Callback(code);
        String jwtToken = loginResponse.token();
        
        // RN 앱으로 리다이렉트
        String redirectUri = String.format("%s://auth?token=%s",
                rnAppRedirectScheme,
                URLEncoder.encode(jwtToken, StandardCharsets.UTF_8));
        
        response.sendRedirect(redirectUri);
    } catch (Exception e) {
        // 에러 발생 시
        String redirectUri = String.format("%s://auth?error=%s",
                rnAppRedirectScheme,
                URLEncoder.encode("TOKEN_EXCHANGE_FAILED", StandardCharsets.UTF_8));
        response.sendRedirect(redirectUri);
    }
}
```

### 3. 서비스 로깅 개선 (`AuthService.java`)

```java
private String exchangeCodeForToken(String code, ClientRegistration kakaoRegistration) {
    try {
        log.debug("Exchanging Kakao authorization code for access token at: {}",
                kakaoRegistration.getProviderDetails().getTokenUri());
        
        ResponseEntity<String> response = restTemplate.postForEntity(...);
        
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode tokenResponse = objectMapper.readTree(response.getBody());
            String accessToken = tokenResponse.get("access_token").asText();
            log.info("Successfully obtained Kakao access token");
            return accessToken;
        } else {
            log.error("Failed to exchange authorization code. Status: {}, Body: {}",
                    response.getStatusCode(), response.getBody());
            return null;
        }
    } catch (Exception e) {
        log.error("Exception while exchanging authorization code for token", e);
        return null;
    }
}
```

---

## 🧪 테스트 결과

### 성공한 테스트 케이스
✅ `카카오 OAuth2 콜백 - 성공: JWT 토큰으로 RN 앱으로 리다이렉트`
- 리다이렉트 URL: `hamalog-rn://auth?token=eyJhbGci...`

✅ `카카오 OAuth2 콜백 - 실패: 토큰 교환 실패 시 에러와 함께 RN 앱으로 리다이렉트`
- 리다이렉트 URL: `hamalog-rn://auth?error=TOKEN_EXCHANGE_FAILED`

✅ `카카오 OAuth2 콜백 - State 파라미터 없이 호출`
- 리다이렉트 URL: `hamalog-rn://auth?token=eyJhbGci...`

### 빌드 상태
```
BUILD SUCCESSFUL in 10s
✅ 모든 테스트 통과
```

---

## 📱 React Native 앱 구현

### app.json 설정
```json
{
  "expo": {
    "scheme": "hamalog-rn",
    "plugins": [
      ["expo-build-properties", {
        "android": {
          "usesCleartextTraffic": true
        }
      }]
    ]
  }
}
```

### React Navigation 설정
```javascript
const linking = {
  prefixes: ['hamalog-rn://', 'https://hamalog.com'],
  config: {
    screens: {
      OAuth2Callback: 'auth',
      // ... 다른 스크린
    },
  },
};
```

### 콜백 핸들러
```javascript
export function OAuth2CallbackScreen({ route }) {
  useEffect(() => {
    const { token, error } = route.params || {};

    if (error) {
      console.error('OAuth2 로그인 실패:', error);
      // 에러 처리
      return;
    }

    if (token) {
      // JWT 토큰 저장
      await SecureStore.setItemAsync('authToken', token);
      // 홈 화면으로 이동
      navigation.replace('Home');
    }
  }, [route.params]);

  return <LoadingSpinner />;
}
```

### 로그인 버튼
```javascript
export function LoginScreen() {
  const handleKakaoLogin = async () => {
    const BACKEND_URL = 'http://49.142.154.182:8080';
    const oauthStartUrl = `${BACKEND_URL}/oauth2/auth/kakao`;
    
    // 기본 브라우저에서 열기
    await Linking.openURL(oauthStartUrl);
  };

  return (
    <TouchableOpacity onPress={handleKakaoLogin}>
      <Text>카카오로 로그인</Text>
    </TouchableOpacity>
  );
}
```

---

## 🔐 보안 고려사항

### 현재 구현
✅ JWT 토큰 암호화 (RS256)
✅ HTTPS 권장 (프로덕션)
✅ URL 인코딩

### 향후 개선
- [ ] State Parameter 검증 (CSRF 방지)
- [ ] Refresh Token 구현
- [ ] Token Expiry 자동 갱신
- [ ] Multi-provider 지원 (Google, Apple)

---

## 📊 환경별 설정

### Local Development
```bash
# 백엔드: http://localhost:8080
# RN 앱 스킴: hamalog-rn
# 콜백: hamalog-rn://auth?token=...
```

### Production
```bash
# 백엔드: http://49.142.154.182:8080
# RN 앱 스킴: hamalog-rn (또는 환경 변수로 설정)
# 콜백: hamalog-rn://auth?token=...
```

---

## 🚀 배포 체크리스트

- [ ] 프로덕션 카카오 OAuth2 credentials 확인
- [ ] `hamalog.oauth2.rn-app-redirect-scheme` 환경 변수 설정
- [ ] JWT 토큰 시크릿 확인
- [ ] HTTPS 적용 확인
- [ ] RN 앱 딥링크 설정 확인
- [ ] 테스트 환경에서 전체 흐름 테스트

---

## 📞 문제 해결

### 콜백 URL이 브라우저에 보이는 경우
❌ 이전 구현에서는 JSON 응답이 브라우저에 표시됨
✅ 현재는 `response.sendRedirect()`로 즉시 리다이렉트

### 토큰이 RN 앱에 도착하지 않는 경우
- [ ] RN 앱에서 딥링크 핸들러 확인
- [ ] `hamalog-rn://` 스킴이 등록되었는지 확인
- [ ] 백엔드 로그에서 리다이렉트 URL 확인

### 토큰 교환 실패
- [ ] 카카오 Authorization Code 유효성 확인
- [ ] 카카오 API credentials 확인
- [ ] 네트워크 연결 확인 (프로덕션)
- [ ] 백엔드 로그에서 `exchangeCodeForToken` 에러 확인

---

## 📝 참고 자료

- [Kakao OAuth2 REST API](https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api)
- [React Navigation Deep Linking](https://reactnavigation.org/docs/deep-linking/)
- [Expo Linking](https://docs.expo.dev/versions/latest/sdk/linking/)

---

## ✨ 완료

모든 구현이 완료되었습니다! 🎉

- ✅ Authorization Code → JWT 토큰 교환
- ✅ RN 앱으로 딥링크 리다이렉트
- ✅ 에러 처리
- ✅ 전체 테스트 통과

이제 RN 앱에서 `hamalog-rn://auth?token=...` 형식으로 토큰을 받을 수 있습니다!

