# GitHub Actions CI/CD 서버 설정 가이드

## 🎯 개요

이 문서는 GitHub Actions를 통한 자동 배포를 위한 온프레미스 서버 설정 가이드입니다.

---

## 📋 사전 요구사항

### 서버 환경
- Ubuntu 20.04 LTS 이상 (또는 CentOS 8 이상)
- Docker 20.10 이상
- Docker Compose 2.0 이상
- SSH 서버 활성화
- 최소 2GB RAM, 10GB 디스크 공간

### GitHub Repository 설정
- Repository Secrets 설정 필요
- SSH 접근 권한 설정

---

## 🔐 1단계: 서버 SSH 키 설정

### SSH 키 생성 (로컬 또는 서버에서)

```bash
# SSH 키 생성
ssh-keygen -t rsa -b 4096 -C "github-actions-deploy" -f ~/.ssh/hamalog_deploy_key

# 공개 키를 서버에 추가
cat ~/.ssh/hamalog_deploy_key.pub >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys

# 개인 키 내용 확인 (GitHub Secrets에 추가할 내용)
cat ~/.ssh/hamalog_deploy_key
```

---

## 🔧 2단계: GitHub Repository Secrets 설정

GitHub Repository → Settings → Secrets and variables → Actions → New repository secret

### 필수 Secrets

| Secret Name | 설명 | 예시 |
|-------------|------|------|
| `SSH_PRIVATE_KEY` | SSH 개인 키 전체 내용 | `-----BEGIN RSA PRIVATE KEY-----\n...` |
| `SERVER_HOST` | 서버 IP 또는 도메인 | `192.168.1.100` 또는 `example.com` |
| `SERVER_USER` | SSH 사용자명 | `ubuntu` 또는 `deploy` |
| `SERVER_PORT` | SSH 포트 | `22` (기본값) |

### Secrets 설정 방법

```bash
# 1. SSH_PRIVATE_KEY: 개인 키 전체를 복사
cat ~/.ssh/hamalog_deploy_key | pbcopy

# 2. GitHub에서 Settings > Secrets > New secret
# Name: SSH_PRIVATE_KEY
# Value: (복사한 내용 붙여넣기)
```

---

## 📁 3단계: 서버에 .env.prod 파일 설정

### 서버에 접속

```bash
ssh user@your-server-ip
```

### 디렉토리 생성

```bash
mkdir -p ~/hamalog-deploy
cd ~/hamalog-deploy
```

### .env.prod 파일 생성

로컬에서 생성한 `.env.prod`를 서버로 복사하거나, 서버에서 직접 생성:

```bash
# 방법 1: 로컬에서 SCP로 복사
scp .env.prod user@server-ip:~/hamalog-deploy/

# 방법 2: 서버에서 직접 생성
nano ~/hamalog-deploy/.env.prod
```

### .env.prod 템플릿

```env
# Database Configuration
DB_NAME=Hamalog
DB_USERNAME=hamalog_user
DB_PASSWORD=<STRONG_PASSWORD_32_CHARS>
MYSQL_ROOT_PASSWORD=<STRONG_PASSWORD_32_CHARS>
SPRING_DATASOURCE_USERNAME=hamalog_user
SPRING_DATASOURCE_PASSWORD=<STRONG_PASSWORD_32_CHARS>
SPRING_DATASOURCE_URL=jdbc:mysql://mysql-hamalog:3306/Hamalog?useSSL=true&requireSSL=true&characterEncoding=UTF-8&serverTimezone=UTC

# JWT Configuration
JWT_SECRET=<BASE64_256BIT_KEY>
JWT_EXPIRY=900000
JWT_REFRESH_TOKEN_EXPIRY=604800000

# Data Encryption Key
HAMALOG_ENCRYPTION_KEY=<BASE64_256BIT_KEY>

# Kakao OAuth2
KAKAO_CLIENT_ID=<YOUR_KAKAO_CLIENT_ID>
KAKAO_CLIENT_SECRET=<YOUR_KAKAO_CLIENT_SECRET>
KAKAO_REDIRECT_URI=http://your-domain.com/oauth2/auth/kakao/callback

# Redis
SPRING_DATA_REDIS_HOST=redis
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=<STRONG_PASSWORD_32_CHARS>

# Spring Profile
SPRING_PROFILES_ACTIVE=prod

# Frontend & CORS
FRONTEND_URL=http://your-domain.com
ALLOWED_ORIGINS=http://your-domain.com

# File Upload
FILE_UPLOAD_DIR=/data/hamalog/uploads
FILE_UPLOAD_MAX_SIZE=5242880

# Logging
LOG_DIR=/var/log/hamalog
```

### 파일 권한 설정

```bash
chmod 600 ~/hamalog-deploy/.env.prod
```

---

## 🚀 4단계: Docker 및 Docker Compose 설치

### Ubuntu

```bash
# Docker 설치
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch="$(dpkg --print-architecture)" signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  "$(. /etc/os-release && echo "$VERSION_CODENAME")" stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# 현재 사용자를 docker 그룹에 추가
sudo usermod -aG docker $USER

# 로그아웃 후 재로그인 (또는 다음 명령 실행)
newgrp docker
```

### CentOS

```bash
sudo yum install -y yum-utils
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker $USER
newgrp docker
```

---

## 📊 5단계: 배포 테스트

### 로컬에서 푸시

```bash
git add .
git commit -m "test: CI/CD deployment"
git push origin main
```

### GitHub Actions 확인

1. GitHub Repository → Actions 탭
2. 실행 중인 워크플로우 확인
3. 각 단계별 로그 확인

### 서버에서 확인

```bash
# 서버 접속
ssh user@server-ip

# 컨테이너 상태 확인
cd ~/hamalog-deploy
docker-compose ps

# 로그 확인
docker-compose logs -f hamalog-app

# 헬스 체크
curl http://localhost:8080/actuator/health
```

---

## 🔍 6단계: 환경 변수 검증

### 배포 후 검증 스크립트

서버에 다음 스크립트 생성:

```bash
#!/bin/bash
# verify-deployment.sh

cd ~/hamalog-deploy

echo "==================================="
echo "Hamalog Deployment Verification"
echo "==================================="

# Check .env.prod exists
if [ ! -f .env.prod ]; then
    echo "❌ .env.prod not found"
    exit 1
else
    echo "✅ .env.prod exists"
fi

# Source environment variables
source .env.prod

# Check required variables
REQUIRED_VARS=(
    "JWT_SECRET"
    "HAMALOG_ENCRYPTION_KEY"
    "KAKAO_CLIENT_ID"
    "KAKAO_CLIENT_SECRET"
    "DB_PASSWORD"
    "MYSQL_ROOT_PASSWORD"
    "SPRING_DATA_REDIS_PASSWORD"
)

ALL_SET=true
for var in "${REQUIRED_VARS[@]}"; do
    if [ -z "${!var}" ]; then
        echo "❌ $var is not set"
        ALL_SET=false
    else
        echo "✅ $var is set (length: ${#!var})"
    fi
done

if [ "$ALL_SET" = false ]; then
    echo ""
    echo "❌ Some required variables are missing"
    exit 1
fi

# Check container status
echo ""
echo "Checking Docker containers..."
docker-compose ps

# Check application health
echo ""
echo "Checking application health..."
if curl -f http://localhost:8080/actuator/health; then
    echo ""
    echo "✅ Application is healthy"
else
    echo ""
    echo "❌ Application health check failed"
    exit 1
fi

echo ""
echo "==================================="
echo "✅ All verifications passed!"
echo "==================================="
```

실행 권한 부여 및 실행:

```bash
chmod +x verify-deployment.sh
./verify-deployment.sh
```

---

## 🔐 7단계: 보안 설정

### 방화벽 설정

```bash
# UFW (Ubuntu)
sudo ufw allow 22/tcp
sudo ufw allow 8080/tcp
sudo ufw enable

# Firewalld (CentOS)
sudo firewall-cmd --permanent --add-port=22/tcp
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload
```

### SSL/TLS 설정 (선택사항)

```bash
# Certbot 설치 (Let's Encrypt)
sudo apt-get install -y certbot

# 인증서 발급
sudo certbot certonly --standalone -d your-domain.com
```

---

## 📅 8단계: 모니터링 및 로그

### 로그 확인

```bash
# 실시간 로그
docker-compose logs -f

# 특정 서비스 로그
docker-compose logs -f hamalog-app

# 최근 100줄
docker-compose logs --tail=100 hamalog-app
```

### 디스크 공간 확인

```bash
# 디스크 사용량
df -h

# Docker 이미지/컨테이너 정리
docker system prune -af --volumes
```

---

## 🆘 트러블슈팅

### 1. SSH 연결 실패

```bash
# SSH 키 권한 확인
chmod 600 ~/.ssh/hamalog_deploy_key

# SSH 연결 테스트
ssh -i ~/.ssh/hamalog_deploy_key -p 22 user@server-ip
```

### 2. 환경 변수 미설정

```bash
# 서버에서 확인
cd ~/hamalog-deploy
cat .env.prod

# 컨테이너 내부 확인
docker exec hamalog-app env | grep JWT_SECRET
```

### 3. 포트 충돌

```bash
# 포트 사용 확인
sudo netstat -tulpn | grep 8080

# 기존 프로세스 종료
sudo kill -9 <PID>
```

### 4. 배포 실패

```bash
# GitHub Actions 로그 확인
# GitHub Repository → Actions → 실패한 워크플로우 → 로그

# 서버 로그 확인
cd ~/hamalog-deploy
docker-compose logs hamalog-app
```

---

## 📚 참고 자료

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)

---

## ✅ 체크리스트

### 서버 설정
- [ ] Docker 설치 완료
- [ ] Docker Compose 설치 완료
- [ ] SSH 키 설정 완료
- [ ] `~/hamalog-deploy/.env.prod` 생성 완료
- [ ] 파일 권한 600 설정
- [ ] 방화벽 포트 개방

### GitHub 설정
- [ ] SSH_PRIVATE_KEY Secret 설정
- [ ] SERVER_HOST Secret 설정
- [ ] SERVER_USER Secret 설정
- [ ] SERVER_PORT Secret 설정 (필요시)

### 배포 확인
- [ ] GitHub Actions 워크플로우 성공
- [ ] Docker 컨테이너 정상 실행
- [ ] 헬스 체크 통과
- [ ] 애플리케이션 접근 가능

---

**작성일**: 2025-11-22  
**업데이트**: CI/CD 파이프라인 자동화

