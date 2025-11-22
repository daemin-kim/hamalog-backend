# ✅ SSH 키 생성 완료 - 즉시 설정 가이드

## 🎉 좋은 소식!

SSH 키가 성공적으로 생성되었습니다!

```
✅ SSH 키 생성 위치: ~/.ssh/id_rsa
✅ 공개키 위치: ~/.ssh/id_rsa.pub
✅ 키 타입: RSA 4096-bit
✅ 클립보드: 개인키가 이미 복사됨!
```

---

## 🚀 **지금 바로 GitHub Secrets에 설정하기**

### 1️⃣ SSH_PRIVATE_KEY 설정

```
1. GitHub Repository 접속
   https://github.com/daemin-kim/hamalog-backend

2. Settings 클릭 (톱니바퀴 아이콘)

3. 왼쪽 메뉴: Secrets and variables → Actions

4. 먼저 기존 SERVER_SSH_KEY 삭제
   → SERVER_SSH_KEY 클릭
   → Delete secret
   → Confirm

5. "New repository secret" 클릭

6. 입력:
   Name: SSH_PRIVATE_KEY
   Value: Cmd+V (이미 클립보드에 복사됨!)

7. "Add secret" 클릭
```

✅ **완료!**

---

## 🔧 **나머지 필수 값 설정** (GitHub UI에서)

### 2️⃣ SERVER_PORT
```
Name: SERVER_PORT
Value: 22
```

### 3️⃣ JWT_EXPIRY
```
Name: JWT_EXPIRY
Value: 900000
```

### 4️⃣ JWT_REFRESH_TOKEN_EXPIRY
```
Name: JWT_REFRESH_TOKEN_EXPIRY
Value: 604800000
```

### 5️⃣ KAKAO_REDIRECT_URI
```
Name: KAKAO_REDIRECT_URI
Value: http://49.142.154.182:8080/oauth2/auth/kakao/callback
```

---

## ✅ 확인

GitHub CLI 설치 후 다음 명령어로 확인:

```bash
gh secret list
```

예상 출력:
```
SSH_PRIVATE_KEY              Updated 1 second ago
SERVER_HOST                  Updated 6 months ago
SERVER_USER                  Updated 6 months ago
SERVER_PORT                  Updated 1 second ago
JWT_SECRET                   Updated 54 minutes ago
JWT_EXPIRY                   Updated 1 second ago
...
```

---

## 🎯 **모든 설정 완료 후**

```bash
git push origin main
```

**배포 시작! 🚀**


