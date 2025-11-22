# ⚡ SSH 키 설정 - 빠른 가이드 (3분)

## 🎯 목표
GitHub Secrets에서 `SERVER_SSH_KEY` 삭제 후 `SSH_PRIVATE_KEY` 새로 생성

---

## 📱 **가장 쉬운 방법 (Mac 기준)**

### 1️⃣ SSH 키 확인 및 복사 (30초)

```bash
# 터미널에서 실행
cat ~/.ssh/id_rsa | pbcopy

# 완료! 클립보드에 복사됨
```

### 2️⃣ GitHub Secrets 삭제 (1분)

```
1. GitHub 접속 → Repository
2. Settings (톱니바퀴) 클릭
3. 왼쪽: Secrets and variables → Actions
4. SERVER_SSH_KEY 클릭
5. Delete secret 버튼 클릭
6. Confirm 클릭
```

### 3️⃣ GitHub Secrets 새로 생성 (1분)

```
1. "New repository secret" 클릭
2. Name 입력: SSH_PRIVATE_KEY
3. Value 붙여넣기 (Cmd+V)
4. "Add secret" 클릭
```

**완료! 🎉**

---

## 💻 **CLI로 자동화 (권장)**

### 한 줄로 끝내기

```bash
# 1. GitHub CLI 로그인 (처음 한 번만)
gh auth login

# 2. SSH 키 자동 업로드
gh secret set SSH_PRIVATE_KEY < ~/.ssh/id_rsa

# 3. 확인
gh secret list
```

**예상 출력:**
```
SSH_PRIVATE_KEY    Updated 5 seconds ago
```

**완료! 🎉**

---

## 🔍 SSH 키 경로별 명령어

### 기본 키 위치

```bash
# 기본 SSH 키 (가장 일반적)
cat ~/.ssh/id_rsa | pbcopy

# GitHub CLI로 업로드
gh secret set SSH_PRIVATE_KEY < ~/.ssh/id_rsa
```

### 다른 위치의 키

```bash
# hamalog_deploy 키를 사용하는 경우
cat ~/.ssh/hamalog_deploy | pbcopy

# GitHub CLI로 업로드
gh secret set SSH_PRIVATE_KEY < ~/.ssh/hamalog_deploy
```

### 키가 어디에 있는지 모르는 경우

```bash
# 모든 SSH 키 확인
ls -la ~/.ssh/

# 예상 출력:
# id_rsa            ← 이것을 사용
# id_rsa.pub
# known_hosts
```

---

## ✅ 설정 후 확인

### GitHub CLI로 확인

```bash
gh secret list | grep SSH_PRIVATE_KEY

# 출력 예시:
# SSH_PRIVATE_KEY    Updated 1 second ago
```

### GitHub 웹사이트에서 확인

```
GitHub Repository
  → Settings
  → Secrets and variables
  → Actions
  → SSH_PRIVATE_KEY이 목록에 있는지 확인
```

---

## 🚀 다음: 나머지 필수 Secrets 설정

SSH_PRIVATE_KEY 설정 후, 다음 3개도 설정하세요:

```bash
# CLI로 한 번에 설정
gh secret set SERVER_PORT -b "22"
gh secret set JWT_EXPIRY -b "900000"
gh secret set JWT_REFRESH_TOKEN_EXPIRY -b "604800000"
gh secret set KAKAO_REDIRECT_URI -b "http://49.142.154.182:8080/oauth2/auth/kakao/callback"
```

---

## 📞 만약 SSH 키가 없다면?

### SSH 키 생성

```bash
# 새로운 SSH 키 생성
ssh-keygen -t rsa -b 4096 -f ~/.ssh/id_rsa -N ""

# 또는 프로젝트용 키
ssh-keygen -t rsa -b 4096 -f ~/.ssh/hamalog_deploy -N ""

# 생성 확인
cat ~/.ssh/id_rsa
```

### 서버에 공개키 등록

```bash
# 서버 접속
ssh ubuntu@49.142.154.182

# 서버에서 실행
mkdir -p ~/.ssh
cat >> ~/.ssh/authorized_keys << 'EOF'
(로컬의 ~/.ssh/id_rsa.pub 내용 붙여넣기)
EOF

chmod 600 ~/.ssh/authorized_keys
```

---

## 📋 체크리스트

```
[ ] 1. SSH 키 확인
   $ cat ~/.ssh/id_rsa

[ ] 2. SSH 키 복사
   $ cat ~/.ssh/id_rsa | pbcopy (Mac)

[ ] 3. GitHub Secrets 삭제
   Settings → Secrets → SERVER_SSH_KEY → Delete

[ ] 4. 새 Secret 생성
   Name: SSH_PRIVATE_KEY
   Value: (붙여넣기)

[ ] 5. Add secret 클릭

[ ] 6. 다른 필수값 설정
   - SERVER_PORT
   - JWT_EXPIRY
   - JWT_REFRESH_TOKEN_EXPIRY
   - KAKAO_REDIRECT_URI
```

---

**완료하면 배포 시작! 🚀**

```bash
git push origin main
```

