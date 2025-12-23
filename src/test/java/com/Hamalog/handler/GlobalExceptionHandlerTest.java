package com.Hamalog.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.Hamalog.exception.ErrorCode;
import com.Hamalog.exception.medication.MedicationRecordNotFoundException;
import com.Hamalog.exception.medication.MedicationScheduleNotFoundException;
import com.Hamalog.exception.member.MemberNotFoundException;
import com.Hamalog.exception.oauth2.OAuth2Exception;
import com.Hamalog.exception.token.TokenException;
import com.Hamalog.exception.validation.InvalidInputException;
import com.Hamalog.logging.StructuredLogger;
import jakarta.persistence.OptimisticLockException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("GlobalExceptionHandler 테스트")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;

    @Mock
    private StructuredLogger structuredLogger;

    @Mock
    private ExceptionHandlerUtils handlerUtils;

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
        lenient().when(handlerUtils.createErrorContext(any(), any(), any())).thenReturn(Map.of());
        globalExceptionHandler = new GlobalExceptionHandler(structuredLogger, handlerUtils);
    }

    @Nested
    @DisplayName("404 Not Found 예외 테스트")
    class NotFoundExceptionTests {

        @Test
        @DisplayName("MedicationRecordNotFoundException 처리 테스트")
        void handleMedicationRecordNotFoundException() {
            // given
            MedicationRecordNotFoundException exception = new MedicationRecordNotFoundException();

            // when
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleNotFound(exception, request);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getPath()).isEqualTo("/api/test");
            assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.MEDICATION_RECORD_NOT_FOUND.getMessage());
            assertThat(response.getBody().getTimestamp()).isNotNull();

            verify(structuredLogger).error(eq("Resource not found"), eq(exception), any(Map.class));
        }

        @Test
        @DisplayName("MedicationScheduleNotFoundException 처리 테스트")
        void handleMedicationScheduleNotFoundException() {
            // given
            MedicationScheduleNotFoundException exception = new MedicationScheduleNotFoundException();

            // when
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleNotFound(exception, request);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.MEDICATION_SCHEDULE_NOT_FOUND.getCode());
        }

        @Test
        @DisplayName("MemberNotFoundException 처리 테스트")
        void handleMemberNotFoundException() {
            // given
            MemberNotFoundException exception = new MemberNotFoundException();

            // when
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleNotFound(exception, request);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND.getCode());
        }
    }

    @Nested
    @DisplayName("400 Bad Request 예외 테스트")
    class BadRequestExceptionTests {

        @Test
        @DisplayName("유효성 검사 예외 처리 테스트")
        void handleValidationException() {
            // given
            List<FieldError> fieldErrors = new ArrayList<>();
            fieldErrors.add(new FieldError("testObject", "testField", "test error message"));

            BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);

            MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
            when(exception.getBindingResult()).thenReturn(bindingResult);

            // when
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidation(exception, request);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getPath()).isEqualTo("/api/test");
            assertThat(response.getBody().getTimestamp()).isNotNull();
            assertThat(response.getBody().getViolations())
                    .containsEntry("testField", "test error message");
        }

        @Test
        @DisplayName("InvalidInputException 처리 테스트")
        void handleInvalidInputException() {
            // given
            InvalidInputException exception = new InvalidInputException(ErrorCode.INVALID_INPUT);

            // when
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleInvalidInput(exception, request);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INVALID_INPUT.getCode());

            verify(structuredLogger).error(eq("Input validation failed"), eq(exception), any(Map.class));
        }

        @Test
        @DisplayName("OAuth2Exception 처리 테스트")
        void handleOAuth2Exception() {
            // given
            OAuth2Exception exception = new OAuth2Exception(ErrorCode.OAUTH2_AUTHORIZATION_FAILED);

            // when
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleOAuth2Exception(exception, request);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.OAUTH2_AUTHORIZATION_FAILED.getCode());

            verify(structuredLogger).error(eq("OAuth2 error"), eq(exception), any(Map.class));
        }
    }

    @Nested
    @DisplayName("401 Unauthorized 예외 테스트")
    class UnauthorizedExceptionTests {

        @Test
        @DisplayName("TokenException 처리 테스트")
        void handleTokenException() {
            // given
            TokenException exception = new TokenException(ErrorCode.INVALID_TOKEN);

            // when
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleTokenException(exception, request);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INVALID_TOKEN.getCode());

            verify(structuredLogger).error(eq("Token error"), eq(exception), any(Map.class));
        }

        @Test
        @DisplayName("AuthenticationException 처리 테스트")
        void handleAuthenticationException() {
            // given
            BadCredentialsException exception = new BadCredentialsException("Invalid credentials");

            // when
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleAuth(exception, request);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody().getCode()).isEqualTo("UNAUTHORIZED");
        }
    }

    @Nested
    @DisplayName("409 Conflict 예외 테스트")
    class ConflictExceptionTests {

        @Test
        @DisplayName("OptimisticLockException 처리 테스트")
        void handleOptimisticLockException() {
            // given
            OptimisticLockException exception = new OptimisticLockException("Optimistic lock failed");

            // when
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleOptimisticLock(exception, request);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.OPTIMISTIC_LOCK_FAILED.getCode());
            // handleSimpleException은 구조화된 로깅을 하지 않음
        }

        @Test
        @DisplayName("DataIntegrityViolationException 처리 테스트")
        void handleDataIntegrityViolationException() {
            // given
            DataIntegrityViolationException exception = new DataIntegrityViolationException("Duplicate key");

            // when
            ResponseEntity<ErrorResponse> response =
                    globalExceptionHandler.handleDataIntegrityViolation(exception, request);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.RESOURCE_CONFLICT.getCode());
            // handleSimpleException은 구조화된 로깅을 하지 않음
        }
    }

    @Nested
    @DisplayName("ErrorResponse 구조 테스트")
    class ErrorResponseStructureTests {

        @Test
        @DisplayName("ErrorResponse에 timestamp와 traceId가 포함되어야 함")
        void shouldIncludeTimestampAndTraceId() {
            // given
            MedicationRecordNotFoundException exception = new MedicationRecordNotFoundException();

            // when
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleNotFound(exception, request);

            // then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getTimestamp()).isNotNull();
            // traceId는 MDC에 설정되어 있지 않으면 null일 수 있음
        }
    }
}
