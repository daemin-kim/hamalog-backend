# 🚀 SSH 포트 22 개방 - 빠른 설정 가이드

**상황**: 포트 8080만 열려있음, 포트 22(SSH)는 닫혀있음  
**목표**: SSH 포트 22 개방

---

## ⚡ 5분 안에 완료하기

### 1️⃣ 서버 운영체제 확인
```bash
uname -a
```

**예상 결과**:
- `Linux ... ubuntu ...` → Ubuntu/Debian 사용
- `Linux ... el7` → CentOS/RHEL 사용
- `Darwin` → macOS 사용

---

## 🐧 Ubuntu/Debian 사용 시

### 단계 1: 방화벽 설정 (UFW)
```bash
# SSH 포트 개방
sudo ufw allow 22/tcp

# 포트 개방 확인
sudo ufw status
```

### 단계 2: SSH 서비스 시작
```bash
# SSH 서비스 시작
sudo systemctl start ssh

# 자동 시작 설정
sudo systemctl enable ssh

# 상태 확인
sudo systemctl status ssh
```

### 단계 3: 포트 개방 확인
```bash
# 포트 확인
netstat -tlnp | grep 22

# 또는
ss -tlnp | grep 22
```

**예상 결과**:
```
tcp        0      0 0.0.0.0:22              0.0.0.0:*               LISTEN      1234/sshd
```

---

## 🔴 CentOS/RHEL 사용 시

### 단계 1: 방화벽 설정 (firewalld)
```bash
# firewalld 시작
sudo systemctl start firewalld
sudo systemctl enable firewalld

# SSH 포트 개방 (영구적)
sudo firewall-cmd --permanent --add-port=22/tcp

# 설정 다시 로드
sudo firewall-cmd --reload

# 확인
sudo firewall-cmd --list-ports
```

### 단계 2: SSH 서비스 시작
```bash
# SSH 서비스 시작
sudo systemctl start sshd

# 자동 시작 설정
sudo systemctl enable sshd

# 상태 확인
sudo systemctl status sshd
```

### 단계 3: 포트 개방 확인
```bash
# 포트 확인
netstat -tlnp | grep 22
# 또는
ss -tlnp | grep 22
```

---

## 🪟 Windows Server 사용 시

### PowerShell (관리자 권한 필요)
```powershell
# PowerShell을 관리자로 실행

# SSH 포트 개방
New-NetFirewallRule `
  -DisplayName "SSH" `
  -Direction Inbound `
  -Action Allow `
  -Protocol TCP `
  -LocalPort 22

# 확인
Get-NetFirewallRule -DisplayName "SSH" | fl
```

---

## 🍎 macOS 사용 시

### Terminal
```bash
# SSH 서비스 시작
sudo launchctl start com.openssh.sshd

# 자동 시작 설정
sudo launchctl enable system/com.openssh.sshd

# 상태 확인
sudo launchctl list | grep sshd
```

---

## ✅ 포트 개방 확인 - 다른 컴퓨터에서 테스트

```bash
# 다른 컴퓨터에서 실행
ssh -v -p 22 ubuntu@your-server-ip

# 또는
ssh -v -p 22 username@your-server-ip
```

**성공 시 표시**:
```
✅ Connected
✅ SSH banner received
✅ Key negotiated
```

**실패 시 표시**:
```
❌ Operation timed out
❌ Connection refused
```

---

## 🎯 완료 확인 리스트

- [ ] 서버에 로그인
- [ ] 운영체제 확인
- [ ] 방화벽에서 포트 22 개방
- [ ] SSH 서비스 시작
- [ ] SSH 서비스 자동 시작 설정
- [ ] 다른 컴퓨터에서 SSH 연결 테스트 성공
- [ ] GitHub Actions 배포 재실행

---

## 🔗 다음 단계

포트 22 개방이 완료되면:

1. **로컬에서 SSH 연결 테스트**
   ```bash
   ssh -v ubuntu@49.142.154.182
   ```

2. **GitHub Actions 워크플로우 재실행**
   - Repository → Actions
   - Latest workflow 선택
   - "Re-run failed jobs" 클릭

3. **배포 로그 확인**
   - "Copy files to server" 단계 확인
   - "Deploy to server" 단계 확인

---

## 📞 문제가 발생한 경우

### ❌ SSH 연결이 여전히 안 될 때

**확인사항**:
1. 포트가 정말 열려있는지 확인
   ```bash
   sudo ufw status
   sudo firewall-cmd --list-ports
   ```

2. SSH 서비스가 실행 중인지 확인
   ```bash
   sudo systemctl status ssh
   sudo systemctl status sshd
   ```

3. SSH 설정에 오류가 없는지 확인
   ```bash
   sudo sshd -t
   ```

4. 다른 서비스가 포트 22를 사용하는지 확인
   ```bash
   sudo lsof -i :22
   sudo netstat -tlnp | grep 22
   ```

### ❌ "Permission denied" 오류

**원인**: SSH 키 권한 문제  
**해결**:
```bash
chmod 600 ~/.ssh/id_rsa
chmod 700 ~/.ssh
```

### ❌ "Connection refused" 오류

**원인**: SSH 서비스가 실행 중이 아님  
**해결**:
```bash
sudo systemctl start ssh
# 또는
sudo systemctl start sshd
```

---

## 📚 상세 가이드

더 자세한 정보는 `SSH_PORT_SETUP_GUIDE.md`를 참고하세요.

---

**마지막 업데이트**: 2025-11-22  
**상태**: ✅ 준비 완료

