# 🔍 Hamalog 프로젝트 종합 평가

> 📅 평가일: 2026년 1월 1일  
> 🎯 목적: 백엔드 개발자 지망생 관점에서의 실무 수준 평가  
> ⏱️ 개발 기간: 약 5.5개월

---

## 📊 종합 점수: **88/100** ⭐⭐⭐⭐

**결론: 신입/주니어 개발자 기준으로 상당히 높은 수준의 프로젝트입니다.**

레이어드 아키텍처, AOP 기반 횡단 관심사, JWT+CSRF 이중 보호, Redis 캐싱, 1400+ 테스트 케이스, ArchUnit 아키텍처 테스트 등 **실무에서 요구하는 패턴들이 잘 녹아 있습니다.** 다만 실제 운영 경험의 부재와 일부 과잉 엔지니어링 요소가 보완점입니다.

---

## 🏆 강점 (실무적으로 우수한 점)

### 1. 아키텍처 일관성 (95/100)

```
Controller → Service → Repository (의존성 방향 준수)
     ↓
   ArchUnit으로 자동 검증
```

**잘한 점:**
- 레이어드 아키텍처 + 도메인별 패키지 분리가 명확
- `ArchitectureRulesTest.java`로 아키텍처 규칙 강제 (Controller→Repository 직접 접근 금지 등)
- DTO, Entity, Service 간 책임 분리가 깔끔
- Record 클래스를 DTO에 적극 활용 (Java 21 현대적 패턴)

**실무 관점:**
> "아키텍처 규칙을 코드로 검증한다"는 것 자체가 시니어 개발자들도 놓치기 쉬운 부분입니다. 이 점만으로도 품질에 대한 의지가 보입니다.

---

### 2. 보안 설계 (92/100)

| 보안 요소 | 구현 상태 | 평가 |
|-----------|-----------|------|
| JWT 인증 | ✅ Access + Refresh Token | 표준적 |
| CSRF 보호 | ✅ Redis 기반 토큰 | SPA 환경 모범 사례 |
| 리소스 소유권 | ✅ AOP `@RequireResourceOwnership` | **우수** |
| Rate Limiting | ✅ Redis + 외부 설정화 | 운영 친화적 |
| 토큰 로테이션 | ✅ Refresh Token Rotation | 탈취 대응 |

**ADR-0002 (JWT + CSRF 이중 보호)** 문서화가 특히 인상적입니다.

**실무 관점:**
> 대부분의 신입 개발자 프로젝트는 "JWT만 있으면 보안 끝"이라고 생각합니다. CSRF까지 고려하고, 그 이유를 ADR로 문서화한 것은 **보안 의식이 높다**는 증거입니다.

---

### 3. 문서화 수준 (95/100)

```
docs/
├── internal/
│   ├── CODING-CONVENTIONS.md      ← 코딩 컨벤션
│   ├── adr/                       ← Architecture Decision Records (6개)
│   └── patterns/                  ← 패턴 가이드 (5개)
├── shared/
│   ├── API-specification.md       ← 프론트엔드용 API 명세
│   └── Project-Structure.md       ← 프로젝트 구조 설명
└── ai/
    └── VIBE-CODING-GUIDE.md       ← AI 협업 가이드
```

**잘한 점:**
- **ADR (Architecture Decision Record)** 6개 작성 - 의사결정 이력 추적 가능
- 내부용(internal)과 외부용(shared) 문서 분리
- AI 협업용 컨텍스트 파일 (`.cursorrules`, `copilot-instructions.md`)

**실무 관점:**
> 많은 회사에서 "왜 이렇게 설계했지?"라는 질문에 답을 못합니다. ADR이 있다면 3년 후에도 의사결정 배경을 알 수 있습니다. **기업 수준의 문서화**입니다.

---

### 4. 테스트 문화 (90/100)

| 테스트 유형 | 파일 수 | 상태 |
|-------------|---------|------|
| 단위 테스트 | 30+ | ✅ |
| 통합 테스트 | 10+ | ✅ |
| E2E 테스트 | 3 | ✅ (Auth, Medication, MoodDiary) |
| 아키텍처 테스트 | 1 | ✅ ArchUnit |
| N+1 재현/해결 테스트 | 2 | ✅ **특별히 우수** |

**잘한 점:**
- `NPlusOneReproductionTest.java` / `NPlusOneSolutionTest.java` - N+1 문제를 테스트로 문서화
- `@DisplayName` 한글로 테스트 의도 명확화
- Nested 클래스로 테스트 계층 구조화

**실무 관점:**
> N+1 문제를 "재현 테스트 → 해결 테스트"로 남긴 것은 **학습 과정을 코드로 기록**한 것입니다. 면접에서 "N+1을 어떻게 해결했나요?"라는 질문에 코드로 보여줄 수 있습니다.

---

### 5. DevOps 성숙도 (88/100)

```yaml
# CI 파이프라인 구성
1. Spotless Check (코드 포맷팅)
2. Tests with Coverage (JaCoCo)
3. Security Scan (OWASP)
4. Docker Build & Push
5. Deploy (조건부)
```

**잘한 점:**
- GitHub Actions CI/CD 파이프라인
- Docker Compose (개발/프로덕션 분리)
- Cloudflare Tunnel (보안 터널, IP 노출 방지)
- Flyway 마이그레이션 (V1~V5)
- git-cliff CHANGELOG 자동화

**실무 관점:**
> Cloudflare Tunnel까지 구성한 것은 **실제 배포 경험**이 있다는 증거입니다. 많은 포트폴리오가 "로컬에서만 돌아감"인데, 이 프로젝트는 운영 환경을 고려했습니다.

---

## ⚠️ 개선 필요 사항

### 1. 실제 운영 관점 부재 (우선순위: ⭐⭐⭐)

**문제:**
- 코드와 문서는 훌륭하지만, **실제 트래픽 처리 경험 증거가 없음**
- 부하 테스트 결과, 병목 발견/해결 사례가 없음

**개선 방법:**
```bash
# JMeter/Gatling으로 부하 테스트 후 결과 문서화
# 예: "100 concurrent users, 1000 requests/sec 처리 확인"
```

**추가할 문서:**
```markdown
## 성능 테스트 결과 (docs/PERFORMANCE-TEST.md)
- 테스트 환경: AWS t3.medium, MySQL 8.0, Redis 7
- 동시 사용자: 100명
- 평균 응답 시간: 45ms
- 95 percentile: 120ms
- 발견된 병목: N+1 쿼리 (해결 완료)
```

---

### 2. 과잉 엔지니어링 경향 (우선순위: ⭐⭐) ✅ 해결됨

> 📅 해결일: 2026-01-02  
> 📄 참고: [COMPLEXITY-JUSTIFICATION.md](internal/COMPLEXITY-JUSTIFICATION.md)

**문제:**
- 헬스케어 앱 규모 대비 JWT+CSRF, Domain Event, 이벤트 스토어 등이 과함
- 실무에서는 "왜 더 단순한 방법을 안 썼는지" 질문받을 수 있음

**해결 내용:**
1. ✅ ADR-0002 (JWT+CSRF) - 트레이드오프 섹션 추가, 규모별 권장 방식 명시
2. ✅ ADR-0003 (AOP) - @RequireResourceOwnership vs @PreAuthorize 비교 추가
3. ✅ ADR-0004 (Domain Event) - Event Store 사용 판단 기준 명시
4. ✅ ADR-0005 (Redis) - 규모별 캐싱 전략 권장 방식 추가
5. ✅ 종합 문서 - `docs/internal/COMPLEXITY-JUSTIFICATION.md` 생성
6. ✅ 토글 옵션 - CSRF, Event Store 활성화/비활성화 설정 추가

**핵심 메시지:**
> "과잉 엔지니어링임을 인지하고 있으며, 학습 목적과 확장성을 고려해 의도적으로 선택했습니다."

---

### 3. 비동기 처리 미흡 (우선순위: ⭐⭐⭐) ✅ 해결됨

> 📅 해결일: 2026-01-03  
> 📄 참고: [ADR-0007](internal/adr/0007-message-queue-redis-stream.md)

**문제:**
- FCM 푸시, 이메일 발송 등 I/O 작업이 동기적으로 처리됨
- 대량 푸시 발송 시 응답 지연 발생 가능

**해결 내용:**
Redis Stream 기반 메시지 큐 시스템을 도입했습니다:

1. ✅ `MessageQueueService` - 메시지 발행 (Producer)
2. ✅ `NotificationConsumerService` - 메시지 소비 및 FCM 발송
3. ✅ `QueuedNotificationService` - 큐 활성화 여부에 따른 Facade
4. ✅ 재시도 로직 (최대 3회) 및 Dead Letter Queue 구현
5. ✅ Discord Webhook 알림 (DLQ 적재 시)
6. ✅ Prometheus 메트릭 수집

**아키텍처:**
```
API 요청 → MessageQueueService → Redis Stream → NotificationConsumerService → FCM
                                       ↓ (실패 시)
                                Dead Letter Queue → Discord 알림
```

**설정 옵션:**
```properties
# 활성화/비활성화
hamalog.queue.enabled=true

# Discord 알림
hamalog.queue.discord.enabled=true
hamalog.queue.discord.webhook-url=https://discord.com/api/webhooks/...
```

**Kafka 대신 Redis Stream을 선택한 이유:**
- 기존 Redis 인프라 활용 (추가 비용 없음)
- 프로젝트 규모에 적합한 처리량
- 낮은 운영 복잡도
- 필요 시 Kafka로 마이그레이션 가능한 설계

---

### 4. 도메인 로직 빈약 - Anemic Domain Model (우선순위: ⭐⭐) ✅ 해결됨

> 📅 해결일: 2026-01-02

**문제:**
- Entity가 Getter/Setter 위주로 구성됨
- 비즈니스 로직이 대부분 Service에 있음

**해결 내용:**
다음 Entity에 도메인 로직(비즈니스 규칙)을 추가했습니다:

1. **MedicationSchedule** - 복약 스케줄
   - `getEndDate()` - 종료일 계산
   - `isExpired()` - 만료 여부 확인
   - `getRemainingDays()` - 남은 일수 계산
   - `getProgressPercentage()` - 진행률 계산 (0~100%)
   - `hasStarted()` - 복약 시작 여부
   - `isOngoing()` - 현재 복약 중 여부
   - `getTotalDosageCount()` - 총 복용 횟수

2. **MedicationRecord** - 복약 기록
   - `isTaken()` / `isSkipped()` - 복용 상태 확인
   - `isDelayed()` - 지연 복용 여부 (30분 기준)
   - `isEarly()` - 조기 복용 여부
   - `isOnTime()` - 정시 복용 여부
   - `getTimeDifferenceMinutes()` - 시간 차이 계산
   - `markAsTaken()` / `markAsSkipped()` - 상태 변경

3. **SideEffectRecord** - 부작용 기록
   - `isLinkedToMedication()` - 약물 연계 여부
   - `getDaysSinceCreated()` - 경과 일수
   - `isRecent()` - 최근 기록 여부 (7일 이내)
   - `getLinkedMedicationName()` - 연계 약물명

4. **MoodDiary** - 마음 일기
   - `isPositiveMood()` / `isNegativeMood()` - 기분 분류
   - `isTemplateType()` / `isFreeFormType()` - 일기 유형
   - `hasContent()` - 내용 유무 확인
   - `getMoodDescription()` - 기분 한글 설명

**테스트 추가:**
- `MedicationScheduleDomainTest.java` - 12개 테스트 케이스
- `MedicationRecordDomainTest.java` - 11개 테스트 케이스

**장점:**
- Entity가 자체 비즈니스 규칙을 알고 있음
- Service 레이어가 더 얇아짐
- 테스트하기 쉬움 (DB 없이 단위 테스트 가능)
    // ...필드들
    
    // 도메인 로직을 Entity로 이동
    public boolean isExpired() {
        return this.startOfAd
            .plusDays(this.prescriptionDays)
            .isBefore(LocalDate.now());
    }
    
    public LocalDate getEndDate() {
        return this.startOfAd.plusDays(this.prescriptionDays);
    }
    
    public int getRemainingDays() {
        return (int) ChronoUnit.DAYS.between(LocalDate.now(), getEndDate());
    }
}
```

---

### 5. 모니터링/Observability 강화 필요 (우선순위: ⭐⭐)

**문제:**
- Prometheus 메트릭 수집은 있으나 대시보드, 알람 부재
- 장애 발생 시 어떻게 감지할 것인지 불명확

**현재:**
```
Prometheus 메트릭 수집 ✅
Grafana 대시보드 ❌
AlertManager 알람 ❌
```

**개선:**
```yaml
# docker-compose.yml에 추가
grafana:
  image: grafana/grafana:latest
  ports:
    - "3000:3000"
  volumes:
    - ./grafana/dashboards:/var/lib/grafana/dashboards
```

**대시보드 예시:**
- API 응답 시간 분포
- 에러율 추이
- Redis 캐시 히트율
- Rate Limiting 차단 비율

---

## 📈 개선 우선순위 로드맵

### Phase 1: 즉시 적용 (1주일)

| 항목 | 작업 | 예상 시간 |
|------|------|-----------|
| 부하 테스트 | JMeter로 기본 시나리오 테스트 | 4시간 |
| 결과 문서화 | `docs/PERFORMANCE-TEST.md` 작성 | 2시간 |
| ADR 보완 | 트레이드오프 섹션 추가 | 2시간 |

### Phase 2: 단기 개선 (2주)

| 항목 | 작업 | 예상 시간 |
|------|------|-----------|
| 비동기 처리 | FCM 푸시 `@Async` 적용 | 4시간 |
| 도메인 로직 이동 | 핵심 2-3개 메서드 Entity로 | 4시간 |
| Grafana 대시보드 | 기본 메트릭 시각화 | 6시간 |

### Phase 3: 중기 개선 (1개월)

| 항목 | 작업 | 예상 시간 |
|------|------|-----------|
| 실사용자 확보 | 지인 5명에게 앱 사용 요청 | - |
| 피드백 수집 | 사용성 이슈 정리 | - |
| 기술 블로그 | 이 프로젝트 경험 3편 작성 | 15시간 |

---

## 💼 면접 대비 포인트

### 이 프로젝트로 어필할 수 있는 것

1. **"아키텍처 규칙을 코드로 검증합니다"**
   - ArchUnit 테스트 보여주기
   - "실수로 Controller에서 Repository 직접 접근해도 CI에서 막힙니다"

2. **"보안 의사결정을 문서화했습니다"**
   - ADR-0002 (JWT+CSRF) 설명
   - "왜 이 방식을 선택했는지 3년 후에도 알 수 있습니다"

3. **"N+1 문제를 테스트로 증명하고 해결했습니다"**
   - `NPlusOneReproductionTest` → `NPlusOneSolutionTest` 순서 설명
   - "문제 재현 → 해결 → 검증까지 TDD로 진행했습니다"

4. **"실제 배포 환경을 구성했습니다"**
   - Cloudflare Tunnel, Docker Compose, CI/CD 설명
   - "로컬이 아닌 실제 서버에서 돌아갑니다"

### 예상 질문 & 답변 준비

| 질문 | 핵심 답변 |
|------|-----------|
| JWT만 쓰면 되는데 왜 CSRF? | SPA 환경에서 Cookie 기반 토큰 저장 시 CSRF 취약. 이중 보호로 방어 |
| 왜 Redis 캐싱? | 자주 조회되는 회원 정보/통계 DB 부하 감소. TTL로 일관성 관리 |
| 테스트 커버리지는? | JaCoCo 기준 80% 이상. 핵심 비즈니스 로직 우선 |
| 과잉 엔지니어링 아닌가? | 맞습니다. 학습 목적 + 확장성 고려. 실무에서는 규모에 맞게 조절 |

---

## 🎯 최종 조언

### 잘하고 있는 것 (유지)
- ✅ 문서화 습관 - 계속 유지하세요
- ✅ 테스트 작성 - 실무에서 가장 중요한 역량
- ✅ 아키텍처 고민 - 신입 중 드문 강점
- ✅ DevOps 경험 - 풀스택 이해도 증명

### 보완이 필요한 것
- ⚠️ **실제 운영 경험**: 부하 테스트 + 실사용자 피드백
- ⚠️ **적절한 복잡도**: "왜 단순하게 안 했나" 설명 준비
- ⚠️ **커뮤니케이션**: 기술 블로그로 설명 능력 증명

### 다음 프로젝트 제안
현재 프로젝트는 "기술 쇼케이스"로 두되, **더 단순한 프로젝트**를 하나 더 만들어 **YAGNI 원칙**을 보여주는 것을 추천합니다.

```
프로젝트 A (현재): 복잡한 기술 스택 → "이것도 할 수 있다"
프로젝트 B (신규): 최소 기능, 빠른 구현 → "적절한 도구 선택 능력"
```

**실무에서는 "적절한 복잡도"가 더 중요합니다.**

---

## 📝 마무리

5개월 반 동안 혼자 이 수준까지 만든 것은 **대단한 성과**입니다. 대부분의 신입 개발자 포트폴리오가 "로컬에서 CRUD만 되는" 수준인데, 이 프로젝트는:

- 아키텍처 규칙 자동 검증
- 보안 의사결정 문서화
- 실제 배포 환경 구성
- 1400+ 테스트 케이스

까지 갖추고 있습니다.

**부족한 것은 "실제 운영 경험"뿐입니다.**
- 지인에게 앱 사용시켜 보기
- 부하 테스트 후 결과 문서화
- 기술 블로그 3편 작성

이 세 가지만 추가하면 **충분히 경쟁력 있는 포트폴리오**가 됩니다.

화이팅! 🚀

---

> 이 문서는 프로젝트 개선을 위한 참고용이며, 정기적으로 업데이트됩니다.

