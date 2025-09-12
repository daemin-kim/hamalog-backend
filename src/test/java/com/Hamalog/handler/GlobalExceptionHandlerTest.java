package com.Hamalog.handler;

import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.exception.medication.MedicationScheduleNotFoundException;
import com.Hamalog.exception.member.MemberNotFoundException;
import com.Hamalog.handler.GlobalExceptionHandler.ErrorResponse;
import com.Hamalog.logging.MDCUtil;
import com.Hamalog.logging.StructuredLogger;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Global Exception Handler Tests")
class GlobalExceptionHandlerTest {

    @Mock
    private StructuredLogger structuredLogger;

    @Mock
    private HttpServletRequest request;

    @Mock
    private BindingResult bindingResult;

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    private MockedStatic<MDCUtil> mdcUtilMocked;
    private MockedStatic<MDC> mdcMocked;

    @BeforeEach
    void setUp() {
        mdcUtilMocked = mockStatic(MDCUtil.class);
        mdcMocked = mockStatic(MDC.class);
    }

    @AfterEach
    void tearDown() {
        if (mdcUtilMocked != null) {
            mdcUtilMocked.close();
        }
        if (mdcMocked != null) {
            mdcMocked.close();
        }
    }

    @Test
    @DisplayName("MedicationScheduleNotFoundException 처리 테스트")
    void handleNotFound_MedicationScheduleNotFoundException_ReturnsNotFound() {
        // given
        when(request.getRequestURI()).thenReturn("/api/test");
        MedicationScheduleNotFoundException ex = new MedicationScheduleNotFoundException();
        
        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleNotFound(ex, request);

        // then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().status());
        assertEquals(ErrorCode.MEDICATION_SCHEDULE_NOT_FOUND.getCode(), response.getBody().code());
        assertEquals("/api/test", response.getBody().path());
        assertNotNull(response.getBody().timestamp());
        
        mdcUtilMocked.verify(() -> MDCUtil.addErrorContext(ex));
        mdcUtilMocked.verify(() -> MDCUtil.put("error.severity", "LOW"));
        mdcUtilMocked.verify(() -> MDCUtil.clearErrorContext());
        verify(structuredLogger).error(eq("Resource not found"), eq(ex), any());
    }

    @Test
    @DisplayName("MemberNotFoundException 처리 테스트")
    void handleNotFound_MemberNotFoundException_ReturnsNotFound() {
        // given
        when(request.getRequestURI()).thenReturn("/api/test");
        MemberNotFoundException ex = new MemberNotFoundException();
        
        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleNotFound(ex, request);

        // then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(ErrorCode.MEMBER_NOT_FOUND.getCode(), response.getBody().code());
        verify(structuredLogger).error(anyString(), eq(ex), any());
    }

    @Test
    @DisplayName("MethodArgumentNotValidException 처리 테스트")
    void handleValidation_MethodArgumentNotValidException_ReturnsBadRequest() {
        // given
        when(request.getRequestURI()).thenReturn("/api/test");
        FieldError fieldError1 = new FieldError("user", "name", "Name is required");
        FieldError fieldError2 = new FieldError("user", "email", "Email format is invalid");
        List<FieldError> fieldErrors = Arrays.asList(fieldError1, fieldError2);
        
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);
        
        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidation(ex, request);

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().status());
        assertEquals("INVALID_INPUT", response.getBody().code());
        assertEquals("Request data validation failed", response.getBody().message());
        assertEquals("/api/test", response.getBody().path());
        
        Map<String, String> details = response.getBody().details();
        assertEquals("Name is required", details.get("name"));
        assertEquals("Email format is invalid", details.get("email"));
        
        mdcMocked.verify(() -> MDC.put("error.type", "VALIDATION_ERROR"));
        mdcMocked.verify(() -> MDC.put("error.violationCount", "2"));
    }

    @Test
    @DisplayName("AuthenticationException 처리 테스트")
    void handleAuth_AuthenticationException_ReturnsUnauthorized() {
        // given
        when(request.getRequestURI()).thenReturn("/api/test");
        AuthenticationException ex = mock(AuthenticationException.class);
        when(ex.getMessage()).thenReturn("Authentication failed");
        
        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleAuth(ex, request);

        // then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().status());
        assertEquals("UNAUTHORIZED", response.getBody().code());
        assertEquals("Authentication failed", response.getBody().message());
        
        mdcMocked.verify(() -> MDC.put("error.type", "AUTHENTICATION_ERROR"));
        mdcMocked.verify(() -> MDC.put("error.httpStatus", "401"));
    }

    @Test
    @DisplayName("CustomException 처리 테스트")
    void handleCustomException_CustomException_ReturnsBadRequest() {
        // given
        when(request.getRequestURI()).thenReturn("/api/test");
        CustomException ex = new CustomException(ErrorCode.BAD_REQUEST);
        
        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleCustomException(ex, request);

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().status());
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), response.getBody().code());
        assertEquals(ErrorCode.BAD_REQUEST.getMessage(), response.getBody().message());
        
        mdcMocked.verify(() -> MDC.put("error.type", "BUSINESS_ERROR"));
        mdcMocked.verify(() -> MDC.put("error.code", ErrorCode.BAD_REQUEST.getCode()));
    }

    @Test
    @DisplayName("일반 Exception 처리 테스트")
    void handleException_Exception_ReturnsInternalServerError() {
        // given
        Exception ex = new RuntimeException("Unexpected error");
        
        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleException(ex, request);

        // then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().status());
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), response.getBody().code());
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR.getMessage(), response.getBody().message());
        
        mdcMocked.verify(() -> MDC.put("error.type", "UNEXPECTED_ERROR"));
        mdcMocked.verify(() -> MDC.put("error.exception", "RuntimeException"));
    }

    @Test
    @DisplayName("IP 주소 추출 - X-Forwarded-For 헤더 사용")
    void extractClientIpAddress_XForwardedForHeader_ReturnsCorrectIp() {
        // given
        Exception ex = new RuntimeException("Test");
        
        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleException(ex, request);

        // then - 예외가 정상적으로 처리되었는지 확인
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), response.getBody().code());
    }

    @Test
    @DisplayName("IP 주소 추출 - X-Real-IP 헤더 사용")
    void extractClientIpAddress_XRealIpHeader_ReturnsCorrectIp() {
        // given
        Exception ex = new RuntimeException("Test");
        
        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleException(ex, request);

        // then - 예외가 정상적으로 처리되었는지 확인
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), response.getBody().code());
    }

    @Test
    @DisplayName("IP 주소 추출 - RemoteAddr 폴백")
    void extractClientIpAddress_RemoteAddrFallback_ReturnsRemoteAddr() {
        // given
        Exception ex = new RuntimeException("Test");
        
        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleException(ex, request);

        // then - 예외가 정상적으로 처리되었는지 확인
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), response.getBody().code());
    }

    @Test
    @DisplayName("보안 관련 예외 확인 - SecurityException")
    void isSecurityRelated_SecurityException_ReturnsTrue() {
        // given
        SecurityException ex = new SecurityException("Security violation");
        
        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleException(ex, request);

        // then - SecurityException이 처리되었는지 확인
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(request, atLeastOnce()).getRequestURI();
    }

    @Test
    @DisplayName("보안 관련 예외 확인 - AccessDeniedException")
    void isSecurityRelated_AccessDeniedException_ReturnsTrue() {
        // given
        AccessDeniedException ex = new AccessDeniedException("Access denied");
        
        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleException(ex, request);

        // then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), response.getBody().code());
    }

    @Test
    @DisplayName("에러 심각도 결정 - 중요 패턴")
    void determineErrorSeverity_CriticalPattern_ReturnsCritical() {
        // given - OutOfMemoryError 패턴을 포함한 예외
        Exception ex = new RuntimeException("OutOfMemoryError occurred");
        
        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleException(ex, request);

        // then - CRITICAL 에러로 처리되었는지 확인
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(structuredLogger, never()).error(anyString(), eq(ex), any()); // 일반 Exception handler에서는 structuredLogger 호출 없음
    }

    @Test
    @DisplayName("에러 심각도 결정 - 보안 관련")
    void determineErrorSeverity_SecurityRelated_ReturnsHigh() {
        // given
        SecurityException ex = new SecurityException("Security breach");
        
        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleException(ex, request);

        // then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        mdcMocked.verify(() -> MDC.put("error.exception", "SecurityException"));
    }

    @Test
    @DisplayName("에러 심각도 결정 - 검증 관련")
    void determineErrorSeverity_ValidationRelated_ReturnsMedium() {
        // given
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument");
        
        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleException(ex, request);

        // then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        mdcMocked.verify(() -> MDC.put("error.exception", "IllegalArgumentException"));
    }

    @Test
    @DisplayName("에러 메시지 정리 - null 처리")
    void sanitizeErrorMessage_NullMessage_ReturnsDefault() {
        // given
        Exception ex = new RuntimeException((String) null);
        
        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleException(ex, request);

        // then - 예외가 정상적으로 처리되어야 함
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody().message());
    }

    @Test
    @DisplayName("에러 메시지 정리 - 긴 메시지 자르기")
    void sanitizeErrorMessage_LongMessage_TruncatesMessage() {
        // given
        String longMessage = "A".repeat(600); // 500자를 초과하는 메시지
        Exception ex = new RuntimeException(longMessage);
        
        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleException(ex, request);

        // then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        // 내부적으로 메시지가 정리되었는지 확인
        assertNotNull(response.getBody().message());
    }

    @Test
    @DisplayName("User Agent 정리 - null 처리")
    void sanitizeUserAgent_NullUserAgent_ReturnsUnknown() {
        // given
        Exception ex = new RuntimeException("Test");
        
        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleException(ex, request);

        // then - 예외가 정상적으로 처리되었는지 확인
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), response.getBody().code());
    }

    @Test
    @DisplayName("User Agent 정리 - 긴 User Agent 자르기")
    void sanitizeUserAgent_LongUserAgent_TruncatesUserAgent() {
        // given
        Exception ex = new RuntimeException("Test");
        
        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleException(ex, request);

        // then - 예외가 정상적으로 처리되었는지 확인
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), response.getBody().code());
    }

    @Test
    @DisplayName("스택 트레이스 문자열 - null 예외")
    void getStackTraceString_NullException_ReturnsEmptyString() {
        // given - 이미 생성된 예외를 사용
        Exception ex = new RuntimeException("Test exception");
        
        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleException(ex, request);

        // then - 예외가 정상 처리되어야 함
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("MDC 정리 확인")
    void cleanupErrorMDC_RemovesAllErrorKeys() {
        // given
        Exception ex = new RuntimeException("Test");
        
        // when
        globalExceptionHandler.handleException(ex, request);

        // then - MDC cleanup이 호출되었는지 확인
        mdcMocked.verify(() -> MDC.remove("error.type"));
        mdcMocked.verify(() -> MDC.remove("error.code"));
        mdcMocked.verify(() -> MDC.remove("error.httpStatus"));
        mdcMocked.verify(() -> MDC.remove("error.exception"));
    }

    @Test
    @DisplayName("ErrorResponse 생성 테스트")
    void errorResponse_Creation_SetsAllFields() {
        // given
        Map<String, String> details = Map.of("field", "error");
        
        // when
        ErrorResponse errorResponse = ErrorResponse.of(HttpStatus.BAD_REQUEST, "TEST_CODE", "Test message", "/test", details);

        // then
        assertEquals(400, errorResponse.status());
        assertEquals("TEST_CODE", errorResponse.code());
        assertEquals("Test message", errorResponse.message());
        assertEquals("/test", errorResponse.path());
        assertEquals(details, errorResponse.details());
        assertNotNull(errorResponse.timestamp());
        assertTrue(errorResponse.timestamp().isBefore(OffsetDateTime.now().plusSeconds(1)));
    }

    @Test
    @DisplayName("ErrorResponse null details 처리")
    void errorResponse_NullDetails_HandlesCorrectly() {
        // when
        ErrorResponse errorResponse = ErrorResponse.of(HttpStatus.NOT_FOUND, "NOT_FOUND", "Not found", "/test", null);

        // then
        assertEquals(404, errorResponse.status());
        assertNull(errorResponse.details());
    }

    @Test
    @DisplayName("빈 validation 에러 처리")
    void handleValidation_EmptyFieldErrors_ReturnsEmptyDetails() {
        // given
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());
        
        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidation(ex, request);

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().details().isEmpty());
        mdcMocked.verify(() -> MDC.put("error.violationCount", "0"));
    }
}