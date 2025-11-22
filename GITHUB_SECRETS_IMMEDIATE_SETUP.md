# 🚨 GitHub Secrets 필수 설정 가이드 (즉시 필요)

## 📋 개요

현재 상황:
- ✅ **11개의 Secrets이 이미 설정됨**
- 🚨 **4개만 추가로 설정하면 배포 가능!**
- 🟡 **2개 더 설정하면 완벽!**

**⚠️ 중요**: 4개의 필수 Secrets이 설정되어야만 배포가 시작됩니다!

---

## 🚀 빠른 시작 (2분)

### ⚠️ 중요: SSH_PRIVATE_KEY 이름 변경

현재 `SERVER_SSH_KEY`로 설정되어 있으나, workflow가 `SSH_PRIVATE_KEY`를 찾고 있습니다.

**해결 방법:**
```
GitHub Repository Settings → Secrets and variables → Actions
→ SERVER_SSH_KEY 클릭
→ "Update secret" 클릭
→ Name을 SSH_PRIVATE_KEY로 변경
→ "Update secret" 클릭
```

또는 새로 생성하고 기존 것 삭제:
```bash
# 기존 값 확인 후 복사
# GitHub UI에서 SERVER_SSH_KEY의 값 복사

# 새로 생성
GitHub → Settings → Secrets → New repository secret
Name: SSH_PRIVATE_KEY
Value: (SERVER_SSH_KEY의 값 붙여넣기)

# 기존 것 삭제
SERVER_SSH_KEY → Delete
```

### 새로 설정해야 할 4개의 필수 Secrets

```
1️⃣ SERVER_PORT = 22
2️⃣ JWT_EXPIRY = 900000
3️⃣ JWT_REFRESH_TOKEN_EXPIRY = 604800000
4️⃣ KAKAO_REDIRECT_URI = http://49.142.154.182:8080/oauth2/auth/kakao/callback
```

### 권장: 추가로 2개 설정

```
5️⃣ DB_NAME = Hamalog
6️⃣ DB_USERNAME = hamalog_user
```

### GitHub CLI로 빠르게 설정 (권장)

```bash
gh secret set SSH_PRIVATE_KEY < ~/.ssh/id_rsa
gh secret set SERVER_PORT -b "22"
gh secret set JWT_EXPIRY -b "900000"
gh secret set JWT_REFRESH_TOKEN_EXPIRY -b "604800000"
gh secret set KAKAO_REDIRECT_URI -b "http://49.142.154.182:8080/oauth2/auth/kakao/callback"
gh secret set DB_NAME -b "Hamalog"
gh secret set DB_USERNAME -b "hamalog_user"
```

### 1단계: 서버 정보 수집

배포 대상 서버의 정보를 준비합니다:

```bash
# 서버에 접속하여 다음 정보를 확인/복사하세요

# SSH 개인키 확인
cat ~/.ssh/id_rsa

# 또는 새로운 SSH 키 생성
ssh-keygen -t rsa -b 4096 -f ~/.ssh/hamalog_deploy -N ""
cat ~/.ssh/hamalog_deploy

# 서버 IP 확인
hostname -I

# 현재 사용자명 확인
whoami
```

### 2단계: 보안 키 생성

```bash
# 로컬에서 실행하여 각 Secret 값 생성

# JWT Secret 생성
openssl rand -base64 32

# 암호화 키 생성
openssl rand -base64 32

# 강한 비밀번호 생성
openssl rand -base64 32 | tr -d "=+/" | cut -c1-32
```

### 3단계: GitHub에 Secrets 설정

```
GitHub Repository 
  → Settings 
  → Secrets and variables 
  → Actions 
  → New repository secret
```

---

## 📝 필수 18개 GitHub Secrets

### 1️⃣ 서버 연결 정보 (4개) - 🚨 필수

| Secret Name | 값 | 예시 | 설명 |
|------------|------|-------|------|
| **SSH_PRIVATE_KEY** | SSH 개인키 | `-----BEGIN RSA PRIVATE KEY-----\n...` | 서버 접속용 SSH 개인키 (전체 내용) |
| **SERVER_HOST** | 서버 IP | `49.142.154.182` | 배포 대상 서버의 IP 주소 |
| **SERVER_USER** | SSH 사용자명 | `ubuntu` | 서버의 SSH 접속 사용자명 |
| **SERVER_PORT** | SSH 포트 | `22` | SSH 접속 포트 (기본: 22) |

### 2️⃣ JWT 설정 (3개) - 🚨 필수

| Secret Name | 값 | 예시 | 설명 |
|------------|------|-------|------|
| **JWT_SECRET** | JWT 서명 키 | Base64 32바이트 | `openssl rand -base64 32` |
| **JWT_EXPIRY** | Access Token 유효시간 | `900000` | 단위: ms (기본: 15분) |
| **JWT_REFRESH_TOKEN_EXPIRY** | Refresh Token 유효시간 | `604800000` | 단위: ms (기본: 7일) |

### 3️⃣ 암호화 (1개) - 🚨 필수

| Secret Name | 값 | 예시 | 설명 |
|------------|------|-------|------|
| **HAMALOG_ENCRYPTION_KEY** | AES-256 키 | Base64 32바이트 | `openssl rand -base64 32` |

### 4️⃣ Kakao OAuth2 (3개) - 🚨 필수

| Secret Name | 값 | 예시 | 설명 |
|------------|------|-------|------|
| **KAKAO_CLIENT_ID** | Kakao 앱 ID | `1234567890` | https://developers.kakao.com에서 발급 |
| **KAKAO_CLIENT_SECRET** | Kakao Client Secret | `xxxxxxxxxxxxx` | https://developers.kakao.com에서 발급 |
| **KAKAO_REDIRECT_URI** | OAuth2 콜백 URI | `http://49.142.154.182:8080/oauth2/auth/kakao/callback` | Kakao 앱 설정과 동일해야 함 |

### 5️⃣ 데이터베이스 (4개) - 🚨 필수

| Secret Name | 값 | 예시 | 설명 |
|------------|------|-------|------|
| **DB_NAME** | DB 이름 | `Hamalog` | 생성될 데이터베이스 이름 |
| **DB_USERNAME** | DB 사용자명 | `hamalog_user` | 데이터베이스 접속 사용자 |
| **DB_PASSWORD** | DB 비밀번호 | 강한 비밀번호 | 12자 이상, 특수문자 포함 |
| **MYSQL_ROOT_PASSWORD** | MySQL Root 비밀번호 | 강한 비밀번호 | 16자 이상, 특수문자 포함 |

### 6️⃣ Redis (1개) - 🚨 필수

| Secret Name | 값 | 예시 | 설명 |
|------------|------|-------|------|
| **SPRING_DATA_REDIS_PASSWORD** | Redis 비밀번호 | 강한 비밀번호 | 12자 이상 |

### 7️⃣ 프론트엔드/CORS (2개) - ✅ 선택 (기본값 제공)

| Secret Name | 값 | 기본값 | 설명 |
|------------|------|-------|------|
| **FRONTEND_URL** | 프론트엔드 URL | `http://49.142.154.182:3000` | 프론트엔드 서버 주소 |
| **ALLOWED_ORIGINS** | CORS 허용 도메인 | `http://49.142.154.182:3000,http://49.142.154.182:8080` | 쉼표로 구분 |

---

## ⚙️ Secrets 설정 방법

### 방법 1: GitHub 웹 인터페이스 (권장 - GUI)

**1단계: Settings 접속**
```
GitHub Repository 
  → Settings (톱니바퀴 아이콘)
  → 왼쪽 메뉴: Secrets and variables
  → Actions
```

**2단계: New repository secret 클릭**

**3단계: 각 Secret 추가**
```
Name: SSH_PRIVATE_KEY
Value: -----BEGIN RSA PRIVATE KEY-----
       MIIEpAIBAAKCAQEA...
       (전체 개인키 내용 붙여넣기)
       -----END RSA PRIVATE KEY-----
```

**4단계: Add secret 클릭**

18개 모두 반복합니다.

### 방법 2: GitHub CLI (빠른 설정)

```bash
# GitHub CLI 설치 (이미 설치되어 있으면 스킵)
# https://cli.github.com

# 로그인
gh auth login

# Secrets 설정 (예시)
gh secret set SSH_PRIVATE_KEY < ~/.ssh/id_rsa
gh secret set SERVER_HOST -b "49.142.154.182"
gh secret set SERVER_USER -b "ubuntu"
gh secret set SERVER_PORT -b "22"
gh secret set JWT_SECRET -b "$(openssl rand -base64 32)"
gh secret set HAMALOG_ENCRYPTION_KEY -b "$(openssl rand -base64 32)"
gh secret set DB_PASSWORD -b "$(openssl rand -base64 32)"
gh secret set MYSQL_ROOT_PASSWORD -b "$(openssl rand -base64 32)"
gh secret set SPRING_DATA_REDIS_PASSWORD -b "$(openssl rand -base64 32)"
gh secret set KAKAO_CLIENT_ID -b "<Kakao App ID>"
gh secret set KAKAO_CLIENT_SECRET -b "<Kakao Client Secret>"
gh secret set KAKAO_REDIRECT_URI -b "http://49.142.154.182:8080/oauth2/auth/kakao/callback"
```

### 방법 3: 자동 생성 스크립트

```bash
chmod +x ./generate-github-secrets.sh
./generate-github-secrets.sh
```

스크립트 출력 값들을 GitHub에 복사하여 설정합니다.

---

## ✅ 검증 체크리스트

Secrets 설정 후 다음을 확인하세요:

```
GitHub Repository Settings → Secrets and variables → Actions

✅ 이미 설정된 것들:
[✓] CR_PAT
[✓] DB_PASSWORD
[✓] HAMALOG_ENCRYPTION_KEY
[✓] JWT_SECRET
[✓] KAKAO_CLIENT_ID
[✓] KAKAO_CLIENT_SECRET
[✓] MYSQL_ROOT_PASSWORD
[✓] SERVER_HOST
[✓] SERVER_USER
[✓] SPRING_DATA_REDIS_PASSWORD

🚨 반드시 설정 필요:
[ ] SSH_PRIVATE_KEY ← SERVER_SSH_KEY 이름 변경 또는 새로 생성
[ ] SERVER_PORT ← 새로 설정
[ ] JWT_EXPIRY ← 새로 설정
[ ] JWT_REFRESH_TOKEN_EXPIRY ← 새로 설정
[ ] KAKAO_REDIRECT_URI ← 새로 설정

🟡 권장 설정:
[ ] DB_NAME ← 새로 설정
[ ] DB_USERNAME ← 새로 설정

🟢 선택 (기본값 있음):
[ ] FRONTEND_URL (선택)
[ ] ALLOWED_ORIGINS (선택)
```

**최소 필수:** 🚨 4-5개 만 설정하면 배포 시작!

---

**자세한 내용은 [GITHUB_SECRETS_TODO.md](GITHUB_SECRETS_TODO.md)를 참고하세요.**

---

## 🔍 배포 테스트

### 1단계: 코드 푸시

```bash
git push origin main
```

### 2단계: GitHub Actions 모니터링

```
GitHub Repository 
  → Actions
  → 최신 workflow run 확인
  → 상세 로그 확인
```

### 3단계: 배포 성공 확인

**성공 시:**
```
✅ Deployment successful - Application is healthy
```

**실패 시:**
```
❌ DEPLOYMENT FAILED: Missing required secrets:...
```

### 4단계: 서버에서 확인

```bash
ssh -p 22 ubuntu@49.142.154.182

# 컨테이너 상태 확인
docker-compose ps

# 로그 확인
docker-compose logs -f hamalog-app

# 헬스 체크
curl http://localhost:8080/actuator/health
```

---

## 🚨 일반적인 에러 해결

### 1. "Missing required secrets" 에러

**원인**: GitHub Secrets이 설정되지 않음

**해결**:
1. GitHub Settings에서 모든 필수 Secrets 확인
2. Secret 값이 비어있지 않은지 확인
3. 특수문자가 제대로 이스케이프되었는지 확인 (특히 SSH_PRIVATE_KEY)

### 2. "SSH connection failed" 에러

**원인**: SERVER_HOST, SERVER_USER, SERVER_PORT 설정 오류

**해결**:
```bash
# 로컬에서 서버에 SSH 접속 테스트
ssh -v -p 22 ubuntu@49.142.154.182

# 성공하면 설정이 올바름
# 실패하면 SERVER_HOST, SERVER_USER, SERVER_PORT 다시 확인
```

### 3. "Client id of registration 'kakao' must not be empty" 에러

**원인**: KAKAO_CLIENT_ID, KAKAO_CLIENT_SECRET 미설정

**해결**:
1. https://developers.kakao.com 접속
2. 앱 선택 → 앱 설정 → 앱 정보
3. REST API 키와 Client Secret 복사
4. GitHub Secrets에 설정

### 4. "Permission denied (publickey)" 에러

**원인**: SSH_PRIVATE_KEY가 잘못되었거나 서버의 authorized_keys가 설정되지 않음

**해결**:
```bash
# 서버에 접속
ssh ubuntu@49.142.154.182

# 공개키 설정 확인
cat ~/.ssh/authorized_keys

# 권한 확인
ls -la ~/.ssh/
# 권한이 700이어야 함
chmod 700 ~/.ssh
chmod 600 ~/.ssh/authorized_keys
```

---

## 📊 설정 우선순위

| 우선순위 | Secret | 필수 여부 | 완료 |
|---------|--------|---------|------|
| 🔴 1순위 | SERVER_HOST | 🚨 필수 | [ ] |
| 🔴 1순위 | SERVER_USER | 🚨 필수 | [ ] |
| 🔴 1순위 | SERVER_PORT | 🚨 필수 | [ ] |
| 🔴 1순위 | SSH_PRIVATE_KEY | 🚨 필수 | [ ] |
| 🟠 2순위 | JWT_SECRET | 🚨 필수 | [ ] |
| 🟠 2순위 | HAMALOG_ENCRYPTION_KEY | 🚨 필수 | [ ] |
| 🟠 2순위 | DB_PASSWORD | 🚨 필수 | [ ] |
| 🟠 2순위 | KAKAO_CLIENT_ID | 🚨 필수 | [ ] |
| 🟠 2순위 | KAKAO_CLIENT_SECRET | 🚨 필수 | [ ] |
| 🟡 3순위 | 나머지 필수 항목 | 🚨 필수 | [ ] |
| 🟢 4순위 | FRONTEND_URL | ✅ 선택 | [ ] |
| 🟢 4순위 | ALLOWED_ORIGINS | ✅ 선택 | [ ] |

**적어도 1순위와 2순위는 반드시 설정하세요!**

---

## 📚 추가 정보

### 관련 문서
- [DEPLOYMENT_TROUBLESHOOTING.md](DEPLOYMENT_TROUBLESHOOTING.md) - 배포 문제 해결
- [GITHUB_SECRETS_SETUP.md](GITHUB_SECRETS_SETUP.md) - 상세 설정 가이드
- [AUTOMATED_DEPLOYMENT_GUIDE.md](AUTOMATED_DEPLOYMENT_GUIDE.md) - 배포 전략

### 유용한 링크
- [GitHub Secrets 공식 문서](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [Kakao Developers](https://developers.kakao.com)
- [OpenSSL 커맨드](https://www.openssl.org/docs/man1.1.1/man1/openssl.html)

---

## 🎯 다음 단계

1. ✅ 모든 18개 Secrets 설정
2. ✅ `git push origin main` 실행
3. ✅ GitHub Actions 탭에서 배포 진행 상황 모니터링
4. ✅ 배포 완료 확인

**예상 배포 시간**: 2-3분

---

**마지막 업데이트**: 2025-11-22  
**상태**: 🚀 배포 준비 완료  
**버전**: 2.0.0

