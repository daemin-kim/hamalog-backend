# JWT_SECRET 배포 환경 이슈 분석 보고서

## 📋 문제 개요

**발생 오류 메시지:**
```
JWT 비밀키가 설정되지 않았습니다. 프로덕션 환경에서는 JWT_SECRET 환경변수를 반드시 설정해야 합니다.
```

**발생 환경:** 프로덕션 컨테이너 배포 환경
**증상:** JWT_SECRET 환경변수가 빈 값으로 전달되어 애플리케이션 시작 실패

---

## 🔍 근본 원인 분석

### 1. 코드 레벨 분석

**위치:** `src/main/java/com/Hamalog/security/jwt/JwtTokenProvider.java`

```java
@PostConstruct
protected void init() {
    // Check if running in production profile
    boolean isProduction = environment.getActiveProfiles().length > 0 && 
                          java.util.Arrays.asList(environment.getActiveProfiles()).contains("prod");
    
    if (secret == null || secret.isBlank()) {  // ← 이 조건이 true가 됨
        if (isProduction) {
            throw new IllegalStateException("JWT 비밀키가 설정되지 않았습니다. 프로덕션 환경에서는 JWT_SECRET 환경변수를 반드시 설정해야 합니다.");
        }
        // ...
    }
}
```

**분석:** 
- `secret.isBlank()`가 true를 반환한다는 것은 JWT_SECRET이 빈 문자열("")로 설정되었음을 의미
- `secret == null`이 아니므로 환경변수는 설정되어 있지만 값이 비어있음

### 2. 설정 주입 분석

**위치:** `src/main/java/com/Hamalog/security/jwt/JwtTokenProvider.java`

```java
public JwtTokenProvider(
        @Value("${jwt.secret:}") String secret,  // ← 기본값이 빈 문자열
        @Value("${jwt.expiry:3600000}") long validityInMilliseconds,
        // ...
) {
```

**분석:**
- Spring의 `@Value("${jwt.secret:}")` 어노테이션에서 기본값이 빈 문자열로 설정됨
- 이는 JWT_SECRET 환경변수가 설정되지 않았을 때 빈 문자열을 주입함

### 3. Docker Compose 설정 분석

**위치:** `docker-compose.yml` (라인 21)
```yaml
environment:
  - JWT_SECRET=${JWT_SECRET:-EzUuJwKK4vLnvk5r7yAgdNP/sa1dL87febZhlayPGjI=}
```

**위치:** `deploy.sh` (라인 58)
```bash
- JWT_SECRET=\${JWT_SECRET:-EzUuJwKK4vLnvk5r7yAgdNP/sa1dL87febZhlayPGjI=}
```

**이론적 분석:**
- Shell 매개변수 확장 `${JWT_SECRET:-default}`는 변수가 unset이거나 null일 때만 기본값 사용
- 하지만 변수가 빈 문자열("")로 설정된 경우에는 빈 문자열을 그대로 사용

---

## 🧪 재현 테스트 결과

**테스트 스크립트:** `jwt-deployment-issue-reproduction.sh`

### Shell 매개변수 확장 테스트
```bash
# 시나리오 A: JWT_SECRET 미설정
unset JWT_SECRET
JWT_SECRET_RESOLVED="${JWT_SECRET:-EzUuJwKK4vLnvk5r7yAgdNP/sa1dL87febZhlayPGjI=}"
# 결과: 'EzUuJwKK4vLnvk5r7yAgdNP/sa1dL87febZhlayPGjI=' (기본값 사용됨)

# 시나리오 B: JWT_SECRET이 빈 문자열
export JWT_SECRET=""
JWT_SECRET_RESOLVED="${JWT_SECRET:-EzUuJwKK4vLnvk5r7yAgdNP/sa1dL87febZhlayPGjI=}"
# 결과: 'EzUuJwKK4vLnvk5r7yAgdNP/sa1dL87febZhlayPGjI=' (기본값 사용됨)
```

**⚠️ 중요 발견:** 
재현 테스트에서 shell 매개변수 확장은 정상적으로 작동했습니다. 이는 문제가 다른 곳에 있을 수 있음을 시사합니다.

---

## 🔄 추가 가능한 원인들

### 1. CI/CD 파이프라인 이슈
- GitHub Actions 또는 다른 CI/CD 도구에서 JWT_SECRET을 빈 문자열로 설정
- 환경변수 파일(.env)에서 `JWT_SECRET=` (값 없음)으로 설정

### 2. 컨테이너 런타임 이슈
- Docker 실행 시 `-e JWT_SECRET=""` 명시적으로 빈 값 전달
- Kubernetes ConfigMap/Secret에서 빈 값 설정

### 3. Spring 설정 우선순위 이슈
- `application-prod.properties`에서 `jwt.secret=${JWT_SECRET}`로 설정 (기본값 없음)
- 환경변수 JWT_SECRET이 빈 문자열로 설정되어 있을 때 이것이 그대로 주입됨

### 4. 환경변수 파일(.env) 이슈
```bash
# .env 파일에 다음과 같은 설정이 있는 경우
JWT_SECRET=
# 또는
JWT_SECRET=""
```

---

## 🔧 근본 해결 방안

### 1. 애플리케이션 레벨 개선

**파일:** `src/main/java/com/Hamalog/security/jwt/JwtTokenProvider.java`

```java
public JwtTokenProvider(
        @Value("${jwt.secret:#{null}}") String secret,  // ← null을 기본값으로 설정
        // ...
) {
    this.secret = secret;
    // ...
}

@PostConstruct
protected void init() {
    boolean isProduction = environment.getActiveProfiles().length > 0 && 
                          java.util.Arrays.asList(environment.getActiveProfiles()).contains("prod");
    
    if (secret == null || secret.trim().isEmpty()) {  // ← trim() 추가로 공백 처리
        if (isProduction) {
            throw new IllegalStateException(
                "JWT 비밀키가 설정되지 않았습니다. 프로덕션 환경에서는 JWT_SECRET 환경변수를 반드시 설정해야 합니다.\n" +
                "현재 JWT_SECRET 값: [" + (secret == null ? "null" : "'" + secret + "'") + "]\n" +
                "해결 방법: JWT_SECRET 환경변수를 Base64 인코딩된 256비트 키로 설정하세요."
            );
        }
        // ...
    }
}
```

### 2. Docker Compose 설정 개선

**현재 문제 설정:**
```yaml
environment:
  - JWT_SECRET=${JWT_SECRET:-EzUuJwKK4vLnvk5r7yAgdNP/sa1dL87febZhlayPGjI=}
```

**개선된 설정:**
```yaml
environment:
  # JWT_SECRET이 설정되지 않았거나 비어있으면 환경변수를 아예 설정하지 않음
  - ${JWT_SECRET:+JWT_SECRET=${JWT_SECRET}}
```

또는 더 명확한 방법:
```yaml
environment:
  - SPRING_PROFILES_ACTIVE=prod
  # JWT_SECRET은 명시적으로 설정된 경우에만 전달
```

### 3. 배포 스크립트 개선

**파일:** `deploy.sh`

```bash
# JWT_SECRET 유효성 검사 추가
validate_environment() {
    echo "🔍 환경변수 유효성 검사 중..."
    
    if [ -z "${JWT_SECRET:-}" ]; then
        echo "⚠️  JWT_SECRET이 설정되지 않았습니다."
        echo "프로덕션 배포를 위해서는 JWT_SECRET 환경변수가 필요합니다."
        echo "예시: export JWT_SECRET=\$(openssl rand -base64 32)"
        exit 1
    elif [ "${#JWT_SECRET}" -lt 32 ]; then
        echo "❌ JWT_SECRET이 너무 짧습니다. 최소 32자 이상의 Base64 키가 필요합니다."
        echo "현재 길이: ${#JWT_SECRET}자"
        exit 1
    else
        echo "✅ JWT_SECRET 검증 완료"
    fi
}

# 배포 전에 유효성 검사 실행
validate_environment
```

### 4. 환경변수 진단 도구 추가

**파일:** `environment-diagnostic.sh` (신규 생성)

```bash
#!/bin/bash

echo "🔍 Hamalog 환경변수 진단 도구"
echo "================================="

echo "환경 정보:"
echo "- 현재 사용자: $(whoami)"
echo "- 현재 디렉토리: $(pwd)"
echo "- 셸: $SHELL"

echo ""
echo "JWT_SECRET 상태 진단:"
if [ -z "${JWT_SECRET:-}" ]; then
    echo "❌ JWT_SECRET: 설정되지 않음 (unset)"
elif [ -z "$JWT_SECRET" ]; then
    echo "⚠️  JWT_SECRET: 빈 문자열로 설정됨"
    echo "값: '$JWT_SECRET' (길이: ${#JWT_SECRET})"
else
    echo "✅ JWT_SECRET: 설정됨"
    echo "길이: ${#JWT_SECRET}자"
    echo "첫 10자: ${JWT_SECRET:0:10}..."
fi

echo ""
echo "Spring Profile 설정:"
echo "SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-'설정되지 않음'}"

echo ""
echo "권장사항:"
if [ -z "${JWT_SECRET:-}" ] || [ -z "$JWT_SECRET" ]; then
    echo "1. JWT_SECRET 환경변수 설정:"
    echo "   export JWT_SECRET=\$(openssl rand -base64 32)"
    echo "2. 설정 확인:"
    echo "   echo \$JWT_SECRET"
fi
```

---

## 🧪 테스트 시나리오

### 1. 환경변수 설정 테스트
```bash
# 시나리오 1: JWT_SECRET 미설정
unset JWT_SECRET
./gradlew bootRun

# 시나리오 2: JWT_SECRET 빈 문자열
export JWT_SECRET=""
SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun

# 시나리오 3: JWT_SECRET 유효한 값
export JWT_SECRET=$(openssl rand -base64 32)
SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun
```

### 2. Docker 환경 테스트
```bash
# 시나리오 1: 환경변수 없이 실행
docker run --rm -e SPRING_PROFILES_ACTIVE=prod hamalog:latest

# 시나리오 2: 빈 JWT_SECRET으로 실행
docker run --rm -e SPRING_PROFILES_ACTIVE=prod -e JWT_SECRET="" hamalog:latest

# 시나리오 3: 유효한 JWT_SECRET으로 실행
docker run --rm -e SPRING_PROFILES_ACTIVE=prod -e JWT_SECRET=$(openssl rand -base64 32) hamalog:latest
```

---

## 📝 임시 해결 방안

### 1. 긴급 대응 (Hot Fix)
배포 전에 다음 명령어로 JWT_SECRET 확인:
```bash
if [ -z "${JWT_SECRET}" ] || [ "${JWT_SECRET}" = "" ]; then
    export JWT_SECRET="EzUuJwKK4vLnvk5r7yAgdNP/sa1dL87febZhlayPGjI="
    echo "임시 JWT_SECRET 설정됨. 보안상 실제 키로 교체 권장."
fi
```

### 2. 모니터링 강화
애플리케이션 시작 시 환경변수 상태 로깅:
```java
@PostConstruct
protected void init() {
    log.info("JWT 초기화 중...");
    log.info("프로필: {}", Arrays.toString(environment.getActiveProfiles()));
    log.info("JWT_SECRET 설정 여부: {}", secret != null ? "설정됨" : "설정되지 않음");
    log.info("JWT_SECRET 길이: {}", secret != null ? secret.length() : 0);
    
    // 기존 로직...
}
```

---

## 🎯 권장사항

### 즉시 조치 사항
1. **환경변수 진단 스크립트 실행**으로 현재 상태 확인
2. **배포 스크립트에 유효성 검사 추가**
3. **에러 메시지 개선**으로 디버깅 정보 제공

### 중장기 개선사항
1. **환경변수 관리 시스템** 도입 (HashiCorp Vault, AWS Secrets Manager 등)
2. **설정 검증 자동화** CI/CD 파이프라인 통합
3. **보안 키 로테이션** 프로세스 구축

---

## 📅 작성 정보
- **작성일:** 2025-09-09
- **작성자:** Junie (Claude Sonnet 4)
- **버전:** 1.0
- **관련 이슈:** JWT_SECRET 배포 환경 빈 값 문제