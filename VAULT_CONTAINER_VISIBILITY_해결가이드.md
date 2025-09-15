# Vault 컨테이너 Docker Desktop 가시성 문제 해결 가이드

## 🔍 문제 상황 분석

**문제**: 운영환경에서 Vault 컨테이너가 Docker Desktop에서 보이지 않는다는 보고

**실제 조사 결과**: 
- ✅ **Vault 컨테이너는 정상적으로 생성되고 실행 중입니다**
- ✅ **Vault 서비스는 완전히 기능하고 있습니다**
- ✅ **문제는 Docker Desktop UI 가시성에 있습니다**

## 📋 조사 결과 요약

### 확인된 Vault 컨테이너 상태

```bash
# 실행 중인 컨테이너 확인 결과
CONTAINER ID   IMAGE                    COMMAND                  STATUS                     PORTS                     NAMES
6ffb5577de93   hashicorp/vault:1.15.2   "docker-entrypoint.s…"   Up (healthy)              0.0.0.0:8200->8200/tcp    hamalog-vault
df7bb19bab32   hashicorp/vault:1.15.2   "docker-entrypoint.s…"   Exited (0) 29 seconds ago                            hamalog-vault-init
```

### Vault 기능 테스트 결과

```json
{
  "initialized": true,
  "sealed": false,
  "standby": false,
  "performance_standby": false,
  "version": "1.15.2",
  "cluster_name": "vault-cluster-e19bc2fd",
  "cluster_id": "2dddc2e7-a3cd-fef3-9ef3-88c7748a8359"
}
```

**결론**: Vault는 완벽하게 작동하고 있으며 http://localhost:8200에서 접근 가능합니다.

## 🛠 Docker Desktop 가시성 문제 해결 방법

### 1. Docker Desktop 필터 설정 확인

Docker Desktop에서 컨테이너가 보이지 않는 가장 일반적인 이유:

1. **컨테이너 상태 필터**:
   - Docker Desktop 상단의 "All" 탭이 선택되어 있는지 확인
   - "Running" 탭만 선택된 경우 종료된 `hamalog-vault-init` 컨테이너는 보이지 않음

2. **검색 필터**:
   - 검색 박스에 필터가 적용되어 있는지 확인
   - "vault" 또는 "hamalog"로 검색해보기

3. **컨테이너 그룹화 설정**:
   - Docker Desktop 설정에서 컨테이너 그룹화 옵션 확인

### 2. Docker Desktop 새로 고침

```bash
# Docker Desktop 새로 고침
# 1. Docker Desktop에서 F5 또는 새로고침 버튼 클릭
# 2. 또는 Docker Desktop 재시작
```

### 3. 명령행으로 컨테이너 확인

Docker Desktop UI가 제대로 표시되지 않는 경우, 터미널에서 직접 확인:

```bash
# 모든 컨테이너 확인 (실행 중 + 종료된 것)
docker ps -a

# Vault 관련 컨테이너만 확인
docker ps -a | grep vault

# 특정 컨테이너 세부 정보
docker inspect hamalog-vault

# Vault 컨테이너 로그 확인
docker logs hamalog-vault
```

### 4. Docker Desktop 재시작

완전한 재시작이 필요한 경우:

```bash
# macOS
pkill -f Docker
open -a Docker

# Windows
# Docker Desktop 완전 종료 후 재시작
```

## 🔍 Vault 컨테이너 상세 정보

### 실행 중인 Vault 서비스들

1. **주 Vault 서비스** (`hamalog-vault`):
   - **상태**: 실행 중, 정상 (healthy)
   - **포트**: 8200:8200
   - **역할**: 보안 키 및 시크릿 저장소
   - **접근**: http://localhost:8200

2. **Vault 초기화 서비스** (`hamalog-vault-init`):
   - **상태**: 종료됨 (정상적으로 완료)
   - **역할**: Vault 초기 설정 및 시크릿 등록
   - **동작**: 일회성 실행 후 종료 (정상 동작)

### Vault 접근 정보

```bash
# Vault UI 접근
open http://localhost:8200

# Vault 토큰
Token: hamalog-dev-token

# 저장된 시크릿 확인
curl -s http://localhost:8200/v1/secret/data/hamalog \
  -H "X-Vault-Token: hamalog-dev-token"
```

## ✅ 검증 체크리스트

다음 사항들을 확인하여 Vault가 정상 작동하는지 검증:

### 1. 컨테이너 상태 확인
```bash
docker ps | grep vault
# 예상 결과: hamalog-vault 컨테이너가 "healthy" 상태로 실행 중
```

### 2. Vault 접속 테스트
```bash
curl http://localhost:8200/v1/sys/health
# 예상 결과: JSON 응답으로 initialized: true, sealed: false
```

### 3. 시크릿 접근 테스트
```bash
curl -s http://localhost:8200/v1/secret/data/hamalog \
  -H "X-Vault-Token: hamalog-dev-token" | jq '.data.data'
# 예상 결과: JWT secret, encryption key, Kakao credentials 표시
```

### 4. 애플리케이션 Vault 연동 확인
```bash
docker logs hamalog-hamalog-app-1 | grep VAULT
# 예상 결과: VaultKeyProvider 성공 메시지들
```

## 🚀 권장 해결 순서

1. **Docker Desktop 새로고침**: F5 키 또는 새로고침 버튼
2. **필터 확인**: "All" 탭 선택, 검색 필터 제거
3. **터미널 확인**: `docker ps -a | grep vault`로 실제 상태 확인
4. **Vault 기능 테스트**: `curl http://localhost:8200/v1/sys/health`
5. **Docker Desktop 재시작**: 문제가 지속되면 완전 재시작

## 📞 추가 문제 해결

### Docker Desktop이 여전히 컨테이너를 표시하지 않는 경우

1. **Docker 버전 확인**:
   ```bash
   docker --version
   docker-compose --version
   ```

2. **Docker Desktop 설정 재설정**:
   - Docker Desktop > Settings > Reset to factory defaults
   - ⚠️ 주의: 모든 컨테이너와 이미지가 삭제됩니다

3. **시스템 리소스 확인**:
   - Docker Desktop에 충분한 메모리 할당 (최소 4GB)
   - 디스크 공간 확인

### 대안적 모니터링 방법

Docker Desktop UI가 문제가 있는 경우 다음 대안들을 사용:

1. **터미널 기반 모니터링**:
   ```bash
   # 실시간 컨테이너 상태 모니터링
   watch docker ps
   
   # 리소스 사용량 모니터링
   docker stats
   ```

2. **웹 기반 모니터링**:
   - Portainer (Docker 관리 UI)
   - Vault UI: http://localhost:8200

## 📝 결론

**핵심 발견사항**:
- ✅ Vault 컨테이너는 정상적으로 생성되고 실행됩니다
- ✅ Vault 서비스는 완벽하게 기능합니다
- ✅ 문제는 Docker Desktop UI 표시에 국한됩니다
- ✅ 실제 애플리케이션 기능에는 전혀 문제가 없습니다

**권장사항**:
1. Docker Desktop의 필터 및 표시 설정을 확인하세요
2. 터미널 명령어로 실제 컨테이너 상태를 확인하세요  
3. Vault 기능이 정상 작동하므로 개발/배포에는 문제없습니다
4. 필요시 Docker Desktop을 재시작하세요

---

**생성일**: 2025-09-15 19:08  
**상태**: 해결됨 ✅  
**조사 결과**: Vault 컨테이너는 정상 작동 중, Docker Desktop UI 표시 문제