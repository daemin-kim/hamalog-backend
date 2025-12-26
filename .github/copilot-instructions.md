# Hamalog - GitHub Copilot Instructions
- ❌ 비즈니스 로직 Controller에 작성 금지
- ❌ 직접 SQL 대신 JPA 쿼리 메서드 사용
- ❌ `FetchType.EAGER` 사용 금지
- ❌ Lombok `@Data` 사용 금지 (Entity에서)
## 금지 사항

- Test: `src/test/java/com/Hamalog/` (동일 구조)
- Repository: `src/main/java/com/Hamalog/repository/{도메인}/`
- Controller: `src/main/java/com/Hamalog/controller/{도메인}/`
- Service: `src/main/java/com/Hamalog/service/{도메인}/`
- DTO: `src/main/java/com/Hamalog/dto/{도메인}/request/`, `.../response/`
- Entity: `src/main/java/com/Hamalog/domain/{도메인}/`
## 파일 위치

```
@CacheEvict(value = "cacheKey", key = "#id")
@Cacheable(value = "cacheKey", key = "#id")
```java
### 캐싱


```
@RequireResourceOwnership(resourceType = "MEDICATION_SCHEDULE", idParam = "id")
```java
### 리소스 소유권 검증

## 주요 패턴

```
}
    }
        }
            verify(xxxRepository).save(any());
            assertThat(result.id()).isNotNull();
            // then
            
            XxxResponse result = xxxService.create(request);
            // when
            
            when(xxxRepository.save(any())).thenReturn(entity);
            XxxRequest request = new XxxRequest(...);
            // given
        void success() {
        @DisplayName("성공: 유효한 요청")
        @Test
    class Create {
    @DisplayName("생성")
    @Nested
    
    private XxxService xxxService;
    @InjectMocks
    
    private XxxRepository xxxRepository;
    @Mock
    
class XxxServiceTest {
@ExtendWith(MockitoExtension.class)
@DisplayName("XXX 서비스 테스트")
```java
### 테스트 생성

```
XXX_NOT_FOUND(HttpStatus.NOT_FOUND, "X001", "리소스를 찾을 수 없습니다"),
// 새 에러 코드 추가 시 ErrorCode enum에 추가

throw ErrorCode.FORBIDDEN.toException();
throw ErrorCode.MEMBER_NOT_FOUND.toException();
// 기존 ErrorCode 사용
```java
### 에러 처리

```
}
    return ResponseEntity.ok(xxxService.findById(id));
public ResponseEntity<XxxResponse> getById(@PathVariable Long id) {
@RequireResourceOwnership(resourceType = "XXX", idParam = "id")
@GetMapping("/{id}")

}
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
    XxxResponse response = xxxService.create(request);
public ResponseEntity<XxxResponse> create(@Valid @RequestBody XxxRequest request) {
@PostMapping
```java
### Controller 메서드 생성

```
}
    return XxxResponse.from(saved);
    // 4. 응답 변환
    
    Xxx saved = xxxRepository.save(entity);
    // 3. 저장
    
    Xxx entity = new Xxx(request.name(), member);
    // 2. 엔티티 생성
    
        .orElseThrow(ErrorCode.MEMBER_NOT_FOUND::toException);
    Member member = memberRepository.findById(request.memberId())
    // 1. 유효성 검증
public XxxResponse createXxx(XxxRequest request) {
@Transactional
```java
### Service 메서드 생성

```
}
    }
        this.member = member;
        this.name = name;
    public Xxx(String name, Member member) {
    
    private Member member;
    @JoinColumn(name = "member_id")
    @ManyToOne(fetch = FetchType.LAZY)
    
    private Long version;
    @Version
    
    private String name;
    @Column(nullable = false, length = 100)
    
    private Long xxxId;
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
public class Xxx {
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "xxx")
@Entity
```java
### Entity 생성

```
}
    }
        );
            entity.getCreatedAt()
            entity.getName(),
            entity.getId(),
        return new XxxResponse(
    public static XxxResponse from(Xxx entity) {
) {
    LocalDateTime createdAt
    String name,
    Long id,
public record XxxResponse(

) {}
    @Size(max = 100) String description
    @NotNull Long fieldName,
public record XxxRequest(
```java
### DTO 생성

## 코드 생성 규칙

- Redis 7, Flyway, Docker
- Spring Security + JWT (jjwt 0.12.6)
- Java 21, Spring Boot 3.4.5, Spring Data JPA, MySQL 8.0
## 기술 스택

- **클래스명**: 영어 PascalCase
- **변수/메서드명**: 영어 camelCase
- **커밋 메시지**: 한글 (Conventional Commits 형식)
- **코드 주석**: 한글
## 언어 규칙

복약 스케줄 관리, 마음 일기, 부작용 기록 기능을 제공합니다.
Hamalog는 Spring Boot 3.4.5 기반의 헬스케어 백엔드 시스템입니다.
## 프로젝트 개요

## 📚 필독 문서
코드 생성 전 반드시 참고해야 할 문서입니다:
- `docs/internal/patterns/ANNOTATION-GUIDE.md` - 커스텀 어노테이션 사용법
- `docs/internal/patterns/ERROR-HANDLING.md` - 에러 처리 패턴
- `docs/internal/patterns/SECURITY-PATTERNS.md` - 보안 패턴
- `docs/internal/patterns/CACHING-PATTERNS.md` - 캐싱 패턴
- `docs/internal/CODING-CONVENTIONS.md` - 코딩 컨벤션

## 🛠️ 도구
- CRUD 스캐폴딩: `./scripts/generate-crud.sh --domain <Name> --fields "<fields>"`
- 코드 포맷팅: `./gradlew spotlessApply`
- 테스트 실행: `./gradlew test`
- CHANGELOG: `git-cliff -o CHANGELOG.md`


