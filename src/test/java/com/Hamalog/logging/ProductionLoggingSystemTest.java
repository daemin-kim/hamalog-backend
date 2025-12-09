package com.Hamalog.logging;

import com.Hamalog.config.TestRedisConfig;
import com.Hamalog.logging.business.BusinessIntelligenceLogger;
import com.Hamalog.logging.events.*;
import com.Hamalog.logging.metrics.JVMMetricsLogger;
import com.Hamalog.logging.security.SecurityEventMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Comprehensive test to verify the enhanced production-ready logging system
 * Tests all major logging components and their integration
 */
@SpringBootTest
@Import(TestRedisConfig.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@TestPropertySource(properties = {
    "app.aop.global-enabled=true",
    "app.aop.api-logging.enabled=true",
    "app.aop.performance.enabled=true",
    "app.aop.audit.enabled=true",
    "logging.level.com.Hamalog=DEBUG"
})
@DisplayName("Production Logging System Tests")
public class ProductionLoggingSystemTest {

    private StructuredLogger structuredLogger;
    private BusinessIntelligenceLogger businessLogger;
    private SecurityEventMonitor securityMonitor;
    private JVMMetricsLogger metricsLogger;

    @BeforeEach
    void setUp() {
        // Initialize components for testing
        structuredLogger = new StructuredLogger();
        businessLogger = new BusinessIntelligenceLogger();
        securityMonitor = new SecurityEventMonitor();
        metricsLogger = new JVMMetricsLogger();
        
        System.out.println("[DEBUG_LOG] Starting production logging system test");
    }

    @Test
    @DisplayName("Test Structured Logger with all event types")
    void testStructuredLogger() {
        System.out.println("[DEBUG_LOG] Testing structured logger with all event types");
        
        // Test API Event
        ApiEvent apiEvent = ApiEvent.builder()
                .httpMethod("POST")
                .path("/api/test")
                .controller("TestController")
                .action("createTest")
                .userId("test-user-123")
                .ipAddress("192.168.1.100")
                .userAgent("Test-Agent/1.0")
                .durationMs(250L)
                .statusCode(201)
                .requestType("EXTERNAL")
                .parameters(createTestParameters())
                .build();
        
        structuredLogger.api(apiEvent);
        System.out.println("[DEBUG_LOG] API event logged successfully");

        // Test Security Event
        SecurityEvent securityEvent = SecurityEvent.builder()
                .eventType("AUTHENTICATION_ATTEMPT")
                .userId("test-user-123")
                .ipAddress("192.168.1.100")
                .userAgent("Test-Agent/1.0")
                .resource("LOGIN")
                .action("AUTHENTICATE")
                .result("SUCCESS")
                .riskLevel("LOW")
                .details("Test authentication event")
                .build();
        
        structuredLogger.security(securityEvent);
        System.out.println("[DEBUG_LOG] Security event logged successfully");

        // Test Performance Event
        PerformanceEvent performanceEvent = PerformanceEvent.builder()
                .operation("TestService.performOperation")
                .durationMs(150L)
                .durationNanos(150_000_000L)
                .performanceLevel("FAST")
                .success(true)
                .userId("test-user-123")
                .methodName("performOperation")
                .className("TestService")
                .memoryBefore(100_000_000L)
                .memoryAfter(105_000_000L)
                .cpuTime(50_000_000L)
                .build();
        
        structuredLogger.performance(performanceEvent);
        System.out.println("[DEBUG_LOG] Performance event logged successfully");

        // Test Business Event
        BusinessEvent businessEvent = BusinessEvent.builder()
                .eventType("MEDICATION_CREATED")
                .entity("MEDICATION_RECORD")
                .action("CREATE")
                .userId("test-user-123")
                .result("SUCCESS")
                .metadata(createBusinessMetadata())
                .build();
        
        structuredLogger.business(businessEvent);
        System.out.println("[DEBUG_LOG] Business event logged successfully");

        // Test Audit Event
        AuditEvent auditEvent = AuditEvent.builder()
                .operation("CREATE_MEDICATION")
                .entityType("MEDICATION_RECORD")
                .entityId("med-123")
                .userId("test-user-123")
                .ipAddress("192.168.1.100")
                .userAgent("Test-Agent/1.0")
                .status("SUCCESS")
                .details("Test medication record creation")
                .build();
        
        structuredLogger.audit(auditEvent);
        System.out.println("[DEBUG_LOG] Audit event logged successfully");
    }

    @Test
    @DisplayName("Test MDC Utility functions")
    void testMDCUtil() {
        System.out.println("[DEBUG_LOG] Testing MDC utility functions");
        
        // Test MDC initialization
        MDCUtil.initializeRequestContext();
        System.out.println("[DEBUG_LOG] Request context initialized");
        
        // Test business context
        MDCUtil.initializeBusinessContext("CREATE", "createMedication", "MEDICATION", "med-123");
        System.out.println("[DEBUG_LOG] Business context initialized");
        
        // Test performance context
        MDCUtil.initializePerformanceContext("TestOperation");
        System.out.println("[DEBUG_LOG] Performance context initialized");
        
        // Test error context
        Exception testException = new RuntimeException("Test exception for logging");
        MDCUtil.addErrorContext(testException);
        System.out.println("[DEBUG_LOG] Error context added");
        
        // Test custom fields
        MDCUtil.put("custom_field", "test_value");
        MDCUtil.put("numeric_field", 12345);
        System.out.println("[DEBUG_LOG] Custom fields added");
        
        // Verify context retrieval
        String correlationId = MDCUtil.get(MDCUtil.CORRELATION_ID);
        String operationType = MDCUtil.get(MDCUtil.OPERATION_TYPE);
        String customValue = MDCUtil.get("custom_field");
        
        System.out.println("[DEBUG_LOG] MDC values retrieved - Correlation: " + correlationId + 
                          ", Operation: " + operationType + ", Custom: " + customValue);
        
        // Test context clearing
        MDCUtil.clearRequestContext();
        MDCUtil.clearBusinessContext();
        MDCUtil.clearPerformanceContext();
        MDCUtil.clearErrorContext();
        System.out.println("[DEBUG_LOG] MDC contexts cleared");
    }

    @Test
    @DisplayName("Test Business Intelligence Logger")
    void testBusinessIntelligenceLogger() {
        System.out.println("[DEBUG_LOG] Testing business intelligence logger");
        
        // Test user registration tracking
        businessLogger.trackUserRegistration("test-user-456", "EMAIL");
        System.out.println("[DEBUG_LOG] User registration tracked");
        
        // Test user login tracking
        businessLogger.trackUserLogin("test-user-456", "PASSWORD", true);
        System.out.println("[DEBUG_LOG] User login tracked");
        
        // Test medication record creation
        businessLogger.trackMedicationRecordCreated("test-user-456", "Aspirin", "DOSAGE");
        System.out.println("[DEBUG_LOG] Medication record creation tracked");
        
        // Test medication schedule creation
        businessLogger.trackMedicationScheduleCreated("test-user-456", "Aspirin", 2);
        System.out.println("[DEBUG_LOG] Medication schedule creation tracked");
        
        // Test side effect reporting
        businessLogger.trackSideEffectReported("test-user-456", "Aspirin", "MILD");
        System.out.println("[DEBUG_LOG] Side effect reporting tracked");
        
        // Test API usage tracking
        businessLogger.trackApiUsage("/api/medications", "POST", 201, 180L);
        System.out.println("[DEBUG_LOG] API usage tracked");
        
        // Test feature usage tracking
        Map<String, Object> featureData = new HashMap<>();
        featureData.put("feature_version", "2.1");
        featureData.put("interaction_type", "click");
        businessLogger.trackFeatureUsage("medication_reminder", "test-user-456", featureData);
        System.out.println("[DEBUG_LOG] Feature usage tracked");
    }

    @Test
    @DisplayName("Test Error Handling and Structured Logging")
    void testErrorHandling() {
        System.out.println("[DEBUG_LOG] Testing error handling and structured logging");
        
        try {
            // Simulate different types of errors
            throw new IllegalArgumentException("Test validation error for logging system");
        } catch (Exception e) {
            Map<String, Object> errorContext = new HashMap<>();
            errorContext.put("test_scenario", "validation_error");
            errorContext.put("error_code", "INVALID_INPUT");
            errorContext.put("user_id", "test-user-789");
            
            structuredLogger.error("Test error logging", e, errorContext);
            System.out.println("[DEBUG_LOG] Validation error logged with structured context");
        }
        
        try {
            // Simulate security-related error
            throw new SecurityException("Test security error for logging system");
        } catch (Exception e) {
            Map<String, Object> errorContext = new HashMap<>();
            errorContext.put("test_scenario", "security_error");
            errorContext.put("error_code", "ACCESS_DENIED");
            errorContext.put("user_id", "test-user-789");
            errorContext.put("resource", "/api/admin/users");
            
            structuredLogger.error("Test security error logging", e, errorContext);
            System.out.println("[DEBUG_LOG] Security error logged with structured context");
        }
    }

    @Test
    @DisplayName("Test Logging Levels and Filtering")
    void testLoggingLevelsAndFiltering() {
        System.out.println("[DEBUG_LOG] Testing logging levels and filtering");
        
        // Test different log levels with structured events
        
        // INFO level - normal operations
        ApiEvent infoEvent = createTestApiEvent("INFO", 200, 100L);
        structuredLogger.api(infoEvent);
        System.out.println("[DEBUG_LOG] INFO level API event logged");
        
        // WARN level - slow operation
        ApiEvent warnEvent = createTestApiEvent("WARN", 200, 2500L);
        structuredLogger.api(warnEvent);
        System.out.println("[DEBUG_LOG] WARN level API event logged (slow operation)");
        
        // ERROR level - failed operation
        ApiEvent errorEvent = createTestApiEvent("ERROR", 500, 1000L);
        structuredLogger.api(errorEvent);
        System.out.println("[DEBUG_LOG] ERROR level API event logged (failed operation)");
        
        // Test performance levels
        PerformanceEvent fastPerfEvent = createTestPerformanceEvent("VERY_FAST", 50L, true);
        structuredLogger.performance(fastPerfEvent);
        System.out.println("[DEBUG_LOG] VERY_FAST performance event logged");
        
        PerformanceEvent slowPerfEvent = createTestPerformanceEvent("VERY_SLOW", 5000L, true);
        structuredLogger.performance(slowPerfEvent);
        System.out.println("[DEBUG_LOG] VERY_SLOW performance event logged");
        
        // Test security risk levels
        SecurityEvent lowRiskEvent = createTestSecurityEvent("LOW", "AUTHENTICATION_SUCCESS");
        structuredLogger.security(lowRiskEvent);
        System.out.println("[DEBUG_LOG] LOW risk security event logged");
        
        SecurityEvent highRiskEvent = createTestSecurityEvent("HIGH", "AUTHENTICATION_FAILURE");
        structuredLogger.security(highRiskEvent);
        System.out.println("[DEBUG_LOG] HIGH risk security event logged");
    }

    @Test
    @DisplayName("Test Production Logging Integration")
    void testProductionLoggingIntegration() {
        System.out.println("[DEBUG_LOG] Testing production logging integration");
        
        // Simulate a complete user flow with comprehensive logging
        String userId = "integration-test-user-999";
        String correlationId = "test-correlation-" + System.currentTimeMillis();
        
        // Initialize request context
        MDCUtil.put(MDCUtil.CORRELATION_ID, correlationId);
        MDCUtil.put(MDCUtil.USER_ID, userId);
        MDCUtil.put(MDCUtil.IP_ADDRESS, "10.0.0.1");
        MDCUtil.put(MDCUtil.USER_AGENT, "Integration-Test-Agent/1.0");
        
        System.out.println("[DEBUG_LOG] Integration test context initialized with correlationId: " + correlationId);
        
        // 1. User registration
        businessLogger.trackUserRegistration(userId, "INTEGRATION_TEST");
        
        // 2. User login
        businessLogger.trackUserLogin(userId, "PASSWORD", true);
        
        // 3. API usage
        businessLogger.trackApiUsage("/api/medications", "GET", 200, 150L);
        
        // 4. Feature usage
        Map<String, Object> featureMetadata = new HashMap<>();
        featureMetadata.put("test_run", "integration");
        featureMetadata.put("timestamp", System.currentTimeMillis());
        businessLogger.trackFeatureUsage("medication_list_view", userId, featureMetadata);
        
        // 5. Business operation
        businessLogger.trackMedicationRecordCreated(userId, "Test Medication", "INTEGRATION_TEST");
        
        System.out.println("[DEBUG_LOG] Integration test completed successfully with correlationId: " + correlationId);
        
        // Clear context
        MDCUtil.clearAllContext();
    }

    // Helper methods for creating test events
    private Map<String, Object> createTestParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("medication_name", "Test Medication");
        params.put("dosage", "10mg");
        params.put("frequency", "twice daily");
        return params;
    }

    private Map<String, Object> createBusinessMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("medication_type", "prescription");
        metadata.put("created_via", "mobile_app");
        metadata.put("version", "2.1.0");
        return metadata;
    }

    private ApiEvent createTestApiEvent(String level, int statusCode, long duration) {
        return ApiEvent.builder()
                .httpMethod("GET")
                .path("/api/test/" + level.toLowerCase())
                .controller("TestController")
                .action("test" + level)
                .userId("test-user-" + level)
                .ipAddress("192.168.1." + statusCode)
                .userAgent("Test-Agent/1.0")
                .durationMs(duration)
                .statusCode(statusCode)
                .requestType("EXTERNAL")
                .parameters(createTestParameters())
                .build();
    }

    private PerformanceEvent createTestPerformanceEvent(String level, long duration, boolean success) {
        return PerformanceEvent.builder()
                .operation("TestService.test" + level)
                .durationMs(duration)
                .durationNanos(duration * 1_000_000L)
                .performanceLevel(level)
                .success(success)
                .userId("test-user-performance")
                .methodName("test" + level)
                .className("TestService")
                .memoryBefore(50_000_000L)
                .memoryAfter(55_000_000L)
                .cpuTime(duration * 500_000L)
                .build();
    }

    private SecurityEvent createTestSecurityEvent(String riskLevel, String eventType) {
        return SecurityEvent.builder()
                .eventType(eventType)
                .userId("test-user-security")
                .ipAddress("192.168.1.200")
                .userAgent("Test-Security-Agent/1.0")
                .resource("TEST_RESOURCE")
                .action("TEST_ACTION")
                .result(riskLevel.equals("LOW") ? "SUCCESS" : "FAILURE")
                .riskLevel(riskLevel)
                .details("Test security event with " + riskLevel + " risk level")
                .build();
    }
}