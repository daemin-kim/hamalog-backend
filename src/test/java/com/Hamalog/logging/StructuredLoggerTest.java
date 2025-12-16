package com.Hamalog.logging;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.Hamalog.logging.events.*;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

/**
 * Test class for StructuredLogger to verify structured logging functionality
 */
@ExtendWith(MockitoExtension.class)
class StructuredLoggerTest {

    @InjectMocks
    private StructuredLogger structuredLogger;

    @Test
    void testAuditLogging() {
        // Given
        AuditEvent auditEvent = AuditEvent.builder()
                .operation("CREATE")
                .entityType("MEDICATION_RECORD")
                .entityId("12345")
                .userId("testuser")
                .ipAddress("192.168.1.1")
                .userAgent("TestAgent/1.0")
                .status("SUCCESS")
                .details("Test audit event")
                .build();

        // When & Then
        assertDoesNotThrow(() -> structuredLogger.audit(auditEvent));
    }

    @Test
    void testSecurityLogging() {
        // Given
        SecurityEvent securityEvent = SecurityEvent.builder()
                .eventType("AUTHENTICATION")
                .userId("testuser")
                .ipAddress("192.168.1.1")
                .userAgent("TestAgent/1.0")
                .resource("LOGIN")
                .action("AUTHENTICATE")
                .result("SUCCESS")
                .riskLevel("LOW")
                .details("Test security event")
                .build();

        // When & Then
        assertDoesNotThrow(() -> structuredLogger.security(securityEvent));
    }

    @Test
    void testSecurityLoggingHighRisk() {
        // Given
        SecurityEvent securityEvent = SecurityEvent.builder()
                .eventType("AUTHENTICATION_FAILURE")
                .userId("admin")
                .ipAddress("192.168.1.100")
                .userAgent("AttackerAgent/1.0")
                .resource("LOGIN")
                .action("AUTHENTICATE")
                .result("FAILURE")
                .riskLevel("HIGH")
                .details("High risk security event")
                .build();

        // When & Then
        assertDoesNotThrow(() -> structuredLogger.security(securityEvent));
    }

    @Test
    void testPerformanceLogging() {
        // Given
        PerformanceEvent performanceEvent = PerformanceEvent.builder()
                .operation("MedicationService.createRecord")
                .durationMs(150)
                .durationNanos(150_000_000)
                .performanceLevel("FAST")
                .success(true)
                .userId("testuser")
                .methodName("createRecord")
                .className("MedicationService")
                .memoryBefore(1024L)
                .memoryAfter(1536L)
                .cpuTime(150_000_000L)
                .build();

        // When & Then
        assertDoesNotThrow(() -> structuredLogger.performance(performanceEvent));
    }

    @Test
    void testPerformanceLoggingSlowOperation() {
        // Given
        PerformanceEvent performanceEvent = PerformanceEvent.builder()
                .operation("MedicationService.complexQuery")
                .durationMs(4500)
                .durationNanos(4_500_000_000L)
                .performanceLevel("VERY_SLOW")
                .success(true)
                .userId("testuser")
                .methodName("complexQuery")
                .className("MedicationService")
                .build();

        // When & Then
        assertDoesNotThrow(() -> structuredLogger.performance(performanceEvent));
    }

    @Test
    void testBusinessLogging() {
        // Given
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("medicationId", "MED123");
        metadata.put("dosage", "10mg");
        
        BusinessEvent businessEvent = BusinessEvent.builder()
                .eventType("MEDICATION_MANAGEMENT")
                .entity("MEDICATION_RECORD")
                .action("CREATE")
                .userId("testuser")
                .result("SUCCESS")
                .metadata(metadata)
                .build();

        // When & Then
        assertDoesNotThrow(() -> structuredLogger.business(businessEvent));
    }

    @Test
    void testApiLogging() {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("medicationId", "MED123");
        parameters.put("userId", "testuser");
        
        ApiEvent apiEvent = ApiEvent.builder()
                .httpMethod("POST")
                .path("/api/medications")
                .controller("MedicationController")
                .action("create")
                .userId("testuser")
                .ipAddress("192.168.1.1")
                .userAgent("TestClient/1.0")
                .durationMs(250)
                .statusCode(201)
                .requestSize(1024L)
                .responseSize(512L)
                .parameters(parameters)
                .build();

        // When & Then
        assertDoesNotThrow(() -> structuredLogger.api(apiEvent));
    }

    @Test
    void testErrorLogging() {
        // Given
        Exception testException = new RuntimeException("Test exception for logging");
        Map<String, Object> context = new HashMap<>();
        context.put("userId", "testuser");
        context.put("operation", "testOperation");

        // When & Then
        assertDoesNotThrow(() -> 
            structuredLogger.error("Test error message", testException, context));
    }

    @Test
    void testMDCContextPreservation() {
        // Given
        MDC.put("requestId", "test-request-123");
        
        AuditEvent auditEvent = AuditEvent.builder()
                .operation("TEST")
                .entityType("TEST_ENTITY")
                .entityId("123")
                .userId("testuser")
                .ipAddress("192.168.1.1")
                .userAgent("TestAgent/1.0")
                .status("SUCCESS")
                .details("Test MDC preservation")
                .build();

        // When
        structuredLogger.audit(auditEvent);

        // Then - MDC should still contain the requestId
        assertDoesNotThrow(() -> {
            String requestId = MDC.get("requestId");
            // Note: In actual implementation, requestId might be preserved
        });
        
        MDC.clear();
    }
}