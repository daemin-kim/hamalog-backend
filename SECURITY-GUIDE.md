# Hamalog 보안 설정 가이드

## 프로덕션 배포 시 필수 환경변수

프로덕션 환경에서 애플리케이션을 실행하기 전에 **반드시** 다음 환경변수를 설정해야 합니다.

### 필수 보안 환경변수

```bash
# JWT 비밀키 (256비트 이상)
export JWT_SECRET=$(openssl rand -base64 32)

# 데이터 암호화 키 (256비트)
export HAMALOG_ENCRYPTION_KEY=$(openssl rand -base64 32)

# OAuth2 Kakao 설정
export KAKAO_CLIENT_ID=your_kakao_client_id
export KAKAO_CLIENT_SECRET=your_kakao_client_secret
export KAKAO_REDIRECT_URI=https://your-domain.com/oauth2/auth/kakao/callback

# 데이터베이스 설정
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/hamalog
export SPRING_DATASOURCE_USERNAME=your_db_username
export SPRING_DATASOURCE_PASSWORD=your_db_password

# Redis 설정
export SPRING_DATA_REDIS_HOST=localhost
export SPRING_DATA_REDIS_PORT=6379
export SPRING_DATA_REDIS_PASSWORD=your_redis_password

# CORS 허용 도메인 (쉼표로 구분)
export ALLOWED_ORIGINS=https://your-frontend-domain.com

# 프론트엔드 URL (OAuth2 리다이렉트용)
export FRONTEND_URL=https://your-frontend-domain.com
```

### 프로덕션 프로필로 실행

```bash
java -jar -Dspring.profiles.active=prod hamalog.jar
```

또는 Docker 환경에서:

```bash
docker run -e SPRING_PROFILES_ACTIVE=prod \
  -e JWT_SECRET=$JWT_SECRET \
  -e HAMALOG_ENCRYPTION_KEY=$HAMALOG_ENCRYPTION_KEY \
  -e KAKAO_CLIENT_ID=$KAKAO_CLIENT_ID \
  -e KAKAO_CLIENT_SECRET=$KAKAO_CLIENT_SECRET \
  ... (기타 환경변수) \
  hamalog:latest
```

## 보안 체크리스트

### 배포 전 확인사항

- [ ] 모든 필수 환경변수가 설정되었는지 확인
- [ ] `spring.profiles.active=prod`로 설정되었는지 확인
- [ ] Swagger UI가 비활성화되었는지 확인 (`/swagger-ui` 접근 불가)
- [ ] 데이터베이스 비밀번호가 강력한지 확인
- [ ] Redis 비밀번호가 설정되었는지 확인
- [ ] HTTPS가 활성화되었는지 확인
- [ ] 방화벽이 적절히 설정되었는지 확인

### 보안 설정 검증

애플리케이션 시작 로그에서 다음을 확인하세요:

```
[JWT_PROVIDER] ✅ JWT configured successfully for production
[SECURITY] Swagger UI disabled for production environment
```

## 보안 기능 개요

### 인증 (Authentication)
- JWT 기반 stateless 인증
- OAuth2 (Kakao) 소셜 로그인
- Refresh Token Rotation으로 토큰 재사용 공격 방지
- 토큰 블랙리스트 (로그아웃 시 즉시 무효화)

### 인가 (Authorization)
- Spring Security Method Level Security (`@PreAuthorize`)
- 리소스 소유권 검증 (`@RequireResourceOwnership`)
- 역할 기반 접근 제어 (확장 가능)

### 데이터 보호
- AES-256-GCM으로 민감 데이터 암호화 (전화번호, 생년월일)
- BCrypt 비밀번호 해싱
- 민감 정보 로깅 마스킹

### 공격 방어
- CSRF 토큰 검증 (SPA 환경 최적화)
- Rate Limiting (인증 엔드포인트, API 엔드포인트)
- SSRF 방지 (외부 HTTP 요청 화이트리스트)
- SQL Injection 방지 (파라미터 바인딩)
- XSS 방지 (CSP 헤더, 입력 검증)
- Open Redirect 방지 (허용 도메인 화이트리스트)
- Clickjacking 방지 (X-Frame-Options: DENY)

### 보안 헤더
- Content-Security-Policy
- HTTP Strict Transport Security (HSTS)
- X-Frame-Options
- X-Content-Type-Options
- X-XSS-Protection
- Referrer-Policy
- Permissions-Policy

