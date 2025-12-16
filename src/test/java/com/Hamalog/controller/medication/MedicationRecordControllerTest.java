package com.Hamalog.controller.medication;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.Hamalog.domain.medication.MedicationRecord;
import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.domain.medication.MedicationTime;
import com.Hamalog.dto.medication.request.MedicationRecordCreateRequest;
import com.Hamalog.dto.medication.request.MedicationRecordUpdateRequest;
import com.Hamalog.service.medication.MedicationRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

@ExtendWith(MockitoExtension.class)
@DisplayName("Medication Record Controller Tests")
class MedicationRecordControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MedicationRecordService medicationRecordService;

    @InjectMocks
    private MedicationRecordController medicationRecordController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(medicationRecordController)
                .setCustomArgumentResolvers(new MockUserDetailsArgumentResolver())
                .setHandlerExceptionResolvers(new SimpleExceptionResolver())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // Register JavaTimeModule for LocalDateTime
    }

    /**
     * Custom argument resolver for @AuthenticationPrincipal UserDetails parameters
     */
    private static class MockUserDetailsArgumentResolver implements HandlerMethodArgumentResolver {
        
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class) &&
                   UserDetails.class.isAssignableFrom(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                    NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            // Return a mock UserDetails for testing
            UserDetails mockUserDetails = mock(UserDetails.class);
            lenient().when(mockUserDetails.getUsername()).thenReturn("testuser");
            return mockUserDetails;
        }
    }

    /**
     * Simple exception resolver for handling service exceptions
     */
    private static class SimpleExceptionResolver implements HandlerExceptionResolver {
        
        @Override
        public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response,
                                           Object handler, Exception ex) {
            // Handle validation and binding exceptions with 400 status
            if (ex.getClass().getSimpleName().contains("MethodArgumentNotValid") ||
                ex.getClass().getSimpleName().contains("TypeMismatch") ||
                ex.getClass().getSimpleName().contains("HttpMessageNotReadable") ||
                ex.getClass().getSimpleName().contains("MissingServletRequest") ||
                ex.getClass().getSimpleName().contains("ConstraintViolation")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return new ModelAndView(); // Return empty ModelAndView to indicate handled
            }
            // Handle general RuntimeExceptions with 500 status
            else if (ex instanceof RuntimeException) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return new ModelAndView(); // Return empty ModelAndView to indicate handled
            }
            return null; // Return null to indicate not handled
        }
    }

    @Test
    @DisplayName("Should get medication records by schedule ID successfully")
    void getMedicationRecords_ValidScheduleId_ShouldReturnRecords() throws Exception {
        // given
        Long scheduleId = 1L;
        MedicationRecord record1 = createTestMedicationRecord(1L);
        MedicationRecord record2 = createTestMedicationRecord(2L);
        List<MedicationRecord> records = Arrays.asList(record1, record2);
        
        when(medicationRecordService.getMedicationRecords(scheduleId)).thenReturn(records);

        // when & then
        mockMvc.perform(get("/api/v1/medication-record/list/{medication-schedule-id}", scheduleId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].medicationRecordId").value(1L))
                .andExpect(jsonPath("$[1].medicationRecordId").value(2L));

        verify(medicationRecordService).getMedicationRecords(scheduleId);
    }

    @Test
    @DisplayName("Should return empty list when no medication records found")
    void getMedicationRecords_NoRecords_ShouldReturnEmptyList() throws Exception {
        // given
        Long scheduleId = 1L;
        when(medicationRecordService.getMedicationRecords(scheduleId)).thenReturn(Collections.emptyList());

        // when & then
        mockMvc.perform(get("/api/v1/medication-record/list/{medication-schedule-id}", scheduleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(medicationRecordService).getMedicationRecords(scheduleId);
    }

    @Test
    @DisplayName("Should get medication record by ID successfully")
    void getMedicationRecordById_ValidId_ShouldReturnRecord() throws Exception {
        // given
        Long recordId = 1L;
        MedicationRecord record = createTestMedicationRecord(recordId);
        
        when(medicationRecordService.getMedicationRecord(recordId)).thenReturn(record);

        // when & then
        mockMvc.perform(get("/api/v1/medication-record/{medication-record-id}", recordId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.medicationRecordId").value(recordId))
                .andExpect(jsonPath("$.medicationScheduleId").value(1L));

        verify(medicationRecordService).getMedicationRecord(recordId);
    }

    @Test
    @DisplayName("Should create medication record successfully")
    void createMedicationRecord_ValidRequest_ShouldReturnCreatedRecord() throws Exception {
        // given
        MedicationRecordCreateRequest request = new MedicationRecordCreateRequest(
                1L, 1L, true, LocalDateTime.now());
        
        MedicationRecord createdRecord = createTestMedicationRecord(1L);
        when(medicationRecordService.createMedicationRecord(any(MedicationRecordCreateRequest.class)))
                .thenReturn(createdRecord);

        // when & then
        mockMvc.perform(post("/api/v1/medication-record")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.medicationRecordId").value(1L));

        verify(medicationRecordService).createMedicationRecord(any(MedicationRecordCreateRequest.class));
    }

    @Test
    @DisplayName("Should create medication record with service call")
    void createMedicationRecord_ValidRequest_ShouldCallService() throws Exception {
        // given - valid request (Bean validation not tested in standalone MockMvc)
        MedicationRecordCreateRequest request = new MedicationRecordCreateRequest(
                1L, 1L, true, LocalDateTime.now());
        
        MedicationRecord createdRecord = createTestMedicationRecord(1L);
        when(medicationRecordService.createMedicationRecord(any(MedicationRecordCreateRequest.class)))
                .thenReturn(createdRecord);

        // when & then
        mockMvc.perform(post("/api/v1/medication-record")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(medicationRecordService).createMedicationRecord(any(MedicationRecordCreateRequest.class));
    }

    @Test
    @DisplayName("Should update medication record successfully")
    void updateMedicationRecord_ValidRequest_ShouldReturnUpdatedRecord() throws Exception {
        // given
        Long recordId = 1L;
        MedicationRecordUpdateRequest request = new MedicationRecordUpdateRequest(
                true, LocalDateTime.now());
        
        MedicationRecord updatedRecord = createTestMedicationRecord(recordId);
        when(medicationRecordService.updateMedicationRecord(eq(recordId), any(MedicationRecordUpdateRequest.class)))
                .thenReturn(updatedRecord);

        // when & then
        mockMvc.perform(put("/api/v1/medication-record/{medication-record-id}", recordId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.medicationRecordId").value(recordId));

        verify(medicationRecordService).updateMedicationRecord(eq(recordId), any(MedicationRecordUpdateRequest.class));
    }

    @Test
    @DisplayName("Should update medication record with service call")
    void updateMedicationRecord_ValidRequest_ShouldCallService() throws Exception {
        // given - valid request (Bean validation not tested in standalone MockMvc)
        Long recordId = 1L;
        MedicationRecordUpdateRequest request = new MedicationRecordUpdateRequest(
                true, LocalDateTime.now());
        
        MedicationRecord updatedRecord = createTestMedicationRecord(recordId);
        when(medicationRecordService.updateMedicationRecord(eq(recordId), any(MedicationRecordUpdateRequest.class)))
                .thenReturn(updatedRecord);

        // when & then
        mockMvc.perform(put("/api/v1/medication-record/{medication-record-id}", recordId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(medicationRecordService).updateMedicationRecord(eq(recordId), any(MedicationRecordUpdateRequest.class));
    }

    @Test
    @DisplayName("Should delete medication record successfully")
    void deleteMedicationRecord_ValidId_ShouldReturnNoContent() throws Exception {
        // given
        Long recordId = 1L;
        doNothing().when(medicationRecordService).deleteMedicationRecord(recordId);

        // when & then
        mockMvc.perform(delete("/api/v1/medication-record/{medication-record-id}", recordId))
                .andExpect(status().isNoContent());

        verify(medicationRecordService).deleteMedicationRecord(recordId);
    }

    @Test
    @DisplayName("Should handle service exceptions properly")
    void getMedicationRecord_ServiceException_ShouldReturnError() throws Exception {
        // given
        Long recordId = 1L;
        when(medicationRecordService.getMedicationRecord(recordId))
                .thenThrow(new RuntimeException("Service error"));

        // when & then
        mockMvc.perform(get("/api/v1/medication-record/{medication-record-id}", recordId))
                .andExpect(status().isInternalServerError());

        verify(medicationRecordService).getMedicationRecord(recordId);
    }

    @Test
    @DisplayName("Should return 400 for malformed JSON")
    void createMedicationRecord_MalformedJson_ShouldReturn400() throws Exception {
        // given - malformed JSON string
        String malformedJson = "{\"invalid\": " + "json" + "}";
        
        // when & then
        mockMvc.perform(post("/api/v1/medication-record")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(medicationRecordService);
    }

    @Test
    @DisplayName("Should return 400 for invalid path variable")
    void getMedicationRecord_InvalidPathVariable_ShouldReturn400() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/medication-record/invalid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(medicationRecordService);
    }

    private MedicationRecord createTestMedicationRecord(Long recordId) {
        // Create mocked MedicationSchedule
        MedicationSchedule mockSchedule = mock(MedicationSchedule.class);
        when(mockSchedule.getMedicationScheduleId()).thenReturn(1L);
        
        // Create mocked MedicationTime
        MedicationTime mockTime = mock(MedicationTime.class);
        when(mockTime.getMedicationTimeId()).thenReturn(1L);
        
        // Create mocked MedicationRecord
        MedicationRecord mockRecord = mock(MedicationRecord.class);
        when(mockRecord.getMedicationRecordId()).thenReturn(recordId);
        when(mockRecord.getMedicationSchedule()).thenReturn(mockSchedule);
        when(mockRecord.getMedicationTime()).thenReturn(mockTime);
        when(mockRecord.getIsTakeMedication()).thenReturn(true);
        when(mockRecord.getRealTakeTime()).thenReturn(LocalDateTime.now());
        
        return mockRecord;
    }
}