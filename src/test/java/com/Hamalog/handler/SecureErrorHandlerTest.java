package com.Hamalog.handler;

import com.Hamalog.security.validation.InputValidationUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.lang.reflect.Method;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecureErrorHandler Tests")
class SecureErrorHandlerTest {

    @Mock
    private Environment environment;

    @Mock
    private InputValidationUtil inputValidationUtil;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private SecureErrorHandler secureErrorHandler;

    @BeforeEach
    void setUp() {
        // Default mocking behavior - using lenient() to avoid unnecessary stubbing exceptions
        lenient().when(request.getRequestURI()).thenReturn("/api/test");
        lenient().when(request.getMethod()).thenReturn("POST");
        lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        lenient().when(inputValidationUtil.sanitizeForLog(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Should handle generic exception correctly")
    void handleGenericException_ShouldReturnInternalServerError() {
        // given
        Exception exception = new RuntimeException("Test error");
        lenient().when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        // when
        ResponseEntity<Map<String, Object>> response = secureErrorHandler.handleGenericException(exception, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("error", "INTERNAL_ERROR");
        assertThat(response.getBody()).containsEntry("message", "서버 내부 오류가 발생했습니다");
        assertThat(response.getBody()).containsEntry("path", "/api/test");
        assertThat(response.getBody()).containsKey("timestamp");
        assertThat(response.getBody()).containsKey("errorId"); // Development mode
        
        verify(inputValidationUtil, atLeastOnce()).sanitizeForLog(anyString());
    }

    @Test
    @DisplayName("Should handle authentication exception correctly")
    void handleAuthenticationException_ShouldReturnUnauthorized() {
        // given
        AuthenticationException exception = new AuthenticationException("Auth failed") {};
        lenient().when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        // when
        ResponseEntity<Map<String, Object>> response = secureErrorHandler.handleAuthenticationException(exception, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("error", "AUTHENTICATION_FAILED");
        assertThat(response.getBody()).containsEntry("message", "인증에 실패했습니다");
        assertThat(response.getBody()).containsEntry("path", "/api/test");
        
        verify(inputValidationUtil, atLeastOnce()).sanitizeForLog(anyString());
    }

    @Test
    @DisplayName("Should handle BadCredentialsException with specific message")
    void handleAuthenticationException_WithBadCredentials_ShouldReturnSpecificMessage() {
        // given
        BadCredentialsException exception = new BadCredentialsException("Bad credentials");
        lenient().when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        // when
        ResponseEntity<Map<String, Object>> response = secureErrorHandler.handleAuthenticationException(exception, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("error", "AUTHENTICATION_FAILED");
        assertThat(response.getBody()).containsEntry("message", "아이디 또는 비밀번호가 올바르지 않습니다");
    }

    @Test
    @DisplayName("Should handle access denied exception correctly")
    void handleAccessDeniedException_ShouldReturnForbidden() {
        // given
        AccessDeniedException exception = new AccessDeniedException("Access denied");
        lenient().when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        // when
        ResponseEntity<Map<String, Object>> response = secureErrorHandler.handleAccessDeniedException(exception, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("error", "ACCESS_DENIED");
        assertThat(response.getBody()).containsEntry("message", "해당 리소스에 접근할 권한이 없습니다");
        assertThat(response.getBody()).containsEntry("path", "/api/test");
        
        verify(inputValidationUtil, atLeastOnce()).sanitizeForLog(anyString());
    }

    @Test
    @DisplayName("Should handle validation exception correctly")
    void handleValidationException_ShouldReturnBadRequestWithFieldErrors() throws Exception {
        // given
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "testObject");
        bindingResult.addError(new FieldError("testObject", "field1", "Field1 error"));
        bindingResult.addError(new FieldError("testObject", "field2", "Field2 error"));
        
        // Create a mock MethodParameter to avoid NullPointerException
        MethodParameter methodParameter = mock(MethodParameter.class);
        when(methodParameter.getParameterIndex()).thenReturn(0);
        when(methodParameter.getExecutable()).thenReturn(mock(java.lang.reflect.Method.class));
        
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);
        lenient().when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        // when
        ResponseEntity<Map<String, Object>> response = secureErrorHandler.handleValidationException(exception, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsEntry("error", "VALIDATION_FAILED");
        assertThat(response.getBody()).containsEntry("message", "입력값 검증에 실패했습니다");
        assertThat(response.getBody()).containsKey("fieldErrors");
        
        @SuppressWarnings("unchecked")
        Map<String, String> fieldErrors = (Map<String, String>) response.getBody().get("fieldErrors");
        assertThat(fieldErrors).isNotNull();
        assertThat(fieldErrors).containsEntry("field1", "Field1 error");
        assertThat(fieldErrors).containsEntry("field2", "Field2 error");
    }

    @Test
    @DisplayName("Should handle input validation exception correctly")
    void handleInputValidationException_ShouldReturnBadRequest() {
        // given
        InputValidationUtil.InputValidationException exception = 
                new InputValidationUtil.InputValidationException("Invalid input");
        lenient().when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        // when
        ResponseEntity<Map<String, Object>> response = secureErrorHandler.handleInputValidationException(exception, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "INVALID_INPUT");
        assertThat(response.getBody()).containsEntry("message", "유효하지 않은 입력값입니다");
        assertThat(response.getBody()).containsEntry("path", "/api/test");
        
        verify(inputValidationUtil, atLeastOnce()).sanitizeForLog(anyString());
    }

    @Test
    @DisplayName("Should handle not found exception correctly")
    void handleNotFoundException_ShouldReturnNotFound() {
        // given
        NoHandlerFoundException exception = new NoHandlerFoundException("GET", "/api/notfound", null);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        // when
        ResponseEntity<Map<String, Object>> response = secureErrorHandler.handleNotFoundException(exception, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("error", "RESOURCE_NOT_FOUND");
        assertThat(response.getBody()).containsEntry("message", "요청하신 리소스를 찾을 수 없습니다");
        assertThat(response.getBody()).containsEntry("path", "/api/test");
        
        // sanitizeForLog is called twice: once in handler, once in createErrorResponse
        verify(inputValidationUtil, times(2)).sanitizeForLog("/api/test");
    }

    @Test
    @DisplayName("Should handle method not supported exception correctly")
    void handleMethodNotSupportedException_ShouldReturnMethodNotAllowed() {
        // given
        HttpRequestMethodNotSupportedException exception = 
                new HttpRequestMethodNotSupportedException("PATCH");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        // when
        ResponseEntity<Map<String, Object>> response = secureErrorHandler.handleMethodNotSupportedException(exception, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).containsEntry("error", "METHOD_NOT_ALLOWED");
        assertThat(response.getBody()).containsEntry("message", "지원하지 않는 HTTP 메서드입니다");
        assertThat(response.getBody()).containsEntry("path", "/api/test");
        
        // sanitizeForLog is called twice for "/api/test": once in handler, once in createErrorResponse
        verify(inputValidationUtil, times(2)).sanitizeForLog("/api/test");
        verify(inputValidationUtil).sanitizeForLog("POST");
    }

    @Test
    @DisplayName("Should handle max upload size exceeded exception correctly")
    void handleMaxUploadSizeExceededException_ShouldReturnPayloadTooLarge() {
        // given
        MaxUploadSizeExceededException exception = new MaxUploadSizeExceededException(1024 * 1024);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        // when
        ResponseEntity<Map<String, Object>> response = secureErrorHandler.handleMaxUploadSizeExceededException(exception, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).containsEntry("error", "FILE_SIZE_EXCEEDED");
        assertThat(response.getBody()).containsEntry("message", "업로드 파일 크기가 제한을 초과했습니다");
        assertThat(response.getBody()).containsEntry("path", "/api/test");
        
        verify(inputValidationUtil).sanitizeForLog("127.0.0.1");
    }

    @Test
    @DisplayName("Should detect client IP from X-Forwarded-For header")
    void getClientIp_WithXForwardedFor_ShouldReturnFirstIP() {
        // given
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.100, 10.0.0.1");
        AuthenticationException exception = new AuthenticationException("Auth failed") {};
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        // when
        secureErrorHandler.handleAuthenticationException(exception, request);

        // then
        // sanitizeForLog is called for IP extraction and path sanitization
        verify(inputValidationUtil, atLeastOnce()).sanitizeForLog("192.168.1.100");
        verify(inputValidationUtil, atLeastOnce()).sanitizeForLog("/api/test");
    }

    @Test
    @DisplayName("Should detect client IP from X-Real-IP header when X-Forwarded-For is not available")
    void getClientIp_WithXRealIP_ShouldReturnRealIP() {
        // given
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("192.168.1.200");
        AccessDeniedException exception = new AccessDeniedException("Access denied");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        // when
        secureErrorHandler.handleAccessDeniedException(exception, request);

        // then
        verify(inputValidationUtil).sanitizeForLog("192.168.1.200");
    }

    @Test
    @DisplayName("Should use remote address when proxy headers are not available")
    void getClientIp_WithoutProxyHeaders_ShouldReturnRemoteAddr() {
        // given
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        AuthenticationException exception = new AuthenticationException("Auth failed") {};
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        // when
        secureErrorHandler.handleAuthenticationException(exception, request);

        // then
        verify(inputValidationUtil).sanitizeForLog("127.0.0.1");
    }

    @Test
    @DisplayName("Should return true for development mode when no profiles are active")
    void isDevelopmentMode_WithNoActiveProfiles_ShouldReturnTrue() {
        // given
        when(environment.getActiveProfiles()).thenReturn(new String[]{});
        Exception exception = new RuntimeException("Test");

        // when
        ResponseEntity<Map<String, Object>> response = secureErrorHandler.handleGenericException(exception, request);

        // then
        assertThat(response.getBody()).containsKey("errorId"); // Should include errorId in development mode
    }

    @Test
    @DisplayName("Should return true for development mode with dev profile")
    void isDevelopmentMode_WithDevProfile_ShouldReturnTrue() {
        // given
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev", "local"});
        Exception exception = new RuntimeException("Test");

        // when
        ResponseEntity<Map<String, Object>> response = secureErrorHandler.handleGenericException(exception, request);

        // then
        assertThat(response.getBody()).containsKey("errorId"); // Should include errorId in development mode
    }

    @Test
    @DisplayName("Should return false for development mode with production profile")
    void isDevelopmentMode_WithProdProfile_ShouldReturnFalse() {
        // given
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        Exception exception = new RuntimeException("Test");

        // when
        ResponseEntity<Map<String, Object>> response = secureErrorHandler.handleGenericException(exception, request);

        // then
        assertThat(response.getBody()).doesNotContainKey("errorId"); // Should not include errorId in production mode
    }

    @Test
    @DisplayName("Should return false for development mode with production profile variant")
    void isDevelopmentMode_WithProductionProfile_ShouldReturnFalse() {
        // given
        when(environment.getActiveProfiles()).thenReturn(new String[]{"production"});
        Exception exception = new RuntimeException("Test");

        // when
        ResponseEntity<Map<String, Object>> response = secureErrorHandler.handleGenericException(exception, request);

        // then
        assertThat(response.getBody()).doesNotContainKey("errorId"); // Should not include errorId in production mode
    }

    @Test
    @DisplayName("Should sanitize all input values through InputValidationUtil")
    void allMethods_ShouldSanitizeInputs() {
        // given
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        when(request.getRequestURI()).thenReturn("/api/malicious<script>");
        // Remove unused stubs that cause UnnecessaryStubbingException
        // when(request.getMethod()).thenReturn("POST<script>");
        // when(request.getRemoteAddr()).thenReturn("127.0.0.1<script>");
        
        Exception exception = new RuntimeException("Test");

        // when
        secureErrorHandler.handleGenericException(exception, request);

        // then
        // sanitizeForLog is called multiple times: once in handler, once in createErrorResponse
        verify(inputValidationUtil, times(2)).sanitizeForLog("/api/malicious<script>");
    }

    @Test
    @DisplayName("Should handle empty X-Forwarded-For header gracefully")
    void getClientIp_WithEmptyXForwardedFor_ShouldFallbackToRemoteAddr() {
        // given
        when(request.getHeader("X-Forwarded-For")).thenReturn("");
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        
        AuthenticationException exception = new AuthenticationException("Auth failed") {};
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        // when
        secureErrorHandler.handleAuthenticationException(exception, request);

        // then
        verify(inputValidationUtil).sanitizeForLog("127.0.0.1");
    }

    @Test
    @DisplayName("Should create proper error response structure")
    void createErrorResponse_ShouldIncludeAllRequiredFields() {
        // given
        Exception exception = new RuntimeException("Test error");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        // when
        ResponseEntity<Map<String, Object>> response = secureErrorHandler.handleGenericException(exception, request);

        // then
        Map<String, Object> body = response.getBody();
        assertThat(body).containsKey("error");
        assertThat(body).containsKey("message");
        assertThat(body).containsKey("timestamp");
        assertThat(body).containsKey("path");
        assertThat(body).containsKey("errorId"); // Development mode
        
        assertThat(body.get("timestamp")).isNotNull();
        assertThat(body.get("timestamp").toString()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*Z");
    }

    @Test
    @DisplayName("Should handle validation exception with multiple field errors")
    void handleValidationException_WithMultipleErrors_ShouldIncludeAllFieldErrors() throws Exception {
        // given
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "testObject");
        bindingResult.addError(new FieldError("testObject", "email", "Invalid email format"));
        bindingResult.addError(new FieldError("testObject", "password", "Password too short"));
        bindingResult.addError(new FieldError("testObject", "name", "Name cannot be blank"));
        
        // Create a mock MethodParameter to avoid NullPointerException
        MethodParameter methodParameter = mock(MethodParameter.class);
        when(methodParameter.getParameterIndex()).thenReturn(0);
        when(methodParameter.getExecutable()).thenReturn(mock(java.lang.reflect.Method.class));
        
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        // when
        ResponseEntity<Map<String, Object>> response = secureErrorHandler.handleValidationException(exception, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        
        @SuppressWarnings("unchecked")
        Map<String, String> fieldErrors = (Map<String, String>) response.getBody().get("fieldErrors");
        assertThat(fieldErrors).hasSize(3);
        assertThat(fieldErrors).containsEntry("email", "Invalid email format");
        assertThat(fieldErrors).containsEntry("password", "Password too short");
        assertThat(fieldErrors).containsEntry("name", "Name cannot be blank");
    }
}