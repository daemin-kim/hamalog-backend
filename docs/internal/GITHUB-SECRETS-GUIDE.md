# GitHub Secrets 설정 가이드

> **목적**: GitHub Actions CI/CD 및 프로덕션 배포에 필요한 환경변수 설정
> 
> **위치**: GitHub Repository → Settings → Secrets and variables → Actions

---

## 📋 필수 Secrets 목록

### 데이터베이스 (Database)

| Secret Name | 설명 | 예시 값 | 생성 방법 |
|-------------|------|---------|----------|
| `DB_NAME` | 데이터베이스 이름 | `Hamalog` | - |
| `DB_USERNAME` | DB 사용자명 | `hamalog_user` | - |
| `DB_PASSWORD` | DB 비밀번호 | `StrongP@ss!2026` | `openssl rand -base64 24` |
| `MYSQL_ROOT_PASSWORD` | MySQL root 비밀번호 | `RootP@ss!2026` | `openssl rand -base64 24` |

### JWT 인증 (Authentication)

| Secret Name | 설명 | 예시 값 | 생성 방법 |
|-------------|------|---------|----------|
| `JWT_SECRET` | JWT 서명 키 (Base64, 32바이트 이상) | `YWJjZGVm...` | `openssl rand -base64 32` |
| `JWT_EXPIRY` | Access Token 만료 시간 (ms) | `900000` (15분) | - |
| `JWT_REFRESH_TOKEN_EXPIRY` | Refresh Token 만료 시간 (ms) | `604800000` (7일) | - |

### 데이터 암호화 (Encryption)

| Secret Name | 설명 | 예시 값 | 생성 방법 |
|-------------|------|---------|----------|
| `HAMALOG_ENCRYPTION_KEY` | AES-256 암호화 키 (Base64) | `YWJjZGVm...` | `openssl rand -base64 32` |

### OAuth2 (Kakao)

| Secret Name | 설명 | 예시 값 | 생성 방법 |
|-------------|------|---------|----------|
| `KAKAO_CLIENT_ID` | Kakao REST API 키 | `abc123...` | [Kakao Developers](https://developers.kakao.com) |
| `KAKAO_CLIENT_SECRET` | Kakao 보안 키 | `xyz789...` | [Kakao Developers](https://developers.kakao.com) |

### Redis

| Secret Name | 설명 | 예시 값 | 생성 방법 |
|-------------|------|---------|----------|
| `SPRING_DATA_REDIS_PASSWORD` | Redis 비밀번호 (선택) | `RedisP@ss!` | `openssl rand -base64 16` |

### Discord 알림 (Monitoring)

| Secret Name | 설명 | 예시 값 | 생성 방법 |
|-------------|------|---------|----------|
| `DISCORD_ERROR_WEBHOOK_URL` | 에러 알림 웹훅 URL | `https://discord.com/api/webhooks/...` | Discord 서버 설정 |
| `DISCORD_DLQ_WEBHOOK_URL` | DLQ 알림 웹훅 URL | `https://discord.com/api/webhooks/...` | Discord 서버 설정 |

### Firebase (Push 알림)

| Secret Name | 설명 | 예시 값 | 생성 방법 |
|-------------|------|---------|----------|
| `FIREBASE_CREDENTIALS` | Firebase 서비스 계정 JSON | `{"type":"service_account",...}` | Firebase Console |

### 코드 커버리지 (Code Coverage)

| Secret Name | 설명 | 예시 값 | 생성 방법 |
|-------------|------|---------|----------|
| `CODECOV_TOKEN` | Codecov 업로드 토큰 | `abc123-def456-...` | [Codecov 설정](https://app.codecov.io/gh/{owner}/{repo}/settings) |

### 배포 서버 (Deployment)

| Secret Name | 설명 | 예시 값 | 생성 방법 |
|-------------|------|---------|----------|
| `SERVER_HOST` | 서버 IP/도메인 | `123.45.67.89` | - |
| `SERVER_USER` | SSH 사용자명 | `ubuntu` | - |
| `SERVER_SSH_KEY` | SSH 개인 키 | `-----BEGIN OPENSSH PRIVATE KEY-----...` | `ssh-keygen -t ed25519` |

---

## 🔧 키 생성 명령어

### Base64 인코딩 256비트 키 생성 (JWT_SECRET, ENCRYPTION_KEY)

```bash
# macOS / Linux
openssl rand -base64 32

# 예시 출력: YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY=
```

### 강력한 비밀번호 생성 (DB_PASSWORD 등)

```bash
# 24바이트 Base64 (약 32자)
openssl rand -base64 24

# 예시 출력: abc123XYZ789def456GHI012
```

### SSH 키 생성 (SERVER_SSH_KEY)

```bash
# Ed25519 키 생성 (권장)
ssh-keygen -t ed25519 -C "github-actions@hamalog" -f ~/.ssh/hamalog_deploy

# 개인 키 내용 확인 (GitHub Secret에 저장)
cat ~/.ssh/hamalog_deploy

# 공개 키 (서버의 ~/.ssh/authorized_keys에 추가)
cat ~/.ssh/hamalog_deploy.pub
```

---

## 📝 GitHub Secrets 설정 방법

1. GitHub 저장소로 이동
2. **Settings** → **Secrets and variables** → **Actions** 클릭
3. **New repository secret** 클릭
4. Name과 Secret 값 입력
5. **Add secret** 클릭

---

## 🔐 환경별 설정

### Production (`prod`)

```yaml
# GitHub Actions workflow에서 사용
env:
  SPRING_PROFILES_ACTIVE: prod
  JWT_SECRET: ${{ secrets.JWT_SECRET }}
  DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
  # ...
```

### Development (`dev`)

로컬 개발 시 `.env.dev` 파일 사용:

```bash
# .env.dev 파일 생성 (git에 커밋하지 않음)
cp .env.example .env.dev
# 값 채우기
```

### Benchmark (`benchmark`)

`docker-compose-benchmark.yml`에 이미 테스트용 값 설정됨.

---

## ⚠️ 보안 주의사항

1. **절대 Git에 커밋하지 않기**: `.env`, `.env.prod`, `.env.dev` 파일
2. **주기적 갱신**: 3-6개월마다 키 교체
3. **최소 권한 원칙**: 각 서비스에 필요한 최소 권한만 부여
4. **로그 확인**: 키가 로그에 노출되지 않는지 확인
5. **접근 제한**: GitHub Secrets는 필요한 사용자만 접근 가능하도록 설정

---

## 📎 관련 파일

- `.env.example` - 환경변수 템플릿
- `docker-compose.yml` - 프로덕션 Docker 설정
- `docker-compose-dev.yml` - 개발 Docker 설정
- `docker-compose-benchmark.yml` - 벤치마크 Docker 설정
- `.github/workflows/deploy.yml` - 배포 워크플로우

