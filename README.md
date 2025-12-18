# Hamalog

Hamalog는 복약 일정, 복약 기록, 부작용, 마음 일기를 한 번에 관리하는 헬스케어 백엔드입니다. 모든 API는 JWT 인증과 리소스 소유권 검증을 기반으로 안전하게 보호됩니다.

## 주요 기능
- **인증/인가**: 이메일 기반 회원 가입/로그인, Refresh Token 로테이션, JWT + CSRF 더블 가드.
- **복약 스케줄·기록**: multipart 이미지 업로드 지원, 페이지네이션, 낙관적 락, 상세 이벤트 로깅.
- **부작용 기록**: Redis 캐시를 활용한 최근 목록 조회, Batch 저장/검증 로직.
- **마음 일기 API**: 하루 1회 작성 제한, 템플릿/자유 서식, @RequireResourceOwnership로 본인만 접근.
- **보안**: AOP 기반 소유권 검증, Rate Limiting, Request Size 모니터링, CSRF + JWT 이중 검증.

## 빠른 시작
```bash
./gradlew build
./gradlew bootRun
```

기본 서버는 `http://localhost:8080` 에서 실행됩니다.

## 테스트
```bash
./gradlew test
```

## 모니터링/메트릭(Prometheus)
- Actuator Prometheus 엔드포인트가 기본 프로파일에서 활성화되어 있습니다.
- 엔드포인트: `http://localhost:8080/actuator/prometheus`
- Gradle 의존성: `io.micrometer:micrometer-registry-prometheus`
- Docker/Infra에서 Prometheus가 해당 엔드포인트를 스크레이프하도록 설정하세요.

## 레이트 리밋 설정
환경변수로 임계값을 손쉽게 조정할 수 있습니다(기본값 괄호).
```
RATE_LIMIT_AUTH_PER_MINUTE=5
RATE_LIMIT_AUTH_PER_HOUR=20
RATE_LIMIT_API_PER_MINUTE=60
RATE_LIMIT_API_PER_HOUR=1000
RATE_LIMIT_DEGRADE_SECONDS=300
RATE_LIMIT_METRICS_ENABLED=true
```

## Vault 연동(옵션)
- 기본값으로 비활성화되어 있습니다.
- 운영에서 활성화하려면 다음 환경변수를 설정하세요.
```
SPRING_CLOUD_VAULT_ENABLED=true
# 예시: 접속 정보
SPRING_CLOUD_VAULT_HOST=vault
SPRING_CLOUD_VAULT_PORT=8200
SPRING_CLOUD_VAULT_SCHEME=http
SPRING_CLOUD_VAULT_TOKEN=your-token
```
- 테스트/개발 프로파일에서는 명시적으로 비활성화되어 있습니다.

## 문서
| 문서 | 설명 |
|------|------|
| [API 명세서](./docs/API-specification.md) | REST API 엔드포인트 상세 명세 |
| [API 참고 문서](./docs/API-reference.md) | 인프라, 스키마, 변경 이력 |
| [프로젝트 구조](./docs/Project-Structure.md) | 디렉토리 구조, 아키텍처, 배포 구성 |
| [개선 보고서](./docs/IMPROVEMENT-REPORT.md) | 리팩토링 및 개선 사항 |

## 기술 스택 & 인프라
- Java 21, Spring Boot 3, JPA/Hibernate
- MySQL (테스트에서는 H2), Redis (CSRF/캐시 용도)
- Gradle, JUnit5, Mockito
- Docker/Docker Compose(선택)
- JWT, OAuth2(Kakao), AOP 기반 보안/로깅 모듈

## 환경 변수 예시
`.env.example`을 참고해 JWT 시크릿, DB/Redis 접속정보 등을 설정하세요.

## 로그·모니터링
- `logs/` 폴더에 애플리케이션, 보안, 성능 로깅이 분리 저장됩니다.
- Aspect 로 성능, 감사, 보안 이벤트를 추적합니다.

## 라이선스
사내 프로젝트용으로 별도 라이선스는 제공되지 않습니다.
