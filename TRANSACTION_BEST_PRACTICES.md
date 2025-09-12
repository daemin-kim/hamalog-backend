# Hamalog 트랜잭션 모범 사례 가이드

*작성일: 2025-01-12*

## 개요

이 문서는 Hamalog 애플리케이션에서 트랜잭션을 올바르게 사용하기 위한 모범 사례와 가이드라인을 제공합니다.

## 🎯 핵심 원칙

### 1. 계층별 책임 분리
```java
// ❌ 잘못된 예: Controller에서 트랜잭션 사용
@RestController
public class BadController {
    @PostMapping("/members")
    @Transactional  // 컨트롤러에서 트랜잭션 사용 금지!
    public ResponseEntity<Member> createMember(@RequestBody CreateRequest request) {
        // 비즈니스 로직과 트랜잭션 관리가 Controller에 혼재
    }
}

// ✅ 올바른 예: Service에서 트랜잭션 관리
@RestController
public class GoodController {
    @PostMapping("/members")
    public ResponseEntity<Member> createMember(@RequestBody CreateRequest request) {
        Member member = memberService.createMember(request);  // Service에서 처리
        return ResponseEntity.ok(member);
    }
}

@Service
@Transactional(readOnly = true)  // 클래스 레벨 기본 설정
public class MemberService {
    @Transactional  // 쓰기 작업은 오버라이드
    public Member createMember(CreateRequest request) {
        // 트랜잭션 관리는 Service 계층에서
    }
}
```

### 2. 표준 트랜잭션 패턴
모든 Service 클래스는 다음 패턴을 따라야 합니다:

```java
@Service
@Transactional(readOnly = true)  // 기본: 읽기 전용
@RequiredArgsConstructor
public class ExampleService {
    
    // 읽기 작업: 추가 어노테이션 불필요
    public List<Entity> findAll() {
        return repository.findAll();
    }
    
    // 쓰기 작업: @Transactional 오버라이드
    @Transactional
    public Entity create(CreateRequest request) {
        Entity entity = buildEntity(request);
        return repository.save(entity);
    }
    
    @Transactional
    public Entity update(Long id, UpdateRequest request) {
        Entity entity = repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException());
        entity.update(request);
        return repository.save(entity);
    }
    
    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
```

### 3. 트랜잭션 경계 분리

#### Redis/외부 시스템 연산 분리
```java
// ❌ 잘못된 예: 트랜잭션 내에서 Redis 작업
@Service
public class BadService {
    @Transactional
    public void deleteMember(String loginId, String token) {
        memberRepository.delete(member);  // DB 작업
        redisService.blacklistToken(token);  // Redis 작업 - 문제!
        // DB 롤백 시 Redis 상태 불일치 발생 가능
    }
}

// ✅ 올바른 예: 이벤트 기반 분리
@Service
public class GoodService {
    @Transactional
    public void deleteMember(String loginId, String token) {
        memberRepository.delete(member);  // DB 작업만
        // 트랜잭션 완료 후 이벤트 발행
        eventPublisher.publishEvent(new MemberDeletedEvent(loginId, token));
    }
}

@Component
public class MemberDeletedEventHandler {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handle(MemberDeletedEvent event) {
        redisService.blacklistToken(event.getToken());  // 트랜잭션 완료 후 실행
    }
}
```

## 📋 설정 가이드라인

### 1. 애플리케이션 설정

**개발 환경 (`application-local.properties`)**:
```properties
# H2 인메모리 데이터베이스
spring.datasource.url=jdbc:h2:mem:hamalog;DB_CLOSE_DELAY=-1
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
```

**운영 환경 (`application-prod.properties`)**:
```properties
# MySQL + 트랜잭션 설정
spring.datasource.url=jdbc:mysql://localhost:3306/hamalog
spring.transaction.default-timeout=30
spring.jpa.properties.hibernate.connection.isolation=2

# HikariCP 연결 풀 최적화
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=20000
```

**테스트 환경 (`application-test.properties`)**:
```properties
# 테스트용 H2 설정
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
spring.jpa.hibernate.ddl-auto=create-drop

# AOP 비활성화 (테스트 간섭 방지)
app.aop.global-enabled=false
```

### 2. AOP 순서 정의
```java
@Order(1) - ApiLoggingAspect      // API 호출 로깅
@Order(2) - PerformanceAspect     // 성능 모니터링
@Order(3) - BusinessAuditAspect   // 비즈니스 감사
@Order(4) - RetryAspect           // 재시도 처리
@Order(5) - CachingAspect         // 캐싱 처리
기본 순서 - TransactionAspect     // 트랜잭션 (LOWEST_PRECEDENCE)
```

## 🚀 성능 최적화

### 1. 읽기 전용 트랜잭션 활용
```java
@Transactional(readOnly = true)
public List<Member> searchMembers(SearchCriteria criteria) {
    // 읽기 전용 트랜잭션 사용으로 성능 향상
    return memberRepository.findByCriteria(criteria);
}
```

### 2. 배치 처리 최적화
```properties
# Hibernate 배치 설정
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
spring.jpa.properties.hibernate.default_batch_fetch_size=100
```

### 3. 트랜잭션 타임아웃 설정
```java
@Transactional(timeout = 10)  // 10초 타임아웃
public void longRunningOperation() {
    // 장기 실행 작업
}
```

## 🔍 모니터링 및 디버깅

### 1. TransactionMetricsService 사용
```java
@Service
public class ExampleService {
    private final TransactionMetricsService metricsService;
    
    @Transactional
    public void monitoredOperation() {
        long startTime = System.currentTimeMillis();
        boolean success = false;
        
        try {
            // 비즈니스 로직
            success = true;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordTransactionExecution("monitoredOperation", duration, success);
        }
    }
}
```

### 2. 로깅 설정
```properties
# 운영 환경
logging.level.org.hibernate.SQL=warn
logging.level.org.springframework.transaction=info

# 개발 환경
logging.level.org.hibernate.SQL=debug
logging.level.org.hibernate.type.descriptor.sql=trace
logging.level.org.springframework.transaction=debug
```

## ⚠️ 주의사항 및 안티패턴

### 1. 피해야 할 패턴
```java
// ❌ Controller에서 @Transactional 사용
@RestController
@Transactional  // 절대 금지!
public class BadController { }

// ❌ 트랜잭션 내에서 외부 호출
@Transactional
public void badMethod() {
    repository.save(entity);
    externalApiClient.call();  // 네트워크 호출 - 위험!
    fileService.writeFile();   // 파일 I/O - 위험!
}

// ❌ 과도한 트랜잭션 범위
@Transactional
public void tooLargeTransaction() {
    // 수십 개의 DB 연산과 복잡한 비즈니스 로직
    // -> 트랜잭션을 작은 단위로 분할 필요
}
```

### 2. 예외 처리 주의점
```java
@Transactional
public void rollbackScenarios() {
    try {
        // 비즈니스 로직
        repository.save(entity);
    } catch (RuntimeException e) {
        // RuntimeException: 자동 롤백
        throw e;
    } catch (CheckedException e) {
        // CheckedException: 롤백되지 않음!
        throw new RuntimeException(e);  // RuntimeException으로 래핑 필요
    }
}

// 명시적 롤백 설정
@Transactional(rollbackFor = Exception.class)
public void rollbackForAllExceptions() {
    // 모든 예외에 대해 롤백
}
```

### 3. 중첩 트랜잭션 고려사항
```java
@Service
public class OuterService {
    @Transactional
    public void outerMethod() {
        innerService.innerMethod();  // 중첩 트랜잭션 전파
    }
}

@Service
public class InnerService {
    @Transactional(propagation = Propagation.REQUIRED)  // 기본: 기존 트랜잭션 참여
    public void innerMethod() { }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)  // 새 트랜잭션 생성
    public void independentMethod() { }
}
```

## 🧪 테스트 모범 사례

### 1. 트랜잭션 테스트
```java
@SpringBootTest
@ActiveProfiles("test")
class TransactionTest {
    
    @Test
    @Transactional
    @Rollback  // 테스트 후 롤백
    void shouldRollbackOnException() {
        // 예외 발생 시 롤백 테스트
        assertThatThrownBy(() -> service.methodThatThrows())
            .isInstanceOf(ExpectedException.class);
        
        // 데이터가 롤백되었는지 확인
        assertFalse(repository.existsById(id));
    }
    
    @Test
    @Transactional(readOnly = true)
    void shouldFailOnReadOnlyViolation() {
        // 읽기 전용 트랜잭션에서 쓰기 작업 시도 시 예외 발생 확인
        assertThatThrownBy(() -> service.writeOperation())
            .isInstanceOf(TransactionSystemException.class);
    }
}
```

### 2. 이벤트 테스트
```java
@SpringBootTest
@RecordApplicationEvents
class EventTest {
    
    @Autowired
    ApplicationEvents events;
    
    @Test
    @Transactional
    void shouldPublishEventAfterCommit() {
        service.deleteMember(memberId);
        
        // 이벤트 발행 확인
        assertThat(events.stream(MemberDeletedEvent.class)).hasSize(1);
    }
}
```

## 📈 성능 모니터링

### 1. 메트릭스 확인
```java
// 트랜잭션 성능 지표 조회
TransactionMetricsSummary metrics = transactionMetricsService.getMetricsSummary();
System.out.println("성공률: " + metrics.successRate + "%");
System.out.println("평균 실행시간: " + metrics.averageExecutionTime + "ms");
```

### 2. Health Check
```java
// 데이터베이스 상태 확인
boolean healthy = transactionMetricsService.isHealthy();
DatabaseHealthStatus status = transactionMetricsService.getDatabaseHealthStatus();
```

## 🔧 문제 해결 가이드

### 1. 자주 발생하는 문제

**문제**: LazyInitializationException
```java
// 해결: @Transactional 범위 내에서 지연 로딩 수행
@Transactional(readOnly = true)
public List<MemberDto> getMembersWithDetails() {
    return memberRepository.findAll().stream()
        .map(member -> {
            member.getSchedules().size();  // 지연 로딩 강제 실행
            return new MemberDto(member);
        })
        .collect(toList());
}
```

**문제**: Connection Pool 고갈
```properties
# HikariCP 모니터링 활성화
spring.datasource.hikari.register-mbeans=true

# 적절한 풀 크기 설정
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
```

**문제**: 데드락 발생
```java
// 해결: 일관된 리소스 접근 순서 유지
@Transactional
public void transferPoints(Long fromId, Long toId, int points) {
    // ID 순서로 정렬하여 데드락 방지
    Long firstId = Math.min(fromId, toId);
    Long secondId = Math.max(fromId, toId);
    
    Member first = memberRepository.findByIdForUpdate(firstId);
    Member second = memberRepository.findByIdForUpdate(secondId);
    
    // 포인트 이체 로직
}
```

### 2. 디버깅 팁
```properties
# 트랜잭션 디버깅 로깅 활성화
logging.level.org.springframework.transaction=DEBUG
logging.level.org.springframework.orm.jpa=DEBUG
logging.level.org.hibernate.engine.transaction=DEBUG
```

## 📚 추가 리소스

- [Spring Transaction Management 공식 문서](https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#transaction)
- [Hibernate Performance Tuning](https://hibernate.org/orm/documentation/)
- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP)

---

**주의**: 이 가이드는 Hamalog 프로젝트의 현재 아키텍처와 설정을 기반으로 작성되었습니다. 프로젝트 요구사항 변경 시 해당 내용을 업데이트해야 합니다.