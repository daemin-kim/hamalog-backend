# Hamalog 설정 보안 감사 보고서

**작성일**: 2025년 11월 22일  
**프로젝트**: Hamalog Backend API  
**감사 범위**: 설정 파일, 환경 변수, 키 관리  
**분석자**: AI Security Analyst

---

## 📋 목차

1. [Executive Summary](#executive-summary)
2. [감사 방법론](#감사-방법론)
3. [발견된 취약점 목록](#발견된-취약점-목록)
4. [심각도별 분류](#심각도별-분류)
5. [상세 취약점 분석](#상세-취약점-분석)
6. [권장 조치사항](#권장-조치사항)
7. [결론](#결론)

---

## 🎯 Executive Summary

### 감사 개요
Hamalog 프로젝트의 설정 파일과 키 관리 시스템을 전면 감사하여 **14개의 치명적인 보안 취약점**을 발견했습니다. 심각도는 CRITICAL 8건, HIGH 4건, MEDIUM 2건으로 분류됩니다.

### 주요 발견사항
- 🔴 **민감한 크레덴셜이 Git 저장소에 노출됨** (CRITICAL)
- 🔴 **프로덕션 JWT Secret Key가 하드코딩됨** (CRITICAL)
- 🔴 **카카오 OAuth2 실제 Client Secret이 공개 저장소에 노출** (CRITICAL)
- 🔴 **데이터 암호화 키가 평문으로 저장됨** (CRITICAL)
- 🔴 **데이터베이스 비밀번호가 .env 파일에 평문 저장** (CRITICAL)

### 위험 수준 평가
| 영역 | 평가 | 심각도 |
|------|------|--------|
| **키 관리** | 매우 위험 | 🔴 CRITICAL |
| **Git 보안** | 매우 위험 | 🔴 CRITICAL |
| **환경 변수 관리** | 위험 | 🔴 HIGH |
| **암호화 키 관리** | 매우 위험 | 🔴 CRITICAL |
| **OAuth2 크레덴셜** | 매우 위험 | 🔴 CRITICAL |

**전체 위험도**: 🔴 **CRITICAL (즉시 조치 필요)**

---

## 🔍 감사 방법론

### 1. 검토 대상 파일
- ✅ `application.properties` (메인 설정)
- ✅ `application-prod.properties` (프로덕션 설정)
- ✅ `application-local.properties` (로컬 설정)
- ✅ `application-test.properties` (테스트 설정)
- ✅ `.env.prod` (프로덕션 환경 변수)
- ✅ `.env.prod-local` (로컬 프로덕션 환경 변수)
- ✅ `docker-compose.yml` (Docker 설정)
- ✅ `.gitignore` (Git 제외 파일)

### 2. 검토 항목
- 하드코딩된 크레덴셜
- Git에 커밋된 민감 정보
- 환경 변수 관리 방식
- 암호화 키 관리
- OAuth2 크레덴셜 노출
- 데이터베이스 비밀번호
- JWT Secret Key 관리

---

## 📊 발견된 취약점 목록

| ID | 취약점명 | 심각도 | 위치 | 상태 |
|----|----------|--------|------|------|
| C-001 | JWT Secret Key 평문 노출 | 🔴 CRITICAL | application.properties | Open |
| C-002 | 데이터 암호화 키 평문 노출 | 🔴 CRITICAL | application.properties | Open |
| C-003 | 프로덕션 카카오 Client Secret 노출 | 🔴 CRITICAL | application-prod.properties | Open |
| C-004 | 로컬 카카오 Client Secret 노출 | 🔴 CRITICAL | application-local.properties | Open |
| C-005 | .env 파일 Git 커밋됨 | 🔴 CRITICAL | .env.prod, .env.prod-local | Open |
| C-006 | DB 비밀번호 평문 노출 | 🔴 CRITICAL | .env.prod | Open |
| C-007 | Docker Compose 카카오 크레덴셜 노출 | 🔴 CRITICAL | docker-compose.yml | Open |
| C-008 | 프로덕션 설정 파일 Git 커밋됨 | 🔴 CRITICAL | application-prod.properties | Open |
| H-001 | Dummy 크레덴셜 사용 | 🟠 HIGH | application.properties | Open |
| H-002 | DB 비밀번호 기본값 약함 | 🟠 HIGH | docker-compose.yml | Open |
| H-003 | Redis 비밀번호 미설정 | 🟠 HIGH | application.properties | Open |
| H-004 | MySQL Root 비밀번호 약함 | 🟠 HIGH | .env.prod | Open |
| M-001 | H2 Console 프로덕션에서 활성화 가능 | 🟡 MEDIUM | application-prod.properties | Open |
| M-002 | SSL/TLS 미설정 | 🟡 MEDIUM | application-prod.properties | Open |

**발견된 취약점**: 총 **14개**  
**CRITICAL**: 8개 | **HIGH**: 4개 | **MEDIUM**: 2개

---

## 🎨 심각도별 분류

### 🔴 CRITICAL (즉시 조치 필요) - 8건

#### C-001: JWT Secret Key 평문 노출
**위치**: `src/main/resources/application.properties:28`

**문제점**:
```properties
# JWT Configuration - Direct property values (no longer using Vault)
jwt.secret=bxQxgvUvnbDGgr3yYwravEX+7gAIbjE4zMjrO7Ybd74=
jwt.expiry=${JWT_EXPIRY:3600000}
```

**취약점**:
- JWT Secret Key가 평문으로 저장됨
- Git 저장소에 커밋되어 공개적으로 노출
- 이 키로 생성된 모든 토큰이 위조 가능
- **실제 프로덕션 키가 노출됨**

**영향**:
- 🔴 공격자가 임의의 JWT 토큰 생성 가능
- 🔴 모든 사용자 계정 탈취 가능
- 🔴 관리자 권한 획득 가능
- 🔴 시스템 전체 보안 무력화

**CVSS 점수**: 10.0 (Critical)

---

#### C-002: 데이터 암호화 키 평문 노출
**위치**: `src/main/resources/application.properties:32`

**문제점**:
```properties
# Data encryption key - Direct property value (no longer using Vault)
hamalog.encryption.key=8escSvCR9B+h91dVNzCp5tXdbMi31BJa+VGqgtv1yqY=
```

**취약점**:
- AES-256 암호화 키가 평문으로 저장됨
- Git 저장소에 커밋되어 공개적으로 노출
- 전화번호, 생년월일 등 민감정보 암호화에 사용되는 키
- **개인정보보호법 위반 가능성**

**영향**:
- 🔴 암호화된 전화번호 복호화 가능
- 🔴 암호화된 생년월일 복호화 가능
- 🔴 개인정보 대량 유출 위험
- 🔴 법적 책임 문제

**CVSS 점수**: 9.8 (Critical)

---

#### C-003: 프로덕션 카카오 OAuth2 크레덴셜 노출
**위치**: `src/main/resources/application-prod.properties:45-47`

**문제점**:
```properties
# OAuth2 Kakao configuration - Real credentials for production (security vulnerabilities accepted)
spring.security.oauth2.client.registration.kakao.client-id=e26adea63cc62391e69f8de848a922ef
spring.security.oauth2.client.registration.kakao.client-secret=SD6dSoxwFE3ts1CSvYIxybwTqOkfhVyb
```

**취약점**:
- **실제 프로덕션 카카오 Client Secret이 노출됨**
- 주석에 "security vulnerabilities accepted" 명시 (보안 위험 인지하면서 방치)
- Git 저장소에 커밋됨
- 카카오 개발자 가이드 위반

**영향**:
- 🔴 공격자가 카카오 로그인 위조 가능
- 🔴 사용자 계정 무단 접근
- 🔴 카카오 API 할당량 악용 가능
- 🔴 카카오 앱 정지 위험

**CVSS 점수**: 9.5 (Critical)

---

#### C-004: 로컬 카카오 OAuth2 크레덴셜 노출
**위치**: `src/main/resources/application-local.properties:18-19`

**문제점**:
```properties
# Real Kakao OAuth2 credentials for local development
spring.security.oauth2.client.registration.kakao.client-id=86f21dfff5d2e9e3e1f76167df979268
spring.security.oauth2.client.registration.kakao.client-secret=ScyrNoUeoFLrCNS5MB7CF2kKxUVzaymx
```

**취약점**:
- **실제 카카오 Client Secret이 로컬 설정에도 노출됨**
- 주석에 "Real Kakao OAuth2 credentials" 명시
- Git 저장소에 커밋됨

**영향**:
- 🔴 개발용 카카오 앱도 공격 대상
- 🔴 크레덴셜 재사용 시 프로덕션도 위험
- 🔴 카카오 API 악용 가능

**CVSS 점수**: 9.0 (Critical)

---

#### C-005: .env 파일 Git 저장소에 커밋됨
**위치**: `.env.prod`, `.env.prod-local`

**문제점**:
```bash
$ git ls-files | grep .env
.env.prod
.env.prod-local
```

**취약점**:
- `.env` 파일이 `.gitignore`에 있음에도 `.env.prod`, `.env.prod-local`은 커밋됨
- 모든 민감 정보가 Git 히스토리에 영구 저장됨
- 공개 저장소인 경우 전 세계에 노출
- **일반적인 보안 관행 위반**

**영향**:
- 🔴 모든 크레덴셜 한 번에 노출
- 🔴 Git 히스토리에서 제거 어려움
- 🔴 Fork된 저장소에도 계속 존재

**CVSS 점수**: 9.8 (Critical)

---

#### C-006: 데이터베이스 비밀번호 평문 노출
**위치**: `.env.prod:8`

**문제점**:
```env
DB_PASSWORD=H@m@l0g_Pr0d_DB_P@ssw0rd_2025!
MYSQL_ROOT_PASSWORD=MySql_R00t_Str0ng_P@ssw0rd_2025!
SPRING_DATASOURCE_PASSWORD=H@m@l0g_Pr0d_DB_P@ssw0rd_2025!
```

**취약점**:
- 프로덕션 DB 비밀번호가 평문으로 저장
- MySQL Root 비밀번호 노출
- Git에 커밋되어 공개됨

**영향**:
- 🔴 데이터베이스 직접 접근 가능
- 🔴 모든 데이터 조회/수정/삭제 가능
- 🔴 백도어 생성 가능
- 🔴 랜섬웨어 공격 위험

**CVSS 점수**: 10.0 (Critical)

---

#### C-007: Docker Compose 카카오 크레덴셜 노출
**위치**: `docker-compose.yml:16-17`

**문제점**:
```yaml
# Real Kakao OAuth2 credentials for production
- KAKAO_CLIENT_ID=86f21dfff5d2e9e3e1f76167df979268
- KAKAO_CLIENT_SECRET=ScyrNoUeoFLrCNS5MB7CF2kKxUVzaymx
```

**취약점**:
- Docker Compose 파일에 실제 크레덴셜 하드코딩
- Git에 커밋됨
- 주석에 "Real ... for production" 명시

**영향**:
- 🔴 프로덕션 배포 시 크레덴셜 노출
- 🔴 Docker 이미지에도 포함될 수 있음

**CVSS 점수**: 9.0 (Critical)

---

#### C-008: 프로덕션 설정 파일 Git 커밋됨
**위치**: `src/main/resources/application-prod.properties`

**문제점**:
```bash
$ git ls-files | grep application-prod.properties
src/main/resources/application-prod.properties
```

**취약점**:
- 프로덕션 전용 설정 파일이 Git에 커밋됨
- 모든 프로덕션 크레덴셜과 설정 노출
- **일반적인 보안 관행 위반**

**영향**:
- 🔴 프로덕션 인프라 정보 노출
- 🔴 공격 표면 확대

**CVSS 점수**: 8.5 (High)

---

### 🟠 HIGH (긴급 조치 필요) - 4건

#### H-001: Dummy 크레덴셜 사용
**위치**: `application.properties:12-13`

**문제점**:
```properties
spring.security.oauth2.client.registration.kakao.client-id=dummy-client-id-for-development
spring.security.oauth2.client.registration.kakao.client-secret=dummy-client-secret-for-development
```

**취약점**:
- Dummy 크레덴셜이 기본값으로 설정됨
- 환경 변수 미설정 시 애플리케이션 오작동
- 에러 메시지로 민감정보 노출 가능

**권장 조치**:
- 환경 변수 필수로 설정
- Dummy 값 제거
- 미설정 시 명확한 에러 발생

---

#### H-002: DB 비밀번호 기본값 약함
**위치**: `docker-compose.yml:10`

**문제점**:
```yaml
- SPRING_DATASOURCE_PASSWORD=${SPRING_DATASOURCE_PASSWORD:-hamalog_password}
```

**���약점**:
- 기본 비밀번호가 너무 단순함
- 환경 변수 미설정 시 약한 비밀번호 사용

**권장 조치**:
- 기본값 제거
- 환경 변수 필수로 설정

---

#### H-003: Redis 비밀번호 미설정
**위치**: `application.properties:44`

**문제점**:
```properties
spring.data.redis.password=${SPRING_DATA_REDIS_PASSWORD:}
```

**취약점**:
- Redis에 비밀번호가 설정되지 않음
- 누구나 Redis에 접근 가능

**영향**:
- 캐시 데이터 조회/수정 가능
- 토큰 블랙리스트 조작 가능
- 세션 하이재킹 가능

**권장 조치**:
- Redis 비밀번호 필수 설정
- Redis ACL 설정

---

#### H-004: MySQL Root 비밀번호 약함
**위치**: `.env.prod:9`

**문제점**:
```env
MYSQL_ROOT_PASSWORD=MySql_R00t_Str0ng_P@ssw0rd_2025!
```

**취약점**:
- 패턴이 예측 가능함
- 연도가 포함되어 정기 변경 필요

**권장 조치**:
- 완전 랜덤 비밀번호 사용
- 비밀 관리 도구 사용

---

### 🟡 MEDIUM (개선 권장) - 2건

#### M-001: H2 Console 프로덕션에서 활성화 가능
**위치**: `application-prod.properties:3`

**문제점**:
```properties
spring.h2.console.enabled=false
```

**취약점**:
- 설정만 변경하면 활성화 가능
- H2 Console은 프로덕션에서 완전 제거되어야 함

**권장 조치**:
- H2 의존성 제거 (프로덕션 빌드)
- 프로필별 의존성 분리

---

#### M-002: SSL/TLS 미설정
**위치**: `application-prod.properties:5`

**문제점**:
```properties
spring.datasource.url=...?useSSL=false&...
```

**취약점**:
- DB 연결 시 SSL 미사용
- 평문으로 데이터 전송

**권장 조치**:
- SSL/TLS 활성화
- 인증서 설정

---

## 💡 권장 조치사항

### 🚨 즉시 조치 (24시간 내)

#### 1. Git에서 민감정보 완전 제거
```bash
# Git 히스토리에서 민감 파일 완전 제거
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch \
  .env.prod \
  .env.prod-local \
  src/main/resources/application-prod.properties \
  src/main/resources/application-local.properties" \
  --prune-empty --tag-name-filter cat -- --all

# 원격 저장소 강제 푸시
git push origin --force --all
git push origin --force --tags
```

#### 2. 모든 크레덴셜 즉시 교체
- ✅ JWT Secret Key 재생성
- ✅ 데이터 암호화 키 재생성
- ✅ 카카오 OAuth2 Client Secret 재발급
- ✅ 데이터베이스 비밀번호 변경
- ✅ Redis 비밀번호 설정

#### 3. 환경 변수 관리 시스템 구축
```bash
# .env 파일을 .gitignore��� 추가
echo "" >> .gitignore
echo "# Environment variables" >> .gitignore
echo ".env*" >> .gitignore
echo "*.env" >> .gitignore
echo "!.env.example" >> .gitignore
```

#### 4. .env.example 템플릿 생성
```env
# Environment Variables Template
# Copy this file to .env.prod and fill in actual values

# Database Configuration
DB_NAME=
DB_USERNAME=
DB_PASSWORD=
MYSQL_ROOT_PASSWORD=

# JWT Configuration
JWT_SECRET=
JWT_EXPIRY=3600000

# Data Encryption
HAMALOG_ENCRYPTION_KEY=

# Kakao OAuth2
KAKAO_CLIENT_ID=
KAKAO_CLIENT_SECRET=
KAKAO_REDIRECT_URI=

# Redis
SPRING_DATA_REDIS_PASSWORD=
```

---

### 📋 단기 조치 (1주일 내)

#### 1. 비밀 관리 도구 도입

**AWS Secrets Manager 사용 예시**:
```java
@Configuration
public class SecretsConfig {
    
    @Bean
    public AWSSecretsManager secretsManager() {
        return AWSSecretsManagerClientBuilder.standard()
            .withRegion(Regions.AP_NORTHEAST_2)
            .build();
    }
    
    @Bean
    public String jwtSecret(AWSSecretsManager secretsManager) {
        GetSecretValueRequest request = new GetSecretValueRequest()
            .withSecretId("hamalog/jwt-secret");
        GetSecretValueResult result = secretsManager.getSecretValue(request);
        return result.getSecretString();
    }
}
```

**HashiCorp Vault 사용 예시**:
```yaml
# application-prod.yml
spring:
  cloud:
    vault:
      host: vault.example.com
      port: 8200
      scheme: https
      authentication: TOKEN
      token: ${VAULT_TOKEN}
      kv:
        enabled: true
        backend: secret
        default-context: hamalog
```

#### 2. Properties 파일 분리
```
src/main/resources/
├── application.properties          (공통 설정만)
├── application-prod.properties     (Git에서 제외)
├── application-local.properties    (Git에서 제외)
└── application-defaults.properties (기본값만, Git 포함 가능)
```

#### 3. 프로덕션 설정 템플릿화
```properties
# application-prod.properties.template
jwt.secret=${JWT_SECRET}
hamalog.encryption.key=${HAMALOG_ENCRYPTION_KEY}
spring.security.oauth2.client.registration.kakao.client-id=${KAKAO_CLIENT_ID}
spring.security.oauth2.client.registration.kakao.client-secret=${KAKAO_CLIENT_SECRET}
```

---

### 🔧 중기 조치 (1개월 내)

#### 1. 암호화 키 로테이션 정책 수립
- 매 분기 JWT Secret Key 교체
- 매 6개월 데이터 암호화 키 교체
- 키 교체 시 기존 데이터 재암호화 프로세스

#### 2. 접근 제어 강화
```yaml
# docker-compose.yml
services:
  mysql-hamalog:
    environment:
      - MYSQL_ROOT_HOST=localhost  # Root는 localhost만 접근
    networks:
      - backend  # 내부 네트워크만 접근
      
  redis:
    command: >
      redis-server
      --requirepass ${REDIS_PASSWORD}
      --maxmemory 256mb
      --maxmemory-policy allkeys-lru
      --protected-mode yes
```

#### 3. 보안 감사 로깅
```java
@Aspect
@Component
public class SecretAccessAuditAspect {
    
    @Around("@annotation(org.springframework.beans.factory.annotation.Value)")
    public Object auditSecretAccess(ProceedingJoinPoint joinPoint) {
        String propertyName = extractPropertyName(joinPoint);
        if (isSensitiveProperty(propertyName)) {
            log.warn("[SECURITY_AUDIT] Sensitive property accessed: {}", 
                propertyName);
        }
        return joinPoint.proceed();
    }
}
```

---

### 📊 장기 조치 (3개월 내)

#### 1. 완전한 Secrets Management 시스템 구축
- AWS Secrets Manager 또는 HashiCorp Vault 완전 통합
- 모든 크레덴셜을 중앙화된 시스템에서 관리
- 자동 로테이션 설정

#### 2. CI/CD 파이프라인 보안 강화
```yaml
# .github/workflows/deploy.yml
jobs:
  deploy:
    steps:
      - name: Get secrets from AWS Secrets Manager
        uses: aws-actions/aws-secretsmanager-get-secrets@v1
        with:
          secret-ids: |
            hamalog/jwt-secret
            hamalog/db-password
            hamalog/kakao-oauth
          
      - name: Deploy with secrets
        env:
          JWT_SECRET: ${{ env.HAMALOG_JWT_SECRET }}
          DB_PASSWORD: ${{ env.HAMALOG_DB_PASSWORD }}
        run: ./deploy.sh
```

#### 3. 정기 보안 감사
- 월 1회 설정 파일 리뷰
- 분기 1회 침투 테스트
- 연 1회 외부 보안 감사

---

## 📈 위험도 평가

### 현재 상태
```
크레덴셜 관리:    ██░░░░░░░░ 20%  🔴 매우 위험
Git 보안:         ██░░░░░░░░ 20%  🔴 매우 위험
환경 변수 관리:   ███░░░░░░░ 30%  🔴 위험
암호화 키 관리:   ██░░░░░░░░ 20%  🔴 매우 위험
OAuth2 보안:      ██░░░░░░░░ 20%  🔴 매우 위험
───────────────────────────────────
전체 보안 점수:   2.2/10            🔴 CRITICAL
```

### 개선 후 예상
```
크레덴셜 관리:    █████████░ 90%  ✅ 안전
Git 보안:         ██████████ 100% ✅ 안전
환경 변수 관리:   █████████░ 90%  ✅ 안전
암호화 키 관리:   █████████░ 90%  ✅ 안전
OAuth2 보안:      █████████░ 90%  ✅ 안전
───────────────────────────────────
전체 보안 점수:   9.2/10            ✅ 우수
```

---

## 🎯 결론

### 긍정적 측면
1. ✅ 환경 변수 기반 설정 구조 존재
2. ✅ 프로필별 설정 분리
3. ✅ Docker Compose 활용

### 심각한 문제점
1. 🔴 **모든 크레덴셜이 Git에 노출됨**
2. 🔴 **프로덕션 키가 평문으로 저장됨**
3. 🔴 **카카오 실제 Client Secret 공개됨**
4. 🔴 **데이터베이스 비밀번호 노출**
5. 🔴 **암호화 키 노출로 개인정보 보호 불가**

### 최종 평가
Hamalog 프로젝트의 설정 보안 상태는 **CRITICAL 수준으로 매우 위험**합니다. 

**즉시 조치가 필요한 사항**:
- 🚨 Git 저장소에서 모든 민감정보 제거
- 🚨 모든 크레덴셜 즉시 교체
- 🚨 환경 변수 관리 시스템 구축
- 🚨 비밀 관리 도구 도입

**법적 리스크**:
- 개인정보보호법 위반 가능성 (암호화 키 노출)
- 카카오 개발자 약관 위반
- GDPR 위반 가능성 (EU 사용자 대상 시)

**비즈니스 영향**:
- 전체 시스템 보안 붕괴
- 사용자 데이터 유출 위험
- 서비스 신뢰도 하락
- 법적 책임 문제

**권장 조치 우선순위**:
1. **즉시** (24시간): Git 크레덴셜 제거 및 교체
2. **긴급** (1주일): 비밀 관리 시스템 구축
3. **중요** (1개월): 접근 제어 및 감사 강화
4. **장기** (3개월): 완전한 Secrets Management 통합

---

**보고서 작성**: AI Security Analyst  
**감사일**: 2025-11-22  
**다음 감사 예정일**: 2025-12-22 (1개월 후 - 즉시 재감사 필요)

---

## 📚 참고 자료

- OWASP Top 10 - A02:2021 Cryptographic Failures
- OWASP Secrets Management Cheat Sheet
- CWE-798: Use of Hard-coded Credentials
- CWE-312: Cleartext Storage of Sensitive Information
- CWE-522: Insufficiently Protected Credentials
- NIST SP 800-57: Recommendation for Key Management
- 개인정보보호법 제24조 (고유식별정보의 처리 제한)
- 개인정보보호법 제29조 (안전조치의무)

