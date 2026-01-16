# 1. 데이터 로드
docker exec -i hamalog-benchmark-mysql mysql -uroot -pbenchmark hamalog_benchmark < scripts/benchmark/init-benchmark-data.sql

# 2. 데이터 확인
docker exec -i hamalog-benchmark-mysql mysql -uroot -pbenchmark hamalog_benchmark -e "SELECT COUNT(*) FROM medication_schedule WHERE member_id = 1;"

# 3. Gatling 실행
./gradlew gatlingRun -Dgatling.simulationClass=com.Hamalog.simulation.LocalMedicationBenchmark# 로컬 벤치마크 테스트 가이드

> **목적**: N+1 문제 개선 전후 성능 비교를 위한 로컬 벤치마크 실행
> 
> **환경**: Docker + Gatling
> 
> **최종 수정**: 2026-01-16

---

## 📋 빠른 시작

### 원클릭 실행 (권장)

```bash
./scripts/benchmark/run-local-benchmark.sh
```

이 스크립트는 다음을 자동으로 수행합니다:
1. Docker 환경 정리
2. 애플리케이션 빌드
3. Docker Compose 시작
4. 헬스체크 대기
5. 테스트 데이터 로드
6. Gatling 벤치마크 실행
7. 결과 저장 및 정리

---

## 🔧 수동 실행 (단계별)

### 1. 기존 환경 정리

```bash
docker-compose -f docker-compose-benchmark.yml down -v
```

### 2. 애플리케이션 빌드

```bash
./gradlew bootJar -x test
```

### 3. Docker 환경 시작

```bash
docker-compose -f docker-compose-benchmark.yml up -d --build
```

### 4. 서비스 시작 대기 (헬스체크)

```bash
# 헬스체크 (UP 상태 확인)
curl http://localhost:8080/actuator/health

# 또는 루프로 대기
while ! curl -s http://localhost:8080/actuator/health | grep -q '"status":"UP"'; do
  echo "Waiting for app..."
  sleep 2
done
echo "App is ready!"
```

### 5. 테스트 데이터 로드

```bash
docker exec -i hamalog-benchmark-mysql mysql -uroot -pbenchmark hamalog_benchmark < scripts/benchmark/init-benchmark-data.sql
```

### 6. 데이터 확인

```bash
# 회원 수 확인
docker exec -i hamalog-benchmark-mysql mysql -uroot -pbenchmark hamalog_benchmark -e "SELECT COUNT(*) AS member_count FROM member;"

# 스케줄 수 확인
docker exec -i hamalog-benchmark-mysql mysql -uroot -pbenchmark hamalog_benchmark -e "SELECT COUNT(*) AS schedule_count FROM medication_schedule WHERE member_id = 1;"

# 복약시간 수 확인
docker exec -i hamalog-benchmark-mysql mysql -uroot -pbenchmark hamalog_benchmark -e "SELECT COUNT(*) AS time_count FROM medication_time;"
```

### 7. Gatling 벤치마크 실행

```bash
./gradlew gatlingRun -Dgatling.simulationClass=com.Hamalog.simulation.LocalMedicationBenchmark
```

### 8. 환경 정리

```bash
docker-compose -f docker-compose-benchmark.yml down -v
```

---

## 📊 벤치마크 API 테스트 (curl)

### 헬스체크

```bash
curl http://localhost:8080/actuator/health
```

### N+1 문제 쿼리 (Before - 느림)

```bash
curl "http://localhost:8080/api/v1/benchmark/medication-schedules/list/1?optimized=false"
```

### 최적화된 쿼리 (After - 빠름)

```bash
curl "http://localhost:8080/api/v1/benchmark/medication-schedules/list/1?optimized=true"
```

### 응답 시간 측정

```bash
# N+1 쿼리 응답 시간
time curl -s "http://localhost:8080/api/v1/benchmark/medication-schedules/list/1?optimized=false" > /dev/null

# 최적화 쿼리 응답 시간
time curl -s "http://localhost:8080/api/v1/benchmark/medication-schedules/list/1?optimized=true" > /dev/null
```

### 반복 테스트 (간단한 부하 테스트)

```bash
# 100번 반복 - N+1
for i in {1..100}; do
  curl -s -o /dev/null -w "%{time_total}\n" "http://localhost:8080/api/v1/benchmark/medication-schedules/list/1?optimized=false"
done

# 100번 반복 - Optimized
for i in {1..100}; do
  curl -s -o /dev/null -w "%{time_total}\n" "http://localhost:8080/api/v1/benchmark/medication-schedules/list/1?optimized=true"
done
```

---

## 🐳 Docker 관련 커맨드

### 로그 확인

```bash
# 앱 로그
docker logs hamalog-benchmark-app -f

# MySQL 로그
docker logs hamalog-benchmark-mysql -f

# 모든 서비스 로그
docker-compose -f docker-compose-benchmark.yml logs -f
```

### 컨테이너 상태 확인

```bash
docker-compose -f docker-compose-benchmark.yml ps
```

### MySQL 직접 접속

```bash
docker exec -it hamalog-benchmark-mysql mysql -uroot -pbenchmark hamalog_benchmark
```

### 컨테이너 재시작

```bash
docker-compose -f docker-compose-benchmark.yml restart app
```

---

## 📈 결과 확인

### Gatling HTML 리포트

```bash
# macOS
open build/reports/gatling/*/index.html

# Linux
xdg-open build/reports/gatling/*/index.html
```

### 저장된 결과

```bash
ls -la benchmark-results/
```

---

## 🔍 디버깅

### Hibernate SQL 로그 확인

앱 로그에서 실행된 SQL 쿼리 확인:

```bash
docker logs hamalog-benchmark-app 2>&1 | grep -A 5 "Hibernate:"
```

### 쿼리 수 비교

- **N+1 문제 (optimized=false)**: 1 + N 쿼리 (N = 스케줄 수)
- **최적화 (optimized=true)**: 1 쿼리 (@EntityGraph fetch join)

---

## ⚠️ 트러블슈팅

### MySQL 연결 오류

```bash
# MySQL이 준비될 때까지 대기
docker exec hamalog-benchmark-mysql mysqladmin ping -uroot -pbenchmark --wait=30
```

### 포트 충돌

```bash
# 사용 중인 포트 확인
lsof -i :8080
lsof -i :3307
lsof -i :6380

# 기존 Docker 컨테이너 정리
docker-compose -f docker-compose-benchmark.yml down -v
```

### Flyway 마이그레이션 오류

```bash
# 볼륨 삭제 후 재시작
docker-compose -f docker-compose-benchmark.yml down -v
docker-compose -f docker-compose-benchmark.yml up -d
```

### 데이터 로드 실패

```bash
# 테이블 구조 확인
docker exec -i hamalog-benchmark-mysql mysql -uroot -pbenchmark hamalog_benchmark -e "DESCRIBE member;"
docker exec -i hamalog-benchmark-mysql mysql -uroot -pbenchmark hamalog_benchmark -e "DESCRIBE medication_schedule;"
```

---

## 📁 관련 파일

| 파일 | 설명 |
|------|------|
| `docker-compose-benchmark.yml` | 벤치마크 Docker 환경 |
| `src/main/resources/application-benchmark.yml` | 벤치마크 프로파일 설정 |
| `scripts/benchmark/run-local-benchmark.sh` | 원클릭 실행 스크립트 |
| `scripts/benchmark/init-benchmark-data.sql` | 테스트 데이터 SQL |
| `src/gatling/kotlin/.../LocalMedicationBenchmark.kt` | Gatling 시뮬레이션 |

---

## 📊 예상 결과

```
===============================================================================
Before (N+1 Problem - optimized=false):
  ├─ Mean Response Time: ~300-500ms
  ├─ Database Queries: 1 + 100 = 101 쿼리
  └─ P95 Response Time: ~600-800ms

After (Optimized - optimized=true):
  ├─ Mean Response Time: ~50-100ms (↓ 80% 개선)
  ├─ Database Queries: 1 쿼리 (fetch join)
  └─ P95 Response Time: ~100-150ms (↓ 85% 개선)
===============================================================================
```

