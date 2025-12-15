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

## 봇 차단 및 DDoS 방어

### BotProtectionFilter

악성 봇 및 취약점 스캐너를 애플리케이션 레벨에서 차단합니다.

**차단 대상:**
- 알려진 취약점 스캐너 (l9scan, Leakix, Nuclei, Nikto 등)
- 자동화된 봇 (Go-http-client, Python-requests 등)
- 의심스러운 경로 접근 (/.env, /.git, /wp-admin 등)

**설정 방법:**
```properties
# application.properties
hamalog.security.bot-protection.enabled=true
hamalog.security.bot-protection.log-blocked=true
```

### 실제 클라이언트 IP 식별

Docker/Nginx 환경에서 실제 클라이언트 IP를 올바르게 식별하기 위한 설정입니다.

**Spring Boot 설정:**
```properties
# X-Forwarded-For 헤더 처리 활성화 (Tomcat의 RemoteIpValve 사용)
server.forward-headers-strategy=native

# 신뢰할 수 있는 프록시 CIDR (Docker 네트워크 포함)
hamalog.security.trusted-proxies=127.0.0.1/32,::1/128,172.16.0.0/12,10.0.0.0/8,192.168.0.0/16
```

**환경변수로 설정:**
```bash
export TRUSTED_PROXIES="127.0.0.1/32,::1/128,172.16.0.0/12,10.0.0.0/8"
```

## Nginx 리버스 프록시 설정 (권장)

프로덕션 환경에서는 Nginx를 앞단에 배치하여 추가 보안 계층을 제공합니다.

### 장점
- 봇 차단을 Nginx 레벨에서 처리하여 애플리케이션 리소스 절약
- SSL/TLS 터미네이션
- Rate Limiting 강화
- 정적 파일 캐싱
- **보안 헤더** (X-Frame-Options, X-Content-Type-Options 등)
- **Slowloris 공격 방어** (타임아웃 설정)

### Nginx 보안 설정 (2025-12 업데이트)

**추가된 보안 헤더:**
```nginx
add_header X-Frame-Options "DENY" always;
add_header X-Content-Type-Options "nosniff" always;
add_header X-XSS-Protection "1; mode=block" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header Permissions-Policy "geolocation=(), microphone=(), camera=()" always;
```

**Slowloris 공격 방어 (타임아웃 설정):**
```nginx
client_body_timeout 10s;
client_header_timeout 10s;
send_timeout 10s;
```

**데이터베이스 포트 보안:**
- Redis/MySQL 포트는 Docker 내부 네트워크에서만 접근 가능 (`expose` 사용)
- 외부에서 직접 데이터베이스 접근 차단

### Docker Compose로 배포

**프로덕션 배포 구조:**
```
외부 요청 → Nginx (80/8080) → hamalog-app (내부 8080)
                                    ↓
                              mysql-hamalog (내부 3306)
                              redis-hamalog (내부 6379)
```

**보안 구성:**
- Nginx가 외부 요청을 받아 hamalog-app으로 프록시
- MySQL/Redis는 외부 포트 노출 없이 Docker 내부 네트워크에서만 접근 가능
- 봇 차단, Rate Limiting이 Nginx 레벨에서 처리되어 애플리케이션 리소스 절약
- X-Real-IP, X-Forwarded-For 헤더를 통해 실제 클라이언트 IP 전달

### 실제 클라이언트 IP 복원 (Docker 환경) ⚠️

Docker 브릿지 네트워크에서는 모든 요청의 소스 IP가 도커 게이트웨이(예: `172.18.0.1`)로 보일 수 있습니다.
이 경우 악성 공격자 IP를 식별하고 차단하기 어려우므로, 반드시 아래 방법 중 하나를 적용해야 합니다.

#### 방법 A: `network_mode: host` 사용 (권장 ⭐)

Docker 네트워크 격리(NAT)를 비활성화하여 Nginx가 직접 실제 IP를 볼 수 있게 합니다.

**`docker-compose.yml` 설정:**
```yaml
services:
  nginx:
    image: nginx:alpine
    network_mode: host  # Docker NAT 우회
    # ports: 설정 불필요 (host 모드에서는 호스트의 80/443 포트 직접 사용)
    volumes:
      - ./nginx-docker.conf:/etc/nginx/conf.d/default.conf:ro

  hamalog-app:
    # Nginx(host 모드)가 접근할 수 있도록 localhost에 바인딩
    ports:
      - "127.0.0.1:8080:8080"
```

**`nginx-docker.conf` upstream 설정:**
```nginx
upstream hamalog_backend {
    # host 모드에서는 Docker 서비스명 대신 localhost 사용
    server 127.0.0.1:8080;
    keepalive 32;
}
```

**장점:**
- Nginx가 `$remote_addr`에서 직접 실제 공인 IP를 볼 수 있음
- 추가 설정 없이 즉시 작동

**주의:**
- 호스트의 80, 443 포트가 다른 프로세스에서 사용 중이면 충돌 발생
- WSL2 환경에서 효과적

#### 방법 B: Cloudflare Tunnel 사용 (보안 권장)

홈 서버 운영 시 포트 포워딩 대신 Cloudflare Tunnel 사용을 권장합니다.

**장점:**
- 서버 공인 IP 노출 없이 안전한 외부 접근
- Cloudflare가 `X-Forwarded-For` 헤더에 실제 IP를 넣어줌
- DDoS 보호 기본 제공

**설정:**
1. 도메인을 Cloudflare에 연결
2. `cloudflared` 컨테이너 배포
3. `nginx-docker.conf`에서 Cloudflare IP 대역 신뢰 설정:
```nginx
# Cloudflare IP 대역 (https://www.cloudflare.com/ips/)
set_real_ip_from 103.21.244.0/22;
set_real_ip_from 103.22.200.0/22;
set_real_ip_from 103.31.4.0/22;
# ... (전체 대역 추가)
real_ip_header CF-Connecting-IP;
```

#### 방법 C: 기존 브릿지 네트워크 + X-Forwarded-For (외부 프록시 필요)

앞단에 로드밸런서(AWS ALB, Cloudflare 등)가 있는 경우에만 작동합니다.

**`nginx-docker.conf` 설정:**
```nginx
# Docker 내부 네트워크 신뢰
set_real_ip_from 172.16.0.0/12;
set_real_ip_from 10.0.0.0/8;
set_real_ip_from 192.168.0.0/16;
real_ip_header X-Forwarded-For;
real_ip_recursive on;
```

**⚠️ 주의:** Docker Standalone 환경에서는 앞단 프록시가 없으면 `X-Forwarded-For` 헤더가 비어있어 작동하지 않습니다.

#### 로그 확인

재배포 후 로그에서 실제 IP가 복원되었는지 확인:
```bash
# Docker Compose 환경
docker-compose logs nginx | tail -20

# 성공 시 예상 로그:
# 123.45.67.89 - - [14/Dec/2025:10:00:00 +0000] "GET / HTTP/1.1" ...
# ↑ 실제 외부 IP가 보여야 함 (172.18.0.1이 아님)
```

**Spring Boot 설정 (`application.properties`):**
```properties
# X-Forwarded-For 헤더 처리 활성화 (이미 설정됨)
server.forward-headers-strategy=native
```

```bash
# 프로덕션 환경 (Nginx 포함)
docker-compose up -d

# 개발 환경 (Nginx 없이)
docker-compose -f docker-compose-dev.yml up -d
```

**GitHub Actions 배포:**
- `docker-build.yml`: Windows 온프레미스 서버에 Nginx + hamalog + MySQL + Redis 배포
- `deploy-main.yml`: Linux 서버에 Docker Compose로 전체 스택 배포

### 수동 Nginx 설정

1. 설정 파일 복사:
```bash
sudo cp nginx-hamalog.conf /etc/nginx/sites-available/hamalog
sudo ln -s /etc/nginx/sites-available/hamalog /etc/nginx/sites-enabled/
```

2. 도메인 및 SSL 인증서 경로 수정:
```nginx
server_name your-domain.com;
ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;
```

3. 설정 테스트 및 적용:
```bash
sudo nginx -t
sudo systemctl reload nginx
```

### Let's Encrypt SSL 인증서 발급

```bash
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d your-domain.com
```

## 모니터링 및 로깅

### 보안 이벤트 로그

보안 관련 이벤트는 별도 로그 파일에 기록됩니다:
- `logs/security.log` - 보안 이벤트 (인증 실패, 권한 거부 등)
- `logs/audit.log` - 감사 로그 (데이터 변경, 로그인/로그아웃)

### 차단된 봇 모니터링

차단된 봇 요청은 다음과 같이 로깅됩니다:
```
[BOT_PROTECTION] Blocked request - IP: x.x.x.x, Reason: BLOCKED_USER_AGENT (l9scan), URI: /, Method: GET
```

### JVM 성능 모니터링

성능 저하 감지 시 경고 로그가 생성됩니다:
```
JVM_METRICS [DEGRADED]: Performance degradation detected - CPU Load: 1.05
```

## 긴급 대응

### 특정 IP 긴급 차단

1. **Nginx 레벨 (권장):**
```nginx
# /etc/nginx/conf.d/blocked-ips.conf
deny 123.456.789.0;
deny 123.456.789.0/24;
```

2. **iptables 레벨:**
```bash
sudo iptables -A INPUT -s 123.456.789.0 -j DROP
```

### DDoS 공격 대응

1. Rate Limiting 강화:
```nginx
limit_req_zone $binary_remote_addr zone=api_limit:10m rate=5r/s;
```

2. Cloudflare 또는 AWS WAF 활성화 고려

