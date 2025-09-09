# JWT_SECRET 배포 가이드

## 🎯 목적
이 가이드는 Hamalog 애플리케이션 배포 시 JWT_SECRET 관련 이슈를 방지하고, 발생 시 빠르게 해결할 수 있도록 돕습니다.

---

## ⚠️ 공통 문제 상황

### 문제: 컨테이너 시작 실패
**오류 메시지:**
```
JWT 비밀키가 설정되지 않았습니다. 프로덕션 환경에서는 JWT_SECRET 환경변수를 반드시 설정해야 합니다.
```

### 근본 원인
이 오류는 다음과 같은 상황에서 발생합니다:
1. **JWT_SECRET 환경변수가 빈 문자열로 설정됨** ← 가장 흔한 원인
2. JWT_SECRET 환경변수가 아예 설정되지 않음 (프로덕션 환경에서만)
3. JWT_SECRET이 너무 짧음 (32자 미만)
4. JWT_SECRET이 유효하지 않은 Base64 형식

---

## 🔍 진단 도구 사용법

### 1. 환경변수 진단 도구 실행
```bash
# 프로젝트 루트에서 실행
./environment-diagnostic.sh
```

**출력 예시 (문제 상황):**
```
🔐 JWT_SECRET 상태 진단:
   상태: ⚠️  빈 문자열로 설정됨 (이것이 배포 오류의 원인!)
   값: ''
   길이: 0

📊 종합 진단 결과:
❌ 치명적: JWT_SECRET이 빈 문자열로 설정되어 있습니다.
   해결방법: export JWT_SECRET=$(openssl rand -base64 32)
```

### 2. 배포 전 유효성 검사
```bash
# 배포 스크립트가 자동으로 환경변수를 검증합니다
./deploy.sh
```

**출력 예시 (문제 감지 시):**
```
🔍 환경변수 유효성 검사 중...
❌ JWT_SECRET이 빈 문자열로 설정되어 있습니다. (이것이 컨테이너 시작 오류의 원인입니다!)
현재 값: '' (길이: 0)
❌ 환경변수 검증에 실패했습니다. 배포를 중단합니다.
```

---

## 🔧 해결 방법

### 1. JWT_SECRET 생성 및 설정
```bash
# 1단계: 보안 키 생성
export JWT_SECRET=$(openssl rand -base64 32)

# 2단계: 설정 확인
echo $JWT_SECRET
# 예시 출력: dGVzdC1qd3Qtc2VjcmV0LWtleS0xMjMtNDU2Nzg5MA==

# 3단계: 길이 확인 (32자 이상이어야 함)
echo ${#JWT_SECRET}
# 예시 출력: 44
```

### 2. 영구 설정 (권장)
```bash
# ~/.bashrc 또는 ~/.zshrc에 추가
echo "export JWT_SECRET=$(openssl rand -base64 32)" >> ~/.bashrc
source ~/.bashrc
```

### 3. Docker 환경에서 설정
```bash
# Docker 컨테이너 실행 시
docker run -e JWT_SECRET="$(openssl rand -base64 32)" -e SPRING_PROFILES_ACTIVE=prod hamalog:latest

# Docker Compose 사용 시 (.env 파일에 추가)
echo "JWT_SECRET=$(openssl rand -base64 32)" >> .env
```

---

## 🚨 배포 환경별 주의사항

### 개발 환경 (default profile)
- JWT_SECRET이 설정되지 않으면 **자동으로 임시 키 생성**
- 애플리케이션 재시작마다 키가 변경됨
- 기존 JWT 토큰이 무효화됨

### 프로덕션 환경 (prod profile)
- JWT_SECRET이 **반드시 설정되어야 함**
- 빈 문자열이나 짧은 키는 시작 실패 원인
- 키 변경 시 모든 사용자가 재로그인 필요

---

## 🐛 문제 해결 시나리오

### 시나리오 1: CI/CD 파이프라인에서 빈 값 전달
**문제:** GitHub Actions나 다른 CI/CD 도구에서 `JWT_SECRET=""`로 설정
**해결:**
```yaml
# GitHub Actions 예시
env:
  JWT_SECRET: ${{ secrets.JWT_SECRET }}
# secrets.JWT_SECRET이 빈 값이 아닌지 확인
```

### 시나리오 2: .env 파일의 잘못된 설정
**문제:** `.env` 파일에 `JWT_SECRET=` (값 없음)으로 설정
**해결:**
```bash
# .env 파일 수정
JWT_SECRET=dGVzdC1qd3Qtc2VjcmV0LWtleS0xMjMtNDU2Nzg5MA==
# 또는 해당 라인 삭제
```

### 시나리오 3: Docker 실행 시 명시적 빈 값 전달
**문제:** `docker run -e JWT_SECRET="" ...`
**해결:**
```bash
# 올바른 방법
docker run -e JWT_SECRET="$(openssl rand -base64 32)" ...
# 또는 환경변수를 아예 설정하지 않음 (개발 환경에서만)
docker run ...
```

---

## 📝 체크리스트

### 배포 전 확인사항
- [ ] `./environment-diagnostic.sh` 실행하여 환경변수 상태 확인
- [ ] JWT_SECRET이 32자 이상인지 확인
- [ ] JWT_SECRET이 유효한 Base64 형식인지 확인
- [ ] 프로덕션 환경에서는 JWT_SECRET이 반드시 설정되어 있는지 확인

### 문제 발생 시 확인사항
- [ ] 에러 메시지에서 "현재 JWT_SECRET 상태" 확인
- [ ] 빈 문자열 (`''`) vs null vs 너무 짧음 구분
- [ ] Spring Profile이 올바르게 설정되어 있는지 확인
- [ ] Docker 환경변수가 올바르게 전달되고 있는지 확인

---

## 🔗 관련 도구 및 명령어

### 환경변수 진단
```bash
# 종합 진단
./environment-diagnostic.sh

# JWT_SECRET만 확인
echo "JWT_SECRET: '${JWT_SECRET:-NOT_SET}'"
echo "길이: ${#JWT_SECRET}"
```

### 키 생성 및 검증
```bash
# 새 키 생성
openssl rand -base64 32

# Base64 유효성 검사
echo "$JWT_SECRET" | base64 -d >/dev/null && echo "유효함" || echo "유효하지 않음"

# 키 길이 확인 (바이트 단위)
echo "$JWT_SECRET" | base64 -d | wc -c
```

### 배포 관련
```bash
# 환경변수와 함께 배포
JWT_SECRET=$(openssl rand -base64 32) ./deploy.sh

# 특정 키로 배포
JWT_SECRET="your-existing-key" ./deploy.sh
```

---

## 📚 추가 정보

### 보안 권장사항
- JWT_SECRET은 최소 256비트(32바이트) 이상 사용
- 정기적으로 키 로테이션 수행
- 키를 코드나 설정 파일에 하드코딩하지 않음
- 프로덕션 환경에서는 보안 키 관리 시스템 사용 권장

### 트러블슈팅 연락처
- 기술 지원: 개발팀
- 긴급 상황: 운영팀
- 문서 업데이트 요청: DevOps 팀

---

## 📅 문서 정보
- **최종 업데이트:** 2025-09-09
- **작성자:** Junie (Claude Sonnet 4)  
- **버전:** 1.0
- **관련 이슈:** JWT_SECRET 배포 환경 빈 값 문제

---

**💡 팁:** 문제가 지속되면 `./environment-diagnostic.sh` 출력 결과와 함께 지원팀에 문의하세요!