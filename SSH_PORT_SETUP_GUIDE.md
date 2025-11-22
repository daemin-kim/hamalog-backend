# SSH 포트(22번) 개방 가이드

**작성 날짜**: 2025-11-22  
**목표**: 배포용 컴퓨터의 SSH 포트(22번) 개방

---

## 📋 목차

1. [현재 상황 확인](#현재-상황-확인)
2. [운영체제별 포트 개방 방법](#운영체제별-포트-개방-방법)
3. [포트 개방 확인](#포트-개방-확인)
4. [SSH 서비스 설정](#ssh-서비스-설정)
5. [보안 설정](#보안-설정)
6. [트러블슈팅](#트러블슈팅)

---

## 현재 상황 확인

### 🔍 현재 상태
- ✅ 포트 8080: **열려있음** (애플리케이션 서버)
- ❌ 포트 22: **닫혀있음** (SSH)
- 필요: SSH 포트 22 개방

### 서버 정보 확인
```bash
# 현재 서버 운영체제 확인
uname -a

# 포트 확인
netstat -tlnp | grep 8080
netstat -tlnp | grep 22
```

---

## 운영체제별 포트 개방 방법

### 🐧 Linux (Ubuntu/Debian)

#### 방법 1: UFW (Uncomplicated Firewall) - 권장
```bash
# UFW 상태 확인
sudo ufw status

# UFW 활성화 (필요시)
sudo ufw enable

# SSH 포트(22) 개방
sudo ufw allow 22/tcp

# 또는 SSH 서비스명으로 개방
sudo ufw allow ssh

# 포트 개방 확인
sudo ufw status
```

**결과 예시**:
```
To                         Action      From
--                         ------      ----
22/tcp                     ALLOW       Anywhere
22/tcp (v6)                ALLOW       Anywhere (v6)
8080/tcp                   ALLOW       Anywhere
8080/tcp (v6)              ALLOW       Anywhere (v6)
```

#### 방법 2: iptables - 고급 사용자
```bash
# 현재 규칙 확인
sudo iptables -L -n

# SSH 포트 개방
sudo iptables -A INPUT -p tcp --dport 22 -j ACCEPT

# 설정 저장 (Debian/Ubuntu)
sudo netfilter-persistent save

# 또는 (CentOS/RHEL)
sudo service iptables save
```

---

### 🔴 Linux (CentOS/RHEL)

#### 방법 1: firewalld - 권장
```bash
# firewalld 상태 확인
sudo systemctl status firewalld

# firewalld 시작 (필요시)
sudo systemctl start firewalld
sudo systemctl enable firewalld

# SSH 서비스 추가 (영구적)
sudo firewall-cmd --permanent --add-service=ssh

# 또는 포트로 추가 (영구적)
sudo firewall-cmd --permanent --add-port=22/tcp

# 설정 다시 로드
sudo firewall-cmd --reload

# 확인
sudo firewall-cmd --list-ports
sudo firewall-cmd --list-services
```

#### 방법 2: iptables
```bash
# SSH 포트 개방
sudo iptables -I INPUT -p tcp --dport 22 -j ACCEPT

# 설정 저장
sudo service iptables save

# 서비스 재시작
sudo systemctl restart iptables
```

---

### 🪟 Windows Server

#### 방법 1: PowerShell (관리자 권한 필요)
```powershell
# PowerShell을 관리자로 실행

# 현재 방화벽 규칙 확인
Get-NetFirewallRule | Select-Object DisplayName, Direction, Action

# SSH 포트 개방 (인바운드)
New-NetFirewallRule `
  -DisplayName "SSH" `
  -Direction Inbound `
  -Action Allow `
  -Protocol TCP `
  -LocalPort 22

# 확인
Get-NetFirewallRule -DisplayName "SSH" | fl
```

#### 방법 2: Windows Defender 방화벽 (GUI)
1. **Windows 설정 열기** → **개인 정보 보호 및 보안** → **방화벽 및 네트워크 보호**
2. **고급 설정 허용** 클릭
3. **인바운드 규칙** → **새 규칙**
4. **포트** 선택 → **다음**
5. **TCP** 선택 → **특정 로컬 포트** → `22` 입력
6. **허용** 선택 → **다음**
7. **도메인, 개인, 공개** 모두 선택
8. 이름: "SSH" 입력 → **마침**

---

### 🍎 macOS

#### 방법 1: System Preferences (GUI)
```bash
# 방화벽 확인
defaults read /Library/Preferences/com.apple.alf globalstate

# 방화벽 비활성화 (SSH만 필요한 경우)
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --setglobalstate off

# SSH 서비스 확인
sudo launchctl list | grep sshd

# SSH 서비스 시작
sudo launchctl start com.openssh.sshd
```

#### 방법 2: Terminal
```bash
# 방화벽 활성화 (기본값)
defaults write /Library/Preferences/com.apple.alf globalstate -int 1

# 특정 앱 허용
/usr/libexec/ApplicationFirewall/socketfilterfw -a /Applications/Terminal.app

# SSH 서비스 시작
sudo launchctl start com.openssh.sshd

# SSH 서비스 자동 시작 설정
sudo launchctl enable system/com.openssh.sshd
```

---

## 포트 개방 확인

### ✅ 포트 개방 확인 방법

#### 1. 로컬에서 포트 확인
```bash
# Linux/macOS
netstat -tlnp | grep 22
# 또는
ss -tlnp | grep 22
# 또는
lsof -i :22

# 출력 예시 (SSH 서비스가 포트 22를 listening 중)
tcp        0      0 0.0.0.0:22              0.0.0.0:*               LISTEN      1234/sshd
```

#### 2. 다른 컴퓨터에서 SSH 연결 테스트
```bash
# SSH 연결 테스트 (verbose mode)
ssh -v -p 22 username@your-server-ip

# 성공 시 출력:
debug1: SSH2_MSG_SERVICE_ACCEPT received
debug1: Authentications that can continue: publickey,password
```

#### 3. nmap 도구로 확인
```bash
# nmap 설치 (Ubuntu/Debian)
sudo apt-get install nmap

# nmap으로 포트 확인
nmap -p 22 localhost
nmap -p 22 your-server-ip

# 출력 예시
PORT   STATE SERVICE
22/tcp open  ssh
```

#### 4. telnet으로 확인
```bash
# telnet 연결 테스트
telnet your-server-ip 22

# 성공 시 SSH 배너 출력:
Trying your-server-ip...
Connected to your-server-ip.
Escape character is '^]'.
SSH-2.0-OpenSSH_7.4
```

---

## SSH 서비스 설정

### SSH 서비스 확인 및 시작

#### Linux
```bash
# SSH 서비스 상태 확인
sudo systemctl status ssh
# 또는
sudo systemctl status sshd

# SSH 서비스 시작
sudo systemctl start ssh
# 또는
sudo systemctl start sshd

# SSH 서비스 자동 시작 설정
sudo systemctl enable ssh
# 또는
sudo systemctl enable sshd

# SSH 서비스 다시 시작 (설정 변경 후)
sudo systemctl restart ssh
```

#### SSH 설정 파일 확인
```bash
# SSH 데몬 설정 파일
sudo cat /etc/ssh/sshd_config

# 주요 설정 확인
grep -E "^Port|^PermitRootLogin|^PasswordAuthentication|^PubkeyAuthentication" /etc/ssh/sshd_config

# 기본 설정 (변경 전 확인 권장)
Port 22
PermitRootLogin no
PasswordAuthentication yes
PubkeyAuthentication yes
```

---

## 보안 설정

### 🔒 추천되는 보안 설정

#### 1. SSH 포트 변경 (선택사항)
```bash
# SSH 설정 파일 편집
sudo nano /etc/ssh/sshd_config

# Port 22를 다른 포트로 변경 (예: 2222)
Port 2222

# 파일 저장 및 종료
# Ctrl+X → Y → Enter

# SSH 서비스 재시작
sudo systemctl restart ssh

# 방화벽에서도 새 포트 개방
sudo ufw allow 2222/tcp
sudo ufw deny 22/tcp
```

#### 2. 비밀번호 인증 비활성화 (권장)
```bash
# SSH 설정 파일 편집
sudo nano /etc/ssh/sshd_config

# 다음 항목 변경
PasswordAuthentication no
PubkeyAuthentication yes

# SSH 서비스 재시작
sudo systemctl restart ssh
```

#### 3. Root 직접 로그인 차단
```bash
# SSH 설정 파일에서 확인
grep PermitRootLogin /etc/ssh/sshd_config

# 기본값이 'no'라면 이미 설정됨
# 필요시 설정
sudo nano /etc/ssh/sshd_config
# PermitRootLogin no

# SSH 서비스 재시작
sudo systemctl restart ssh
```

#### 4. 방화벽 규칙 - SSH 접근 제한 (선택사항)
```bash
# 특정 IP에서만 SSH 접근 허용
sudo ufw allow from 203.0.113.0 to any port 22

# 또는 특정 IP 범위
sudo ufw allow from 203.0.113.0/24 to any port 22

# 확인
sudo ufw status numbered
```

---

## 트러블슈팅

### 🔧 포트 개방 확인되지 않는 경우

#### 문제 1: SSH 서비스가 실행 중이 아님
```bash
# 현재 상태 확인
sudo systemctl status ssh

# 시작
sudo systemctl start ssh

# 자동 시작 설정
sudo systemctl enable ssh
```

#### 문제 2: 방화벽이 포트를 차단
```bash
# 현재 방화벽 규칙 확인
sudo ufw status
# 또는
sudo firewall-cmd --list-all

# 포트 개방 확인
sudo ufw show added
# 또는
sudo firewall-cmd --list-ports
```

#### 문제 3: SSH 설정 파일에 오류
```bash
# SSH 설정 파일 검증
sudo sshd -t

# 정상이면 출력 없음
# 오류가 있으면 오류 메시지 표시

# 설정 파일 확인
sudo nano /etc/ssh/sshd_config
```

#### 문제 4: 포트가 다른 서비스에 의해 사용 중
```bash
# 포트 22를 사용하는 프로세스 확인
sudo lsof -i :22
# 또는
sudo netstat -tlnp | grep :22

# 예상치 못한 프로세스가 포트를 사용 중이면
# 해당 프로세스 종료 또는 SSH 포트 변경
```

---

## 📝 빠른 체크리스트

- [ ] 서버 운영체제 확인
- [ ] 방화벽 설정에서 포트 22 개방
- [ ] SSH 서비스 시작 및 활성화
- [ ] SSH 포트 개방 확인 (netstat, ss, lsof)
- [ ] 다른 컴퓨터에서 SSH 연결 테스트
- [ ] GitHub Actions에서 배포 테스트

---

## 🎯 설정 완료 후

### 1. GitHub Actions 워크플로우 테스트
```bash
# 로컬에서 먼저 테스트
ssh -v -p 22 ubuntu@your-server-ip "echo 'SSH connection successful'"

# 성공하면 GitHub Actions에서 배포 재실행
```

### 2. 배포 재실행
```
Repository → Actions → Latest workflow → Re-run failed jobs
```

### 3. 배포 로그 확인
```
Copy files to server 단계에서 재시도 로직 작동 확인
```

---

## 📚 참고 자료

- [OpenSSH Manual](https://man.openbsd.org/sshd_config)
- [UFW Documentation](https://help.ubuntu.com/community/UFW)
- [firewalld Documentation](https://firewalld.org/)
- [SSH Best Practices](https://www.ssh.com/ssh/best-practices)

---

## 🆘 추가 지원이 필요한 경우

현재 서버의 정보를 제공해주시면 더 구체적인 가이드를 제공할 수 있습니다:

1. **운영체제**: Ubuntu/Debian, CentOS/RHEL, Windows Server, macOS 등
2. **현재 방화벽**: UFW, firewalld, iptables, Windows Defender 등
3. **방화벽 상태**: `sudo ufw status` 또는 `sudo firewall-cmd --list-all` 결과
4. **SSH 서비스 상태**: `sudo systemctl status ssh` 결과

---

**작성자**: GitHub Copilot  
**최종 검토**: 2025-11-22  
**상태**: ✅ 완료

