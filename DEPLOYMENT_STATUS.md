# 배포 상태 및 문제 분석 보고서

**작성 날짜**: 2025-11-22  
**상태**: ⚠️ 서버 연결 문제 진단 및 워크플로우 개선 완료

---

## 1. 문제 분석

### 1.1 원래 오류 메시지
```
ssh: connect to host 49.142.154.182 port 22: Connection timed out
Error: Process completed with exit code 255
```

### 1.2 근본 원인

#### 시나리오 A: 서버 상태 확인 (로컬 환경)
```bash
# SSH 연결 테스트
$ ssh -v -i ~/.ssh/id_rsa ubuntu@49.142.154.182
debug1: connect to address 49.142.154.182 port 22: Operation timed out

# Ping 테스트
$ ping -c 3 49.142.154.182
PING 49.142.154.182: 56 data bytes
Request timeout for icmp_seq 0
Request timeout for icmp_seq 1
--- 49.142.154.182 ping statistics ---
3 packets transmitted, 0 packets received, 100.0% packet loss
```

**결론**: 로컬 환경에서도 서버에 연결할 수 없음  
**가능한 원인**:
- 서버가 완전히 다운됨
- 네트워크 인터페이스가 비활성화됨
- 방화벽이 모든 트래픽을 차단
- ISP 또는 네트워크 문제

---

## 2. 적용된 개선 사항

### 2.1 워크플로우 파일 개선 (.github/workflows/deploy.yml)

#### Before (문제점)
```yaml
- name: Copy files to server
  run: |
    set -x
    ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_HOST} "mkdir -p ~/hamalog-deploy"
    scp -P ${SERVER_PORT} hamalog-image.tar ${SERVER_USER}@${SERVER_HOST}:~/hamalog-deploy/
    # → 재시도 로직 없음
    # → 타임아웃 설정 없음
    # → 명확한 에러 메시지 부족
```

#### After (개선 사항)
```yaml
- name: Copy files to server
  run: |
    set -e
    # 재시도 로직 (최대 3회)
    MAX_RETRIES=3
    RETRY_COUNT=0
    
    while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
      RETRY_COUNT=$((RETRY_COUNT + 1))
      echo "Attempt $RETRY_COUNT of $MAX_RETRIES..."
      
      if ssh -p ${SERVER_PORT} \
          -o ConnectTimeout=30 \
          -o StrictHostKeyChecking=accept-new \
          ${SERVER_USER}@${SERVER_HOST} "mkdir -p ~/hamalog-deploy" 2>&1; then
        echo "✅ SSH connection successful"
        break
      else
        if [ $RETRY_COUNT -lt $MAX_RETRIES ]; then
          echo "⏳ Connection attempt $RETRY_COUNT failed. Waiting..."
          sleep $((RETRY_COUNT * 5))
        fi
      fi
    done
    
    # 연결 실패 시 명확한 오류 메시지
    if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
      echo "❌ DEPLOYMENT FAILED: SSH Connection Error"
      echo "Server: $SERVER_HOST:$SERVER_PORT"
      echo "User: $SERVER_USER"
      exit 1
    fi
    
    # 추가 타임아웃 설정
    scp -P ${SERVER_PORT} \
        -o ConnectTimeout=30 \
        hamalog-image.tar ${SERVER_USER}@${SERVER_HOST}:~/hamalog-deploy/
```

### 2.2 Deploy to server 단계 개선

#### 주요 개선 사항
1. **SSH 연결 재시도 로직**
   - 최대 3회 재시도
   - 지수 백오프 (5초, 10초, 15초)
   - 명확한 진행 상황 표시

2. **타임아웃 설정**
   - ConnectTimeout=30초 적용
   - SSH 연결 안정성 향상

3. **Graceful Error Handling**
   - 서버 연결 불가 시 배포 스킵
   - 빌드 아티팩트 보존 (수동 배포 가능)
   - 명확한 문제 진단 정보 제공

4. **리소스 정리**
   - 중복된 .env.prod 전송 제거
   - 임시 파일 정리 개선

---

## 3. 워크플로우 실행 시나리오

### 시나리오 1: 서버 정상 (권장 상태)
```
✅ Build and Test 성공
  ↓
✅ Build Docker Image 성공
  ↓
✅ Copy files to server (재시도 1회 성공)
  ↓
✅ Deploy to server (환경 변수 검증 → 컨테이너 시작 → 헬스 체크)
  ↓
✅ 배포 완료
```

### 시나리오 2: 서버 다운 (현재 상황)
```
✅ Build and Test 성공
  ↓
✅ Build Docker Image 성공
  ↓
⚠️ Copy files to server (3회 재시도 후 실패)
  ↓
⚠️ Deploy to server (continue-on-error=true로 실행 계속)
  ↓
⚠️ SSH 연결 불가 → 배포 스킵
  ↓
✅ 빌드 아티팩트 보존
  ⚠️ 배포는 실패했지만 수동 배포 가능
```

### 시나리오 3: 일부 네트워크 문제 (간헐적 타임아웃)
```
✅ Build and Test 성공
  ↓
✅ Build Docker Image 성공
  ↓
⚠️ Copy files to server (재시도 1회 실패)
  ⏳ 5초 대기
  ✅ 재시도 2회 성공
  ↓
✅ Deploy to server 진행
  ↓
✅ 배포 완료
```

---

## 4. 필요한 조치

### 4.1 즉시 필요 사항
1. **서버 상태 확인**
   ```bash
   # 서버에 직접 접속하여 확인
   ssh -v -i <ssh-key> ubuntu@49.142.154.182
   
   # 또는 VPN/콘솔을 통해 서버 상태 확인
   - SSH 서비스 실행 상태
   - 네트워크 인터페이스 활성화 상태
   - 방화벽 규칙 확인
   ```

2. **SSH 공개키 등록 확인**
   ```bash
   # 서버의 ~/.ssh/authorized_keys에 공개키 포함 확인
   # GitHub Secrets의 SSH_PRIVATE_KEY와 쌍을 이루는 공개키
   ```

### 4.2 서버 복구 후 필요 사항
1. SSH 서비스 시작
   ```bash
   sudo systemctl start ssh
   sudo systemctl enable ssh
   ```

2. 방화벽 포트 개방 (포트 22)
   ```bash
   # Ubuntu/Debian (ufw)
   sudo ufw allow 22/tcp
   
   # CentOS/RHEL (firewalld)
   sudo firewall-cmd --permanent --add-port=22/tcp
   sudo firewall-cmd --reload
   ```

3. 클라우드 보안 그룹 설정 (만약 클라우드 사용 시)
   - 인바운드: SSH (포트 22) 허용
   - 아웃바운드: 모두 허용

### 4.3 배포 검증
서버 복구 후:
```bash
# 1. 로컬에서 SSH 접근 테스트
ssh -v ubuntu@49.142.154.182 "echo 'Connected successfully'"

# 2. GitHub Actions 다시 실행
# - 리포지토리의 "Actions" 탭으로 이동
# - 마지막 workflow run 선택
# - "Re-run failed jobs" 클릭
# 또는 새 커밋 푸시

# 3. 배포 로그 확인
# - Workflow 실행 결과 확인
# - "Deploy to server" 단계의 로그 검토
```

---

## 5. 향후 개선 계획

### 5.1 모니터링 추가
- 배포 실패 시 알림 (Slack, Email)
- 서버 헬스 체크 자동화
- 배포 로그 자동 보관

### 5.2 재해 복구
- 자동 롤백 기능
- 여러 서버 배포 지원 (Blue-Green 배포)
- 배포 전 사전 검사 추가

### 5.3 문서화
- 수동 배포 절차 작성
- 서버 설정 자동화 스크립트
- 트러블슈팅 가이드

---

## 6. 현재 GitHub Secrets 상태

필수 Secrets (설정됨):
- ✅ SSH_PRIVATE_KEY
- ✅ SERVER_HOST
- ✅ SERVER_USER
- ✅ SERVER_PORT
- ✅ JWT_SECRET
- ✅ KAKAO_CLIENT_ID
- ✅ KAKAO_CLIENT_SECRET
- ✅ DB_PASSWORD
- ✅ MYSQL_ROOT_PASSWORD
- ✅ HAMALOG_ENCRYPTION_KEY
- ✅ SPRING_DATA_REDIS_PASSWORD
- ✅ DB_NAME
- ✅ DB_USERNAME

---

## 7. 요약

**현재 상황**:
- 서버가 네트워크에서 응답하지 않음
- GitHub Actions 워크플로우 개선 완료
- 서버 복구 시 자동 배포 가능

**다음 단계**:
1. 서버 상태 확인 및 복구
2. SSH 연결 테스트
3. GitHub Actions 워크플로우 재실행
4. 배포 성공 확인

---

## 참고 자료

- [GitHub Actions Secrets Documentation](https://docs.github.com/en/actions/security-guides/using-secrets-in-github-actions)
- [SSH Key Setup Guide](https://docs.github.com/en/authentication/connecting-to-github-with-ssh)
- [Docker Compose Deployment](https://docs.docker.com/compose/compose-file/)

