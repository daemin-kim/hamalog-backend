# 09. ArchUnit 기반 아키텍처 테스트

> **코드로 아키텍처 규칙을 정의하고 자동 검증하여 레이어드 아키텍처를 강제하는 테스트 전략**

---

## 📋 목차

1. [문제 상황](#1-문제-상황-problem)
2. [해결 전략](#2-해결-전략-solution-strategy)
3. [구현 상세](#3-구현-상세-implementation)
4. [효과 및 검증](#4-효과-및-검증-results)
5. [면접 대비 Q&A](#5-면접-대비-qa)

---

## 1. 문제 상황 (Problem)

### 1.1 아키텍처 규칙 위반 문제

프로젝트가 성장하면서 아키텍처 규칙이 점점 무너집니다:

```
📊 흔한 아키텍처 위반 사례

Before (초기 설계):
┌──────────────┐
│  Controller  │ ─── "Service만 호출해야 해!"
├──────────────┤
│   Service    │ ─── "Repository만 접근해야 해!"
├──────────────┤
│  Repository  │
└──────────────┘

After (6개월 후):
┌──────────────┐
│  Controller  │ ───→ Repository 직접 호출 (❌ 위반)
│              │ ───→ 다른 Controller 의존 (❌ 위반)
├──────────────┤
│   Service    │ ←─── Repository가 Service 호출 (❌ 순환)
├──────────────┤
│  Repository  │
└──────────────┘
```

### 1.2 문서로만 정의된 규칙의 한계

| 문제 | 설명 |
|------|------|
| **인지 부하** | 개발자가 모든 규칙을 기억해야 함 |
| **코드 리뷰 의존** | 리뷰어가 놓치면 위반 코드가 머지됨 |
| **점진적 침식** | 작은 위반이 쌓여 아키텍처 붕괴 |
| **신규 멤버** | 규칙을 모르는 개발자의 실수 |

```
실제 발생 시나리오:

1. 개발자 A: "급하니까 Controller에서 Repository 직접 호출하자"
2. 코드 리뷰: 놓침 (또는 "나중에 리팩토링하자"로 머지)
3. 개발자 B: "A가 이렇게 했으니 나도 해도 되겠지"
4. 6개월 후: 레이어 경계가 무의미해짐
```

### 1.3 기존 검증 방법의 한계

| 방법 | 한계 |
|------|------|
| **코드 리뷰** | 사람이 하므로 실수 가능, 일관성 없음 |
| **SonarQube** | 아키텍처 규칙은 커스텀 필요 |
| **문서화** | 강제력 없음, 업데이트 안 됨 |
| **교육** | 일시적 효과, 반복 필요 |

---

## 2. 해결 전략 (Solution Strategy)

### 2.1 ArchUnit 도입

**ArchUnit**은 **코드로 아키텍처 규칙을 정의**하고 **JUnit으로 자동 검증**하는 라이브러리입니다.

```
┌────────────────────────────────────────────────────────────────┐
│                     ArchUnit 동작 방식                          │
│                                                                 │
│  1. 규칙 정의 (Java 코드)                                       │
│     noClasses()                                                 │
│         .that().resideInAPackage("..controller..")              │
│         .should().dependOnClassesThat()                         │
│         .resideInAPackage("..repository..")                     │
│                                                                 │
│  2. 클래스 스캔                                                 │
│     ClassFileImporter().importPackages("com.Hamalog")           │
│                                                                 │
│  3. 규칙 검증                                                   │
│     rule.check(importedClasses)                                 │
│                                                                 │
│  4. 결과                                                        │
│     ✅ 통과 또는 ❌ 위반 상세 리포트                            │
│                                                                 │
└────────────────────────────────────────────────────────────────┘
```

### 2.2 왜 ArchUnit인가?

| 특성 | 이점 |
|------|------|
| **Java 코드** | IDE 자동완성, 타입 안전성 |
| **JUnit 통합** | 기존 테스트 인프라 재활용 |
| **CI/CD 연동** | 빌드 파이프라인에서 자동 검증 |
| **즉각적 피드백** | 커밋 전 로컬에서 확인 가능 |
| **문서화 효과** | 테스트 코드가 곧 규칙 문서 |

### 2.3 검증할 규칙 카테고리

```
┌────────────────────────────────────────────────────────────────┐
│                    아키텍처 규칙 카테고리                        │
│                                                                 │
│  1. 계층형 아키텍처 규칙                                        │
│     - Controller → Repository 직접 접근 금지                    │
│     - Service → Controller 의존 금지                            │
│     - Repository → Service 의존 금지                            │
│                                                                 │
│  2. 네이밍 컨벤션 규칙                                          │
│     - Controller 클래스는 'Controller' 접미사                   │
│     - Service 클래스는 'Service' 접미사                         │
│     - Repository는 'Repository' 접미사                          │
│                                                                 │
│  3. 어노테이션 규칙                                             │
│     - Controller는 @RestController                              │
│     - Service는 @Service 또는 @Component                        │
│                                                                 │
│  4. 패키지 구조 규칙                                            │
│     - Entity는 domain 패키지에만                                │
│     - DTO는 dto 패키지에만                                      │
│                                                                 │
└────────────────────────────────────────────────────────────────┘
```

---

## 3. 구현 상세 (Implementation)

### 3.1 의존성 추가

```groovy
// build.gradle
dependencies {
    // ArchUnit - 아키텍처 테스트
    testImplementation 'com.tngtech.archunit:archunit-junit5:1.2.1'
}
```

### 3.2 계층형 아키텍처 규칙 테스트

```java
/**
 * ArchUnit을 사용한 아키텍처 규칙 테스트
 *
 * 이 테스트는 프로젝트의 아키텍처 규칙을 자동으로 검증합니다:
 * - 계층 간 의존성 규칙
 * - 네이밍 컨벤션
 * - 패키지 구조 규칙
 */
@DisplayName("아키텍처 규칙 테스트")
class ArchitectureRulesTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        // ============================================================
        // 클래스 스캔: 테스트 코드 제외하고 프로덕션 코드만
        // ============================================================
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.Hamalog");
    }

    // ============================================================
    // 계층형 아키텍처 규칙
    // ============================================================

    @Nested
    @DisplayName("계층형 아키텍처 규칙")
    class LayeredArchitectureTest {

        /**
         * Controller → Repository 직접 접근 금지
         * 
         * 이유:
         * - 비즈니스 로직이 Controller에 분산됨
         * - 트랜잭션 관리 어려움
         * - 테스트하기 어려움
         * 
         * 해결: Controller → Service → Repository
         */
        @Test
        @DisplayName("Controller는 Repository를 직접 접근하지 않아야 함")
        void controllersShouldNotAccessRepositoriesDirectly() {
            noClasses()
                    .that().resideInAPackage("..controller..")
                    .should().accessClassesThat().resideInAPackage("..repository..")
                    .because("Controller는 Service를 통해서만 데이터에 접근해야 합니다")
                    .check(importedClasses);
        }

        /**
         * Service → Controller 의존 금지
         * 
         * 이유:
         * - 순환 의존성 발생
         * - Service의 재사용성 저하
         * - 테스트 어려움
         */
        @Test
        @DisplayName("Service는 Controller에 의존하지 않아야 함")
        void servicesShouldNotDependOnControllers() {
            noClasses()
                    .that().resideInAPackage("..service..")
                    .should().dependOnClassesThat().resideInAPackage("..controller..")
                    .because("Service는 Controller에 의존하면 안됩니다 (순환 의존성 방지)")
                    .check(importedClasses);
        }

        /**
         * Repository → Service 의존 금지
         * 
         * 이유:
         * - 하위 레이어가 상위 레이어에 의존하면 안 됨
         * - Repository는 순수 데이터 접근 계층
         */
        @Test
        @DisplayName("Repository는 Service에 의존하지 않아야 함")
        void repositoriesShouldNotDependOnServices() {
            noClasses()
                    .that().resideInAPackage("..repository..")
                    .should().dependOnClassesThat().resideInAPackage("..service..")
                    .because("Repository는 Service에 의존하면 안됩니다")
                    .check(importedClasses);
        }
    }
}
```

### 3.3 네이밍 컨벤션 규칙

```java
@Nested
@DisplayName("네이밍 컨벤션 규칙")
class NamingConventionTest {

    /**
     * Controller 클래스 네이밍 규칙
     * 
     * 이유:
     * - 일관된 네이밍으로 가독성 향상
     * - 클래스 역할을 이름에서 즉시 파악
     */
    @Test
    @DisplayName("Controller 클래스는 'Controller' 접미사를 가져야 함")
    void controllersShouldHaveControllerSuffix() {
        classes()
                .that().resideInAPackage("..controller..")
                .and().areAnnotatedWith(RestController.class)
                .should().haveSimpleNameEndingWith("Controller")
                .because("REST Controller는 'Controller' 접미사를 가져야 합니다")
                .check(importedClasses);
    }

    /**
     * Service 클래스 네이밍 규칙
     */
    @Test
    @DisplayName("Service 클래스는 'Service' 접미사를 가져야 함")
    void servicesShouldHaveServiceSuffix() {
        classes()
                .that().resideInAPackage("..service..")
                .and().areAnnotatedWith(Service.class)
                .should().haveSimpleNameEndingWith("Service")
                .because("Service 클래스는 'Service' 접미사를 가져야 합니다")
                .check(importedClasses);
    }

    /**
     * Repository 인터페이스 네이밍 규칙
     */
    @Test
    @DisplayName("Repository 인터페이스는 'Repository' 접미사를 가져야 함")
    void repositoriesShouldHaveRepositorySuffix() {
        classes()
                .that().resideInAPackage("..repository..")
                .and().areInterfaces()
                .should().haveSimpleNameEndingWith("Repository")
                .because("Repository 인터페이스는 'Repository' 접미사를 가져야 합니다")
                .check(importedClasses);
    }
}
```

### 3.4 어노테이션 규칙

```java
@Nested
@DisplayName("어노테이션 규칙")
class AnnotationRulesTest {

    /**
     * Controller는 @RestController 어노테이션 필수
     * 
     * 이유:
     * - REST API 응답 자동 직렬화
     * - @Controller + @ResponseBody 조합보다 명시적
     */
    @Test
    @DisplayName("Controller는 @RestController 어노테이션을 가져야 함")
    void controllersShouldBeAnnotatedWithRestController() {
        classes()
                .that().resideInAPackage("..controller..")
                .and().haveSimpleNameEndingWith("Controller")
                .and().areNotInterfaces()
                .should().beAnnotatedWith(RestController.class)
                .because("REST API Controller는 @RestController 어노테이션이 필요합니다")
                .check(importedClasses);
    }

    /**
     * Service는 Spring Bean으로 등록 필수
     */
    @Test
    @DisplayName("Service는 @Service 또는 @Component 어노테이션을 가져야 함")
    void servicesShouldBeAnnotatedWithServiceOrComponent() {
        classes()
                .that().resideInAPackage("..service..")
                .and().haveSimpleNameEndingWith("Service")
                .and().areNotInterfaces()
                .should().beAnnotatedWith(Service.class)
                .orShould().beAnnotatedWith(Component.class)
                .because("Service 클래스는 Spring Bean으로 등록되어야 합니다")
                .check(importedClasses);
    }
}
```

### 3.5 패키지 구조 규칙

```java
@Nested
@DisplayName("패키지 구조 규칙")
class PackageStructureTest {

    /**
     * Entity는 domain 패키지에만 존재
     * 
     * 이유:
     * - 도메인 모델 집중
     * - 패키지 구조로 역할 파악
     */
    @Test
    @DisplayName("Entity 클래스는 domain 패키지에 존재해야 함")
    void entityClassesShouldResideInDomainPackage() {
        classes()
                .that().areAnnotatedWith(Entity.class)
                .should().resideInAPackage("..domain..")
                .because("JPA Entity는 domain 패키지에 있어야 합니다")
                .check(importedClasses);
    }

    /**
     * DTO는 dto 패키지에만 존재
     */
    @Test
    @DisplayName("DTO 클래스는 dto 패키지에 존재해야 함")
    void dtoClassesShouldResideInDtoPackage() {
        classes()
                .that().haveSimpleNameEndingWith("Request")
                .or().haveSimpleNameEndingWith("Response")
                .should().resideInAPackage("..dto..")
                .because("DTO는 dto 패키지에 있어야 합니다")
                .check(importedClasses);
    }
}
```

### 3.6 추가 유용한 규칙들

```java
@Nested
@DisplayName("추가 아키텍처 규칙")
class AdditionalRulesTest {

    /**
     * Entity에서 @Data 사용 금지
     * 
     * 이유:
     * - equals/hashCode 문제 (영속성 컨텍스트)
     * - 무분별한 Setter 노출
     * - toString으로 지연 로딩 문제
     */
    @Test
    @DisplayName("Entity는 Lombok @Data를 사용하지 않아야 함")
    void entitiesShouldNotUseLombokData() {
        noClasses()
                .that().areAnnotatedWith(Entity.class)
                .should().beAnnotatedWith(lombok.Data.class)
                .because("Entity에서 @Data는 equals/hashCode, Setter 문제를 유발합니다")
                .check(importedClasses);
    }

    /**
     * 순환 의존성 금지
     */
    @Test
    @DisplayName("패키지 간 순환 의존성이 없어야 함")
    void noPackageCycles() {
        slices().matching("com.Hamalog.(*)..")
                .should().beFreeOfCycles()
                .because("패키지 간 순환 의존성은 유지보수를 어렵게 합니다")
                .check(importedClasses);
    }

    /**
     * FetchType.EAGER 사용 금지
     */
    @Test
    @DisplayName("@ManyToOne, @OneToMany는 FetchType.LAZY를 사용해야 함")
    void associationsShouldUseLazyFetching() {
        // 필드에 적용된 어노테이션 검사
        fields()
                .that().areAnnotatedWith(ManyToOne.class)
                .or().areAnnotatedWith(OneToMany.class)
                .should().beAnnotatedWith(new DescribedPredicate<>("FetchType.LAZY") {
                    @Override
                    public boolean test(JavaField field) {
                        ManyToOne manyToOne = field.getAnnotationOfType(ManyToOne.class);
                        if (manyToOne != null) {
                            return manyToOne.fetch() == FetchType.LAZY;
                        }
                        OneToMany oneToMany = field.getAnnotationOfType(OneToMany.class);
                        if (oneToMany != null) {
                            return oneToMany.fetch() == FetchType.LAZY;
                        }
                        return true;
                    }
                })
                .because("N+1 문제 방지를 위해 FetchType.LAZY를 사용해야 합니다")
                .check(importedClasses);
    }
}
```

### 3.7 테스트 실행 및 결과

```bash
# 테스트 실행
./gradlew test --tests "com.Hamalog.architecture.*"

# 결과 예시 (성공)
> Task :test
ArchitectureRulesTest > LayeredArchitectureTest > Controller는 Repository를 직접 접근하지 않아야 함 PASSED
ArchitectureRulesTest > LayeredArchitectureTest > Service는 Controller에 의존하지 않아야 함 PASSED
ArchitectureRulesTest > NamingConventionTest > Controller 클래스는 'Controller' 접미사를 가져야 함 PASSED
...

BUILD SUCCESSFUL

# 결과 예시 (실패)
ArchitectureRulesTest > LayeredArchitectureTest > Controller는 Repository를 직접 접근하지 않아야 함 FAILED
    java.lang.AssertionError: Architecture Violation [Priority: MEDIUM] - 
    Rule 'no classes that reside in a package '..controller..' should access classes 
    that reside in a package '..repository..'' was violated (1 times):
    
    Method <com.Hamalog.controller.medication.MedicationScheduleController.getAll()> 
    accesses <com.Hamalog.repository.medication.MedicationScheduleRepository>
    
    in (MedicationScheduleController.java:45)
```

---

## 4. 효과 및 검증 (Results)

### 4.1 아키텍처 품질 보장

| 지표 | 효과 |
|------|------|
| **레이어 위반** | 자동 감지, 빌드 실패 |
| **네이밍 일관성** | 강제, 리뷰 부담 감소 |
| **순환 의존성** | 즉시 발견 |
| **규칙 문서화** | 테스트 코드가 곧 문서 |

### 4.2 개발 프로세스 개선

```
Before:
개발자 코드 작성 → 코드 리뷰 (규칙 위반 발견?) → 수정 요청 → 재작업

After:
개발자 코드 작성 → 로컬 테스트 실패 → 즉시 수정 → 코드 리뷰 (규칙 검증 불필요)
```

### 4.3 CI/CD 통합

```yaml
# .github/workflows/ci.yml
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
      - name: Run Architecture Tests
        run: ./gradlew test --tests "com.Hamalog.architecture.*"
```

### 4.4 규칙 카테고리별 테스트 현황

| 카테고리 | 테스트 수 | 상태 |
|----------|----------|------|
| 계층형 아키텍처 | 3개 | ✅ 통과 |
| 네이밍 컨벤션 | 3개 | ✅ 통과 |
| 어노테이션 규칙 | 2개 | ✅ 통과 |
| 패키지 구조 | 1개 | ✅ 통과 |

---

## 5. 면접 대비 Q&A

### Q1. ArchUnit을 도입한 이유는?

> **모범 답변**
> 
> 아키텍처 규칙을 **자동으로 검증**하기 위해 도입했습니다.
> 
> 기존 문제:
> - 문서로만 정의된 규칙은 강제력 없음
> - 코드 리뷰에서 놓치면 위반 코드가 머지됨
> - 시간이 지나면 아키텍처 침식
> 
> ArchUnit의 장점:
> 1. **Java 코드로 규칙 정의**: IDE 지원, 타입 안전
> 2. **JUnit 통합**: 기존 테스트 인프라 활용
> 3. **CI/CD 연동**: 빌드 시 자동 검증
> 4. **즉각적 피드백**: 로컬에서 바로 확인
> 
> 결과: 아키텍처 규칙 위반이 커밋 전에 발견됩니다.

### Q2. 어떤 아키텍처 규칙들을 검증하나요?

> **모범 답변**
> 
> 4가지 카테고리의 규칙을 검증합니다:
> 
> **1. 계층형 아키텍처**:
> - Controller → Repository 직접 접근 금지
> - 하위 레이어가 상위 레이어에 의존 금지
> 
> **2. 네이밍 컨벤션**:
> - Controller, Service, Repository 접미사 규칙
> 
> **3. 어노테이션 규칙**:
> - @RestController, @Service 필수
> 
> **4. 패키지 구조**:
> - Entity는 domain 패키지에만
> - DTO는 dto 패키지에만
> 
> 추가로 Entity에서 @Data 사용 금지, FetchType.LAZY 강제 등도 검증합니다.

### Q3. 테스트 코드가 아키텍처 문서 역할을 한다는 게 무슨 의미인가요?

> **모범 답변**
> 
> 전통적인 아키텍처 문서의 문제:
> - 코드와 동기화 안 됨
> - 업데이트를 잊음
> - 실제 구현과 괴리
> 
> ArchUnit 테스트의 장점:
> ```java
> @Test
> @DisplayName("Controller는 Repository를 직접 접근하지 않아야 함")
> void controllersShouldNotAccessRepositoriesDirectly() {
>     noClasses()
>         .that().resideInAPackage("..controller..")
>         .should().accessClassesThat().resideInAPackage("..repository..")
>         .because("Controller는 Service를 통해서만 데이터에 접근해야 합니다")
>         .check(importedClasses);
> }
> ```
> 
> - **규칙이 코드로 표현됨**: 읽기만 해도 규칙 이해
> - **항상 최신 상태**: 위반 시 테스트 실패
> - **실행 가능한 문서**: 검증까지 자동화

### Q4. 새로운 규칙을 추가할 때 기존 코드가 위반하면 어떻게 하나요?

> **모범 답변**
> 
> 단계적으로 도입합니다:
> 
> **1. 예외 처리 (임시)**:
> ```java
> noClasses()
>     .that().resideInAPackage("..controller..")
>     .and().doNotHaveFullyQualifiedName(
>         "com.Hamalog.controller.LegacyController")  // 예외
>     .should().accessClassesThat().resideInAPackage("..repository..")
>     .check(importedClasses);
> ```
> 
> **2. 기술 부채 이슈 등록**:
> - 예외 처리된 클래스들을 리팩토링 대상으로 등록
> 
> **3. 점진적 수정**:
> - 스프린트마다 일부씩 수정
> - 수정 완료 시 예외 제거
> 
> **4. 최종 상태**:
> - 모든 예외 제거, 규칙 완전 적용

### Q5. ArchUnit의 성능은 어떤가요?

> **모범 답변**
> 
> 클래스 스캔에 시간이 걸리지만 최적화 가능합니다:
> 
> **기본 성능**:
> - 약 500개 클래스 스캔: 1~2초
> - 대형 프로젝트: 5~10초
> 
> **최적화 방법**:
> 
> 1. **캐싱** (같은 클래스 재사용):
>    ```java
>    @BeforeAll
>    static void setUp() {
>        importedClasses = new ClassFileImporter()
>            .importPackages("com.Hamalog");
>    }
>    ```
> 
> 2. **범위 제한**:
>    ```java
>    // 특정 패키지만 스캔
>    importPackages("com.Hamalog.controller", "com.Hamalog.service")
>    ```
> 
> 3. **병렬 테스트 제외**:
>    - 클래스 스캔은 한 번만 하도록 설계

### Q6. ArchUnit과 SonarQube의 차이는?

> **모범 답변**
> 
> 상호 보완적입니다:
> 
> | 특성 | ArchUnit | SonarQube |
> |------|----------|-----------|
> | **역할** | 아키텍처 규칙 | 코드 품질 전반 |
> | **커스텀 규칙** | Java 코드로 쉽게 | 플러그인 개발 필요 |
> | **실행 위치** | 로컬 + CI | 주로 CI/CD |
> | **피드백 속도** | 즉각적 | 빌드 후 |
> 
> 조합 활용:
> - **ArchUnit**: 아키텍처 규칙 (레이어, 의존성)
> - **SonarQube**: 코드 스멜, 보안 취약점, 커버리지

### Q7. 레이어드 아키텍처를 강제하는 이유는?

> **모범 답변**
> 
> **관심사의 분리**를 강제하기 위해서입니다:
> 
> ```
> Controller: HTTP 요청/응답, 유효성 검사
> Service: 비즈니스 로직, 트랜잭션
> Repository: 데이터 접근
> ```
> 
> 강제하지 않으면:
> 1. 비즈니스 로직이 Controller에 분산
> 2. 트랜잭션 관리 어려움
> 3. 단위 테스트 불가
> 4. 재사용성 저하
> 
> 예시:
> ```java
> // ❌ Bad: Controller에서 Repository 직접 접근
> @GetMapping
> public List<Schedule> getAll() {
>     return scheduleRepository.findAll();  // 비즈니스 로직은?
> }
> 
> // ✅ Good: Service 통해 접근
> @GetMapping
> public List<ScheduleResponse> getAll() {
>     return scheduleService.findAll();  // Service에서 비즈니스 로직 처리
> }
> ```

### Q8. 순환 의존성 검사는 왜 중요한가요?

> **모범 답변**
> 
> 순환 의존성은 **시스템 복잡도를 기하급수적으로 증가**시킵니다:
> 
> ```
> A → B → C → A  (순환!)
> 
> 문제:
> - A를 이해하려면 B, C도 이해해야
> - B를 수정하면 A, C에 영향
> - 테스트 시 모두 목킹 필요
> - 모듈 분리 불가능
> ```
> 
> ArchUnit으로 검사:
> ```java
> @Test
> void noPackageCycles() {
>     slices().matching("com.Hamalog.(*)..")
>             .should().beFreeOfCycles()
>             .check(importedClasses);
> }
> ```
> 
> 발견 시:
> - 공통 모듈 추출
> - 인터페이스로 의존성 역전
> - 이벤트 기반 통신

### Q9. Entity에서 @Data를 금지하는 이유는?

> **모범 답변**
> 
> JPA Entity의 특성과 @Data가 충돌합니다:
> 
> **1. equals/hashCode 문제**:
> - @Data는 모든 필드로 equals 생성
> - 영속성 컨텍스트에서 동일 객체 판단 오류
> - 해결: @Id 필드만으로 equals/hashCode 정의
> 
> **2. Setter 노출**:
> - 모든 필드에 Setter 생성
> - 도메인 불변성 보장 불가
> - 해결: 비즈니스 메서드로 상태 변경
> 
> **3. toString 문제**:
> - 연관 엔티티까지 출력
> - 지연 로딩 트리거, LazyInitializationException
> - 해결: @ToString.Exclude 또는 직접 정의
> 
> 권장:
> ```java
> @Entity
> @Getter
> @NoArgsConstructor(access = AccessLevel.PROTECTED)
> public class Member {
>     // @Data 대신 필요한 것만
> }
> ```

### Q10. ArchUnit 테스트도 TDD로 작성하나요?

> **모범 답변**
> 
> **규칙 우선 접근**을 권장합니다:
> 
> **1. 규칙 먼저 정의**:
> ```java
> // 새 프로젝트 시작 시 또는 리팩토링 시
> @Test
> void controllersShouldNotAccessRepositories() {
>     // 이 규칙을 먼저 정의
> }
> ```
> 
> **2. 기존 코드 검증**:
> ```bash
> ./gradlew test
> # 위반 발견 → 수정 또는 예외 처리
> ```
> 
> **3. 새 코드 작성**:
> - 규칙을 인지한 상태로 개발
> - 위반 시 즉시 피드백
> 
> **4. 규칙 추가 시**:
> - 일단 테스트로 규칙 정의
> - 실패하면 기존 코드 검토
> - 점진적 수정
> 
> TDD와 유사하게, **규칙(테스트)이 코드를 이끕니다**.

---

## 📎 관련 문서

- [ArchitectureRulesTest.java](../../src/test/java/com/Hamalog/architecture/ArchitectureRulesTest.java)
- [CODING-CONVENTIONS.md](../internal/CODING-CONVENTIONS.md)
- [ArchUnit 공식 문서](https://www.archunit.org/userguide/html/000_Index.html)

