# Docker Desktop Vault Container 설정 가이드

## 문제 해결: Vault 컨테이너가 Docker Desktop에서 보이지 않는 이유

현재 상황에서 Vault 컨테이너가 Docker Desktop에 표시되지 않는 주된 이유는 **Docker Desktop이 실행되지 않았기 때문**입니다.

## 🚀 해결 방법

### 1단계: Docker Desktop 시작

#### macOS에서:
```bash
# Docker Desktop 애플리케이션 실행
open -a Docker

# 또는 Launchpad에서 Docker Desktop 아이콘 클릭
# 또는 Applications 폴더에서 Docker.app 실행
```

#### Windows에서:
- 시작 메뉴에서 "Docker Desktop" 검색하여 실행
- 또는 바탕화면의 Docker Desktop 아이콘 더블클릭

#### Linux에서:
```bash
# Docker 서비스 시작
sudo systemctl start docker

# Docker 서비스 활성화 (부팅 시 자동 시작)
sudo systemctl enable docker
```

### 2단계: Docker Desktop 실행 상태 확인

Docker Desktop이 완전히 시작될 때까지 기다린 후 다음 명령어로 확인:

```bash
# Docker 데몬 상태 확인
docker info

# Docker 버전 확인
docker --version

# 실행 중인 컨테이너 목록 보기
docker ps -a
```

**정상 출력 예시:**
```
Docker version 28.3.2, build 578ccf6
Server:
 Context:    desktop-linux
 Debug Mode: false
 ...
```

### 3단계: Vault 컨테이너 시작

Docker Desktop이 정상적으로 실행된 후:

```bash
# Hamalog 프로젝트 디렉토리로 이동
cd /Users/daeminkim/ideaProjects/Hamalog

# Vault 컨테이너만 시작
docker-compose up -d vault

# 컨테이너 상태 확인
docker-compose ps

# Vault 로그 확인
docker-compose logs vault
```

### 4단계: Docker Desktop에서 확인

Docker Desktop을 열고 다음을 확인:

1. **Containers** 탭 클릭
2. **`hamalog-vault`** 컨테이너가 표시되는지 확인
3. 컨테이너 상태가 **"Running"**인지 확인
4. 포트 **8200:8200**이 매핑되어 있는지 확인

## 🔧 추가 설정 (선택사항)

더 나은 Docker Desktop 가시성을 위해 docker-compose.yml에 라벨을 추가할 수 있습니다:

```yaml
services:
  vault:
    image: hashicorp/vault:1.15.2
    container_name: hamalog-vault
    labels:
      - "com.hamalog.service=vault"
      - "com.hamalog.description=HashiCorp Vault for secure secret management"
      - "com.hamalog.version=1.15.2"
    ports:
      - "8200:8200"
    # ... 기존 설정 유지
```

## 🧪 테스트 및 검증

### 1. Vault 컨테이너 작동 확인
```bash
# Vault 상태 확인
curl -s http://localhost:8200/v1/sys/health | jq '.'

# 또는 간단한 헬스체크
curl -f http://localhost:8200/v1/sys/health && echo "Vault is healthy"
```

### 2. Docker Desktop UI 확인 사항
- Containers 목록에서 `hamalog-vault` 표시
- 컨테이너 상태: Running (녹색)
- CPU/Memory 사용량 표시
- 로그 탭에서 Vault 시작 메시지 확인

## 🔍 문제 해결

### Docker Desktop이 시작되지 않는 경우:

#### macOS:
```bash
# Docker Desktop 재시작
pkill -f Docker
open -a Docker

# 권한 문제 해결
sudo chown -R $(whoami) ~/.docker
```

#### 리소스 부족 문제:
- Docker Desktop 설정에서 메모리 할당량 확인 (최소 4GB 권장)
- CPU 코어 수 확인 (최소 2 코어 권장)

#### 포트 충돌 문제:
```bash
# 8200 포트 사용 중인 프로세스 확인
lsof -i :8200

# 필요시 해당 프로세스 종료
sudo kill -9 <PID>
```

## 📋 완전한 시작 순서

```bash
# 1. Docker Desktop 시작 (GUI 또는 명령어)
open -a Docker

# 2. Docker 상태 확인 (30초 정도 대기)
docker info

# 3. Vault 컨테이너 시작
docker-compose up -d vault

# 4. 상태 확인
docker-compose ps

# 5. Vault 접근 테스트
curl http://localhost:8200/v1/sys/health

# 6. Docker Desktop에서 UI 확인
# - Containers 탭에서 hamalog-vault 확인
# - 상태가 Running인지 확인
```

## 🎯 예상 결과

성공적으로 설정된 경우 Docker Desktop에서 다음을 볼 수 있습니다:

1. **Container Name**: `hamalog-vault`
2. **Image**: `hashicorp/vault:1.15.2`
3. **Status**: Running
4. **Ports**: `8200:8200`
5. **Created**: 방금 전 또는 최근
6. **Actions**: Start/Stop/Delete 버튼들

## 📞 추가 도움

위 단계를 따라도 문제가 지속되는 경우:

1. Docker Desktop 버전 업데이트
2. 시스템 재시작 후 재시도
3. Docker Desktop 재설치 고려

---

**핵심 포인트**: Vault 컨테이너 자체는 올바르게 구성되어 있습니다. Docker Desktop을 시작하기만 하면 정상적으로 표시됩니다.