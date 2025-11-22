# GitHub Secrets 설정 가이드

## 개요
Hamalog 프로젝트의 자동 배포를 위해 GitHub Secrets에 환경 변수들을 설정해야 합니다.

---

## 필수 Secrets 설정

### 1단계: GitHub Repository Settings 접속
```
GitHub Repository → Settings → Secrets and variables → Actions
```

### 2단계: 다음 Secrets를 생성하세요

#### SSH/서버 연결 정보
| Secret Name | 값 | 예시 | 설명 |
|------------|------|-------|------|
| `SSH_PRIVATE_KEY` | SSH 개인키 내용 | `-----BEGIN RSA PRIVATE KEY-----\n...` | 서버 접속용 SSH 개인키 |
| `SERVER_HOST` | 서버 IP 주소 | `49.142.154.182` | 배포 대상 서버 IP |
| `SERVER_USER` | SSH 사용자명 | `deploy` | 서버 SSH 접속 사용자 |
| `SERVER_PORT` | SSH 포트 | `22` | 서버 SSH 포트 (기본값: 22) |

#### JWT 설정
| Secret Name | 값 | 예시 | 설명 |
|------------|------|-------|------|
| `JWT_SECRET` | JWT 서명 키 | Base64 인코딩된 32바이트 키 | JWT 토큰 서명용 비밀키 |
| `JWT_EXPIRY` | 유효시간 (ms) | `900000` | Access Token 유효시간 (기본: 15분) |
| `JWT_REFRESH_TOKEN_EXPIRY` | 갱신 토큰 유효시간 (ms) | `604800000` | Refresh Token 유효시간 (기본: 7일) |

#### 암호화 설정
| Secret Name | 값 | 예시 | 설명 |
|------------|------|-------|------|
| `HAMALOG_ENCRYPTION_KEY` | 데이터 암호화 키 | Base64 인코딩된 32바이트 키 | AES-256 암호화용 키 |

#### 카카오 OAuth2 설정
| Secret Name | 값 | 예시 | 설명 |
|------------|------|-------|------|
| `KAKAO_CLIENT_ID` | 카카오 App ID | `1234567890` | Kakao Developers에서 발급 |
| `KAKAO_CLIENT_SECRET` | 카카오 Client Secret | `xxxxxxxxxxxxx` | Kakao Developers에서 발급 |
| `KAKAO_REDIRECT_URI` | OAuth2 콜백 URI | `http://49.142.154.182:8080/oauth2/auth/kakao/callback` | 카카오 로그인 콜백 주소 |

#### 데이터베이스 설정
| Secret Name | 값 | 예시 | 설명 |
|------------|------|-------|------|
| `DB_NAME` | 데이터베이스명 | `Hamalog` | MySQL 데이터베이스 이름 |
| `DB_USERNAME` | DB 사용자명 | `hamalog_user` | MySQL 접속 사용자 |
| `DB_PASSWORD` | DB 비밀번호 | `strong_password_here` | MySQL 접속 비밀번호 |
| `MYSQL_ROOT_PASSWORD` | MySQL Root 비밀번호 | `root_password_here` | MySQL root 비밀번호 |

#### Redis 설정
| Secret Name | 값 | 예시 | 설명 |
|------------|------|-------|------|
| `SPRING_DATA_REDIS_PASSWORD` | Redis 비밀번호 | `redis_password_here` | Redis 접속 비밀번호 |

#### 프론트엔드/CORS 설정
| Secret Name | 값 | 예시 | 설명 |
|------------|------|-------|------|
| `FRONTEND_URL` | 프론트엔드 URL | `http://49.142.154.182:3000` | 프론트엔드 서버 주소 |
| `ALLOWED_ORIGINS` | CORS 허용 도메인 | `http://49.142.154.182:3000,http://49.142.154.182:8080` | CORS 허용 도메인 (쉼표 구분) |

---

## Secrets 생성 스크립트

### 자동 생성 (권장)

로컬에서 다음 스크립트를 실행하면 모든 값을 자동으로 생성합니다:

```bash
chmod +x ./generate-github-secrets.sh
./generate-github-secrets.sh
```

스크립트가 생성한 출력 내용을 복사하여 GitHub Repository Settings에서 Secrets을 생성하세요.

### 수동 생성

#### 1. JWT_SECRET 생성
```bash
# Linux/macOS
openssl rand -base64 32

# 또는 Python
python3 -c "import base64, os; print(base64.b64encode(os.urandom(32)).decode())"
```

#### 2. HAMALOG_ENCRYPTION_KEY 생성
```bash
openssl rand -base64 32
```

#### 3. SSH_PRIVATE_KEY 설정
```bash
# 기존 SSH 키가 있으면
cat ~/.ssh/id_rsa

# 없으면 새로 생성
ssh-keygen -t rsa -b 4096 -f ~/.ssh/hamalog_deploy -N ""
cat ~/.ssh/hamalog_deploy
```

---

## 검증

### 1. 모든 Secrets 확인
GitHub Repository Settings에서 다음을 확인하세요:
- [ ] SSH_PRIVATE_KEY
- [ ] SERVER_HOST
- [ ] SERVER_USER
- [ ] SERVER_PORT
- [ ] JWT_SECRET
- [ ] JWT_EXPIRY
- [ ] JWT_REFRESH_TOKEN_EXPIRY
- [ ] HAMALOG_ENCRYPTION_KEY
- [ ] KAKAO_CLIENT_ID
- [ ] KAKAO_CLIENT_SECRET
- [ ] KAKAO_REDIRECT_URI
- [ ] DB_NAME
- [ ] DB_USERNAME
- [ ] DB_PASSWORD
- [ ] MYSQL_ROOT_PASSWORD
- [ ] SPRING_DATA_REDIS_PASSWORD
- [ ] FRONTEND_URL
- [ ] ALLOWED_ORIGINS

### 2. 배포 테스트
```bash
git push origin main
```

GitHub Actions 탭에서 배포 작업의 진행 상황을 모니터링하세요.

---

## 문제 해결

### "Missing environment variables" 에러
**원인**: GitHub Secrets이 설정되지 않았거나 값이 비어있음  
**해결**: 위 표를 참고하여 모든 Secrets을 설정하세요

### "SSH connection failed" 에러
**원인**: SERVER_HOST, SERVER_USER, SERVER_PORT 값이 잘못되었거나 SSH 키가 없음  
**해결**: 
1. 서버 정보 확인: `SERVER_HOST`, `SERVER_USER`, `SERVER_PORT`
2. SSH 키 확인: `SSH_PRIVATE_KEY`에 올바른 개인키가 설정되었는지 확인

### "Client id of registration 'kakao' must not be empty" 에러
**원인**: KAKAO_CLIENT_ID가 설정되지 않음  
**해결**: 
1. Kakao Developers에서 앱 생성
2. Client ID와 Client Secret 발급
3. GitHub Secrets에 `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET` 설정

### "Database connection failed" 에러
**원인**: DB 설정이 잘못됨  
**해결**: 
- DB_NAME, DB_USERNAME, DB_PASSWORD 확인
- 서버의 MySQL이 실행 중인지 확인

---

## 보안 주의사항

⚠️ **중요**: 다음 사항을 반드시 지키세요

1. **절대 Git에 커밋하지 말 것**
   - `.env` 파일
   - SSH 개인키
   - 환경 변수 값들

2. **강력한 비밀번호 사용**
   - DB_PASSWORD: 12자 이상, 특수문자 포함
   - MYSQL_ROOT_PASSWORD: 16자 이상, 대소문자+숫자+특수문자 포함

3. **정기적인 키 로테이션**
   - JWT_SECRET: 월 1회 이상 변경
   - HAMALOG_ENCRYPTION_KEY: 분기 1회 변경
   - 카카오 Client Secret: 분기 1회 변경

4. **접근 제어**
   - SSH 키 파일 권한: `600` 이하
   - 배포 서버의 .env.prod 파일 권한: `600`

---

## 자동 배포 프로세스

모든 Secrets이 설정되면 다음과 같은 프로세스가 자동으로 작동합니다:

```
git push origin main
    ↓
GitHub Actions 트리거
    ↓
1. Build & Test (1322 tests)
    ↓
2. Docker Image Build
    ↓
3. SSH를 통한 배포 서버 연결
    ↓
4. .env.prod 파일 자동 생성
    ↓
5. Docker Compose 실행
    ↓
6. 헬스 체크
    ↓
✅ 배포 완료 (2-3분)
```

---

**마지막 업데이트**: 2025-11-22  
**버전**: 1.0.0

