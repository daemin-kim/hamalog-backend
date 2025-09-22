package com.Hamalog.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MDCUtil Tests")
class MDCUtilTest {

    @Mock
    private Authentication mockAuthentication;

    @Mock
    private SecurityContext mockSecurityContext;

    @BeforeEach
    void setUp() {
        MDC.clear();
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Should initialize request context with correlation and request IDs")
    void initializeRequestContext_WithoutRequest_SetsBasicContext() {
        // when
        MDCUtil.initializeRequestContext();

        // then
        assertThat(MDC.get(MDCUtil.CORRELATION_ID)).isNotNull();
        assertThat(MDC.get(MDCUtil.REQUEST_ID)).isNotNull();
        assertThat(MDC.get(MDCUtil.HOSTNAME)).isNotNull();
        assertThat(MDC.get(MDCUtil.THREAD_NAME)).isEqualTo(Thread.currentThread().getName());
        assertThat(MDC.get(MDCUtil.START_TIME)).isNotNull();
        assertThat(MDC.get(MDCUtil.USER_ID)).isEqualTo("anonymous");
    }

    @Test
    @DisplayName("Should initialize request context with HTTP request information")
    void initializeRequestContext_WithHttpRequest_SetsRequestInfo() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/test");
        request.setRemoteAddr("192.168.1.1");
        request.addHeader("User-Agent", "Mozilla/5.0 Test Browser");
        
        ServletRequestAttributes attributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attributes);

        // when
        MDCUtil.initializeRequestContext();

        // then
        assertThat(MDC.get(MDCUtil.HTTP_METHOD)).isEqualTo("GET");
        assertThat(MDC.get(MDCUtil.REQUEST_URI)).isEqualTo("/api/test");
        assertThat(MDC.get(MDCUtil.IP_ADDRESS)).isEqualTo("192.168.1.1");
        assertThat(MDC.get(MDCUtil.USER_AGENT)).contains("Mozilla/5.0");
        assertThat(MDC.get(MDCUtil.SESSION_ID)).isEqualTo("no-session");
    }

    @Test
    @DisplayName("Should initialize request context with authenticated user")
    void initializeRequestContext_WithAuthenticatedUser_SetsUserId() {
        // given
        when(mockAuthentication.isAuthenticated()).thenReturn(true);
        when(mockAuthentication.getName()).thenReturn("testuser");
        when(mockSecurityContext.getAuthentication()).thenReturn(mockAuthentication);
        SecurityContextHolder.setContext(mockSecurityContext);

        // when
        MDCUtil.initializeRequestContext();

        // then
        assertThat(MDC.get(MDCUtil.USER_ID)).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should handle anonymous user correctly")
    void initializeRequestContext_WithAnonymousUser_SetsAnonymous() {
        // given
        when(mockAuthentication.isAuthenticated()).thenReturn(true);
        when(mockAuthentication.getName()).thenReturn("anonymousUser");
        when(mockSecurityContext.getAuthentication()).thenReturn(mockAuthentication);
        SecurityContextHolder.setContext(mockSecurityContext);

        // when
        MDCUtil.initializeRequestContext();

        // then
        assertThat(MDC.get(MDCUtil.USER_ID)).isEqualTo("anonymous");
    }

    @Test
    @DisplayName("Should handle session ID when session exists")
    void initializeRequestContext_WithSession_SetsSessionId() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(); // This creates a session
        
        ServletRequestAttributes attributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attributes);

        // when
        MDCUtil.initializeRequestContext();

        // then
        assertThat(MDC.get(MDCUtil.SESSION_ID)).isNotEqualTo("no-session");
        assertThat(MDC.get(MDCUtil.SESSION_ID)).isNotNull();
    }

    @Test
    @DisplayName("Should initialize business context correctly")
    void initializeBusinessContext_ValidInputs_SetsContext() {
        // given
        String operationType = "CREATE";
        String operationName = "createMedication";
        String entityType = "Medication";
        String entityId = "123";

        // when
        MDCUtil.initializeBusinessContext(operationType, operationName, entityType, entityId);

        // then
        assertThat(MDC.get(MDCUtil.OPERATION_TYPE)).isEqualTo(operationType);
        assertThat(MDC.get(MDCUtil.OPERATION_NAME)).isEqualTo(operationName);
        assertThat(MDC.get(MDCUtil.ENTITY_TYPE)).isEqualTo(entityType);
        assertThat(MDC.get(MDCUtil.ENTITY_ID)).isEqualTo(entityId);
    }

    @Test
    @DisplayName("Should initialize performance context correctly")
    void initializePerformanceContext_ValidInput_SetsContext() {
        // given
        String operationName = "processPayment";

        // when
        MDCUtil.initializePerformanceContext(operationName);

        // then
        assertThat(MDC.get(MDCUtil.OPERATION_NAME)).isEqualTo(operationName);
        assertThat(MDC.get(MDCUtil.START_TIME)).isNotNull();
        assertThat(Long.parseLong(MDC.get(MDCUtil.START_TIME))).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should add error context when throwable is provided")
    void addErrorContext_WithThrowable_SetsErrorInfo() {
        // given
        RuntimeException exception = new RuntimeException("Test error message");

        // when
        MDCUtil.addErrorContext(exception);

        // then
        assertThat(MDC.get(MDCUtil.ERROR_TYPE)).isEqualTo("RuntimeException");
        assertThat(MDC.get(MDCUtil.ERROR_MESSAGE)).contains("Test error message");
    }

    @Test
    @DisplayName("Should handle null throwable gracefully")
    void addErrorContext_WithNullThrowable_DoesNothing() {
        // when
        MDCUtil.addErrorContext(null);

        // then
        assertThat(MDC.get(MDCUtil.ERROR_TYPE)).isNull();
        assertThat(MDC.get(MDCUtil.ERROR_MESSAGE)).isNull();
    }

    @Test
    @DisplayName("Should put string value with key")
    void put_StringValue_SetsCorrectly() {
        // given
        String key = "testKey";
        String value = "testValue";

        // when
        MDCUtil.put(key, value);

        // then
        assertThat(MDC.get(key)).isEqualTo(value);
    }

    @Test
    @DisplayName("Should put object value with key")
    void put_ObjectValue_SetsCorrectly() {
        // given
        String key = "testKey";
        Integer value = 123;

        // when
        MDCUtil.put(key, value);

        // then
        assertThat(MDC.get(key)).isEqualTo("123");
    }

    @Test
    @DisplayName("Should handle null key gracefully")
    void put_NullKey_DoesNothing() {
        // when
        MDCUtil.put(null, "value");

        // then
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    @DisplayName("Should handle null value gracefully")
    void put_NullValue_DoesNothing() {
        // when
        MDCUtil.put("key", (String) null);

        // then
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    @DisplayName("Should get value from MDC")
    void get_ExistingKey_ReturnsValue() {
        // given
        String key = "testKey";
        String value = "testValue";
        MDC.put(key, value);

        // when
        String result = MDCUtil.get(key);

        // then
        assertThat(result).isEqualTo(value);
    }

    @Test
    @DisplayName("Should return null for non-existent key")
    void get_NonExistentKey_ReturnsNull() {
        // when
        String result = MDCUtil.get("nonExistentKey");

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should remove key from MDC")
    void remove_ExistingKey_RemovesKey() {
        // given
        String key = "testKey";
        MDC.put(key, "testValue");

        // when
        MDCUtil.remove(key);

        // then
        assertThat(MDC.get(key)).isNull();
    }

    @Test
    @DisplayName("Should handle null key in remove gracefully")
    void remove_NullKey_DoesNothing() {
        // given
        MDC.put("existingKey", "value");

        // when
        MDCUtil.remove(null);

        // then
        assertThat(MDC.get("existingKey")).isEqualTo("value");
    }

    @Test
    @DisplayName("Should clear request context")
    void clearRequestContext_Called_ClearsRequestSpecificEntries() {
        // given
        MDC.put(MDCUtil.CORRELATION_ID, "test");
        MDC.put(MDCUtil.REQUEST_ID, "test");
        MDC.put(MDCUtil.HTTP_METHOD, "GET");
        MDC.put(MDCUtil.REQUEST_URI, "/test");
        MDC.put(MDCUtil.IP_ADDRESS, "127.0.0.1");
        MDC.put(MDCUtil.USER_AGENT, "test-agent");
        MDC.put(MDCUtil.SESSION_ID, "test-session");
        MDC.put(MDCUtil.START_TIME, "123456789");

        // when
        MDCUtil.clearRequestContext();

        // then
        assertThat(MDC.get(MDCUtil.CORRELATION_ID)).isNull();
        assertThat(MDC.get(MDCUtil.REQUEST_ID)).isNull();
        assertThat(MDC.get(MDCUtil.HTTP_METHOD)).isNull();
        assertThat(MDC.get(MDCUtil.REQUEST_URI)).isNull();
        assertThat(MDC.get(MDCUtil.IP_ADDRESS)).isNull();
        assertThat(MDC.get(MDCUtil.USER_AGENT)).isNull();
        assertThat(MDC.get(MDCUtil.SESSION_ID)).isNull();
        assertThat(MDC.get(MDCUtil.START_TIME)).isNull();
    }

    @Test
    @DisplayName("Should clear business context")
    void clearBusinessContext_Called_ClearsBusinessSpecificEntries() {
        // given
        MDC.put(MDCUtil.OPERATION_TYPE, "CREATE");
        MDC.put(MDCUtil.OPERATION_NAME, "test");
        MDC.put(MDCUtil.ENTITY_TYPE, "User");
        MDC.put(MDCUtil.ENTITY_ID, "123");
        MDC.put(MDCUtil.ACTION_TYPE, "UPDATE");
        MDC.put(MDCUtil.BUSINESS_CONTEXT, "test-context");

        // when
        MDCUtil.clearBusinessContext();

        // then
        assertThat(MDC.get(MDCUtil.OPERATION_TYPE)).isNull();
        assertThat(MDC.get(MDCUtil.OPERATION_NAME)).isNull();
        assertThat(MDC.get(MDCUtil.ENTITY_TYPE)).isNull();
        assertThat(MDC.get(MDCUtil.ENTITY_ID)).isNull();
        assertThat(MDC.get(MDCUtil.ACTION_TYPE)).isNull();
        assertThat(MDC.get(MDCUtil.BUSINESS_CONTEXT)).isNull();
    }

    @Test
    @DisplayName("Should clear performance context")
    void clearPerformanceContext_Called_ClearsPerformanceSpecificEntries() {
        // given
        MDC.put(MDCUtil.OPERATION_NAME, "test");
        MDC.put(MDCUtil.START_TIME, "123456789");
        MDC.put(MDCUtil.EXECUTION_TIME, "1000");

        // when
        MDCUtil.clearPerformanceContext();

        // then
        assertThat(MDC.get(MDCUtil.OPERATION_NAME)).isNull();
        assertThat(MDC.get(MDCUtil.START_TIME)).isNull();
        assertThat(MDC.get(MDCUtil.EXECUTION_TIME)).isNull();
    }

    @Test
    @DisplayName("Should clear error context")
    void clearErrorContext_Called_ClearsErrorSpecificEntries() {
        // given
        MDC.put(MDCUtil.ERROR_TYPE, "RuntimeException");
        MDC.put(MDCUtil.ERROR_MESSAGE, "Test error");

        // when
        MDCUtil.clearErrorContext();

        // then
        assertThat(MDC.get(MDCUtil.ERROR_TYPE)).isNull();
        assertThat(MDC.get(MDCUtil.ERROR_MESSAGE)).isNull();
    }

    @Test
    @DisplayName("Should clear all MDC context")
    void clearAllContext_Called_ClearsAllMDCEntries() {
        // given
        MDC.put(MDCUtil.CORRELATION_ID, "test");
        MDC.put(MDCUtil.REQUEST_ID, "test");
        MDC.put(MDCUtil.USER_ID, "test");

        // when
        MDCUtil.clearAllContext();

        // then
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    @DisplayName("Should get copy of current context")
    void getCopyOfContext_Called_ReturnsContextCopy() {
        // given
        MDC.put(MDCUtil.CORRELATION_ID, "test-correlation");
        MDC.put(MDCUtil.USER_ID, "test-user");

        // when
        Map<String, String> context = MDCUtil.getCopyOfContext();

        // then
        assertThat(context).isNotNull();
        assertThat(context.get(MDCUtil.CORRELATION_ID)).isEqualTo("test-correlation");
        assertThat(context.get(MDCUtil.USER_ID)).isEqualTo("test-user");
    }

    @Test
    @DisplayName("Should set context from map")
    void setContext_WithValidMap_SetsContextCorrectly() {
        // given
        Map<String, String> contextMap = new HashMap<>();
        contextMap.put(MDCUtil.CORRELATION_ID, "restored-correlation");
        contextMap.put(MDCUtil.USER_ID, "restored-user");

        // when
        MDCUtil.setContext(contextMap);

        // then
        assertThat(MDC.get(MDCUtil.CORRELATION_ID)).isEqualTo("restored-correlation");
        assertThat(MDC.get(MDCUtil.USER_ID)).isEqualTo("restored-user");
    }

    @Test
    @DisplayName("Should handle null context map gracefully")
    void setContext_WithNullMap_DoesNothing() {
        // given
        MDC.put(MDCUtil.CORRELATION_ID, "existing");

        // when
        MDCUtil.setContext(null);

        // then
        assertThat(MDC.get(MDCUtil.CORRELATION_ID)).isEqualTo("existing");
    }

    @Test
    @DisplayName("Should handle unauthenticated user")
    void initializeRequestContext_WithUnauthenticatedUser_SetsAnonymous() {
        // given
        when(mockAuthentication.isAuthenticated()).thenReturn(false);
        when(mockSecurityContext.getAuthentication()).thenReturn(mockAuthentication);
        SecurityContextHolder.setContext(mockSecurityContext);

        // when
        MDCUtil.initializeRequestContext();

        // then
        assertThat(MDC.get(MDCUtil.USER_ID)).isEqualTo("anonymous");
    }

    @Test
    @DisplayName("Should handle null authentication")
    void initializeRequestContext_WithNullAuthentication_SetsAnonymous() {
        // given
        when(mockSecurityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(mockSecurityContext);

        // when
        MDCUtil.initializeRequestContext();

        // then
        assertThat(MDC.get(MDCUtil.USER_ID)).isEqualTo("anonymous");
    }

    @Test
    @DisplayName("Should preserve existing MDC values when clearing specific context")
    void clearRequestContext_WithMixedMDC_PreservesNonRequestValues() {
        // given
        MDC.put(MDCUtil.CORRELATION_ID, "test");
        MDC.put(MDCUtil.OPERATION_TYPE, "should-remain");

        // when
        MDCUtil.clearRequestContext();

        // then
        assertThat(MDC.get(MDCUtil.CORRELATION_ID)).isNull(); // Should be cleared
        assertThat(MDC.get(MDCUtil.OPERATION_TYPE)).isEqualTo("should-remain"); // Should remain
    }

    @Test
    @DisplayName("Should handle complex error messages")
    void addErrorContext_WithComplexException_SetsErrorInfo() {
        // given
        IllegalArgumentException exception = new IllegalArgumentException("Invalid input: value must be positive");

        // when
        MDCUtil.addErrorContext(exception);

        // then
        assertThat(MDC.get(MDCUtil.ERROR_TYPE)).isEqualTo("IllegalArgumentException");
        assertThat(MDC.get(MDCUtil.ERROR_MESSAGE)).contains("Invalid input");
    }

    @Test
    @DisplayName("Should handle exception with null message")
    void addErrorContext_WithNullMessage_SetsErrorType() {
        // given
        RuntimeException exception = new RuntimeException((String) null);

        // when
        MDCUtil.addErrorContext(exception);

        // then
        assertThat(MDC.get(MDCUtil.ERROR_TYPE)).isEqualTo("RuntimeException");
        // Error message might be null or sanitized, just check it doesn't crash
        assertThat(MDC.get(MDCUtil.ERROR_MESSAGE)).isNotNull();
    }
}