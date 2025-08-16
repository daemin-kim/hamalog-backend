package com.Hamalog.controller.medication;

import com.Hamalog.domain.medication.AlarmType;
import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.dto.medication.request.MedicationScheduleCreateRequest;
import com.Hamalog.dto.medication.request.MedicationScheduleUpdateRequest;
import com.Hamalog.service.medication.FileStorageService;
import com.Hamalog.service.medication.MedicationScheduleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(MedicationScheduleController.class)
@DisplayName("MedicationSchedule Controller Unit Tests")
class MedicationScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MedicationScheduleService medicationScheduleService;

    @MockitoBean
    private FileStorageService fileStorageService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("복약 스케줄 목록 조회 성공")
    @WithMockUser(username = "testuser")
    void getMedicationSchedules_Success() throws Exception {
        // Given
        Long memberId = 1L;
        List<MedicationSchedule> schedules = Arrays.asList();
        Page<MedicationSchedule> schedulePage = new PageImpl<>(schedules);
        
        given(medicationScheduleService.isOwner(memberId, "testuser"))
                .willReturn(true);
        given(medicationScheduleService.getMedicationSchedules(anyLong(), any(Pageable.class)))
                .willReturn(schedulePage);

        // When & Then
        mockMvc.perform(get("/medication-schedule/list/{member-id}", memberId)
                .with(user("testuser")))
                .andExpect(status().isOk());

        verify(medicationScheduleService).isOwner(memberId, "testuser");
        verify(medicationScheduleService).getMedicationSchedules(anyLong(), any(Pageable.class));
    }

    @Test
    @DisplayName("복약 스케줄 목록 조회 실패 - 권한 없음")
    @WithMockUser(username = "testuser")
    void getMedicationSchedules_Forbidden() throws Exception {
        // Given
        Long memberId = 1L;
        
        given(medicationScheduleService.isOwner(memberId, "testuser"))
                .willReturn(false);

        // When & Then
        mockMvc.perform(get("/medication-schedule/list/{member-id}", memberId)
                .with(user("testuser")))
                .andExpect(status().isForbidden());

        verify(medicationScheduleService).isOwner(memberId, "testuser");
    }

    @Test
    @DisplayName("복약 스케줄 상세 조회 성공")
    @WithMockUser(username = "testuser")
    void getMedicationScheduleById_Success() throws Exception {
        // Given
        Long medicationScheduleId = 1L;
        MedicationSchedule mockSchedule = new MedicationSchedule(null, "테스트 약물", "테스트 병원", 
                LocalDate.now(), "메모", LocalDate.now(), 7, 3, AlarmType.SOUND);
        
        given(medicationScheduleService.getMedicationSchedule(medicationScheduleId))
                .willReturn(mockSchedule);
        given(medicationScheduleService.isOwner(anyLong(), any(String.class)))
                .willReturn(true);

        // When & Then
        mockMvc.perform(get("/medication-schedule/{medication-schedule-id}", medicationScheduleId)
                .with(user("testuser")))
                .andExpect(status().isOk());

        verify(medicationScheduleService).getMedicationSchedule(medicationScheduleId);
    }

    @Test
    @DisplayName("복약 스케줄 상세 조회 실패 - 권한 없음")
    @WithMockUser(username = "testuser")
    void getMedicationScheduleById_Forbidden() throws Exception {
        // Given
        Long medicationScheduleId = 1L;
        MedicationSchedule mockSchedule = new MedicationSchedule(null, "테스트 약물", "테스트 병원", 
                LocalDate.now(), "메모", LocalDate.now(), 7, 3, AlarmType.SOUND);
        
        given(medicationScheduleService.getMedicationSchedule(medicationScheduleId))
                .willReturn(mockSchedule);
        given(medicationScheduleService.isOwner(anyLong(), any(String.class)))
                .willReturn(false);

        // When & Then
        mockMvc.perform(get("/medication-schedule/{medication-schedule-id}", medicationScheduleId)
                .with(user("testuser")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("복약 스케줄 생성 성공 - 이미지 파일 포함")
    @WithMockUser(username = "testuser")
    void createMedicationSchedule_SuccessWithImage() throws Exception {
        // Given
        MedicationScheduleCreateRequest createRequest = new MedicationScheduleCreateRequest(
                1L, "테스트 약물", "테스트 병원", "2024-01-01", "메모", 
                "2024-01-01", 7, 3, "SOUND");
        
        MockMultipartFile imageFile = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "test image content".getBytes());
        
        MockMultipartFile dataFile = new MockMultipartFile(
                "data", "", "application/json", objectMapper.writeValueAsBytes(createRequest));
        
        MedicationSchedule mockCreatedSchedule = new MedicationSchedule(null, "테스트 약물", "테스트 병원", 
                LocalDate.now(), "메모", LocalDate.now(), 7, 3, AlarmType.SOUND);
        
        given(medicationScheduleService.isOwner(createRequest.memberId(), "testuser"))
                .willReturn(true);
        given(medicationScheduleService.createMedicationSchedule(any(MedicationScheduleCreateRequest.class)))
                .willReturn(mockCreatedSchedule);

        // When & Then
        mockMvc.perform(multipart("/medication-schedule")
                .file(dataFile)
                .file(imageFile)
                .with(user("testuser"))
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated());

        verify(medicationScheduleService).isOwner(createRequest.memberId(), "testuser");
        verify(medicationScheduleService).createMedicationSchedule(any(MedicationScheduleCreateRequest.class));
    }

    @Test
    @DisplayName("복약 스케줄 생성 성공 - 이미지 파일 없이")
    @WithMockUser(username = "testuser")
    void createMedicationSchedule_SuccessWithoutImage() throws Exception {
        // Given
        MedicationScheduleCreateRequest createRequest = new MedicationScheduleCreateRequest(
                1L, "테스트 약물", "테스트 병원", "2024-01-01", "메모", 
                "2024-01-01", 7, 3, "SOUND");
        
        MockMultipartFile dataFile = new MockMultipartFile(
                "data", "", "application/json", objectMapper.writeValueAsBytes(createRequest));
        
        MedicationSchedule mockCreatedSchedule = new MedicationSchedule(null, "테스트 약물", "테스트 병원", 
                LocalDate.now(), "메모", LocalDate.now(), 7, 3, AlarmType.SOUND);
        
        given(medicationScheduleService.isOwner(createRequest.memberId(), "testuser"))
                .willReturn(true);
        given(medicationScheduleService.createMedicationSchedule(any(MedicationScheduleCreateRequest.class)))
                .willReturn(mockCreatedSchedule);

        // When & Then
        mockMvc.perform(multipart("/medication-schedule")
                .file(dataFile)
                .with(user("testuser"))
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated());

        verify(medicationScheduleService).isOwner(createRequest.memberId(), "testuser");
        verify(medicationScheduleService).createMedicationSchedule(any(MedicationScheduleCreateRequest.class));
    }

    @Test
    @DisplayName("복약 스케줄 생성 실패 - 권한 없음")
    @WithMockUser(username = "testuser")
    void createMedicationSchedule_Forbidden() throws Exception {
        // Given
        MedicationScheduleCreateRequest createRequest = new MedicationScheduleCreateRequest(
                1L, "테스트 약물", "테스트 병원", "2024-01-01", "메모", 
                "2024-01-01", 7, 3, "SOUND");
        
        MockMultipartFile dataFile = new MockMultipartFile(
                "data", "", "application/json", objectMapper.writeValueAsBytes(createRequest));
        
        given(medicationScheduleService.isOwner(createRequest.memberId(), "testuser"))
                .willReturn(false);

        // When & Then
        mockMvc.perform(multipart("/medication-schedule")
                .file(dataFile)
                .with(user("testuser"))
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isForbidden());

        verify(medicationScheduleService).isOwner(createRequest.memberId(), "testuser");
    }

    @Test
    @DisplayName("복약 스케줄 수정 성공")
    @WithMockUser(username = "testuser")
    void updateMedicationSchedule_Success() throws Exception {
        // Given
        Long medicationScheduleId = 1L;
        MedicationScheduleUpdateRequest updateRequest = new MedicationScheduleUpdateRequest(
                "수정된 약물", "수정된 병원", LocalDate.now(), "수정된 메모", 
                LocalDate.now(), 10, 2, AlarmType.VIBE);
        
        MedicationSchedule mockSchedule = new MedicationSchedule(null, "테스트 약물", "테스트 병원", 
                LocalDate.now(), "메모", LocalDate.now(), 7, 3, AlarmType.SOUND);
        
        MedicationSchedule mockUpdatedSchedule = new MedicationSchedule(null, "수정된 약물", "수정된 병원", 
                LocalDate.now(), "수정된 메모", LocalDate.now(), 10, 2, AlarmType.VIBE);
        
        given(medicationScheduleService.getMedicationSchedule(medicationScheduleId))
                .willReturn(mockSchedule);
        given(medicationScheduleService.isOwner(anyLong(), any(String.class)))
                .willReturn(true);
        given(medicationScheduleService.updateMedicationSchedule(anyLong(), any(MedicationScheduleUpdateRequest.class)))
                .willReturn(mockUpdatedSchedule);

        // When & Then
        mockMvc.perform(put("/medication-schedule/{medication-schedule-id}", medicationScheduleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .with(user("testuser")))
                .andExpect(status().isOk());

        verify(medicationScheduleService).getMedicationSchedule(medicationScheduleId);
        verify(medicationScheduleService).updateMedicationSchedule(anyLong(), any(MedicationScheduleUpdateRequest.class));
    }

    @Test
    @DisplayName("복약 스케줄 수정 실패 - 권한 없음")
    @WithMockUser(username = "testuser")
    void updateMedicationSchedule_Forbidden() throws Exception {
        // Given
        Long medicationScheduleId = 1L;
        MedicationScheduleUpdateRequest updateRequest = new MedicationScheduleUpdateRequest(
                "수정된 약물", "수정된 병원", LocalDate.now(), "수정된 메모", 
                LocalDate.now(), 10, 2, AlarmType.VIBE);
        
        MedicationSchedule mockSchedule = new MedicationSchedule(null, "테스트 약물", "테스트 병원", 
                LocalDate.now(), "메모", LocalDate.now(), 7, 3, AlarmType.SOUND);
        
        given(medicationScheduleService.getMedicationSchedule(medicationScheduleId))
                .willReturn(mockSchedule);
        given(medicationScheduleService.isOwner(anyLong(), any(String.class)))
                .willReturn(false);

        // When & Then
        mockMvc.perform(put("/medication-schedule/{medication-schedule-id}", medicationScheduleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .with(user("testuser")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("복약 스케줄 삭제 성공")
    @WithMockUser(username = "testuser")
    void deleteMedicationSchedule_Success() throws Exception {
        // Given
        Long medicationScheduleId = 1L;
        MedicationSchedule mockSchedule = new MedicationSchedule(null, "테스트 약물", "테스트 병원", 
                LocalDate.now(), "메모", LocalDate.now(), 7, 3, AlarmType.SOUND);
        
        given(medicationScheduleService.getMedicationSchedule(medicationScheduleId))
                .willReturn(mockSchedule);
        given(medicationScheduleService.isOwner(anyLong(), any(String.class)))
                .willReturn(true);
        willDoNothing().given(medicationScheduleService).deleteMedicationSchedule(medicationScheduleId);

        // When & Then
        mockMvc.perform(delete("/medication-schedule/{medication-schedule-id}", medicationScheduleId)
                .with(user("testuser")))
                .andExpect(status().isNoContent());

        verify(medicationScheduleService).getMedicationSchedule(medicationScheduleId);
        verify(medicationScheduleService).deleteMedicationSchedule(medicationScheduleId);
    }

    @Test
    @DisplayName("복약 스케줄 삭제 실패 - 권한 없음")
    @WithMockUser(username = "testuser")
    void deleteMedicationSchedule_Forbidden() throws Exception {
        // Given
        Long medicationScheduleId = 1L;
        MedicationSchedule mockSchedule = new MedicationSchedule(null, "테스트 약물", "테스트 병원", 
                LocalDate.now(), "메모", LocalDate.now(), 7, 3, AlarmType.SOUND);
        
        given(medicationScheduleService.getMedicationSchedule(medicationScheduleId))
                .willReturn(mockSchedule);
        given(medicationScheduleService.isOwner(anyLong(), any(String.class)))
                .willReturn(false);

        // When & Then
        mockMvc.perform(delete("/medication-schedule/{medication-schedule-id}", medicationScheduleId)
                .with(user("testuser")))
                .andExpect(status().isForbidden());
    }
}