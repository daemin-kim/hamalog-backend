# Hamalog Development Guidelines

## Build/Configuration Instructions

### Prerequisites
- **Java 21** (OpenJDK 21 recommended)
- **Gradle** (wrapper included, use `./gradlew`)
- **Docker** (for containerized deployment)
- **Redis** (for caching and rate limiting)
- **MySQL** (for production database)

### Build Commands
```bash
# Build the project
./gradlew build

# Run locally (development)
./gradlew bootRun

# Build Docker image
docker build -t hamalog:latest .

# Run with Docker Compose (includes Redis)
docker-compose up -d
```

### Environment Configuration
The application uses profile-specific configuration:

**Development (default)**:
- Uses H2 in-memory database
- Console logging with detailed patterns
- AOP aspects enabled for debugging
- Dummy OAuth2 credentials

**Production (`prod` profile)**:
- Requires MySQL database configuration
- JSON structured logging
- Environment variables for secrets:
  ```bash
  docker run -e SPRING_PROFILES_ACTIVE=prod \
    -e SPRING_DATASOURCE_URL=jdbc:mysql://host:3306/hamalog \
    -e SPRING_DATASOURCE_USERNAME=username \
    -e SPRING_DATASOURCE_PASSWORD=password \
    -e JWT_SECRET=your-base64-secret \
    -e KAKAO_CLIENT_ID=your-client-id \
    -e KAKAO_CLIENT_SECRET=your-client-secret \
    hamalog:latest
  ```

### Key Configuration Points
- **JWT Secrets**: Must be Base64-encoded, 256-bit keys
- **Data Encryption**: Uses AES encryption for sensitive fields
- **Redis**: Required for rate limiting and token blacklisting
- **OAuth2**: Kakao integration with proper redirect URIs

## Testing Information

### Test Configuration
Tests use a specialized `application-test.properties` configuration:
- **AOP aspects disabled** to prevent interference
- **H2 in-memory database** with `create-drop` strategy
- **Dummy credentials** for JWT and OAuth2
- **Reduced logging** to minimize test noise

### Running Tests
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.Hamalog.controller.auth.AuthControllerTest"

# Run tests with detailed output
./gradlew test --info

# Generate test report
./gradlew test jacocoTestReport
```

### Test Structure and Patterns
Tests follow these conventions:

**Test Class Structure**:
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Feature Description Tests")
class FeatureTest {
    
    @Mock
    private DependencyService dependencyService;
    
    @InjectMocks
    private FeatureService featureService;
    
    @BeforeEach
    void setUp() {
        // Test setup
    }
    
    @Test
    @DisplayName("Should perform expected behavior when condition")
    void methodName_Condition_ExpectedBehavior() {
        // given
        // Test data setup
        
        // when
        // Method execution
        
        // then
        // Assertions
    }
}
```

**Key Testing Libraries**:
- **JUnit 5** for test framework
- **Mockito** for mocking dependencies
- **AssertJ** for fluent assertions
- **MockMvc** for web layer testing
- **Spring Security Test** for security testing

### Adding New Tests
1. Create test class in appropriate package under `src/test/java`
2. Use `@DisplayName` for descriptive test names
3. Follow Given-When-Then structure
4. Mock external dependencies
5. Test both success and failure scenarios
6. Include edge cases and validation tests

### Example Test Execution
The project includes a simple test demonstrating the setup:
```bash
./gradlew test --tests "com.Hamalog.demo.SimpleCalculatorTest"
```

## Development Information

### Architecture Overview
Hamalog is a Spring Boot 3.x application with:
- **Layered Architecture**: Controllers → Services → Repositories
- **AOP-driven Cross-cutting Concerns**: Logging, monitoring, caching, retry
- **Security-first Design**: JWT authentication, OAuth2, data encryption
- **Observability**: Structured logging, performance monitoring, audit trails

### Code Style and Patterns

**Package Structure**:
```
com.Hamalog/
├── aop/              # Cross-cutting concerns (logging, monitoring, etc.)
├── config/           # Configuration classes
├── controller/       # REST controllers
├── domain/           # Entity classes
├── dto/              # Data Transfer Objects
├── exception/        # Custom exceptions
├── handler/          # Exception handlers
├── logging/          # Structured logging components
├── repository/       # Data access layer
├── security/         # Security components (JWT, encryption, etc.)
└── service/          # Business logic layer
```

**Naming Conventions**:
- **Classes**: PascalCase (`UserService`, `AuthController`)
- **Methods**: camelCase with descriptive names (`authenticateUser()`)
- **Test Methods**: `methodName_Condition_ExpectedBehavior()`
- **Constants**: SCREAMING_SNAKE_CASE
- **Packages**: lowercase with dots

**Lombok Usage**:
- Use `@Slf4j` for logging
- `@Data`, `@Builder` for DTOs
- `@RequiredArgsConstructor` for dependency injection
- Avoid `@AllArgsConstructor` in entities

### AOP Configuration
The project uses extensive AOP with configurable aspects:

**Available Aspects**:
- **API Logging**: Request/response logging with performance metrics
- **Performance Monitoring**: Method execution time tracking
- **Business Audit**: User action auditing with IP/user-agent
- **Retry**: Automatic retry with exponential backoff
- **Caching**: Redis-based caching with local fallback

**Configuration Properties** (in `application.properties`):
```properties
# Global AOP control
app.aop.global-enabled=true

# Individual aspect control
app.aop.api-logging.enabled=true
app.aop.performance.enabled=true
app.aop.audit.enabled=true
app.aop.retry.enabled=true
app.aop.cache.enabled=true
```

### Security Implementation
- **JWT Authentication**: Custom token provider with blacklisting
- **OAuth2**: Kakao integration with success handlers
- **Data Encryption**: Field-level encryption for sensitive data
- **Rate Limiting**: Redis-based with configurable limits
- **Resource Ownership**: Aspect-based authorization checks

### Logging Strategy
**Development**: Human-readable console output
**Production**: JSON structured logs with metadata

**Log Categories**:
- **Application logs**: `./logs/hamalog.log`
- **Audit logs**: `./logs/audit.log`
- **Performance logs**: `./logs/performance.log`
- **Security logs**: `./logs/security.log`

**MDC Context**: Request ID, User ID, IP Address automatically tracked

### Database Patterns
- **JPA Entities** with encryption converters
- **Composite keys** using `@IdClass`
- **Soft deletes** where applicable
- **N+1 query prevention** (see test examples)

### Error Handling
- **Global exception handler** with structured error responses
- **Custom exceptions** extending `CustomException`
- **Validation** using Bean Validation annotations
- **Audit logging** of all exceptions

### Performance Considerations
- **Connection pooling** configured for production
- **Redis caching** with configurable TTL
- **AOP performance monitoring** with threshold alerts
- **Database query optimization** with JPA query methods

### Debugging Tips
1. **Enable DEBUG logging** for specific packages in development
2. **Use AOP performance monitoring** to identify slow methods
3. **Check audit logs** for user action tracking
4. **Monitor Redis cache** hit/miss ratios
5. **Review structured logs** in production for troubleshooting

### Development Workflow
1. **Feature Development**: Create feature branch from `main`
2. **Testing**: Write tests before implementing features
3. **Security Review**: Ensure all endpoints are properly secured
4. **Performance Check**: Monitor AOP performance metrics
5. **Documentation**: Update API documentation in `API-specification.md`

### Common Commands
```bash
# Start development environment
./gradlew bootRun --args='--spring.profiles.active=local'

# Run with specific logging
./gradlew bootRun --args='--logging.level.com.Hamalog=DEBUG'

# Build and run tests
./gradlew clean build

# Generate API documentation
# Access at: http://localhost:8080/swagger-ui.html

# Check application health
curl http://localhost:8080/actuator/health
```

---

*Last updated: 2025-09-10*
*Hamalog version: 0.0.1-SNAPSHOT*