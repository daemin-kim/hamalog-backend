package com.Hamalog.controller.medication;

import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.dto.medication.request.MedicationScheduleCreateRequest;
import com.Hamalog.dto.medication.request.MedicationScheduleUpdateRequest;
import com.Hamalog.service.medication.FileStorageService;
import com.Hamalog.service.medication.MedicationScheduleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("Medication Schedule Controller Tests")
class MedicationScheduleControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MedicationScheduleService medicationScheduleService;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private MedicationScheduleController medicationScheduleController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(medicationScheduleController)
                .setCustomArgumentResolvers(new MockUserDetailsArgumentResolver(), new MockPageableArgumentResolver())
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
            UserDetails mockUserDetails = mock(UserDetails.class);
            lenient().when(mockUserDetails.getUsername()).thenReturn("testuser");
            return mockUserDetails;
        }
    }

    /**
     * Custom argument resolver for Pageable parameters
     */
    private static class MockPageableArgumentResolver implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return Pageable.class.isAssignableFrom(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                    NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            String page = webRequest.getParameter("page");
            String size = webRequest.getParameter("size");
            
            int pageNumber = page != null ? Integer.parseInt(page) : 0;
            int pageSize = size != null ? Integer.parseInt(size) : 20;
            
            return PageRequest.of(pageNumber, pageSize);
        }
    }

    /**
     * Simple exception resolver for handling service exceptions
     */
    private static class SimpleExceptionResolver implements HandlerExceptionResolver {

        @Override
        public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response,
                                           Object handler, Exception ex) {
            if (ex.getClass().getSimpleName().contains("MethodArgumentNotValid") ||
                ex.getClass().getSimpleName().contains("TypeMismatch") ||
                ex.getClass().getSimpleName().contains("HttpMessageNotReadable") ||
                ex.getClass().getSimpleName().contains("MissingServletRequest") ||
                ex.getClass().getSimpleName().contains("ConstraintViolation")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return new ModelAndView();
            }
            else if (ex instanceof RuntimeException) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return new ModelAndView();
            }
            return null;
        }
    }

    @Test
    @DisplayName("Should get medication schedules by member ID successfully")
    void getMedicationSchedules_ValidMemberId_ShouldReturnScheduleList() throws Exception {
        // given
        Long memberId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        MedicationSchedule schedule1 = createTestMedicationSchedule(1L);
        MedicationSchedule schedule2 = createTestMedicationSchedule(2L);
        List<MedicationSchedule> schedules = Arrays.asList(schedule1, schedule2);
        Page<MedicationSchedule> schedulePage = new PageImpl<>(schedules, pageable, schedules.size());

        when(medicationScheduleService.getMedicationSchedules(eq(memberId), any(Pageable.class)))
                .thenReturn(schedulePage);

        // when & then
        mockMvc.perform(get("/medication-schedule/list/{member-id}", memberId)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.schedules.length()").value(2))
                .andExpect(jsonPath("$.schedules[0].medicationScheduleId").value(1L))
                .andExpect(jsonPath("$.schedules[1].medicationScheduleId").value(2L))
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(10));

        verify(medicationScheduleService).getMedicationSchedules(eq(memberId), any(Pageable.class));
    }

    @Test
    @DisplayName("Should return empty list when no medication schedules found")
    void getMedicationSchedules_NoSchedules_ShouldReturnEmptyList() throws Exception {
        // given
        Long memberId = 1L;
        Page<MedicationSchedule> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);

        when(medicationScheduleService.getMedicationSchedules(eq(memberId), any(Pageable.class)))
                .thenReturn(emptyPage);

        // when & then
        mockMvc.perform(get("/medication-schedule/list/{member-id}", memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schedules.length()").value(0))
                .andExpect(jsonPath("$.totalCount").value(0));

        verify(medicationScheduleService).getMedicationSchedules(eq(memberId), any(Pageable.class));
    }

    @Test
    @DisplayName("Should get medication schedule by ID successfully")
    void getMedicationScheduleById_ValidId_ShouldReturnSchedule() throws Exception {
        // given
        Long scheduleId = 1L;
        MedicationSchedule schedule = createTestMedicationSchedule(scheduleId);

        when(medicationScheduleService.getMedicationSchedule(scheduleId)).thenReturn(schedule);

        // when & then
        mockMvc.perform(get("/medication-schedule/{medication-schedule-id}", scheduleId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.medicationScheduleId").value(scheduleId))
                .andExpect(jsonPath("$.name").value("Test Medication"));

        verify(medicationScheduleService).getMedicationSchedule(scheduleId);
    }

    @Test
    @DisplayName("Should create medication schedule successfully without image")
    void createMedicationSchedule_ValidRequestWithoutImage_ShouldReturnCreatedSchedule() throws Exception {
        // given
        MedicationScheduleCreateRequest request = createTestCreateRequest();
        MedicationSchedule createdSchedule = createTestMedicationSchedule(1L);

        when(medicationScheduleService.createMedicationSchedule(any(MedicationScheduleCreateRequest.class)))
                .thenReturn(createdSchedule);

        // when & then
        mockMvc.perform(multipart("/medication-schedule")
                .part(createJsonPart("data", objectMapper.writeValueAsString(request))))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.medicationScheduleId").value(1L))
                .andExpect(jsonPath("$.name").value("Test Medication"));

        verify(medicationScheduleService).createMedicationSchedule(any(MedicationScheduleCreateRequest.class));
    }

    @Test
    @DisplayName("Should create medication schedule successfully with image")
    void createMedicationSchedule_ValidRequestWithImage_ShouldReturnCreatedSchedule() throws Exception {
        // given
        MedicationScheduleCreateRequest request = createTestCreateRequest();
        MedicationSchedule createdSchedule = createTestMedicationSchedule(1L);

        // Create a valid JPEG file header (Magic Number: FF D8 FF E0 ...)
        byte[] validJpegHeader = new byte[] {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, // JPEG magic number
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,     // JFIF header
            0x01, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00      // Additional JPEG data
        };
        MockMultipartFile imageFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", validJpegHeader);

        when(medicationScheduleService.createMedicationSchedule(any(MedicationScheduleCreateRequest.class)))
                .thenReturn(createdSchedule);

        // when & then
        mockMvc.perform(multipart("/medication-schedule")
                .file(imageFile)
                .part(createJsonPart("data", objectMapper.writeValueAsString(request))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.medicationScheduleId").value(1L));

        verify(medicationScheduleService).createMedicationSchedule(any(MedicationScheduleCreateRequest.class));
    }

    @Test
    @DisplayName("Should update medication schedule successfully")
    void updateMedicationSchedule_ValidRequest_ShouldReturnUpdatedSchedule() throws Exception {
        // given
        Long scheduleId = 1L;
        MedicationScheduleUpdateRequest request = createTestUpdateRequest();
        MedicationSchedule updatedSchedule = createTestMedicationSchedule(scheduleId);

        when(medicationScheduleService.updateMedicationSchedule(eq(scheduleId), any(MedicationScheduleUpdateRequest.class)))
                .thenReturn(updatedSchedule);

        // when & then
        mockMvc.perform(put("/medication-schedule/{medication-schedule-id}", scheduleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.medicationScheduleId").value(scheduleId));

        verify(medicationScheduleService).updateMedicationSchedule(eq(scheduleId), any(MedicationScheduleUpdateRequest.class));
    }

    @Test
    @DisplayName("Should delete medication schedule successfully")
    void deleteMedicationSchedule_ValidId_ShouldReturnNoContent() throws Exception {
        // given
        Long scheduleId = 1L;
        doNothing().when(medicationScheduleService).deleteMedicationSchedule(scheduleId);

        // when & then
        mockMvc.perform(delete("/medication-schedule/{medication-schedule-id}", scheduleId))
                .andExpect(status().isNoContent());

        verify(medicationScheduleService).deleteMedicationSchedule(scheduleId);
    }

    @Test
    @DisplayName("Should handle service exceptions properly")
    void getMedicationSchedule_ServiceException_ShouldReturnError() throws Exception {
        // given
        Long scheduleId = 1L;
        when(medicationScheduleService.getMedicationSchedule(scheduleId))
                .thenThrow(new RuntimeException("Schedule not found"));

        // when & then
        mockMvc.perform(get("/medication-schedule/{medication-schedule-id}", scheduleId))
                .andExpect(status().isInternalServerError());

        verify(medicationScheduleService).getMedicationSchedule(scheduleId);
    }

    @Test
    @DisplayName("Should return 400 for invalid path variable")
    void getMedicationSchedule_InvalidPathVariable_ShouldReturn400() throws Exception {
        // when & then
        mockMvc.perform(get("/medication-schedule/invalid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(medicationScheduleService);
    }

    @Test
    @DisplayName("Should handle pagination parameters correctly")
    void getMedicationSchedules_WithPagination_ShouldPassCorrectParameters() throws Exception {
        // given
        Long memberId = 1L;
        Page<MedicationSchedule> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(1, 5), 0);

        when(medicationScheduleService.getMedicationSchedules(eq(memberId), any(Pageable.class)))
                .thenReturn(emptyPage);

        // when & then
        mockMvc.perform(get("/medication-schedule/list/{member-id}", memberId)
                .param("page", "1")
                .param("size", "5")
                .param("sort", "medicationName,asc"))
                .andExpect(status().isOk());

        verify(medicationScheduleService).getMedicationSchedules(eq(memberId), any(Pageable.class));
    }

    @Test
    @DisplayName("Should return 400 for malformed JSON in update request")
    void updateMedicationSchedule_MalformedJson_ShouldReturn400() throws Exception {
        // given
        Long scheduleId = 1L;
        String malformedJson = "{\"invalid\": " + "json" + "}";

        // when & then
        mockMvc.perform(put("/medication-schedule/{medication-schedule-id}", scheduleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(medicationScheduleService);
    }

    @Test
    @DisplayName("Should handle multipart request without data part")
    void createMedicationSchedule_MissingDataPart_ShouldReturn400() throws Exception {
        // given
        MockMultipartFile imageFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", "test image content".getBytes());

        // when & then
        mockMvc.perform(multipart("/medication-schedule")
                .file(imageFile))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(medicationScheduleService);
    }

    // Helper methods
    private MedicationSchedule createTestMedicationSchedule(Long scheduleId) {
        // Mock Member object
        com.Hamalog.domain.member.Member mockMember = mock(com.Hamalog.domain.member.Member.class);
        when(mockMember.getMemberId()).thenReturn(1L);

        // Mock MedicationSchedule
        MedicationSchedule mockSchedule = mock(MedicationSchedule.class);
        when(mockSchedule.getMedicationScheduleId()).thenReturn(scheduleId);
        when(mockSchedule.getMember()).thenReturn(mockMember);
        when(mockSchedule.getName()).thenReturn("Test Medication");
        when(mockSchedule.getHospitalName()).thenReturn("Test Hospital");
        when(mockSchedule.getPrescriptionDate()).thenReturn(java.time.LocalDate.now());
        when(mockSchedule.getMemo()).thenReturn("Test memo");
        when(mockSchedule.getStartOfAd()).thenReturn(java.time.LocalDate.now());
        when(mockSchedule.getPrescriptionDays()).thenReturn(7);
        when(mockSchedule.getPerDay()).thenReturn(2);
        when(mockSchedule.getAlarmType()).thenReturn(com.Hamalog.domain.medication.AlarmType.SOUND);
        return mockSchedule;
    }

    private MedicationScheduleCreateRequest createTestCreateRequest() {
        return new MedicationScheduleCreateRequest(
                1L, // memberId
                "Test Medication", // name
                "Test Hospital", // hospitalName
                "2024-01-01", // prescriptionDate
                "Test memo", // memo
                "2024-01-02", // startOfAd
                7, // prescriptionDays
                2, // perDay
                "SOUND" // alarmType
        );
    }

    private MedicationScheduleUpdateRequest createTestUpdateRequest() {
        return new MedicationScheduleUpdateRequest(
                "Updated Medication", // name
                "Updated Hospital", // hospitalName
                java.time.LocalDate.now(), // prescriptionDate
                "Updated memo", // memo
                java.time.LocalDate.now().plusDays(1), // startOfAd
                10, // prescriptionDays
                3, // perDay
                com.Hamalog.domain.medication.AlarmType.VIBE // alarmType
        );
    }

    private org.springframework.mock.web.MockPart createJsonPart(String name, String content) {
        org.springframework.mock.web.MockPart part = new org.springframework.mock.web.MockPart(name, content.getBytes());
        part.getHeaders().set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        return part;
    }
}