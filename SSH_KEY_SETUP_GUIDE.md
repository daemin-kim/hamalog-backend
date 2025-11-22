# SSH 키 확인 및 GitHub Secrets 설정 가이드

## 📋 현재 상황

- ❌ GitHub Secrets에서 직접 이름 변경 불가
- ✅ 기존 `SERVER_SSH_KEY` 삭제 후 `SSH_PRIVATE_KEY` 새로 생성 필요

---

## 🔍 Step 1: 로컬에서 SSH 키 확인

### 방법 1: 기존 SSH 키 확인 (가장 간단)

```bash
# 기본 위치의 SSH 키 확인
cat ~/.ssh/id_rsa

# 또는 다른 위치가 있으면
cat ~/.ssh/hamalog_deploy
ls -la ~/.ssh/
```

**출력 예시:**
```
-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEA3x8y9jK...
... (많은 줄들)
-----END RSA PRIVATE KEY-----
```

### 방법 2: 모든 SSH 키 확인

```bash
# SSH 디렉토리의 모든 키 목록
ls -la ~/.ssh/

# 예상 출력:
# id_rsa         (기본 키)
# id_rsa.pub     (공개키)
# hamalog_deploy (프로젝트용 키)
# hamalog_deploy.pub
# known_hosts
# authorized_keys
```

### 방법 3: SSH 키가 있는지 확인

```bash
# 기본 SSH 키 존재 확인
[ -f ~/.ssh/id_rsa ] && echo "✅ SSH 키 있음" || echo "❌ SSH 키 없음"

# 전체 키 개수 확인
ls -1 ~/.ssh/ | grep -E "^id_|_key$|_rsa$" | wc -l
```

---

## 🔑 Step 2: SSH 키 내용 확인하기

### ✅ **가장 쉬운 방법 (터미널)**

```bash
# 기본 SSH 키 확인
cat ~/.ssh/id_rsa

# 전체 출력을 파일에 저장 (복사하기 쉽게)
cat ~/.ssh/id_rsa > ~/ssh_key.txt
cat ~/ssh_key.txt
```

**복사 방법:**
```bash
# 1. 내용 전체 선택 (터미널에서 Command+A)
# 2. 복사 (Command+C)
# 3. GitHub에 붙여넣기
```

### ✅ **Mac에서 클립보드로 복사**

```bash
# 기본 키를 클립보드에 복사
cat ~/.ssh/id_rsa | pbcopy

# 그 후 GitHub에 바로 붙여넣기 (Cmd+V)
```

### ✅ **Linux에서 클립보드로 복사**

```bash
# Linux (xclip 필요)
cat ~/.ssh/id_rsa | xclip -selection clipboard

# 또는
cat ~/.ssh/id_rsa | xsel --clipboard --input
```

### ✅ **Windows에서 클립보드로 복사**

```bash
# PowerShell
Get-Content ~/.ssh/id_rsa | Set-Clipboard

# 또는 Git Bash
cat ~/.ssh/id_rsa | clip
```

---

## 🚀 Step 3: GitHub Secrets 삭제 및 새로 생성

### 삭제 단계

1. **GitHub Repository 접속**
   ```
   https://github.com/[username]/[repository]
   ```

2. **Settings 클릭**
   ```
   Repository → Settings (톱니바퀴 아이콘)
   ```

3. **Secrets 메뉴 접속**
   ```
   왼쪽 메뉴 → Secrets and variables → Actions
   ```

4. **SERVER_SSH_KEY 삭제**
   ```
   SERVER_SSH_KEY 클릭
   → Delete secret 클릭
   → 확인
   ```

### 새로 생성 단계

1. **New repository secret 클릭**

2. **정보 입력**
   ```
   Name: SSH_PRIVATE_KEY
   Value: (SSH 키 전체 내용 붙여넣기)
   ```

3. **Add secret 클릭**

---

## ⚡ **GitHub CLI로 한 번에 하기 (권장)**

### 설치 확인

```bash
# GitHub CLI 설치 확인
gh --version

# 설치되지 않았으면
# macOS: brew install gh
# Linux: https://github.com/cli/cli/releases
# Windows: choco install gh
```

### 로그인

```bash
# GitHub 계정으로 로그인
gh auth login

# 선택 사항:
# - GitHub.com 선택
# - HTTPS 선택
# - Y (인증 토큰 저장)
```

### SSH 키 설정 (한 줄!)

```bash
# 기본 SSH 키 설정
gh secret set SSH_PRIVATE_KEY < ~/.ssh/id_rsa

# 완료! 자동으로 GitHub에 업로드됨
```

**또는 다른 경로의 키:**
```bash
gh secret set SSH_PRIVATE_KEY < ~/.ssh/hamalog_deploy
```

### 설정 확인

```bash
# 생성된 Secret 확인 (내용은 보이지 않음)
gh secret list

# 예상 출력:
# SSH_PRIVATE_KEY    Updated 1 second ago
```

---

## 📝 상세 설정 가이드

### 방법 A: 수동 복사 (GUI)

```
1. 터미널에서 SSH 키 출력
   $ cat ~/.ssh/id_rsa

2. 전체 내용 선택 및 복사 (Cmd+C or Ctrl+C)

3. GitHub 접속
   Settings → Secrets and variables → Actions

4. New repository secret 클릭

5. 입력
   Name: SSH_PRIVATE_KEY
   Value: (붙여넣기 - Cmd+V or Ctrl+V)

6. Add secret 클릭
```

### 방법 B: 파일로 저장 (더 안전)

```bash
# 1. SSH 키를 파일로 저장
cat ~/.ssh/id_rsa > ~/tmp_ssh_key.txt

# 2. 파일 확인
cat ~/tmp_ssh_key.txt

# 3. 파일 열기 (에디터로)
nano ~/tmp_ssh_key.txt
# 또는
open ~/tmp_ssh_key.txt  (Mac)
# 또는
cat ~/tmp_ssh_key.txt | pbcopy  (Mac - 클립보드로)

# 4. GitHub에 붙여넣기

# 5. 임시 파일 삭제 (보안)
rm ~/tmp_ssh_key.txt
```

### 방법 C: GitHub CLI (가장 안전하고 빠름)

```bash
# 1. GitHub CLI 로그인
gh auth login

# 2. SSH 키 직접 업로드 (한 줄!)
gh secret set SSH_PRIVATE_KEY < ~/.ssh/id_rsa

# 3. 확인
gh secret list | grep SSH_PRIVATE_KEY
```

---

## ✅ 검증: SSH 키가 제대로 설정되었는지 확인

### 방법 1: GitHub CLI

```bash
# Secret 목록 확인
gh secret list

# 출력:
# SSH_PRIVATE_KEY    Updated 5 seconds ago
# SERVER_HOST        Updated 6 months ago
# ...
```

### 방법 2: GitHub 웹 사이트

```
GitHub Repository → Settings → Secrets and variables → Actions
→ SSH_PRIVATE_KEY이 목록에 있는지 확인
```

### 방법 3: 배포로 테스트

```bash
# 배포 실행
git push origin main

# GitHub Actions 탭에서 로그 확인
# "Copy files to server" 단계의 로그 보기
```

---

## 🔒 보안 주의사항

⚠️ **매우 중요:**

```
✅ 안전한 것:
- 터미널에서 cat ~/.ssh/id_rsa로 확인하기
- GitHub Secrets에 저장하기 (암호화됨)
- GitHub CLI로 업로드하기

❌ 위험한 것:
- SSH 키를 Slack에 공유
- SSH 키를 이메일로 전송
- SSH 키를 일반 파일로 저장하고 방치
- SSH 키를 GitHub Repository 파일에 커밋
```

---

## 📋 체크리스트

```
[ ] 1. 로컬에서 SSH 키 확인
   $ cat ~/.ssh/id_rsa

[ ] 2. SSH 키 내용 확인 (-----BEGIN RSA...로 시작)

[ ] 3. GitHub Secrets에서 SERVER_SSH_KEY 삭제

[ ] 4. 새로운 Secret 생성
   Name: SSH_PRIVATE_KEY
   Value: SSH 키 전체 내용

[ ] 5. Add secret 클릭

[ ] 6. 다른 필수 Secrets 설정
   - SERVER_PORT = 22
   - JWT_EXPIRY = 900000
   - JWT_REFRESH_TOKEN_EXPIRY = 604800000
   - KAKAO_REDIRECT_URI = http://49.142.154.182:8080/oauth2/auth/kakao/callback

[ ] 7. 배포 테스트
   $ git push origin main
```

---

## 🚀 다음 단계

SSH_PRIVATE_KEY 설정 후:

```bash
# 나머지 필수 Secrets 설정
gh secret set SERVER_PORT -b "22"
gh secret set JWT_EXPIRY -b "900000"
gh secret set JWT_REFRESH_TOKEN_EXPIRY -b "604800000"
gh secret set KAKAO_REDIRECT_URI -b "http://49.142.154.182:8080/oauth2/auth/kakao/callback"

# 배포
git push origin main
```

**배포 시작 준비 완료! 🎉**

---

**마지막 업데이트:** 2025-11-22  
**상태:** 📋 설정 준비 완료  
**버전:** 1.0.0

