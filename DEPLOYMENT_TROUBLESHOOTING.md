# GitHub Actions 배포 트러블슈팅 가이드

## 개요
GitHub Actions를 통한 자동 배포 중 발생하는 일반적인 문제들과 해결 방법을 설명합니다.

---

## 배포 실패 원인 및 해결

### 1. "Missing environment variables" 에러

#### 증상
```
❌ DEPLOYMENT FAILED: Missing required environment variables:
 JWT_SECRET KAKAO_CLIENT_ID DB_PASSWORD ...
```

#### 원인
GitHub Secrets이 설정되지 않았거나 빈 값으로 설정됨

#### 해결 방법

**1단계: GitHub Secrets 확인**
```
GitHub Repository → Settings → Secrets and variables → Actions
```

**2단계: 누락된 Secrets 추가**
```bash
chmod +x ./generate-github-secrets.sh
./generate-github-secrets.sh
```

출력된 값들을 GitHub Settings에서 각각 추가합니다.

**3단계: 배포 재시도**
```bash
git push origin main
```

---

### 2. "Client id of registration 'kakao' must not be empty" 에러

#### 증상
```
org.springframework.beans.factory.BeanCreationException: 
Client id of registration 'kakao' must not be empty
```

#### 원인
- `KAKAO_CLIENT_ID` 또는 `KAKAO_CLIENT_SECRET`이 설정되지 않음
- 또는 빈 값으로 설정됨

#### 해결 방법

**1단계: Kakao Developers 콘솔에서 앱 확인**
```
https://developers.kakao.com → 내 애플리케이션
```

**2단계: REST API Key와 Client Secret 복사**
```
앱 설정 → 앱 정보 → REST API 키
앱 설정 → 보안 → Client Secret
```

**3단계: GitHub Secrets 업데이트**
```
Settings → Secrets and variables → Actions

Name: KAKAO_CLIENT_ID
Value: <REST API 키>

Name: KAKAO_CLIENT_SECRET
Value: <Client Secret>

Name: KAKAO_REDIRECT_URI
Value: http://<서버_IP>:8080/oauth2/auth/kakao/callback
```

**4단계: Kakao 앱 설정에서 리다이렉트 URI 확인**
```
앱 설정 → 플랫폼 → Web
리다이렉트 URI에 위의 값이 등록되어 있는지 확인
```

---

### 3. "SSH connection failed" 에러

#### 증상
```
ssh: connect to host <HOST> port <PORT>: Connection refused
Permission denied (publickey).
```

#### 원인
- `SERVER_HOST`, `SERVER_USER`, `SERVER_PORT`가 잘못됨
- `SSH_PRIVATE_KEY`가 올바르지 않음
- 서버의 SSH 서비스가 실행 중이 아님

#### 해결 방법

**1단계: 서버 연결성 테스트 (로컬에서)**
```bash
# 기본 포트 22로 연결 테스트
ssh -v <USERNAME>@<SERVER_IP>

# 커스텀 포트로 연결 테스트
ssh -v -p <PORT> <USERNAME>@<SERVER_IP>
```

**2단계: GitHub Secrets 확인**
```
Settings → Secrets and variables → Actions

SERVER_HOST: <서버_IP_주소>
SERVER_USER: <SSH_사용자명>
SERVER_PORT: <SSH_포트>
SSH_PRIVATE_KEY: <SSH_개인키_전체_내용>
```

**3단계: SSH 개인키 재생성 및 설정**
```bash
# 로컬에서 SSH 키 생성 (있을 경우 기존 키 사용)
cat ~/.ssh/id_rsa

# 또는 새로운 키 생성
ssh-keygen -t rsa -b 4096 -f ~/.ssh/hamalog_deploy -N ""
cat ~/.ssh/hamalog_deploy
```

출력된 전체 내용을 `SSH_PRIVATE_KEY` Secrets에 붙여넣습니다.

**4단계: 서버에 공개키 등록**
```bash
# 서버에 접속하여
ssh <USERNAME>@<SERVER_IP>

# 다음 명령어 실행
mkdir -p ~/.ssh
# 개발 머신의 공개키를 서버로 복사
# cat ~/.ssh/hamalog_deploy.pub를 서버의 ~/.ssh/authorized_keys에 추가

chmod 700 ~/.ssh
chmod 600 ~/.ssh/authorized_keys
```

---

### 4. "Container health check failed" 에러

#### 증상
```
❌ Deployment failed - Application health check failed
```

#### 원인
- 애플리케이션이 정상적으로 시작되지 않음
- 데이터베이스 연결 실패
- Redis 연결 실패
- 필수 환경 변수 누락

#### 해결 방법

**1단계: 서버 접속**
```bash
ssh -p <SERVER_PORT> <SERVER_USER>@<SERVER_HOST>
```

**2단계: 컨테이너 로그 확인**
```bash
cd ~/hamalog-deploy

# 최근 로그 확인
docker-compose logs --tail=50 hamalog-app

# 실시간 로그 확인
docker-compose logs -f hamalog-app
```

**3단계: 환경 변수 확인**
```bash
cat .env.prod

# 또는 실행 중인 컨테이너 환경 변수 확인
docker exec hamalog-app-1 env | grep -E "JWT|KAKAO|DB_"
```

**4단계: 데이터베이스 연결 테스트**
```bash
# MySQL 컨테이너에 접속
docker exec mysql-hamalog mysql -u ${DB_USERNAME} -p${DB_PASSWORD} -e "SELECT 1;"

# Redis 연결 테스트
docker exec redis-hamalog redis-cli ping
```

**5단계: 컨테이너 재시작**
```bash
docker-compose restart hamalog-app
```

---

### 5. ".env.prod not found" 에러

#### 증상
```
❌ DEPLOYMENT FAILED: .env.prod not found!
```

#### 원인
- 배포 스크립트가 .env.prod 파일 생성에 실패
- 서버 디렉토리 권한 문제

#### 해결 방법

**1단계: 서버 접속**
```bash
ssh -p <SERVER_PORT> <SERVER_USER>@<SERVER_HOST>
```

**2단계: 배포 디렉토리 확인**
```bash
ls -la ~/hamalog-deploy/

# .env.prod가 있는지 확인
cat ~/hamalog-deploy/.env.prod
```

**3단계: 수동으로 .env.prod 생성**
```bash
cd ~/hamalog-deploy

# 기존 파일이 있으면 백업
[ -f .env.prod ] && cp .env.prod .env.prod.bak

# 새로운 .env.prod 생성
cat > .env.prod << 'EOF'
JWT_SECRET=<GitHub Secrets에서 복사>
JWT_EXPIRY=900000
JWT_REFRESH_TOKEN_EXPIRY=604800000
HAMALOG_ENCRYPTION_KEY=<GitHub Secrets에서 복사>
KAKAO_CLIENT_ID=<Kakao App ID>
KAKAO_CLIENT_SECRET=<Kakao Client Secret>
KAKAO_REDIRECT_URI=http://<SERVER_IP>:8080/oauth2/auth/kakao/callback
DB_NAME=Hamalog
DB_USERNAME=hamalog_user
DB_PASSWORD=<GitHub Secrets에서 복사>
MYSQL_ROOT_PASSWORD=<GitHub Secrets에서 복사>
SPRING_DATASOURCE_USERNAME=hamalog_user
SPRING_DATASOURCE_PASSWORD=<DB_PASSWORD와 동일>
SPRING_DATASOURCE_URL=jdbc:mysql://mysql-hamalog:3306/Hamalog?useSSL=true&requireSSL=true&characterEncoding=UTF-8&serverTimezone=UTC
SPRING_DATA_REDIS_HOST=redis
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=<GitHub Secrets에서 복사>
SPRING_PROFILES_ACTIVE=prod
FRONTEND_URL=http://<SERVER_IP>:3000
ALLOWED_ORIGINS=http://<SERVER_IP>:3000,http://<SERVER_IP>:8080
FILE_UPLOAD_DIR=/data/hamalog/uploads
FILE_UPLOAD_MAX_SIZE=5242880
LOG_DIR=/var/log/hamalog
EOF

# 파일 권한 설정
chmod 600 .env.prod

# 파일 확인
cat .env.prod
```

**4단계: 컨테이너 재시작**
```bash
docker-compose down
docker-compose up -d

# 헬스 체크
sleep 30
curl http://localhost:8080/actuator/health
```

---

## 배포 성공 확인

배포가 성공하면 다음과 같은 로그가 표시됩니다:

```
✅ All required environment variables are set

Stopping existing containers...
Starting new containers...

Waiting for application to start (30 seconds)...

Running health check...
{"status":"UP"}

================================================
✅ Deployment successful - Application is healthy
================================================

Container logs:
... (최근 50줄의 로그)
```

---

## 실시간 모니터링

### 배포 진행 상황 확인

**GitHub Actions 탭에서:**
```
Repository → Actions → 최신 workflow run → Logs
```

### 서버에서 실시간 모니터링

```bash
ssh -p <SERVER_PORT> <SERVER_USER>@<SERVER_HOST>

# 컨테이너 상태 확인
docker-compose ps

# 실시간 로그 확인
docker-compose logs -f

# CPU/메모리 사용량 확인
docker stats
```

### 애플리케이션 헬스 체크

```bash
# 서버에서
curl http://localhost:8080/actuator/health

# 또는 외부에서
curl http://<SERVER_IP>:8080/actuator/health
```

---

## 성능 최적화

### 배포 시간 단축

| 단계 | 시간 | 최적화 방법 |
|------|------|-----------|
| Build & Test | ~45초 | 캐시 활용, 병렬 테스트 |
| Docker Image | ~10초 | 레이어 캐시 활용 |
| SSH 배포 | ~30초 | 불필요한 파일 제외 |
| 헬스 체크 | ~30초 | 병렬 요청 |
| **총 시간** | **~2분** | - |

### 롤백 전략

배포 실패 시 이전 버전으로 빠르게 복구:

```bash
ssh -p <SERVER_PORT> <SERVER_USER>@<SERVER_HOST>

cd ~/hamalog-deploy

# 이전 이미지 로드
docker load -i hamalog-image-backup.tar

# 컨테이너 재시작
docker-compose down
docker-compose up -d
```

---

## 자주 묻는 질문 (FAQ)

### Q1: 배포 중에 서비스가 중단되나요?
**A**: 무중단 배포 구현:
- 로드 밸런서 뒤에서 컨테이너 교체
- Blue-Green 배포 전략 사용 (향후 개선)

### Q2: 배포 실패 시 자동 롤백되나요?
**A**: 현재는 수동 롤백. 다음 버전에서 자동 롤백 추가 예정.

### Q3: GitHub Secrets을 잘못 설정했으면?
**A**: 언제든지 다시 설정할 수 있습니다:
```
Settings → Secrets and variables → Actions → 해당 Secret 수정
```

### Q4: 로컬에서 테스트하려면?
**A**: 
```bash
chmod +x ./generate-credentials.sh
./generate-credentials.sh
./gradlew bootRun --args='--spring.profiles.active=local'
```

### Q5: 배포 로그를 어디서 확인하나요?
**A**: 
- GitHub: Repository → Actions → 최신 workflow
- 서버: `docker-compose logs -f hamalog-app`

---

## 더 알아보기

- **배포 자동화**: [AUTOMATED_DEPLOYMENT_GUIDE.md](AUTOMATED_DEPLOYMENT_GUIDE.md)
- **GitHub Secrets 설정**: [GITHUB_SECRETS_SETUP.md](GITHUB_SECRETS_SETUP.md)
- **API 명세**: [API-specification.md](API-specification.md)
- **로컬 개발**: [README.md](README.md)

---

**마지막 업데이트**: 2025-11-22  
**버전**: 1.0.0

