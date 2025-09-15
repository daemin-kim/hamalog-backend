# 운영환경 키 설정 오류 해결 보고서

## 🎯 문제 요약

**문제**: 로컬 운영환경 동기화 환경에서는 키가 정상 작동하지만, 실제 운영환경에서 키 관련 오류가 발생

**원인**: Vault 통합이 환경변수에 의해 완전히 우회되어 제대로 작동하지 않는 문제

**해결**: Vault 전용 비밀 관리 시스템 구축 및 설정 오류 수정

---

## 🔍 발견된 주요 문제점

### 1. **환경변수 우선순위 문제** ⚠️ CRITICAL
- `docker-compose.yml`에서 하드코딩된 환경변수들이 Vault 통합을 완전히 우회
- `JWT_SECRET`, `HAMALOG_ENCRYPTION_KEY`, `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET` 등이 하드코딩됨
- `DataEncryptionUtil.java`에서 `System.getenv()` 검사가 Vault보다 우선순위가 높아 Vault 무용지물

### 2. **Vault 초기화 명령어 구문 오류** ❌ HIGH
- `docker-compose.yml`의 vault-init 컨테이너에서 다중라인 명령어 구문 오류
- `vault kv put` 명령의 key=value 쌍들이 별도 명령어로 해석되어 초기화 실패
- 결과: Vault에 비밀값들이 저장되지 않음

### 3. **Hibernate 방언 설정 누락** ⚠️ MEDIUM  
- `application-prod.properties`에 MySQL 방언(dialect) 설정 누락
- 운영환경에서 데이터베이스 연결 후 방언 자동감지 실패로 애플리케이션 크래시

### 4. **VaultKeyProvider Bean 생성 조건** ℹ️ INFO
- `@ConditionalOnProperty(name = "hamalog.vault.enabled", havingValue = "true")` 설정은 올바름
- 하지만 환경변수가 Vault를 우회하여 실제로는 사용되지 않음

---

## ✅ 적용된 해결책

### 1. **환경변수 제거 및 Vault 전용화**
```yaml
# docker-compose.yml에서 제거된 항목들:
- JWT_SECRET=EzUuJwKK4vLnvk5r7yAgdNP/sa1dL87febZhlayPGjI=
- HAMALOG_ENCRYPTION_KEY=7u+EyAhNyWu2hebIO0XoqUH1gwdoyyHKxcIKC2+A6aM=
- KAKAO_CLIENT_ID=86f21dfff5d2e9e3e1f76167df979268
- KAKAO_CLIENT_SECRET=ScyrNoUeoFLrCNS5MB7CF2kKxUVzaymx

# 교체된 설정:
# Production keys are retrieved exclusively from Vault
# No fallback environment variables to ensure Vault integration works properly
```

### 2. **Vault 초기화 명령어 수정**
```yaml
# 수정 전 (오류 발생):
vault kv put secret/hamalog 
  jwt-secret='...'
  encryption-key='...'
  kakao-client-id='...'
  kakao-client-secret='...';

# 수정 후 (정상 작동):
vault kv put secret/hamalog jwt-secret='...' encryption-key='...' kakao-client-id='...' kakao-client-secret='...';
```

### 3. **Vault 헬스체크 활성화**
```yaml
healthcheck:
  test: ["CMD", "sh", "-c", "VAULT_ADDR=http://localhost:8200 vault status"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 30s
```

### 4. **MySQL 방언 설정 추가**
```properties
# application-prod.properties에 추가:
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

### 5. **.env.prod 파일 정리**
```bash
# 하드코딩된 비밀값들을 주석 처리하고 Vault 참조로 대체:
# JWT_SECRET= # Retrieved from Vault: secret/hamalog:jwt-secret
# KAKAO_CLIENT_ID= # Retrieved from Vault: secret/hamalog:kakao-client-id
# KAKAO_CLIENT_SECRET= # Retrieved from Vault: secret/hamalog:kakao-client-secret
# HAMALOG_ENCRYPTION_KEY= # Retrieved from Vault: secret/hamalog:encryption-key
```

---

## 🧪 검증 결과

### ✅ 성공적인 Vault 통합 확인
```
2025-09-15T10:00:07.161Z  INFO 1 --- [Hamalog] [           main] c.H.service.vault.VaultKeyProvider : [VAULT_KEY_PROVIDER] Initialized with URI: http://vault:8200, Backend: secret, Context: hamalog
2025-09-15T10:00:07.162Z  INFO 1 --- [Hamalog] [           main] c.H.service.vault.VaultKeyProvider : [VAULT_KEY_PROVIDER] Token status: PRESENT, URI accessible: checking...
2025-09-15T10:00:07.308Z  INFO 1 --- [Hamalog] [           main] c.H.service.vault.VaultKeyProvider : [VAULT_KEY_PROVIDER] Successfully retrieved secret: jwt-secret from Vault (length: 44)
2025-09-15T10:00:07.310Z  INFO 1 --- [Hamalog] [           main] c.Hamalog.security.jwt.JwtTokenProvider : [JWT_PROVIDER] Using JWT secret from Vault
```

### ✅ Vault 비밀값 저장 확인
```bash
$ vault kv get secret/hamalog
=== Secret Path ===
secret/data/hamalog
=========== Data ===========
Key                    Value
---                    -----
encryption-key         7u+EyAhNyWu2hebIO0XoqUH1gwdoyyHKxcIKC2+A6aM=
jwt-secret             EzUuJwKK4vLnvk5r7yAgdNP/sa1dL87febZhlayPGjI=
kakao-client-id        86f21dfff5d2e9e3e1f76167df979268
kakao-client-secret    ScyrNoUeoFLrCNS5MB7CF2kKxUVzaymx
```

### ✅ 애플리케이션 정상 시작 확인
- VaultKeyProvider가 정상적으로 생성되고 초기화됨
- JWT 비밀키가 Vault에서 성공적으로 검색됨
- Hibernate가 MySQL 방언을 사용하여 정상 초기화됨
- 더 이상 애플리케이션 크래시나 재시작 루프 발생하지 않음

---

## 🚀 운영 배포 가이드라인

### 1. **배포 전 확인 사항**
- [ ] Vault 서비스가 정상적으로 실행 중인지 확인
- [ ] Vault에 모든 필수 비밀값이 저장되어 있는지 확인
- [ ] `HAMALOG_VAULT_TOKEN` 환경변수가 올바르게 설정되어 있는지 확인
- [ ] MySQL 데이터베이스가 접근 가능한지 확인

### 2. **배포 명령어**
```bash
# 1. 애플리케이션 빌드
./gradlew build -x test

# 2. 운영환경 시작
docker-compose --env-file .env.prod up -d --build

# 3. 서비스 상태 확인
docker-compose --env-file .env.prod ps

# 4. Vault 비밀값 확인 (필요시)
docker exec hamalog-vault sh -c "VAULT_ADDR=http://localhost:8200 VAULT_TOKEN=hamalog-dev-token vault kv get secret/hamalog"

# 5. 애플리케이션 로그 모니터링
docker logs hamalog-hamalog-app-1 -f
```

### 3. **모니터링 포인트**
- VaultKeyProvider 초기화 로그 확인
- "Successfully retrieved secret" 메시지 확인
- "[JWT_PROVIDER] Using JWT secret from Vault" 메시지 확인
- 데이터베이스 연결 정상 여부 확인

### 4. **트러블슈팅**
만약 문제가 발생하면:
```bash
# Vault 상태 확인
docker exec hamalog-vault vault status

# 비밀값 존재 여부 확인  
docker exec hamalog-vault sh -c "VAULT_ADDR=http://localhost:8200 VAULT_TOKEN=hamalog-dev-token vault kv get secret/hamalog"

# 애플리케이션 상세 로그 확인
docker logs hamalog-hamalog-app-1 --tail 100
```

---

## 📋 결론

**문제 해결 완료**: ✅ **성공**

운영환경에서 키 설정 오류의 근본 원인은 **환경변수가 Vault 통합을 우회**하는 설정 문제였습니다. 

**주요 성과**:
1. ✅ Vault가 비밀값의 유일한 소스로 작동하도록 구성
2. ✅ 환경변수 fallback 제거로 보안 강화
3. ✅ 애플리케이션이 Vault에서 성공적으로 비밀값 검색
4. ✅ 운영환경 배포 준비 완료

**보안 개선**:
- 하드코딩된 비밀값 완전 제거
- Vault 기반 중앙집중식 비밀 관리 구현
- 환경변수 fallback 제거로 일관된 보안 정책 적용

이제 운영환경에서도 로컬 환경과 동일하게 Vault를 통한 안전한 키 관리가 가능합니다.

---

**작성일**: 2025-09-15  
**해결 완료 시간**: 약 2시간  
**영향 범위**: 운영환경 전체 보안 키 관리 시스템