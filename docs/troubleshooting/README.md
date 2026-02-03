# 트러블슈팅 로그 (Troubleshooting Logs)

> **목적**: 문제 해결 과정을 체계적으로 기록하여 재발 방지 및 포트폴리오 활용

---

## 📋 개요

이 디렉토리는 프로젝트에서 발생한 문제와 해결 과정을 기록하는 **트러블슈팅 로그(TSL)**를 보관합니다.

단순히 "해결했다"가 아닌, **어떤 가설을 세우고 어떻게 검증했는지** 과정을 기록합니다.

---

## 📂 파일 네이밍 규칙

```
TSL-{번호}-{간략한-설명}.md
```

| 예시 | 설명 |
|------|------|
| `TSL-0001-hikari-connection-pool-exhaustion.md` | HikariCP 커넥션 풀 고갈 문제 |
| `TSL-0002-n-plus-one-query-medication-api.md` | 복약 API N+1 쿼리 문제 |
| `TSL-0003-redis-stream-consumer-lag.md` | Redis Stream 컨슈머 지연 문제 |

---

## 🏷️ 상태 레이블

| 상태 | 설명 |
|------|------|
| `🔴 Open` | 문제 조사 중 |
| `🟡 In Progress` | 해결 방안 적용 중 |
| `🟢 Resolved` | 해결 완료 |
| `⚪ Closed` | 해결 후 재발 방지 조치 완료 |

---

## 🚀 빠른 시작

### 새 트러블슈팅 로그 작성

```bash
# 템플릿 복사
cp docs/troubleshooting/TSL-0001-template.md docs/troubleshooting/TSL-XXXX-{설명}.md

# 번호는 순차적으로 부여 (0001, 0002, ...)
```

---

## 📝 트러블슈팅 로그 작성 가이드라인

### 필수 섹션

1. **현상 (Observation)**: 무엇이 발생했는가?
2. **가설 수립 (Hypothesis)**: 왜 발생했다고 생각하는가? (최소 3개)
3. **검증 (Verification)**: 각 가설을 어떻게 검증했는가?
4. **근본 원인 (Root Cause)**: 실제 원인은 무엇이었는가?
5. **해결 (Solution)**: 어떻게 해결했는가?
6. **결과 (Result)**: Before/After 비교
7. **재발 방지 (Prevention)**: 동일 문제 재발을 막기 위한 조치

### 작성 팁

| 항목 | 권장 사항 |
|------|----------|
| **가설 개수** | 최소 3개 이상 (비교/기각 과정이 실력을 보여줌) |
| **에러 로그** | 핵심 부분만 발췌 (너무 길면 별도 파일로) |
| **수치화** | 가능한 Before/After 수치 비교 (응답시간, 에러율 등) |
| **커밋 링크** | 관련 커밋 해시 및 PR 링크 포함 |

---

## 📎 관련 문서

- [DECISION-TROUBLESHOOTING-GUIDE.md](../internal/DECISION-TROUBLESHOOTING-GUIDE.md) - 상세 작성 가이드
- [ADR 디렉토리](../internal/adr/) - 의사결정 기록
- [포트폴리오](../portfolio/) - 정제된 결과물

---

## 📑 트러블슈팅 로그 목록

| 번호 | 제목 | 상태 | 작성일 |
|------|------|------|--------|
| - | (아직 작성된 로그 없음) | - | - |

> 💡 새 트러블슈팅 로그 작성 후 이 목록에 추가해주세요.
