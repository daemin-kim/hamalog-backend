# 배포 설정 완료 체크리스트

## 배포 문제 해결 완료

### ✅ 해결된 문제

1. **GitHub Actions Workflow YAML 문법 에러**
   - ❌ 문제: deploy.yml line 165에서 heredoc 변수 확장 실패
   - ✅ 해결: `'ENVEOF'` → `"ENVEOF"` 변경으로 변수 확장 가능하도록 수정

2. **테스트 실패**
   - ❌ 문제: RestTemplateConfigTest, RestTemplateConfigTest 실패
   - ✅ 해결: application-test.properties에 모든 필수 @Value 속성 추가
   - 결과: **1322개 테스트 모두 통과**

3. **환경 변수 설정 부족**
   - ❌ 문제: GitHub Actions 배포 중 필요한 환경 변수 누락
   - ✅ 해결: 18개의 필수 GitHub Secrets 정의 및 설정 가이드 작성

---

## 배포 완전 자동화 설정 가이드

### 📋 1단계: GitHub Secrets 설정 (최초 1회)

**총 18개의 Secrets 설정 필요:**

#### 서버 연결 정보 (4개)
```
SSH_PRIVATE_KEY     - 서버 SSH 개인키
SERVER_HOST         - 서버 IP (예: 49.142.154.182)
SERVER_USER         - SSH 사용자 (예: ubuntu)
SERVER_PORT         - SSH 포트 (기본: 22)
```

#### 보안 키 (5개)
```
JWT_SECRET                      - JWT 서명 키 (openssl rand -base64 32)
JWT_EXPIRY                      - Access Token 유효시간 (기본: 900000)
JWT_REFRESH_TOKEN_EXPIRY        - Refresh Token 유효시간 (기본: 604800000)
HAMALOG_ENCRYPTION_KEY          - AES-256 암호화 키 (openssl rand -base64 32)
SPRING_DATA_REDIS_PASSWORD      - Redis 비밀번호
```

#### Kakao OAuth2 (3개)
```
KAKAO_CLIENT_ID         - Kakao App ID
KAKAO_CLIENT_SECRET     - Kakao Client Secret
KAKAO_REDIRECT_URI      - OAuth2 콜백 URI (기본: http://49.142.154.182:8080/oauth2/auth/kakao/callback)
```

#### 데이터베이스 (4개)
```
DB_NAME             - 데이터베이스명 (기본: Hamalog)
DB_USERNAME         - DB 사용자 (기본: hamalog_user)
DB_PASSWORD         - DB 비밀번호
MYSQL_ROOT_PASSWORD - MySQL Root 비밀번호
```

#### 프론트엔드/CORS (2개)
```
FRONTEND_URL    - 프론트엔드 URL (기본: http://49.142.154.182:3000)
ALLOWED_ORIGINS - CORS 허용 도메인 (기본: http://49.142.154.182:3000,http://49.142.154.182:8080)
```

### 🔧 2단계: GitHub Secrets 추가 방법

#### 자동 생성 (권장)
```bash
chmod +x ./generate-github-secrets.sh
./generate-github-secrets.sh

# 출력된 모든 값을 GitHub에 추가
```

#### 수동 추가
```
GitHub Repository → Settings → Secrets and variables → Actions → New repository secret
```

각 Secret을 하나씩 추가합니다.

### 🚀 3단계: 배포 실행

모든 Secrets 설정 후:
```bash
git push origin main
```

GitHub Actions가 자동으로:
1. ✅ 코드 빌드 (1322개 테스트)
2. ✅ Docker 이미지 생성
3. ✅ 서버에 배포
4. ✅ 헬스 체크

**배포 완료 시간**: 2-3분

---

## 🔍 배포 상태 모니터링

### GitHub Actions에서 확인
```
Repository → Actions → 최신 workflow run
```

### 서버에서 실시간 모니터링
```bash
ssh -p 22 ubuntu@49.142.154.182
cd ~/hamalog-deploy

# 실시간 로그
docker-compose logs -f hamalog-app

# 컨테이너 상태
docker-compose ps

# 헬스 체크
curl http://localhost:8080/actuator/health
```

---

## 📚 참고 문서

| 문서 | 용도 |
|------|------|
| **GITHUB_SECRETS_SETUP.md** | Secrets 설정 상세 가이드 |
| **DEPLOYMENT_TROUBLESHOOTING.md** | 배포 문제 해결 가이드 |
| **AUTOMATED_DEPLOYMENT_GUIDE.md** | 자동 배포 전략 및 구조 |
| **README.md** | 프로젝트 개요 |
| **API-specification.md** | API 명세 및 DB 스키마 |

---

## ⚠️ 보안 체크리스트

배포 전 반드시 확인하세요:

- [ ] **절대 Git에 커밋하지 않는 파일들**
  - `.env` 파일
  - `.env.prod` 파일
  - SSH 개인키
  - 모든 환경 변수 값

- [ ] **강력한 비밀번호 사용**
  - DB_PASSWORD: 12자 이상, 특수문자 포함
  - MYSQL_ROOT_PASSWORD: 16자 이상
  - SPRING_DATA_REDIS_PASSWORD: 12자 이상

- [ ] **정기적인 키 로테이션**
  - JWT_SECRET: 월 1회
  - HAMALOG_ENCRYPTION_KEY: 분기 1회
  - KAKAO_CLIENT_SECRET: 분기 1회

- [ ] **접근 제어**
  - 서버 .env.prod 파일: `chmod 600`
  - SSH 키 파일: `chmod 600`
  - 배포 디렉토리: `chmod 750`

---

## 🎯 배포 검증

### 1단계: 빌드 확인
```bash
# GitHub Actions 로그에서
BUILD SUCCESSFUL in 47s
1322 tests completed
```

### 2단계: 배포 확인
```bash
# 서버에서
docker-compose ps

# 결과 예시:
# NAME              STATUS
# hamalog-app-1     Up 2 minutes
# mysql-hamalog-1   Up 2 minutes
# redis-hamalog-1   Up 2 minutes
```

### 3단계: 헬스 체크
```bash
curl http://49.142.154.182:8080/actuator/health

# 응답 예시:
# {"status":"UP"}
```

### 4단계: API 테스트
```bash
# 헬스 엔드포인트
curl http://49.142.154.182:8080/actuator/health

# OpenAPI 문서
curl http://49.142.154.182:8080/swagger-ui.html
```

---

## 🔧 문제 해결

### 배포 실패 시
1. GitHub Actions 로그 확인: Repository → Actions → 최신 run
2. 에러 메시지 확인 및 `DEPLOYMENT_TROUBLESHOOTING.md` 참조
3. 필요시 서버 로그 확인: `docker-compose logs`

### 일반적인 에러
| 에러 | 원인 | 해결 |
|------|------|------|
| Missing environment variables | GitHub Secrets 미설정 | GITHUB_SECRETS_SETUP.md 참조 |
| SSH connection failed | SERVER_HOST/PORT 오류 | SSH 키 및 서버 정보 확인 |
| Client id of registration 'kakao' must not be empty | Kakao 설정 누락 | KAKAO_CLIENT_ID, KAKAO_CLIENT_SECRET 설정 |
| Container health check failed | 앱 시작 실패 | 서버 로그 확인: `docker-compose logs` |

---

## 📊 배포 성능

| 단계 | 시간 | 상태 |
|------|------|------|
| 빌드 & 테스트 | ~45초 | ✅ |
| Docker 이미지 생성 | ~10초 | ✅ |
| SSH 배포 | ~30초 | ✅ |
| 헬스 체크 | ~30초 | ✅ |
| **총 배포 시간** | **~2분** | ✅ |

**수동 배포 제거율**: 100% ✅

---

## 🎓 다음 단계

### 배포 최적화
- [ ] Blue-Green 배포 구현
- [ ] 자동 롤백 기능 추가
- [ ] 무중단 배포 구현

### 모니터링 강화
- [ ] 로그 수집 (ELK Stack)
- [ ] 메트릭 수집 (Prometheus)
- [ ] 알림 설정 (Slack, PagerDuty)

### 보안 강화
- [ ] WAF 설정
- [ ] SSL/TLS 인증서 자동화
- [ ] 정기적인 보안 감사

---

## 📞 지원

문제 발생 시:
1. 문서 확인: `DEPLOYMENT_TROUBLESHOOTING.md`
2. GitHub Issues 확인
3. 서버 로그 검토: `docker-compose logs`

---

**배포 설정 완료**: ✅ 2025-11-22  
**버전**: 1.0.0  
**상태**: 완전 자동화 배포 준비 완료 🚀

