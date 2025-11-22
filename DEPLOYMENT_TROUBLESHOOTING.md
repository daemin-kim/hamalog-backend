# GitHub Actions 배포 실패 해결 가이드

## 🚨 증상: 환경 변수 미설정 오류

```
time="2025-11-22T02:10:39Z" level=warning msg="The \"DB_NAME\" variable is not set. Defaulting to a blank string."
time="2025-11-22T02:10:39Z" level=warning msg="The \"JWT_SECRET\" variable is not set. Defaulting to a blank string."
...
Caused by: java.lang.IllegalStateException: Client id of registration 'kakao' must not be empty.
```

---

## 🔍 원인 분석

### 1. `.env.prod` 파일 미생성
서버의 `~/hamalog-deploy/.env.prod` 파일이 존재하지 않거나 비어있음

### 2. 환경 변수 로드 실패
Docker Compose가 `env_file: .env.prod`를 참조하지만 파일이 없어 빈 문자열로 기본값 설정

### 3. Spring Boot 시작 실패
필수 환경 변수(특히 KAKAO_CLIENT_ID)가 비어있어 애플리케이션 시작 실패

---

## ✅ 해결 방법

### Option 1: 서버에서 크레덴셜 생성 (권장)

#### 1단계: 서버 접속
```bash
ssh user@49.142.154.182
```

#### 2단계: 배포 디렉토리 생성
```bash
mkdir -p ~/hamalog-deploy
cd ~/hamalog-deploy
```

#### 3단계: 크레덴셜 생성 스크립트 복사
로컬에서 서버로 파일 전송:
```bash
# 로컬 터미널에서 실행
scp generate-credentials.sh user@49.142.154.182:~/hamalog-deploy/
scp check-deployment-readiness.sh user@49.142.154.182:~/hamalog-deploy/
```

#### 4단계: 크레덴셜 생성
```bash
# 서버에서 실행
cd ~/hamalog-deploy
chmod +x generate-credentials.sh
./generate-credentials.sh
```

출력:
```
✅ JWT Secret generated
✅ Encryption Key generated
✅ Database passwords generated
📁 File created: .env.prod
🔒 File permissions: 600
```

#### 5단계: 카카오 OAuth2 설정
```bash
nano .env.prod
```

다음 값을 실제 카카오 개발자 콘솔에서 발급받은 값으로 변경:
```env
KAKAO_CLIENT_ID=<실제_카카오_클라이언트_ID>
KAKAO_CLIENT_SECRET=<실제_카카오_클라이언트_시크릿>
```

저장: `Ctrl+O` → `Enter` → `Ctrl+X`

#### 6단계: 배포 준비 상태 확인
```bash
chmod +x check-deployment-readiness.sh
./check-deployment-readiness.sh
```

예상 출력:
```
================================================
✅ Pre-deployment check PASSED
================================================

Server is ready for deployment!
```

#### 7단계: 로컬에서 푸시
```bash
# 로컬 터미널에서
git push origin main
```

GitHub Actions가 자동으로 배포를 진행합니다.

---

### Option 2: 기존 .env.prod 수정

#### 서버에 `.env.prod`가 이미 있지만 비어있는 경우:

```bash
ssh user@49.142.154.182
cd ~/hamalog-deploy

# .env.example 다운로드
curl -O https://raw.githubusercontent.com/your-repo/hamalog-backend/main/.env.example

# .env.prod 생성
cp .env.example .env.prod

# 편집
nano .env.prod
```

**필수 설정 항목**:
```env
# JWT (openssl rand -base64 32)
JWT_SECRET=<32바이트_Base64_인코딩_키>

# Encryption (openssl rand -base64 32)
HAMALOG_ENCRYPTION_KEY=<32바이트_Base64_인코딩_키>

# Kakao OAuth2 (카카오 개발자 콘솔)
KAKAO_CLIENT_ID=<실제_값>
KAKAO_CLIENT_SECRET=<실제_값>
KAKAO_REDIRECT_URI=http://49.142.154.182:8080/oauth2/auth/kakao/callback

# Database
DB_NAME=Hamalog
DB_USERNAME=hamalog_user
DB_PASSWORD=<강력한_비밀번호>
MYSQL_ROOT_PASSWORD=<강력한_비밀번호>

# Redis
SPRING_DATA_REDIS_PASSWORD=<강력한_비밀번호>

# Spring
SPRING_PROFILES_ACTIVE=prod

# Frontend
FRONTEND_URL=http://49.142.154.182:3000
ALLOWED_ORIGINS=http://49.142.154.182:3000,http://49.142.154.182:8080
```

권한 설정:
```bash
chmod 600 .env.prod
```

---

## 🔍 배포 상태 확인

### GitHub Actions 로그 확인
1. GitHub Repository → Actions 탭
2. 최신 워크플로우 선택
3. "Deploy to server" 단계 로그 확인

### 서버에서 직접 확인

```bash
# 서버 접속
ssh user@49.142.154.182
cd ~/hamalog-deploy

# 컨테이너 상태
docker-compose ps

# 로그 확인
docker-compose logs -f hamalog-app

# 환경 변수 확인 (마스킹됨)
docker exec hamalog-app env | grep -E "JWT|KAKAO|DB" | sed 's/=.*/=***MASKED***/'
```

---

## 🆘 트러블슈팅

### 문제 1: ".env.prod not found"
```bash
# 해결
cd ~/hamalog-deploy
./generate-credentials.sh
nano .env.prod  # 카카오 크레덴셜 설정
```

### 문제 2: "Client id must not be empty"
```bash
# .env.prod 확인
cat .env.prod | grep KAKAO_CLIENT_ID

# 비어있으면 설정
nano .env.prod
```

### 문제 3: "Permission denied"
```bash
# 권한 수정
chmod 600 .env.prod
ls -l .env.prod  # -rw------- 확인
```

### 문제 4: 환경 변수가 로드되지 않음
```bash
# Docker Compose 재시작
docker-compose down
docker-compose up -d

# 로그 확인
docker-compose logs hamalog-app
```

### 문제 5: 헬스 체크 실패
```bash
# 애플리케이션 로그 확인
docker-compose logs --tail=100 hamalog-app

# 컨테이너 재시작
docker-compose restart hamalog-app

# 헬스 엔드포인트 직접 확인
curl http://localhost:8080/actuator/health
```

---

## 📋 체크리스트

배포 전 확인사항:

### 서버 설정
- [ ] `~/hamalog-deploy` 디렉토리 생성됨
- [ ] `.env.prod` 파일 생성됨
- [ ] `.env.prod` 파일 권한 600
- [ ] 모든 필수 환경 변수 설정됨
- [ ] KAKAO_CLIENT_ID 실제 값으로 설정됨
- [ ] KAKAO_CLIENT_SECRET 실제 값으로 설정됨

### GitHub Secrets 설정
- [ ] SSH_PRIVATE_KEY 설정됨
- [ ] SERVER_HOST 설정됨 (49.142.154.182)
- [ ] SERVER_USER 설정됨

### 배포 검증
- [ ] `check-deployment-readiness.sh` 실행 성공
- [ ] GitHub Actions 워크플로우 성공
- [ ] 컨테이너 정상 실행 확인
- [ ] 헬스 체크 통과

---

## 📞 추가 도움말

### 크레덴셜 안전하게 생성

```bash
# JWT Secret (256-bit)
openssl rand -base64 32

# Encryption Key (256-bit)
openssl rand -base64 32

# Strong Password (32 chars)
openssl rand -base64 32 | tr -d "=+/" | cut -c1-32
```

### 카카오 OAuth2 설정 확인

1. [카카오 개발자 콘솔](https://developers.kakao.com) 접속
2. 내 애플리케이션 선택
3. 앱 설정 → 요약 정보
   - REST API 키 → `KAKAO_CLIENT_ID`
4. 제품 설정 → 카카오 로그인 → 보안
   - Client Secret → `KAKAO_CLIENT_SECRET`
5. Redirect URI 확인
   - `http://49.142.154.182:8080/oauth2/auth/kakao/callback` 등록되어 있는지 확인

---

**작성일**: 2025-11-22  
**문서 버전**: 1.0  
**관련 문서**: GITHUB_ACTIONS_SERVER_SETUP.md, DEPLOYMENT_SECURITY_GUIDE.md

