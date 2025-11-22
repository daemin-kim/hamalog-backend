# ✅ 새로 설정해야 할 GitHub Secrets 목록

## 📊 현황 분석

### ✅ 이미 설정된 Secrets (11개)

| Secret Name | 상태 | 설명 |
|------------|------|------|
| CR_PAT | ✅ 설정됨 | GitHub 접근 토큰 |
| DB_PASSWORD | ✅ 설정됨 | 데이터베이스 비밀번호 |
| HAMALOG_ENCRYPTION_KEY | ✅ 설정됨 | 데이터 암호화 키 |
| JWT_SECRET | ✅ 설정됨 | JWT 서명 키 |
| KAKAO_CLIENT_ID | ✅ 설정됨 | 카카오 앱 ID |
| KAKAO_CLIENT_SECRET | ✅ 설정됨 | 카카오 클라이언트 시크릿 |
| MYSQL_ROOT_PASSWORD | ✅ 설정됨 | MySQL Root 비밀번호 |
| SERVER_HOST | ✅ 설정됨 | 서버 IP |
| SERVER_USER | ✅ 설정됨 | SSH 사용자명 |
| SPRING_DATA_REDIS_PASSWORD | ✅ 설정됨 | Redis 비밀번호 |
| SERVER_SSH_KEY | ⚠️ 이름 변경 필요 | SSH 개인키 (SSH_PRIVATE_KEY로 변경) |

---

## 🚨 **새로 설정해야 할 Secrets (7개)**

### 🔴 **최우선 필수 (3개)** - 지금 바로 설정!

#### 1️⃣ **SSH_PRIVATE_KEY** ⚠️ 중요!
```
이름: SSH_PRIVATE_KEY
값: 기존 SERVER_SSH_KEY의 값을 복사
```

**⚠️ 중요 사항:**
- 현재 `SERVER_SSH_KEY`라는 이름으로 설정되어 있음
- GitHub Actions workflow가 `SSH_PRIVATE_KEY`라는 이름을 찾고 있음
- **GitHub UI에서 이름 변경 불가** → 삭제 후 새로 생성 필요

**해결 방법:**
1. 기존 `SERVER_SSH_KEY` 삭제
2. `SSH_PRIVATE_KEY` 새로 생성

**🚀 한 줄로 끝내기 (CLI 권장):**
```bash
# 로컬 SSH 키를 GitHub에 자동 업로드
gh secret set SSH_PRIVATE_KEY < ~/.ssh/id_rsa

# 확인
gh secret list | grep SSH_PRIVATE_KEY
```

**📖 상세 가이드:** [SSH_KEY_QUICK_SETUP.md](SSH_KEY_QUICK_SETUP.md) 참고

#### 2️⃣ **SERVER_PORT**
```
이름: SERVER_PORT
값: 22
```

**설명:** SSH 접속 포트 (기본값: 22)

#### 3️⃣ **KAKAO_REDIRECT_URI**
```
이름: KAKAO_REDIRECT_URI
값: http://49.142.154.182:8080/oauth2/auth/kakao/callback
```

**설명:** 카카오 로그인 콜백 URI

---

### 🟠 **필수 (2개)** - JWT 토큰 설정

#### 4️⃣ **JWT_EXPIRY**
```
이름: JWT_EXPIRY
값: 900000
```

**설명:** Access Token 유효시간 (단위: 밀리초, 15분)

#### 5️⃣ **JWT_REFRESH_TOKEN_EXPIRY**
```
이름: JWT_REFRESH_TOKEN_EXPIRY
값: 604800000
```

**설명:** Refresh Token 유효시간 (단위: 밀리초, 7일)

---

### 🟡 **권장 (2개)** - 데이터베이스 설정

#### 6️⃣ **DB_NAME**
```
이름: DB_NAME
값: Hamalog
```

**설명:** 생성될 데이터베이스 이름

#### 7️⃣ **DB_USERNAME**
```
이름: DB_USERNAME
값: hamalog_user
```

**설명:** 데이터베이스 접속 사용자명

---

### 🟢 **선택사항 (2개)** - CORS 및 프론트엔드

| Secret Name | 권장값 | 설명 |
|------------|-------|------|
| **FRONTEND_URL** | `http://49.142.154.182:3000` | 프론트엔드 서버 주소 (기본값 제공) |
| **ALLOWED_ORIGINS** | `http://49.142.154.182:3000,http://49.142.154.182:8080` | CORS 허용 도메인 (기본값 제공) |

---

## 📋 **설정 순서 (권장)**

### **1단계: SSH 설정 (최우선)**
```
1. SERVER_SSH_KEY를 SSH_PRIVATE_KEY로 이름 변경
   또는 값을 복사해서 SSH_PRIVATE_KEY 새로 생성
2. SERVER_SSH_KEY 삭제
```

### **2단계: 서버 연결 정보**
```
1. SERVER_PORT = 22
```

### **3단계: Kakao OAuth2**
```
1. KAKAO_REDIRECT_URI = http://49.142.154.182:8080/oauth2/auth/kakao/callback
```

### **4단계: JWT 토큰**
```
1. JWT_EXPIRY = 900000
2. JWT_REFRESH_TOKEN_EXPIRY = 604800000
```

### **5단계: 데이터베이스**
```
1. DB_NAME = Hamalog
2. DB_USERNAME = hamalog_user
```

### **6단계: 선택사항 (필요시)**
```
1. FRONTEND_URL = http://49.142.154.182:3000
2. ALLOWED_ORIGINS = http://49.142.154.182:3000,http://49.142.154.182:8080
```

---

## ⚡ **빠른 설정 (GitHub CLI 사용)**

```bash
# SSH 키 이름 변경이 안 되면, 기존 값 복사 후 새로 생성
gh secret set SSH_PRIVATE_KEY < ~/.ssh/id_rsa

# 또는 이미 있는 값 복사
gh secret set SERVER_PORT -b "22"
gh secret set JWT_EXPIRY -b "900000"
gh secret set JWT_REFRESH_TOKEN_EXPIRY -b "604800000"
gh secret set KAKAO_REDIRECT_URI -b "http://49.142.154.182:8080/oauth2/auth/kakao/callback"
gh secret set DB_NAME -b "Hamalog"
gh secret set DB_USERNAME -b "hamalog_user"
```

---

## ✅ **최종 체크리스트**

배포 전에 다음을 확인하세요:

```
GitHub Repository Settings → Secrets and variables → Actions

필수 (반드시 설정):
[ ] SSH_PRIVATE_KEY ← 🚨 반드시 필요 (SERVER_SSH_KEY 대체)
[ ] SERVER_HOST ✅ 이미 설정됨
[ ] SERVER_USER ✅ 이미 설정됨
[ ] SERVER_PORT ← 새로 설정 필요
[ ] JWT_SECRET ✅ 이미 설정됨
[ ] JWT_EXPIRY ← 새로 설정 필요
[ ] JWT_REFRESH_TOKEN_EXPIRY ← 새로 설정 필요
[ ] HAMALOG_ENCRYPTION_KEY ✅ 이미 설정됨
[ ] KAKAO_CLIENT_ID ✅ 이미 설정됨
[ ] KAKAO_CLIENT_SECRET ✅ 이미 설정됨
[ ] KAKAO_REDIRECT_URI ← 새로 설정 필요
[ ] DB_PASSWORD ✅ 이미 설정됨
[ ] MYSQL_ROOT_PASSWORD ✅ 이미 설정됨
[ ] SPRING_DATA_REDIS_PASSWORD ✅ 이미 설정됨

권장 (설정하면 좋음):
[ ] DB_NAME ← 새로 설정 필요
[ ] DB_USERNAME ← 새로 설정 필요

선택 (기본값 있음):
[ ] FRONTEND_URL (선택)
[ ] ALLOWED_ORIGINS (선택)
```

---

## 🚀 **설정 완료 후 배포**

모든 설정 완료 후:

```bash
git push origin main
```

GitHub Actions가 자동으로 배포를 시작합니다! 🎉

**예상 배포 시간:** 2-3분

---

## 📝 **요약**

| 상태 | 개수 | 항목 |
|------|------|------|
| ✅ 이미 설정 | 11개 | CR_PAT, DB_PASSWORD, HAMALOG_ENCRYPTION_KEY 등 |
| 🚨 반드시 설정 | 1개 | **SSH_PRIVATE_KEY** (SERVER_SSH_KEY 대체) |
| ⚠️ 새로 설정 필요 | 3개 | SERVER_PORT, KAKAO_REDIRECT_URI, JWT_EXPIRY, JWT_REFRESH_TOKEN_EXPIRY |
| 🟡 권장 설정 | 2개 | DB_NAME, DB_USERNAME |
| 🟢 선택 설정 | 2개 | FRONTEND_URL, ALLOWED_ORIGINS |

**최소 필수:** 🚨 1개 + ⚠️ 3개 = **4개 설정 필요**  
**모두 설정하면:** 4개 + 🟡 2개 = **6개 새로 추가**

---

**마지막 업데이트:** 2025-11-22  
**상태:** 📋 설정 준비 완료  
**버전:** 1.0.0

