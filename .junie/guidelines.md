# Hamalog Development Guidelines

## Project Overview
Hamalog is a Spring Boot 3.4.5 medication management application with JWT authentication, OAuth2 integration (Kakao), and comprehensive side effect tracking capabilities.

## Build & Configuration

### Prerequisites
- Java 21 (toolchain configured in build.gradle)
- Gradle 8.13+ (wrapper included)
- MySQL database (or H2 for testing)

### Build Instructions

#### Local Development Build
```bash
# Clean and build
./gradlew clean build

# Run the application
./gradlew bootRun

# Build JAR only
./gradlew bootJar
```

#### Docker Build
```bash
# Build the application JAR first
./gradlew bootJar

# Build Docker image
docker build -t hamalog .

# Run container
docker run -p 8080:8080 hamalog
```

### Environment Configuration

#### Required Environment Variables
The application uses externalized configuration for sensitive values:

```bash
# Database Configuration
export DB_HOST=localhost          # Default: db
export DB_PORT=3306              # Default: 3306
export DB_NAME=hamalog           # Default: hamalog
export DB_USERNAME=your_user     # Default: user
export DB_PASSWORD=your_password # Default: password

# JWT Configuration (REQUIRED)
export JWT_SECRET=$(openssl rand -base64 32)  # Generate secure 256-bit key
export JWT_EXPIRY=3600000        # Default: 1 hour

# OAuth2 Kakao Integration
export KAKAO_CLIENT_ID=your_kakao_client_id
export KAKAO_CLIENT_SECRET=your_kakao_client_secret
```

#### Application Properties Highlights
- **Database**: MySQL with fallback to H2 for testing
- **File Upload**: `/data/hamalog/images` (configurable via `hamalog.upload.image-dir`)
- **Logging**: Structured logging with request ID tracking via MDC
- **Security**: JWT + OAuth2 (Kakao) authentication

## Testing

### Running Tests

#### All Tests
```bash
# Run all tests
./gradlew test

# Run with detailed output
./gradlew test --info
```

#### Specific Test Classes
```bash
# Run specific test class
./gradlew test --tests "com.Hamalog.service.sideEffect.SideEffectServiceTest"

# Run specific test method
./gradlew test --tests "com.Hamalog.service.sideEffect.SideEffectServiceTest.getRecentSideEffects_Success"
```

### Test Structure & Best Practices

#### Unit Testing Example
The project follows standard Spring Boot testing practices with JUnit 5 and Mockito:

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Service Tests")
class ServiceTest {
    
    @Mock
    private Repository repository;
    
    @InjectMocks
    private Service service;
    
    @Test
    @DisplayName("Test description")
    void testMethod() {
        // Given
        given(repository.method()).willReturn(expectedValue);
        
        // When
        Result result = service.method();
        
        // Then
        assertThat(result).isEqualTo(expected);
        verify(repository).method();
    }
}
```

#### Testing Dependencies
- **JUnit 5**: Primary testing framework
- **Mockito**: Mocking framework with BDD style (`given().willReturn()`)
- **Spring Boot Test**: Integration testing support
- **Spring Security Test**: Security-specific testing utilities

#### Domain Object Testing
When testing with domain objects (e.g., `Member`), use the Builder pattern:

```java
Member member = Member.builder()
    .loginId("testuser")
    .password("password")
    .name("Test User")
    .phoneNumber("010-1234-5678")
    .birth(LocalDate.of(1990, 1, 1))
    .createdAt(LocalDateTime.now())
    .build();
```

## Development Information

### Architecture Patterns

#### Project Structure
```
src/main/java/com/Hamalog/
├── controller/          # REST controllers with proper validation
├── service/            # Business logic layer
├── repository/         # Data access layer (Spring Data JPA)
├── domain/             # Entity classes with Lombok
├── dto/                # Request/Response DTOs
├── security/           # Security configuration & JWT handling
├── config/             # Spring configuration classes
├── exception/          # Custom exceptions and global error handling
└── aop/                # Aspect-oriented programming (logging)
```

#### Key Design Patterns
- **Repository Pattern**: Spring Data JPA repositories
- **DTO Pattern**: Separate request/response objects
- **Builder Pattern**: Lombok @Builder for entity creation
- **AOP**: Cross-cutting concerns (logging, security)

### Code Style & Conventions

#### Lombok Usage
The project heavily uses Lombok annotations:
- `@Entity @Getter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor` for entities
- `@Service @Transactional(readOnly = true)` for services
- `@RequiredArgsConstructor` for dependency injection

#### Record Usage
DTOs use Java records for immutability:
```java
public record RecentSideEffectResponse(List<String> recentSideEffect) {}
```

#### Validation
- Bean Validation with custom messages in `ValidationMessages.properties`
- Controller-level validation with `@Valid`

#### Exception Handling
- Global exception handling via `@RestControllerAdvice`
- Custom exceptions extending base `CustomException`
- Structured error responses with `ErrorCode` enum

### Security Configuration

#### Authentication Flow
1. JWT token-based authentication
2. OAuth2 integration with Kakao
3. Token blacklist service for logout functionality
4. Custom UserDetailsService for user loading

#### Key Security Components
- `JwtAuthenticationFilter`: Token validation
- `JwtTokenProvider`: Token generation/validation
- `TokenBlacklistService`: Token revocation
- `OAuth2AuthenticationSuccessHandler`: OAuth2 success handling

### Database Considerations

#### JPA Configuration
- **Show SQL**: Disabled in production (`spring.jpa.show-sql=false`)
- **Hibernate Logging**: Configurable logging levels
- **Connection Pool**: Default HikariCP configuration

#### Entity Design
- Use `@EqualsAndHashCode(of = "id")` for entities
- Composite keys with `@IdClass` pattern
- Proper `@ToString(exclude = "sensitiveField")` usage

### Monitoring & Logging

#### Request Tracking
- Custom `RequestLoggingFilter` with request ID generation
- MDC (Mapped Diagnostic Context) for request correlation
- Structured console logging with request IDs

#### AOP Logging
- `ApiLoggingAspect` for method-level logging
- Performance monitoring capabilities

### Development Tips

#### Common Issues
1. **JWT Secret**: Must be properly configured via environment variable
2. **Database Connection**: Ensure MySQL is running or use H2 profile for testing
3. **File Upload**: Verify upload directory permissions (`/data/hamalog/images`)
4. **OAuth2**: Kakao client credentials required for OAuth2 functionality

#### Debugging
- Enable SQL logging: Set `spring.jpa.show-sql=true` and logging levels to `debug`
- Use request ID correlation in logs for request tracing
- JWT token debugging via token blacklist service logs

## Additional Notes

- The application supports both H2 (development) and MySQL (production) databases
- Docker Compose is disabled by default (`spring.docker.compose.enabled=false`)
- OpenAPI documentation available via SpringDoc OpenAPI
- Deprecation warnings in `JwtTokenProvider.java` should be addressed in future updates