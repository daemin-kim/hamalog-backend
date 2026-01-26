# 기술적 의사결정 및 문제해결 기록 가이드

> **"왜 이렇게 구현했는지"**와 **"어떻게 문제를 해결했는지"**를 체계적으로 기록하는 가이드입니다.
> 
> 이 문서는 포트폴리오에서 **기술적 깊이**를 증명하고, 면접에서 **논리적 사고력**을 보여주는 핵심 자료가 됩니다.

---

## 📋 목차

1. [개요](#1-개요)
2. [Phase 1: ADR - 구현 전/중 의사결정 기록](#2-phase-1-adr---구현-전중-의사결정-기록)
3. [Phase 2: 트러블슈팅 로그 - 문제해결 수사 일지](#3-phase-2-트러블슈팅-로그---문제해결-수사-일지)
4. [Phase 3: 문서 패키징 및 공유](#4-phase-3-문서-패키징-및-공유)
5. [파일 위치 및 네이밍 규칙](#5-파일-위치-및-네이밍-규칙)

---

## 1. 개요

### 1.1 왜 기록해야 하는가?

| 기존 포트폴리오 | 개선된 포트폴리오 |
|----------------|------------------|
| "Redis Stream을 도입했습니다" | "RabbitMQ, Kafka, Redis Stream 중 **왜** Redis Stream을 선택했는지" |
| "N+1 문제를 해결했습니다" | "어떤 **가설**을 세우고, 어떤 것을 **기각**하며 원인을 찾았는지" |
| 결과 중심 | **과정 중심** |

면접관은 **정답을 맞힌 것**보다 **틀린 가설을 어떻게 논리적으로 배제했는지**를 더 높게 평가합니다.

### 1.2 문서 유형

| 유형 | 작성 시점 | 목적 | 저장 위치 |
|------|----------|------|----------|
| **ADR** | 구현 전/중 | 기술 선택의 근거 기록 | `docs/internal/adr/` |
| **트러블슈팅 로그** | 문제 발생 시 | 해결 과정 기록 | `docs/troubleshooting/` |
| **포트폴리오** | 구현 후 | 정제된 결과물 공유 | `docs/portfolio/` |

---

## 2. Phase 1: ADR - 구현 전/중 의사결정 기록

> **ADR (Architecture Decision Record)**: 코드를 짜기 전에 **'왜'**를 남기는 가장 현업스러운 방식

### 2.1 확장된 ADR 템플릿

```markdown
# ADR-{번호}: {제목}

> 예: ADR-0008: 채팅 시스템의 실시간 통신 프로토콜 선정

## 상태
{Draft | Proposed | Accepted | Deprecated | Superseded by ADR-XXX}

## 1. 배경 (Context)

### 현재 상황
{해결해야 할 문제와 현재 상황을 설명합니다}

### 제약 조건
- {제약 조건 1} (예: 개발 기간 3일)
- {제약 조건 2} (예: AWS 프리티어 사용)
- {제약 조건 3} (예: Redis 이미 도입됨)

## 2. 고려한 대안 (Options)

### Option A: {대안명}
- **설명**: {간략한 설명}
- **장점**: 
  - {장점 1}
  - {장점 2}
- **단점**: 
  - {단점 1}
  - {단점 2}
- **적합성**: ⭐⭐⭐☆☆

### Option B: {대안명}
- **설명**: {간략한 설명}
- **장점**: 
  - {장점 1}
- **단점**: 
  - {단점 1}
- **적합성**: ⭐⭐☆☆☆

### Option C: {대안명}
- **설명**: {간략한 설명}
- **장점**: 
  - {장점 1}
- **단점**: 
  - {단점 1}
- **적합성**: ⭐⭐⭐⭐☆

## 3. 결정 (Decision)

### 선택
**Option C: {대안명}**

### 선택 이유
{왜 이 옵션을 선택했는지 구체적으로 설명}

예시:
> 채팅이라기보다 '알림'에 가까운 기능이며, 클라이언트 → 서버 데이터 전송이 적어 
> 굳이 WebSocket의 유지보수 비용을 감당할 필요가 없다고 판단함.

## 4. 트레이드오프 & 후속 조치 (Consequences)

### 얻은 것 (Pros)
- {장점 1}
- {장점 2}

### 잃은 것 (Cons)
- {단점 1}
- {단점 2}

### 후속 조치
- {조치 1} (예: 인터페이스 추상화하여 추후 전환 용이하게)
- {조치 2} (예: 모니터링 대시보드 추가)

## 5. 참고 자료
- {관련 링크 1}
- {관련 문서 2}
```

### 2.2 실제 작성 예시

아래는 Hamalog에서 실제로 작성된 ADR 예시입니다:

```markdown
# ADR-0007: 메시지 큐 도입 - Redis Stream 선택

## 상태
Accepted

## 1. 배경 (Context)

### 현재 상황
Discord 알림 발송 시 외부 API 호출(200~500ms)이 트랜잭션 안에서 동기 처리되어
사용자 응답 지연 및 외부 서비스 장애 시 전체 서비스 영향

### 제약 조건
- 개인 프로젝트로 인프라 비용 최소화 필요
- Redis 7이 이미 캐시/Rate Limiting용으로 도입됨
- 메시지 유실 시 치명적이지 않음 (알림 재발송 가능)

## 2. 고려한 대안 (Options)

### Option A: RabbitMQ
- **장점**: AMQP 표준, 신뢰성 높음, 다양한 라우팅
- **단점**: 별도 인프라 필요, 학습 곡선
- **적합성**: ⭐⭐⭐☆☆

### Option B: Apache Kafka
- **장점**: 대용량 처리, 이벤트 소싱 적합
- **단점**: 오버엔지니어링, 리소스 과다
- **적합성**: ⭐⭐☆☆☆

### Option C: Redis Stream
- **장점**: 이미 사용 중인 Redis 활용, Consumer Group 지원, 경량
- **단점**: 메시지 영속성 RabbitMQ 대비 약함
- **적합성**: ⭐⭐⭐⭐⭐

## 3. 결정 (Decision)

### 선택
**Option C: Redis Stream**

### 선택 이유
1. 이미 Redis 7이 도입되어 추가 인프라 비용 없음
2. Consumer Group으로 다중 Consumer 처리 가능
3. 알림 메시지 특성상 영속성보다 처리량이 중요
4. Spring Data Redis의 StreamListener로 간편 구현

## 4. 트레이드오프 & 후속 조치 (Consequences)

### 얻은 것
- API 응답 시간 200ms → 50ms (75% 개선)
- 외부 서비스 장애 격리
- 인프라 단순화

### 잃은 것
- 메시지 영속성 보장 약함 (Redis 재시작 시 유실 가능)
- RabbitMQ 대비 라우팅 기능 제한적

### 후속 조치
- Redis AOF 영속화 설정 적용
- Dead Letter 처리 로직 구현
- 재시도 전략 (3회, Exponential Backoff)
```

### 2.3 ADR 작성 가이드라인

| 항목 | 권장 사항 |
|------|----------|
| **작성 시점** | 코드 작성 **전** 또는 **중** |
| **작성 분량** | 1~2페이지 (너무 길면 안 읽힘) |
| **대안 개수** | 최소 2개 이상 (비교 없이는 의미 없음) |
| **결정 이유** | "~라서"보다 "~하기 때문에" (논리적 연결) |

---

## 3. Phase 2: 트러블슈팅 로그 - 문제해결 수사 일지

> 에러가 터졌을 때 구글링부터 하지 마세요. **'형사'**가 되어 수사 일지를 쓰세요.

### 3.1 트러블슈팅 로그 템플릿

```markdown
# TSL-{번호}: {에러명/현상}

> 예: TSL-0001: 대량 트래픽 발생 시 Connection Pool 고갈 및 500 에러

## 1. 현상 (Observation)

### 발생 일시
{YYYY-MM-DD HH:mm}

### 발생 환경
- 환경: {로컬 | 개발 | 스테이징 | 운영}
- 조건: {재현 조건} (예: JMeter 1000명 동시 접속 시)

### 증상
{구체적인 증상 설명}

### 에러 로그
```
{에러 로그 붙여넣기}
```

### 영향 범위
- {영향 1} (예: 전체 API 응답 지연 5초 이상)
- {영향 2} (예: 에러율 30% 발생)

## 2. 가설 수립 (Hypothesis)

> 💡 최소 3개 이상의 가설을 세우세요. 가설을 세우는 것 자체가 실력입니다.

### 가설 1: {가설명}
- **근거**: {왜 이 가설을 세웠는지}
- **검증 방법**: {어떻게 확인할 것인지}

### 가설 2: {가설명}
- **근거**: {왜 이 가설을 세웠는지}
- **검증 방법**: {어떻게 확인할 것인지}

### 가설 3: {가설명}
- **근거**: {왜 이 가설을 세웠는지}
- **검증 방법**: {어떻게 확인할 것인지}

## 3. 검증 (Verification)

### 가설 1 검증
- **검증 방법**: {실제 수행한 검증}
- **결과**: {결과}
- **판정**: ❌ 기각 / ✅ 채택

### 가설 2 검증
- **검증 방법**: {실제 수행한 검증}
- **결과**: {결과}
- **판정**: ❌ 기각 / ✅ 채택

### 가설 3 검증
- **검증 방법**: {실제 수행한 검증}
- **결과**: {결과}
- **판정**: ❌ 기각 / ✅ 채택

## 4. 근본 원인 (Root Cause)

### 원인 분석
{가설 검증을 통해 밝혀진 근본 원인}

### 원인 코드/설정
```java
// 문제가 된 코드
```

## 5. 해결 (Solution)

### 해결 방안
{어떻게 해결했는지}

### 수정된 코드/설정
```java
// 수정된 코드
```

### 관련 커밋
- {커밋 해시 및 링크}

## 6. 결과 (Result)

### Before
- {개선 전 수치}

### After
- {개선 후 수치}

### 검증 방법
{어떻게 해결을 확인했는지}

## 7. 재발 방지 (Prevention)

- [ ] {재발 방지 조치 1}
- [ ] {재발 방지 조치 2}
- [ ] {모니터링/알림 추가}

## 8. 배운 점 (Lessons Learned)

- {배운 점 1}
- {배운 점 2}
```

### 3.2 실제 작성 예시

```markdown
# TSL-0001: HikariCP Connection Pool 고갈로 인한 500 에러

## 1. 현상 (Observation)

### 발생 일시
2026-01-15 14:30

### 발생 환경
- 환경: 로컬 (부하 테스트 중)
- 조건: JMeter User 500명 동시 접속, Ramp-up 10초

### 증상
- HikariCP Connection Timeout 발생
- 응답 지연 5초 이상
- 에러율 약 25%

### 에러 로그
```
HikariPool-1 - Connection is not available, request timed out after 30000ms.
java.sql.SQLTransientConnectionException: HikariPool-1 - Connection is not available
```

### 영향 범위
- 복약 기록 생성 API 전체 장애
- 다른 API도 연쇄적으로 지연

## 2. 가설 수립 (Hypothesis)

### 가설 1: Slow Query로 인한 커넥션 점유
- **근거**: 커넥션이 오래 점유되면 풀이 고갈될 수 있음
- **검증 방법**: MySQL Slow Query 로그 확인

### 가설 2: Connection Pool Size 부족
- **근거**: 기본값 10개로는 500명 동시 처리 불가능
- **검증 방법**: Pool Size를 50으로 늘려서 테스트

### 가설 3: 트랜잭션 범위가 너무 넓음
- **근거**: @Transactional 안에 외부 API 호출이 포함되면 커넥션을 오래 점유
- **검증 방법**: 코드 리뷰로 트랜잭션 범위 확인

## 3. 검증 (Verification)

### 가설 1 검증
- **검증 방법**: `SHOW VARIABLES LIKE 'slow_query_log'` 및 로그 확인
- **결과**: Slow Query 없음 (모든 쿼리 100ms 미만)
- **판정**: ❌ 기각

### 가설 2 검증
- **검증 방법**: `spring.datasource.hikari.maximum-pool-size=50` 설정 후 재테스트
- **결과**: 여전히 Connection Timeout 발생 (다소 지연될 뿐)
- **판정**: ❌ 기각

### 가설 3 검증
- **검증 방법**: `MedicationRecordService.createRecord()` 코드 리뷰
- **결과**: Discord 알림 발송(외부 API, 200~500ms)이 @Transactional 안에 있음
- **판정**: ✅ 채택

## 4. 근본 원인 (Root Cause)

### 원인 분석
복약 기록 생성 시 Discord 알림 발송 로직이 트랜잭션 안에 포함되어 있었음.
외부 API 호출에 200~500ms가 소요되는 동안 DB 커넥션을 계속 점유.
동시 요청이 많아지면 커넥션 풀이 빠르게 고갈됨.

### 원인 코드
```java
@Transactional
public MedicationRecordResponse createRecord(MedicationRecordRequest request) {
    // 1. DB 작업 (10ms)
    MedicationRecord record = medicationRecordRepository.save(...);
    
    // 2. 외부 API 호출 (200~500ms) - 이 동안 커넥션 점유!
    discordNotificationService.sendNotification(record);
    
    return MedicationRecordResponse.from(record);
}
```

## 5. 해결 (Solution)

### 해결 방안
1. Discord 알림 발송을 트랜잭션 외부로 분리
2. Redis Stream 메시지 큐를 통한 비동기 처리

### 수정된 코드
```java
@Transactional
public MedicationRecordResponse createRecord(MedicationRecordRequest request) {
    // 1. DB 작업만 트랜잭션으로 묶음
    MedicationRecord record = medicationRecordRepository.save(...);
    
    // 2. 도메인 이벤트 발행 (동기)
    eventPublisher.publishEvent(new MedicationRecordCreatedEvent(record));
    
    return MedicationRecordResponse.from(record);
}

// 별도 리스너에서 비동기 처리
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleRecordCreated(MedicationRecordCreatedEvent event) {
    messageQueueService.sendNotification(event.getRecord());
}
```

### 관련 커밋
- `feat: Redis Stream 기반 비동기 알림 처리 (#47)`

## 6. 결과 (Result)

### Before
- 평균 응답 시간: 450ms
- 에러율: 25% (500명 동시 접속 시)
- Connection Pool 고갈 발생

### After
- 평균 응답 시간: 50ms (89% 개선)
- 에러율: 0%
- Connection Pool 안정적 유지

### 검증 방법
동일 조건(JMeter 500명)으로 부하 테스트 재실행

## 7. 재발 방지 (Prevention)

- [x] 코드 리뷰 체크리스트에 "트랜잭션 범위 내 외부 호출 금지" 추가
- [x] ArchUnit 테스트로 @Transactional 메서드 내 외부 호출 탐지
- [ ] HikariCP 메트릭 Prometheus 연동 및 알림 설정

## 8. 배운 점 (Lessons Learned)

- 트랜잭션 범위는 DB 작업만 포함해야 한다
- 외부 API 호출은 반드시 비동기 또는 트랜잭션 외부에서 처리
- Connection Pool Size를 늘리는 것은 근본 해결이 아님
```

### 3.3 트러블슈팅 작성 가이드라인

| 항목 | 권장 사항 |
|------|----------|
| **작성 시점** | 문제 해결 **직후** (기억이 생생할 때) |
| **가설 개수** | 최소 3개 (기각된 가설도 가치 있음) |
| **검증 과정** | 각 가설별 구체적인 검증 방법과 결과 기록 |
| **정량적 결과** | Before/After 수치 비교 필수 |

---

## 4. Phase 3: 문서 패키징 및 공유

### 4.1 README 연동

`docs/PORTFOLIO.md` 또는 프로젝트 루트 `README.md`에 아래 섹션을 추가하세요:

```markdown
## 🔥 Technical Issues & Solutions

| # | 문제 상황 | 핵심 해결 방법 | 상세 문서 |
|---|----------|---------------|----------|
| 1 | Connection Pool 고갈 | 트랜잭션 외부 비동기 처리 | [TSL-0001](docs/troubleshooting/TSL-0001-connection-pool-exhaustion.md) |
| 2 | N+1 쿼리 성능 저하 | EntityGraph + DTO Projection | [TSL-0002](docs/troubleshooting/TSL-0002-n-plus-one-query.md) |
| 3 | 분산락 데드락 발생 | 락 획득 순서 통일 + Timeout 설정 | [TSL-0003](docs/troubleshooting/TSL-0003-distributed-lock-deadlock.md) |
```

### 4.2 GitHub Issues 연동

GitHub Issues를 트러블슈팅 로그 작성 도구로 활용할 수 있습니다:

1. Issue 생성 시 트러블슈팅 템플릿 사용
2. 해결 후 Label을 `resolved`로 변경
3. 상세 로그는 `docs/troubleshooting/`에 정리

### 4.3 커밋 메시지 연동

```bash
# 일반 커밋
fix: HikariCP Connection Pool 고갈 문제 해결

- 트랜잭션 범위에서 외부 API 호출 분리
- Redis Stream 비동기 처리 도입
- 상세: docs/troubleshooting/TSL-0001-connection-pool-exhaustion.md
```

---

## 5. 파일 위치 및 네이밍 규칙

### 5.1 디렉토리 구조

```
docs/
├── internal/
│   ├── adr/                          # 아키텍처 의사결정 기록
│   │   ├── 0001-adr-template.md
│   │   ├── 0002-jwt-csrf-dual-protection.md
│   │   └── ...
│   └── DECISION-TROUBLESHOOTING-GUIDE.md  # 이 문서
├── troubleshooting/                  # 트러블슈팅 로그 (NEW)
│   ├── README.md
│   ├── TSL-0001-connection-pool-exhaustion.md
│   └── TSL-0002-n-plus-one-query.md
├── portfolio/                        # 정제된 포트폴리오 문서
│   ├── 01-jwt-csrf-protection.md
│   └── ...
└── PORTFOLIO.md                      # 메인 포트폴리오
```

### 5.2 파일 네이밍 규칙

| 문서 유형 | 패턴 | 예시 |
|----------|------|------|
| ADR | `{번호 4자리}-{케밥케이스}.md` | `0008-realtime-protocol-selection.md` |
| 트러블슈팅 | `TSL-{번호 4자리}-{케밥케이스}.md` | `TSL-0001-connection-pool-exhaustion.md` |
| 포트폴리오 | `{번호 2자리}-{케밥케이스}.md` | `01-jwt-csrf-protection.md` |

### 5.3 상태 관리

**ADR 상태**:
- `Draft`: 작성 중
- `Proposed`: 검토 대기
- `Accepted`: 채택됨
- `Deprecated`: 폐기됨
- `Superseded by ADR-XXX`: 다른 ADR로 대체됨

**트러블슈팅 상태** (파일 상단에 표시):
- `🔴 Investigating`: 조사 중
- `🟡 In Progress`: 해결 진행 중
- `🟢 Resolved`: 해결 완료

---

## 부록: 빠른 참조 체크리스트

### ADR 작성 전 체크리스트

- [ ] 해결해야 할 문제가 명확한가?
- [ ] 최소 2개 이상의 대안을 고려했는가?
- [ ] 각 대안의 장단점을 객관적으로 분석했는가?
- [ ] 선택 이유가 논리적인가?
- [ ] 트레이드오프를 인지하고 후속 조치를 계획했는가?

### 트러블슈팅 작성 전 체크리스트

- [ ] 현상을 재현 가능하게 기록했는가?
- [ ] 에러 로그를 첨부했는가?
- [ ] 최소 3개의 가설을 세웠는가?
- [ ] 각 가설의 검증 방법을 구체적으로 기록했는가?
- [ ] 기각된 가설도 "왜 기각되었는지" 기록했는가?
- [ ] Before/After 정량적 비교가 있는가?
- [ ] 재발 방지 조치를 계획했는가?

---

> 📝 **기억하세요**: 면접관은 "무엇을 했는지"보다 "왜 그렇게 했는지"를 묻습니다.
> 이 문서들이 쌓이면, 그것이 바로 당신의 **기술적 깊이**입니다.
