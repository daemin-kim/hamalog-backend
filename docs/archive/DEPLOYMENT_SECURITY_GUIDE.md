# Hamalog 온프레미스 Docker 배포 보안 가이드

## 🎯 개요

이 가이드는 온프레미스 환경에서 Docker를 사용하여 Hamalog를 안전하게 배포하는 방법을 설명합니다.

---

## 📋 사전 요구사항

- Docker 20.10 이상
- Docker Compose 2.0 이상
- OpenSSL (크레덴셜 생성용)
- 512MB 이상의 여유 메모리
- 10GB 이상의 디스크 공간

---

## 🔐 1단계: 크레덴셜 생성

### 자동 생성 (권장)

```bash
# 프로젝트 루트 디렉토리에서 실행
./generate-credentials.sh
```

이 스크립트는 다음을 자동으로 생성합니다:
- JWT Secret Key (256-bit)
- 데이터 암호화 키 (AES-256)
- 데이터베이스 비밀번호
- MySQL Root 비밀번호
- Redis 비밀번호

### 수동 생성

```bash
# JWT Secret 생성 (256-bit)
openssl rand -base64 32

# 데이터 암호화 키 생성 (256-bit)
openssl rand -base64 32

# 안전한 비밀번호 생성
openssl rand -base64 32 | tr -d "=+/" | cut -c1-32
```

---

## 📝 2단계: 환경 변수 설정

### .env.prod 파일 편집

```bash
# .env.example을 복사하여 시작
cp .env.example .env.prod

# 보안을 위해 파일 권한 설정
chmod 600 .env.prod

# 편집
nano .env.prod
```

### 필수 설정 항목

#### 1. 카카오 OAuth2 크레덴셜
[카카오 개발자 콘솔](https://developers.kakao.com)에서 발급:

```env
KAKAO_CLIENT_ID=your_actual_client_id
KAKAO_CLIENT_SECRET=your_actual_client_secret
KAKAO_REDIRECT_URI=http://your-domain.com/oauth2/auth/kakao/callback
```

#### 2. 도메인 설정

```env
FRONTEND_URL=http://your-domain.com
ALLOWED_ORIGINS=http://your-domain.com,http://your-domain.com:3000
```

#### 3. 데이터베이스 URL (SSL 사용 시)

```env
SPRING_DATASOURCE_URL=jdbc:mysql://mysql-hamalog:3306/Hamalog?useSSL=true&requireSSL=true&characterEncoding=UTF-8&serverTimezone=UTC
```

---

## 🔒 3단계: SSL/TLS 인증서 설정 (선택사항)

### MySQL SSL 인증서 생성

```bash
# SSL 디렉토리 생성
mkdir -p ./secrets/mysql-ssl
cd ./secrets/mysql-ssl

# CA 키 및 인증서 생성
openssl genrsa 2048 > ca-key.pem
openssl req -new -x509 -nodes -days 3650 -key ca-key.pem -out ca.pem

# 서버 키 및 인증서 생성
openssl req -newkey rsa:2048 -days 3650 -nodes -keyout server-key.pem -out server-req.pem
openssl rsa -in server-key.pem -out server-key.pem
openssl x509 -req -in server-req.pem -days 3650 -CA ca.pem -CAkey ca-key.pem -set_serial 01 -out server-cert.pem

# 권한 설정
chmod 600 *.pem
```

### Docker Compose 볼륨 마운트

```yaml
mysql-hamalog:
  volumes:
    - ./secrets/mysql-ssl:/etc/mysql/ssl:ro
```

---

## 🚀 4단계: 배포

### 초기 배포

```bash
# 이미지 빌드
docker-compose build

# 컨테이너 시작
docker-compose up -d

# 로그 확인
docker-compose logs -f hamalog-app
```

### 헬스 체크

```bash
# 애플리케이션 상태 확인
curl http://localhost:8080/actuator/health

# 예상 응답: {"status":"UP"}
```

---

## 🔍 5단계: 보안 검증

### 크레덴셜 노출 확인

```bash
# Git 추적 파일 확인
git ls-files | grep -E "\.env|application.*properties"

# 결과에 .env.prod, application-prod.properties가 없어야 함
```

### 파일 권한 확인

```bash
# .env.prod 권한 확인 (600이어야 함)
ls -l .env.prod

# 출력: -rw------- (owner만 읽기/쓰기)
```

### 환경 변수 로드 확인

```bash
# 컨테이너 환경 변수 확인 (민감정보는 마스킹됨)
docker exec hamalog-app env | grep -E "JWT|KAKAO|MYSQL" | sed 's/=.*/=***MASKED***/'
```

---

## 📊 6단계: 모니터링 및 유지보수

### 로그 모니터링

```bash
# 실시간 로그
docker-compose logs -f

# 특정 서비스 로그
docker-compose logs -f hamalog-app
docker-compose logs -f mysql-hamalog
docker-compose logs -f redis
```

### 백업

```bash
# 데이터베이스 백업
docker exec mysql-hamalog mysqldump -u root -p${MYSQL_ROOT_PASSWORD} Hamalog > backup_$(date +%Y%m%d).sql

# Redis 백업
docker exec redis redis-cli -a ${SPRING_DATA_REDIS_PASSWORD} --no-auth-warning SAVE
docker cp redis:/data/dump.rdb ./backup_redis_$(date +%Y%m%d).rdb
```

### 크레덴셜 로테이션

```bash
# 1. 새 크레덴셜 생성
./generate-credentials.sh

# 2. .env.prod 백업
cp .env.prod .env.prod.backup

# 3. 새 .env.prod 편집 (카카오 크레덴셜 등 수동 설정)
nano .env.prod

# 4. 데이터 재암호화 (필요 시)
# ... 애플리케이션 특정 로직 실행

# 5. 재배포
docker-compose down
docker-compose up -d
```

---

## ⚠️ 7단계: 보안 체크리스트

### 배포 전 확인사항

- [ ] `.env.prod` 파일이 Git에 커밋되지 않음
- [ ] `application-prod.properties`가 Git에 커밋되지 않음
- [ ] `.gitignore`에 민감 파일이 모두 추가됨
- [ ] 모든 크레덴셜이 환경 변수로 설정됨
- [ ] 강력한 비밀번호 사용 (32자 이상)
- [ ] Redis에 비밀번호가 설정됨
- [ ] MySQL SSL/TLS 활성화 (선택사항)
- [ ] 파일 권한이 올바르게 설정됨 (.env.prod: 600)

### 운영 중 정기 확인사항

- [ ] 로그 파일 모니터링 (보안 이벤트)
- [ ] 디스크 공간 확인
- [ ] 백업 정기 실행 및 검증
- [ ] 크레덴셜 로테이션 (3-6개월)
- [ ] 의존성 업데이트 및 보안 패치
- [ ] 컨테이너 이미지 업데이트

---

## 🆘 트러블슈팅

### 환경 변수가 로드되지 않음

```bash
# .env.prod 파일 존재 확인
ls -la .env.prod

# Docker Compose에서 env_file 설정 확인
grep "env_file" docker-compose.yml

# 컨테이너 재시작
docker-compose restart hamalog-app
```

### Redis 연결 실패

```bash
# Redis 비밀번호 확인
docker exec redis redis-cli -a ${SPRING_DATA_REDIS_PASSWORD} --no-auth-warning PING

# 응답: PONG

# 비밀번호 불일치 시
# 1. .env.prod의 SPRING_DATA_REDIS_PASSWORD 확인
# 2. docker-compose.yml의 Redis 설정 확인
# 3. 컨테이너 재시작
```

### MySQL 연결 실패

```bash
# MySQL 접속 테스트
docker exec -it mysql-hamalog mysql -u ${DB_USERNAME} -p${DB_PASSWORD} Hamalog

# SSL 연결 확인
docker exec mysql-hamalog mysql -u root -p${MYSQL_ROOT_PASSWORD} -e "SHOW STATUS LIKE 'Ssl_cipher';"
```

### JWT 토큰 검증 실패

```bash
# JWT_SECRET 환경 변수 설정 확인
docker exec hamalog-app printenv | grep JWT_SECRET

# 길이 확인 (최소 32바이트)
echo -n ${JWT_SECRET} | wc -c

# 재생성 및 재배포
./generate-credentials.sh
docker-compose restart hamalog-app
```

---

## 📚 참고 자료

- [Docker Security Best Practices](https://docs.docker.com/engine/security/)
- [MySQL SSL/TLS Configuration](https://dev.mysql.com/doc/refman/8.0/en/using-encrypted-connections.html)
- [Redis Security](https://redis.io/docs/management/security/)
- [OWASP Docker Security](https://cheatsheetseries.owasp.org/cheatsheets/Docker_Security_Cheat_Sheet.html)

---

## 🔄 업데이트 이력

- **2025-11-22**: 초기 버전 작성
  - 온프레미스 Docker 배포 가이드
  - 크레덴셜 자동 생성 스크립트
  - 보안 체크리스트

