package com.Hamalog.handler;

import com.Hamalog.exception.medication.MedicationRecordNotFoundException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.logging.StructuredLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Mock
    private StructuredLogger structuredLogger;

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
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
