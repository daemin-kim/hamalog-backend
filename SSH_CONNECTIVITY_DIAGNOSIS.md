# SSH 연결 타임아웃 문제 진단 및 해결

## 📋 현재 상황

- GitHub Actions에서 배포 시 SSH 연결이 타임아웃됨
- 에러: `ssh: connect to host *** port ***: Connection timed out`
- 서버의 22번 포트가 열렸으나 외부 통신 불가능

## 🔍 문제 원인 분석

### 1. **GitHub Actions 러너의 IP 주소**
GitHub Actions는 동적 IP 범위에서 실행되므로, 서버의 방화벽이 특정 IP로 제한되어 있다면 연결 실패

### 2. **서버 포트 상태 확인 필요**
- SSH 데몬이 실제로 22번 포트에서 수신 중인지 확인
- 방화벽 규칙이 올바르게 설정되었는지 확인

### 3. **네트워크 경로 문제**
- ISP 또는 라우터에서 SSH 포트를 차단할 수 있음
- NAT/포트포워딩이 제대로 설정되지 않았을 수 있음

## ✅ 해결 방법

### 단계 1: 서버에서 SSH 상태 확인

```bash
# SSH 데몬 상태 확인
sudo systemctl status ssh

# 또는
sudo systemctl status sshd

# 22번 포트 리스닝 확인
sudo netstat -tlnp | grep :22
# 또는
sudo ss -tlnp | grep :22

# 방화벽 규칙 확인 (Linux UFW 사용 시)
sudo ufw status

# 22번 포트 열기 (필요시)
sudo ufw allow 22/tcp
sudo ufw enable
```

### 단계 2: 포트 접근성 테스트

**로컬에서 테스트:**
```bash
# 현재 컴퓨터에서 SSH 접속 테스트
ssh -v -p 22 <SERVER_USER>@<SERVER_HOST>
```

**외부 도구를 사용한 테스트:**
```bash
# nc(netcat) 사용
nc -zv -w 10 <SERVER_HOST> 22

# telnet 사용
telnet <SERVER_HOST> 22
```

### 단계 3: GitHub Actions에서 네트워크 연결 테스트

임시로 다음을 워크플로우에 추가하여 연결 상태 확인:

```yaml
- name: Test SSH connectivity
  run: |
    echo "Testing DNS resolution..."
    nslookup ${{ secrets.SERVER_HOST }}
    
    echo "Testing TCP connection..."
    timeout 10 bash -c '</dev/tcp/${{ secrets.SERVER_HOST }}/22' && echo "✅ Port 22 is open" || echo "❌ Port 22 is closed or unreachable"
    
    echo "Testing with nc..."
    nc -zv -w 10 ${{ secrets.SERVER_HOST }} 22
```

### 단계 4: 방화벽/보안 그룹 설정

**온프레미스 방화벽 (Linux UFW):**
```bash
# 특정 포트 허용
sudo ufw allow 22/tcp

# 모든 포트 상태 확인
sudo ufw status numbered

# 활성화
sudo ufw enable
```

**라우터 포트포워딩:**
- 라우터 관리 페이지에서 포트포워딩 설정
- 외부 포트 22 → 내부 IP:22로 설정

### 단계 5: SSH 키 권한 확인

```bash
# SSH 디렉토리 권한
chmod 700 ~/.ssh

# SSH 키 권한
chmod 600 ~/.ssh/id_rsa

# authorized_keys 권한
chmod 600 ~/.ssh/authorized_keys
```

## 🔐 보안 권장사항

1. **SSH 기본 포트 변경 (선택사항)**
   - /etc/ssh/sshd_config에서 Port 설정 변경
   - 방화벽에서 새로운 포트 열기
   - GitHub Secrets에서 SERVER_PORT 업데이트

2. **SSH 접근 제한**
   - 알려진 GitHub Actions IP 범위만 허용 (변동성 높음)
   - IP 화이트리스트 대신 키 기반 인증 사용
   - fail2ban 사용하여 무차별 대입 공격 방지

3. **SSH 설정 강화 (/etc/ssh/sshd_config)**
   ```bash
   # 루트 로그인 비활성화
   PermitRootLogin no
   
   # 비밀번호 인증 비활성화 (키만 사용)
   PasswordAuthentication no
   
   # 타임아웃 설정
   ClientAliveInterval 60
   ClientAliveCountMax 3
   ```

## 📊 연결 흐름 다이어그램

```
GitHub Actions Runner
       ↓
DNS Resolution: SERVER_HOST 조회
       ↓
TCP Connection: SERVER_HOST:SERVER_PORT로 연결 시도
       ↓
SSH Handshake: SSH 프로토콜 협상
       ↓
Key Exchange: 퍼블릭 키 인증
       ↓
Command Execution: 배포 작업 실행
```

## 🛠️ 즉시 시도해볼 사항

1. **서버에서 SSH 상태 확인:**
   ```bash
   sudo systemctl restart ssh
   sudo systemctl status ssh
   ```

2. **로컬에서 연결 테스트:**
   ```bash
   ssh -v -p 22 <USER>@<HOST>
   ```

3. **방화벽 재설정:**
   ```bash
   sudo ufw reset
   sudo ufw default deny incoming
   sudo ufw default allow outgoing
   sudo ufw allow 22/tcp
   sudo ufw allow 8080/tcp
   sudo ufw enable
   ```

4. **GitHub Actions 워크플로우 재실행:**
   변경 사항이 적용된 후 워크플로우를 수동으로 재실행

## 📞 추가 지원

문제가 지속되면:
- SSH 상세 로그 확인: `ssh -vvv`
- 서버 SSH 데몬 로그: `sudo tail -f /var/log/auth.log`
- 라우터 포트포워딩 설정 재확인

