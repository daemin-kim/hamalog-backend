package com.Hamalog.controller.medication;

import com.Hamalog.domain.medication.MedicationRecord;
import com.Hamalog.dto.medication.request.MedicationRecordCreateRequest;
import com.Hamalog.dto.medication.request.MedicationRecordUpdateRequest;
import com.Hamalog.service.medication.MedicationRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
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
@WebMvcTest(MedicationRecordController.class)
@DisplayName("MedicationRecord Controller Unit Tests")
class MedicationRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MedicationRecordService medicationRecordService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("복약 기록 목록 조회 성공")
    @WithMockUser(username = "testuser")
    void getMedicationRecords_Success() throws Exception {
        // Given
        Long medicationScheduleId = 1L;
        List<MedicationRecord> mockRecords = Arrays.asList();
        
        given(medicationRecordService.isOwnerOfSchedule(medicationScheduleId, "testuser"))
                .willReturn(true);
        given(medicationRecordService.getMedicationRecords(medicationScheduleId))
                .willReturn(mockRecords);

        // When & Then
        mockMvc.perform(get("/medication-record/list/{medication-schedule-id}", medicationScheduleId)
                .with(user("testuser")))
                .andExpect(status().isOk());

        verify(medicationRecordService).isOwnerOfSchedule(medicationScheduleId, "testuser");
        verify(medicationRecordService).getMedicationRecords(medicationScheduleId);
    }

    @Test
    @DisplayName("복약 기록 목록 조회 실패 - 권한 없음")
    @WithMockUser(username = "testuser")
    void getMedicationRecords_Forbidden() throws Exception {
        // Given
        Long medicationScheduleId = 1L;
        
        given(medicationRecordService.isOwnerOfSchedule(medicationScheduleId, "testuser"))
                .willReturn(false);

        // When & Then
        mockMvc.perform(get("/medication-record/list/{medication-schedule-id}", medicationScheduleId)
                .with(user("testuser")))
                .andExpect(status().isForbidden());

        verify(medicationRecordService).isOwnerOfSchedule(medicationScheduleId, "testuser");
    }

    @Test
    @DisplayName("복약 기록 상세 조회 성공")
    @WithMockUser(username = "testuser")
    void getMedicationRecordById_Success() throws Exception {
        // Given
        Long medicationRecordId = 1L;
        MedicationRecord mockRecord = new MedicationRecord(null, null, true, LocalDateTime.now());
        
        given(medicationRecordService.isOwnerOfRecord(medicationRecordId, "testuser"))
                .willReturn(true);
        given(medicationRecordService.getMedicationRecord(medicationRecordId))
                .willReturn(mockRecord);

        // When & Then
        mockMvc.perform(get("/medication-record/{medication-record-id}", medicationRecordId)
                .with(user("testuser")))
                .andExpect(status().isOk());

        verify(medicationRecordService).isOwnerOfRecord(medicationRecordId, "testuser");
        verify(medicationRecordService).getMedicationRecord(medicationRecordId);
    }

    @Test
    @DisplayName("복약 기록 상세 조회 실패 - 권한 없음")
    @WithMockUser(username = "testuser")
    void getMedicationRecordById_Forbidden() throws Exception {
        // Given
        Long medicationRecordId = 1L;
        
        given(medicationRecordService.isOwnerOfRecord(medicationRecordId, "testuser"))
                .willReturn(false);

        // When & Then
        mockMvc.perform(get("/medication-record/{medication-record-id}", medicationRecordId)
                .with(user("testuser")))
                .andExpect(status().isForbidden());

        verify(medicationRecordService).isOwnerOfRecord(medicationRecordId, "testuser");
    }

    @Test
    @DisplayName("복약 기록 생성 성공")
    @WithMockUser(username = "testuser")
    void createMedicationRecord_Success() throws Exception {
        // Given
        MedicationRecordCreateRequest createRequest = new MedicationRecordCreateRequest(
                1L, 1L, true, LocalDateTime.now()
        );
        
        MedicationRecord mockCreatedRecord = new MedicationRecord(null, null, true, LocalDateTime.now());
        
        given(medicationRecordService.isOwnerOfSchedule(createRequest.medicationScheduleId(), "testuser"))
                .willReturn(true);
        given(medicationRecordService.createMedicationRecord(any(MedicationRecordCreateRequest.class)))
                .willReturn(mockCreatedRecord);

        // When & Then
        mockMvc.perform(post("/medication-record")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
                .with(user("testuser")))
                .andExpect(status().isCreated());

        verify(medicationRecordService).isOwnerOfSchedule(createRequest.medicationScheduleId(), "testuser");
        verify(medicationRecordService).createMedicationRecord(any(MedicationRecordCreateRequest.class));
    }

    @Test
    @DisplayName("복약 기록 생성 실패 - 권한 없음")
    @WithMockUser(username = "testuser")
    void createMedicationRecord_Forbidden() throws Exception {
        // Given
        MedicationRecordCreateRequest createRequest = new MedicationRecordCreateRequest(
                1L, 1L, true, LocalDateTime.now()
        );
        
        given(medicationRecordService.isOwnerOfSchedule(createRequest.medicationScheduleId(), "testuser"))
                .willReturn(false);

        // When & Then
        mockMvc.perform(post("/medication-record")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
                .with(user("testuser")))
                .andExpect(status().isForbidden());

        verify(medicationRecordService).isOwnerOfSchedule(createRequest.medicationScheduleId(), "testuser");
    }

    @Test
    @DisplayName("복약 기록 수정 성공")
    @WithMockUser(username = "testuser")
    void updateMedicationRecord_Success() throws Exception {
        // Given
        Long medicationRecordId = 1L;
        MedicationRecordUpdateRequest updateRequest = new MedicationRecordUpdateRequest(
                false, LocalDateTime.now()
        );
        
        MedicationRecord mockUpdatedRecord = new MedicationRecord(null, null, false, LocalDateTime.now());
        
        given(medicationRecordService.isOwnerOfRecord(medicationRecordId, "testuser"))
                .willReturn(true);
        given(medicationRecordService.updateMedicationRecord(anyLong(), any(MedicationRecordUpdateRequest.class)))
                .willReturn(mockUpdatedRecord);

        // When & Then
        mockMvc.perform(put("/medication-record/{medication-record-id}", medicationRecordId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .with(user("testuser")))
                .andExpect(status().isOk());

        verify(medicationRecordService).isOwnerOfRecord(medicationRecordId, "testuser");
        verify(medicationRecordService).updateMedicationRecord(anyLong(), any(MedicationRecordUpdateRequest.class));
    }

    @Test
    @DisplayName("복약 기록 수정 실패 - 권한 없음")
    @WithMockUser(username = "testuser")
    void updateMedicationRecord_Forbidden() throws Exception {
        // Given
        Long medicationRecordId = 1L;
        MedicationRecordUpdateRequest updateRequest = new MedicationRecordUpdateRequest(
                false, LocalDateTime.now()
        );
        
        given(medicationRecordService.isOwnerOfRecord(medicationRecordId, "testuser"))
                .willReturn(false);

        // When & Then
        mockMvc.perform(put("/medication-record/{medication-record-id}", medicationRecordId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .with(user("testuser")))
                .andExpect(status().isForbidden());

        verify(medicationRecordService).isOwnerOfRecord(medicationRecordId, "testuser");
    }

    @Test
    @DisplayName("복약 기록 삭제 성공")
    @WithMockUser(username = "testuser")
    void deleteMedicationRecord_Success() throws Exception {
        // Given
        Long medicationRecordId = 1L;
        
        given(medicationRecordService.isOwnerOfRecord(medicationRecordId, "testuser"))
                .willReturn(true);
        willDoNothing().given(medicationRecordService).deleteMedicationRecord(medicationRecordId);

        // When & Then
        mockMvc.perform(delete("/medication-record/{medication-record-id}", medicationRecordId)
                .with(user("testuser")))
                .andExpect(status().isNoContent());

        verify(medicationRecordService).isOwnerOfRecord(medicationRecordId, "testuser");
        verify(medicationRecordService).deleteMedicationRecord(medicationRecordId);
    }

    @Test
    @DisplayName("복약 기록 삭제 실패 - 권한 없음")
    @WithMockUser(username = "testuser")
    void deleteMedicationRecord_Forbidden() throws Exception {
        // Given
        Long medicationRecordId = 1L;
        
        given(medicationRecordService.isOwnerOfRecord(medicationRecordId, "testuser"))
                .willReturn(false);

        // When & Then
        mockMvc.perform(delete("/medication-record/{medication-record-id}", medicationRecordId)
                .with(user("testuser")))
                .andExpect(status().isForbidden());

        verify(medicationRecordService).isOwnerOfRecord(medicationRecordId, "testuser");
    }
}