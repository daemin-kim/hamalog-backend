package com.Hamalog.handler;

import com.Hamalog.exception.medication.MedicationRecordNotFoundException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.security.filter.TrustedProxyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;

    @Mock
    private StructuredLogger structuredLogger;

    @Mock
    private TrustedProxyService trustedProxyService;

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
        lenient().when(trustedProxyService.resolveClientIp(any())).thenReturn(Optional.of("192.0.2.1"));
        globalExceptionHandler = new GlobalExceptionHandler(trustedProxyService);
        ReflectionTestUtils.setField(globalExceptionHandler, "structuredLogger", structuredLogger);
    }

    @Test
    @DisplayName("MedicationRecordNotFoundException 처리 테스트")
    void handleMedicationRecordNotFoundException() {
        // given
        MedicationRecordNotFoundException exception = new MedicationRecordNotFoundException();

        // when
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = globalExceptionHandler.handleNotFound(exception, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.MEDICATION_RECORD_NOT_FOUND.getMessage());

        verify(structuredLogger).error(eq("Resource not found"), eq(exception), any(Map.class));
    }

    @Test
    @DisplayName("유효성 검사 예외 처리 테스트")
    void handleValidationException() throws Exception {
        // given
        List<FieldError> fieldErrors = new ArrayList<>();
        fieldErrors.add(new FieldError("testObject", "testField", "test error message"));

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);

        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);

        // when
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = globalExceptionHandler.handleValidation(exception, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
        assertThat(response.getBody().getViolations())
            .containsEntry("testField", "test error message");
    }
}
