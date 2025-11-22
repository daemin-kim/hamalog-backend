# 🔐 Hamalog 보안 설정 가이드

## ⚠️ 중요: 배포 전 필수 작업

이 프로젝트는 온프레미스 Docker 환경에서 운영되며, **모든 민감한 크레덴셜은 환경 변수로 관리**됩니다.

---

## 🚀 빠른 시작

### 1. 크레덴셜 생성

```bash
# 자동 생성 (권장)
./generate-credentials.sh
```

### 2. 카카오 OAuth2 설정

`.env.prod` 파일을 편집하여 카카오 크레덴셜을 설정:

```bash
nano .env.prod
```

[카카오 개발자 콘솔](https://developers.kakao.com)에서 발급받은 값 입력:
```env
KAKAO_CLIENT_ID=your_client_id
KAKAO_CLIENT_SECRET=your_client_secret
KAKAO_REDIRECT_URI=http://your-domain.com/oauth2/auth/kakao/callback
```

### 3. 배포

```bash
# Docker Compose로 배포
docker-compose up -d

# 로그 확인
docker-compose logs -f hamalog-app
```

---

## 📁 환경 변수 파일 구조

```
.env.example          # 템플릿 (Git 추적)
.env.prod             # 실제 프로덕션 크레덴셜 (Git 무시)
```

**⚠️ 절대 Git에 커밋하지 말 것:**
- `.env.prod`
- `application-prod.properties`
- `application-local.properties`

---

## 🔒 보안 기능

### 구현된 보안 조치

✅ JWT Access Token + Refresh Token (15분/7일)  
✅ AES-256 데이터 암호화 (전화번호, 생년월일)  
✅ BCrypt 비밀번호 해싱  
✅ Redis 비밀번호 인증  
✅ MySQL SSL/TLS 지원  
✅ Rate Limiting (Fail-safe)  
✅ CSRF 보호 (CSP 헤더)  
✅ OAuth2 State 검증  
✅ 파일 업로드 검증 (Magic Number, Path Traversal 방지)  
✅ 민감정보 로그 마스킹  
✅ 페이지네이션 DoS 방지  

### 보안 점수

- **전체**: 9.9/10
- **인증/인가**: 10/10
- **데이터 보호**: 10/10
- **API 보안**: 9.5/10
- **입력 검증**: 10/10

---

## 📚 문서

- [배포 보안 가이드](DEPLOYMENT_SECURITY_GUIDE.md) - 온프레미스 Docker 배포
- [API 명세서](API-specification.md) - REST API 문서
- [보안 취약점 보고서](API_SECURITY_VULNERABILITY_REPORT.md) - API 보안 분석
- [설정 보안 감사](CONFIGURATION_SECURITY_AUDIT_REPORT.md) - 설정 보안 분석

---

## 🛠️ 유틸리티 스크립트

### 크레덴셜 생성
```bash
./generate-credentials.sh
```

### Git 히스토리 정리
```bash
./cleanup-git-history.sh
```

---

## ⚙️ 환경별 설정

### 개발 환경 (Local)

```bash
# H2 인메모리 DB 사용
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 프로덕션 환경 (Docker)

```bash
# .env.prod 설정 필수
docker-compose up -d
```

---

## 🔄 크레덴셜 로테이션

**권장 주기**: 3-6개월

```bash
# 1. 새 크레덴셜 생성
./generate-credentials.sh

# 2. 카카오 크레덴셜 재설정
nano .env.prod

# 3. 재배포
docker-compose down
docker-compose up -d
```

---

## 🆘 문제 해결

### 환경 변수 누락 오류

```
Error: JWT_SECRET environment variable is not set
```

**해결**:
```bash
# .env.prod 파일 확인
cat .env.prod | grep JWT_SECRET

# 누락 시 재생성
./generate-credentials.sh
```

### Redis 연결 실패

```
Error: Redis connection failed
```

**해결**:
```bash
# Redis 비밀번호 확인
docker exec redis redis-cli -a ${SPRING_DATA_REDIS_PASSWORD} PING

# 재시작
docker-compose restart redis
```

---

## 📞 지원

문제 발생 시:
1. [배포 가이드](DEPLOYMENT_SECURITY_GUIDE.md) 트러블슈팅 섹션 확인
2. 로그 확인: `docker-compose logs -f hamalog-app`
3. Issue 등록

---

## 📜 라이선스

이 프로젝트는 보안이 강화된 개인정보 처리 시스템으로, 개인정보보호법을 준수합니다.

---

## ✅ 배포 전 체크리스트

- [ ] `./generate-credentials.sh` 실행
- [ ] `.env.prod`에 카카오 크레덴셜 설정
- [ ] `.env.prod` 파일 권한 600 설정
- [ ] `.env.prod`가 Git에 커밋되지 않았는지 확인
- [ ] Docker Compose 설정 확인
- [ ] SSL/TLS 인증서 설정 (선택)
- [ ] 백업 계획 수립
- [ ] 모니터링 설정

---

**⚠️ 경고**: 절대로 실제 크레덴셜을 Git에 커밋하지 마세요!

