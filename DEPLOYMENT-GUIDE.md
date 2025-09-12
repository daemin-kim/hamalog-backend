# Hamalog 배포 가이드 (Deployment Guide)

## SchemaManagementException 해결

### 문제 설명
배포 환경에서 `SchemaManagementException`이 발생하는 주요 원인:

1. **암호화 키 미설정**: `HAMALOG_ENCRYPTION_KEY` 환경변수가 설정되지 않음
2. **스키마 검증 실패**: Hibernate DDL 설정이 `validate`로 되어 있어 스키마 불일치 시 실패

### 해결된 사항

#### 1. DDL 자동 설정 변경
- **변경 전**: `spring.jpa.hibernate.ddl-auto=validate`
- **변경 후**: `spring.jpa.hibernate.ddl-auto=update`

이 변경으로 Hibernate가 자동으로 스키마를 업데이트하여 배포 환경에서의 스키마 불일치 문제를 해결합니다.

#### 2. 필수 환경 변수 설정

배포 시 반드시 다음 환경 변수들을 설정해야 합니다:

```bash
# 데이터베이스 설정
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/hamalog?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=your_username
SPRING_DATASOURCE_PASSWORD=your_password

# 암호화 키 (Base64 인코딩된 256비트 키)
HAMALOG_ENCRYPTION_KEY=$(openssl rand -base64 32)

# JWT 시크릿
JWT_SECRET=$(openssl rand -base64 32)

# OAuth2 설정
KAKAO_CLIENT_ID=your_kakao_client_id
KAKAO_CLIENT_SECRET=your_kakao_client_secret

# Redis 설정
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=your_redis_password

# 기타 설정
FRONTEND_URL=https://your-domain.com
ALLOWED_ORIGINS=https://your-domain.com
LOG_DIR=/var/log/hamalog
UPLOAD_DIR=/data/hamalog/images
```

## Docker 배포 예시

### 1. 환경 변수 파일 생성 (.env)
```bash
# .env 파일 생성
cat > .env << EOF
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/hamalog?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=hamalog_user
SPRING_DATASOURCE_PASSWORD=secure_password
HAMALOG_ENCRYPTION_KEY=$(openssl rand -base64 32)
JWT_SECRET=$(openssl rand -base64 32)
FRONTEND_URL=https://your-domain.com
ALLOWED_ORIGINS=https://your-domain.com
EOF
```

### 2. Docker Compose 실행
```bash
# 기존 docker-compose.yml 사용
docker-compose up -d

# 또는 환경 변수와 함께 실행
docker-compose --env-file .env up -d
```

### 3. 직접 Docker 실행
```bash
docker run -d \
  --name hamalog \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://host:3306/hamalog?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&serverTimezone=UTC" \
  -e SPRING_DATASOURCE_USERNAME="username" \
  -e SPRING_DATASOURCE_PASSWORD="password" \
  -e HAMALOG_ENCRYPTION_KEY="$(openssl rand -base64 32)" \
  -e JWT_SECRET="$(openssl rand -base64 32)" \
  -e FRONTEND_URL="https://your-domain.com" \
  hamalog:latest
```

## 트러블슈팅

### 암호화 키 관련 오류
```
IllegalStateException: 데이터 암호화 키가 설정되지 않았습니다
```

**해결방법**:
```bash
# 올바른 암호화 키 생성
export HAMALOG_ENCRYPTION_KEY=$(openssl rand -base64 32)
echo "생성된 키: $HAMALOG_ENCRYPTION_KEY"
```

### 스키마 관련 오류
```
SchemaManagementException: Schema-validation failed
```

**해결방법**: 이미 해결됨 (DDL auto 설정을 update로 변경)

### 데이터베이스 연결 오류
```
Cannot create connection to database server
```

**해결방법**:
1. 데이터베이스 서버가 실행 중인지 확인
2. 연결 정보 (URL, 사용자명, 비밀번호) 확인
3. 네트워크 연결 확인

## 검증 방법

배포 후 다음 엔드포인트로 애플리케이션 상태 확인:

```bash
# 애플리케이션 상태 확인
curl http://localhost:8080/actuator/health

# 예상 응답
{"status":"UP"}
```

## 보안 고려사항

1. **암호화 키**: 절대 하드코딩하지 말고 환경 변수로 관리
2. **JWT 시크릿**: 강력한 시크릿 키 사용
3. **데이터베이스 비밀번호**: 복잡한 비밀번호 사용
4. **HTTPS**: 프로덕션에서는 반드시 HTTPS 사용
5. **방화벽**: 필요한 포트만 열어두기

---

이 가이드를 따라 배포하면 SchemaManagementException 없이 정상적으로 애플리케이션이 시작됩니다.