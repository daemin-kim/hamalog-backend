package com.Hamalog.handler;

import com.Hamalog.security.filter.TrustedProxyService;
import com.Hamalog.security.validation.InputValidationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SecureErrorHandlerTest {

    @InjectMocks
    private SecureErrorHandler secureErrorHandler;

    @Mock
    private Environment environment;

    @Mock
    private InputValidationUtil inputValidationUtil;

    @Mock
    private TrustedProxyService trustedProxyService;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(inputValidationUtil.sanitizeForLog(anyString())).thenAnswer(i -> i.getArgument(0));
        when(trustedProxyService.resolveClientIp(any())).thenReturn(Optional.of("127.0.0.1"));
    }

    @Test
    void handleGenericException_ShouldReturnInternalServerError() {
        // Given
        Exception ex = new RuntimeException("테스트 예외");

        // When
        ResponseEntity<Map<String, Object>> response = secureErrorHandler.handleGenericException(ex, request);

        // Then
        verifyErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 내부 오류가 발생했습니다");
    }

    @Test
    void handleAuthenticationException_ShouldReturnUnauthorized() {
        // Given
        BadCredentialsException ex = new BadCredentialsException("잘못된 인증 정보");

        // When
        ResponseEntity<Map<String, Object>> response = secureErrorHandler.handleAuthenticationException(ex, request);

        // Then
        verifyErrorResponse(response, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED",
            "아이디 또는 비밀번호가 올바르지 않습니다");
    }

    @Test
    void handleAccessDeniedException_ShouldReturnForbidden() {
        // Given
        AccessDeniedException ex = new AccessDeniedException("접근 거부");

        // When
        ResponseEntity<Map<String, Object>> response = secureErrorHandler.handleAccessDeniedException(ex, request);

        // Then
        verifyErrorResponse(response, HttpStatus.FORBIDDEN, "ACCESS_DENIED",
            "해당 리소스에 접근할 권한이 없습니다");
    }

    @Test
    void handleValidationException_ShouldReturnBadRequest() {
        // Given
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        List<FieldError> fieldErrors = new ArrayList<>();
        fieldErrors.add(new FieldError("test", "field1", "must not be empty"));

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);

        // When
        ResponseEntity<Map<String, Object>> response = secureErrorHandler.handleValidationException(ex, request);

        // Then
        verifyErrorResponse(response, HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
            "입력값 검증에 실패했습니다");

        @SuppressWarnings("unchecked")
        Map<String, String> fieldErrorsMap = (Map<String, String>) response.getBody().get("fieldErrors");
        assertNotNull(fieldErrorsMap);
        assertFalse(fieldErrorsMap.isEmpty());
    }

    @Test
    void handleInputValidationException_ShouldReturnBadRequest() {
        // Given
        InputValidationUtil.InputValidationException ex =
            new InputValidationUtil.InputValidationException("잘못된 입력값");

        // When
        ResponseEntity<Map<String, Object>> response =
            secureErrorHandler.handleInputValidationException(ex, request);

        // Then
        verifyErrorResponse(response, HttpStatus.BAD_REQUEST, "INVALID_INPUT",
            "유효하지 않은 입력값입니다");
    }

    private void verifyErrorResponse(ResponseEntity<Map<String, Object>> response,
                                   HttpStatus expectedStatus,
                                   String expectedCode,
                                   String expectedMessage) {
        assertNotNull(response);
        assertEquals(expectedStatus, response.getStatusCode());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(expectedCode, body.get("code"));
        assertEquals(expectedMessage, body.get("message"));
        assertNotNull(body.get("timestamp"));
        assertNotNull(body.get("path"));
        assertNotNull(body.get("errorId"));
    }
}
