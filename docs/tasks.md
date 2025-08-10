# Hamalog Improvement Tasks Checklist

Note: Check off tasks as you complete them. Tasks are ordered from foundational architecture/security to feature-level refinements, performance, observability, testing, and documentation.

1. [ ] Establish environment configuration and secrets management
   - [ ] Externalize secrets (JWT secret, OAuth keys) using environment variables or a secrets manager; remove hard-coded defaults from application.properties
   - [ ] Introduce Spring profiles (local, dev, prod, test) with profile-specific properties and secure defaults
   - [ ] Add sample configuration file application-example.properties with placeholders and guidance
   - [ ] Validate presence/strength of required env vars at startup (fail-fast with clear error)

2. [ ] Define API architecture and authentication strategy
   - [ ] Decide between pure stateless JWT-based API vs. formLogin + sessions; simplify SecurityConfig accordingly
   - [ ] If stateless API: remove formLogin, set session management to stateless, and clarify OAuth2 flow if retained
   - [ ] Document authentication flow in README and align all clients

3. [ ] Harden JWT configuration and token lifecycle
   - [ ] Enforce minimum 256-bit Base64-encoded secret and rotate via configuration
   - [ ] Add refresh token flow and rotation policy (if required by product)
   - [ ] Implement token blacklist or short access token with refresh to support logout/invalidation
   - [ ] Add clock skew handling in JwtTokenProvider and centralize token properties

4. [ ] Improve request validation across DTOs
   - [ ] Add javax/jakarta validation annotations (@NotNull, @NotBlank, @Size, @Pattern, @PastOrPresent, etc.) to request DTOs
   - [ ] Add @Valid to controller method parameters and handle MethodArgumentNotValidException in GlobalExceptionHandler
   - [ ] Normalize date/time handling: use typed fields (LocalDate/LocalDateTime) in all requests with consistent JSON format via @JsonFormat or global ObjectMapper settings

5. [ ] Standardize API error responses and status codes
   - [ ] Enhance GlobalExceptionHandler to map specific domain exceptions to appropriate HTTP status (404, 400, 409, 401/403)
   - [ ] Return consistent error schema (timestamp, path, code, message, details/violations)
   - [ ] Add localization-ready error messages or stable error codes for clients

6. [ ] Secure and minimize logging
   - [ ] Update ApiLoggingAspect to avoid logging sensitive data (passwords, tokens, PII) and large payloads
   - [ ] Mask/omit headers like Authorization; limit result logging to summaries (e.g., id/count) rather than full objects
   - [ ] Add correlation ID (MDC) per request for traceability; log requestId across logs

7. [ ] Refine controller and API design
   - [ ] Add pagination and filtering to list endpoints (e.g., GET /medication-schedule/list/{member-id}) using Pageable
   - [ ] Align URL naming and REST semantics (e.g., consider /members/{id}/medication-schedules)
   - [ ] Return DTOs instead of entities from controllers to decouple persistence from API
   - [ ] Consistently use ResponseEntity with proper status codes and Location header on create

8. [ ] Service and domain consistency
   - [ ] Fix MedicationScheduleService.update to preserve or update image path correctly instead of overwriting with empty string
   - [ ] Unify date handling in create/update (avoid String parsing in service; parse/validate at DTO level)
   - [ ] Validate enum conversions (AlarmType) and handle invalid values gracefully with clear error messages
   - [ ] Consider transactional boundaries: readOnly on queries, explicit propagation where necessary

9. [ ] Repository and database considerations
   - [ ] Add indexes for frequently queried fields (e.g., memberId on MedicationSchedule)
   - [ ] Evaluate fetch strategies to prevent N+1 (use fetch joins or entity graphs as needed)
   - [ ] Introduce optimistic locking (@Version) where concurrent updates are possible
   - [ ] Consider pagination-friendly repository methods instead of returning full lists

10. [ ] File storage robustness and security
    - [ ] Validate uploaded file content type and size limits; reject unsupported types
    - [ ] Store and return normalized path/URL; avoid exposing filesystem paths in API
    - [ ] Sanitize original filenames and avoid directory traversal; store metadata if needed
    - [ ] Abstract storage to allow switching to S3 or cloud storage via interface and profile
    - [ ] Add cleanup strategy for orphaned files when schedules are deleted/updated

11. [ ] Security policy hardening
    - [ ] Tighten SecurityConfig requestMatchers to explicitly allow only necessary public endpoints (e.g., /auth/**, /swagger-ui/**, /v3/api-docs/**)
    - [ ] Add CORS configuration for allowed origins/methods/headers
    - [ ] Add rate limiting (e.g., Bucket4j/Spring Cloud Gateway or at controller) for sensitive endpoints
    - [ ] Validate authentication principal types and authorities consistently

12. [ ] Observability and monitoring
    - [ ] Define log levels and categories; reduce Hibernate SQL logs for prod
    - [ ] Add application metrics (Micrometer/Prometheus) and health checks (Spring Boot Actuator)
    - [ ] Add structured logging (JSON) support via profile for centralized logging

13. [ ] API documentation and discoverability
    - [ ] Ensure OpenAPI/Swagger annotations cover all endpoints and DTO schemas
    - [ ] Expose Swagger UI and OpenAPI JSON under /v3/api-docs and secure appropriately
    - [ ] Generate API contract automatically in CI and publish artifact

14. [ ] Testing improvements
    - [ ] Add unit tests for JwtTokenProvider (create/validate/claims, expiry, skew)
    - [ ] Add controller slice tests with MockMvc including validation errors and security filters
    - [ ] Add service tests for MedicationScheduleService (create/update/delete image handling, enum parsing)
    - [ ] Add repository tests for queries and pagination
    - [ ] Add integration tests with Testcontainers for DB and optional storage emulation
    - [ ] Add AOP logging tests to ensure masking and performance impact bounds

15. [ ] Performance and scalability
    - [ ] Introduce caching where appropriate (e.g., reference data), with eviction strategies
    - [ ] Review DTO sizes and avoid returning large graphs; use projections where applicable
    - [ ] Add async handling or scheduling for heavy tasks (e.g., image processing) if needed

16. [ ] Data migration and schema management
    - [ ] Introduce Flyway or Liquibase for versioned DB migrations
    - [ ] Create baseline migration scripts and integrate into build

17. [ ] Developer experience and CI/CD
    - [ ] Add Makefile/Gradle tasks for common operations (test, verify, run profiles)
    - [ ] Set up CI pipeline (build, test, static analysis, coverage threshold)
    - [ ] Add code quality tools (SpotBugs/PMD/Checkstyle) and style guides
    - [ ] Enforce conventional commits and changelog generation

18. [ ] Consistency and utilities
    - [ ] Introduce a common API response wrapper (optional) or ensure consistent patterns across controllers
    - [ ] Introduce mapping layer (e.g., MapStruct) for DTO↔entity conversions
    - [ ] Add utility for parsing/formatting dates and central ObjectMapper config

19. [ ] Internationalization (i18n) readiness
    - [ ] Externalize user-facing messages to message bundles
    - [ ] Ensure error messages can be localized via Accept-Language

20. [ ] Security testing and audits
    - [ ] Add dependency vulnerability scanning (OWASP Dependency-Check or Snyk)
    - [ ] Add basic static security tests (e.g., Spring Security test utilities) and fuzz tests for inputs
    - [ ] Review and harden headers (HSTS, X-Content-Type-Options, X-Frame-Options) via Spring Security config

21. [ ] Cleanup and housekeeping
    - [ ] Remove unused endpoints, code paths, and dead comments
    - [ ] Add package-by-feature structure if desirable for growth (e.g., feature modules: medication, sideEffect, auth)
    - [ ] Document domain model relationships and lifecycle of key aggregates
