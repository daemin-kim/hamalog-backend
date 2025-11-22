# 완전 자동화 배포 가이드 (GitHub Secrets 사용)

## 🎯 개요

이제 **메인 브랜치에 푸시만 하면** 모든 배포가 자동으로 완료됩니다!

서버에 SSH 접속하거나 `.env.prod` 파일을 수동으로 생성할 필요가 없습니다.

---

## 🚀 완전 자동화 방식

### 기존 방식 (수동 설정 필요)
```
1. 서버 SSH 접속
2. ./generate-credentials.sh 실행
3. .env.prod 편집 (카카오 크레덴셜 설정)
4. 권한 설정 (chmod 600)
5. git push origin main
```

### 새로운 방식 (완전 자동화) ⭐
```
1. GitHub Secrets 설정 (최초 1회만)
2. git push origin main
   → 끝! 🎉
```

---

## 📋 초기 설정 (최초 1회만)

### Step 1: GitHub Secrets용 크레덴셜 생성

로컬에서 실행:
```bash
./generate-github-secrets.sh
```

출력 예시:
```
================================================
Hamalog GitHub Secrets Generator
================================================

✅ 크레덴셜 생성 완료!

================================================
다음 값들을 GitHub Repository Secrets에 설정하세요:
================================================

Name: JWT_SECRET
Value: abc123...xyz789

Name: HAMALOG_ENCRYPTION_KEY
Value: def456...uvw012

...
```

### Step 2: GitHub Secrets 설정

1. **GitHub Repository 접속**
   - `https://github.com/your-username/hamalog-backend`

2. **Settings → Secrets and variables → Actions**

3. **New repository secret 클릭**

4. **필수 Secrets 추가** (총 14개)

#### 🔐 보안 크레덴셜 (자동 생성됨 - 복사하여 설정)
| Secret Name | 설명 | 생성 방법 |
|-------------|------|-----------|
| `JWT_SECRET` | JWT 토큰 서명 키 | 스크립트 출력 복사 |
| `HAMALOG_ENCRYPTION_KEY` | 데이터 암호화 키 | 스크립트 출력 복사 |
| `DB_PASSWORD` | 데이터베이스 비밀번호 | 스크립트 출력 복사 |
| `MYSQL_ROOT_PASSWORD` | MySQL Root 비밀번호 | 스크립트 출력 복사 |
| `SPRING_DATA_REDIS_PASSWORD` | Redis 비밀번호 | 스크립트 출력 복사 |

#### 🔑 카카오 OAuth2 (수동 설정 필요)
| Secret Name | 설명 | 가져오는 방법 |
|-------------|------|--------------|
| `KAKAO_CLIENT_ID` | 카카오 REST API 키 | [카카오 개발자 콘솔](https://developers.kakao.com) → 앱 선택 → 앱 키 |
| `KAKAO_CLIENT_SECRET` | 카카오 Client Secret | 카카오 개발자 콘솔 → 제품 설정 → 카카오 로그인 → 보안 |

#### 🌐 서버 연결
| Secret Name | 설명 | 값 예시 |
|-------------|------|---------|
| `SERVER_HOST` | 서버 IP 주소 | `49.142.154.182` |
| `SERVER_USER` | SSH 사용자명 | `ubuntu` 또는 `ec2-user` |
| `SERVER_PORT` | SSH 포트 | `22` (기본값) |
| `SSH_PRIVATE_KEY` | SSH 개인 키 | `cat ~/.ssh/id_rsa` 전체 내용 |

#### ⚙️ 선택 설정 (기본값 있음, 변경 시에만 설정)
| Secret Name | 기본값 | 설명 |
|-------------|--------|------|
| `JWT_EXPIRY` | `900000` | JWT 만료 시간 (15분) |
| `JWT_REFRESH_TOKEN_EXPIRY` | `604800000` | Refresh Token 만료 (7일) |
| `KAKAO_REDIRECT_URI` | `http://49.142.154.182:8080/oauth2/auth/kakao/callback` | 카카오 리다이렉트 URI |
| `DB_NAME` | `Hamalog` | 데이터베이스 이름 |
| `DB_USERNAME` | `hamalog_user` | DB 사용자명 |
| `FRONTEND_URL` | `http://49.142.154.182:3000` | 프론트엔드 URL |
| `ALLOWED_ORIGINS` | `http://49.142.154.182:3000,...` | CORS 허용 도메인 |

---

## 🎬 배포 방법 (매우 간단!)

### 1. 코드 수정
```bash
# 평소대로 코드 작성
git add .
git commit -m "feat: 새로운 기능 추가"
```

### 2. 메인 브랜치에 푸시
```bash
git push origin main
```

### 3. 자동으로 진행되는 작업들 ✨

GitHub Actions가 자동으로:

1. ✅ **코드 빌드**
   - JDK 17 설정
   - Gradle 빌드

2. ✅ **테스트 실행**
   - 1322개 테스트 실행
   - 커버리지 리포트 생성

3. ✅ **Docker 이미지 생성**
   - Dockerfile 기반 빌드
   - 이미지 최적화

4. ✅ **서버에 .env.prod 자동 생성** ⭐ 신규!
   - GitHub Secrets에서 값 읽기
   - 서버에 `.env.prod` 자동 생성
   - 파일 권한 자동 설정 (600)

5. ✅ **환경 변수 검증**
   - 11개 필수 변수 확인
   - 값 길이 검증
   - 마스킹된 값 표시

6. ✅ **컨테이너 배포**
   - 기존 컨테이너 중지
   - 새 컨테이너 시작
   - 헬스 체크

### 4. 배포 완료 확인

GitHub Repository → Actions 탭에서 실시간 진행 상황 확인

성공 시:
```
================================================
✅ Deployment successful - Application is healthy
================================================
```

---

## 🔍 배포 상태 확인

### GitHub Actions 로그
1. Repository → Actions 탭
2. 최신 워크플로우 선택
3. 각 단계별 로그 확인

### 서버에서 직접 확인 (선택사항)
```bash
# 서버 접속
ssh user@49.142.154.182

# 컨테이너 상태
cd ~/hamalog-deploy
docker-compose ps

# 로그 확인
docker-compose logs -f hamalog-app

# .env.prod 파일 확인 (자동 생성됨)
ls -l .env.prod
# 출력: -rw------- (권한 600)

# 헬스 체크
curl http://localhost:8080/actuator/health
```

---

## 🆘 트러블슈팅

### 문제: "Secret not found"
```
Error: Secret JWT_SECRET not found
```

**해결**:
1. GitHub Repository → Settings → Secrets 확인
2. 누락된 Secret 추가
3. 다시 푸시

### 문제: 카카오 로그인 실패
```
Error: Client id must not be empty
```

**해결**:
1. `KAKAO_CLIENT_ID` Secret 확인
2. 카카오 개발자 콘솔에서 REST API 키 확인
3. Secret 업데이트

### 문제: 헬스 체크 실패
```
❌ Application health check failed
```

**해결**:
```bash
# 서버에서 로그 확인
ssh user@49.142.154.182
cd ~/hamalog-deploy
docker-compose logs --tail=100 hamalog-app

# 컨테이너 재시작
docker-compose restart hamalog-app
```

---

## 🔄 Secrets 업데이트

### 언제 업데이트가 필요한가?

- ⏰ **정기 로테이션**: 3-6개월마다 보안 크레덴셜 변경
- 🔐 **보안 사고**: 크레덴셜 노출 의심 시 즉시 변경
- 🔑 **카카오 키 변경**: 카카오 앱 설정 변경 시

### 업데이트 방법

1. **새 크레덴셜 생성**
   ```bash
   ./generate-github-secrets.sh
   ```

2. **GitHub Secrets 업데이트**
   - Repository → Settings → Secrets
   - 변경할 Secret 선택
   - "Update secret" 클릭
   - 새 값 입력

3. **재배포**
   ```bash
   git commit --allow-empty -m "chore: Rotate credentials"
   git push origin main
   ```

---

## 📊 자동화 이전/이후 비교

| 작업 | 기존 방식 | 새로운 방식 |
|------|-----------|-------------|
| **초기 설정** | 서버 SSH 접속 필요 | GitHub Secrets만 설정 |
| **배포 시간** | 5-10분 (수동 작업 포함) | 2-3분 (완전 자동) |
| **수동 작업** | 서버 접속, 파일 편집 | 없음 (푸시만) |
| **에러 가능성** | 높음 (수동 오타 등) | 낮음 (자동화) |
| **크레덴셜 관리** | 서버 파일 | GitHub Secrets (중앙화) |
| **보안** | 파일 권한 의존 | GitHub 암호화 저장 |

---

## ✅ 체크리스트

### 최초 설정 (1회만)
- [ ] `./generate-github-secrets.sh` 실행
- [ ] 14개 GitHub Secrets 설정
  - [ ] JWT_SECRET
  - [ ] HAMALOG_ENCRYPTION_KEY
  - [ ] DB_PASSWORD
  - [ ] MYSQL_ROOT_PASSWORD
  - [ ] SPRING_DATA_REDIS_PASSWORD
  - [ ] KAKAO_CLIENT_ID ⚠️ 중요
  - [ ] KAKAO_CLIENT_SECRET ⚠️ 중요
  - [ ] SERVER_HOST
  - [ ] SERVER_USER
  - [ ] SERVER_PORT
  - [ ] SSH_PRIVATE_KEY
  - [ ] (선택) JWT_EXPIRY
  - [ ] (선택) KAKAO_REDIRECT_URI
  - [ ] (선택) FRONTEND_URL

### 매 배포마다
- [ ] `git push origin main`
- [ ] GitHub Actions 성공 확인

---

## 🎉 완성!

이제 **코드 푸시만으로 모든 배포가 자동 완료**됩니다!

```bash
# 전부 다 이것뿐!
git push origin main
```

서버 접속도, 파일 편집도, 권한 설정도 필요 없습니다! 🚀

---

## 📞 참고 문서

- `GITHUB_ACTIONS_SERVER_SETUP.md` - GitHub Actions 상세 설정
- `DEPLOYMENT_TROUBLESHOOTING.md` - 배포 문제 해결
- `DEPLOYMENT_SECURITY_GUIDE.md` - 보안 가이드

---

**작성일**: 2025-11-22  
**방식**: GitHub Secrets 기반 완전 자동화  
**수동 작업**: 없음 (푸시만!)

