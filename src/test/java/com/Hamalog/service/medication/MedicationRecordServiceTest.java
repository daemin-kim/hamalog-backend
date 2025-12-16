package com.Hamalog.service.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.Hamalog.domain.medication.MedicationRecord;
import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.domain.medication.MedicationTime;
import com.Hamalog.domain.member.Member;
import com.Hamalog.dto.medication.request.MedicationRecordCreateRequest;
import com.Hamalog.dto.medication.request.MedicationRecordUpdateRequest;
import com.Hamalog.exception.medication.MedicationRecordNotFoundException;
import com.Hamalog.exception.medication.MedicationScheduleNotFoundException;
import com.Hamalog.exception.medication.MedicationTimeNotFoundException;
import com.Hamalog.repository.medication.MedicationRecordRepository;
import com.Hamalog.repository.medication.MedicationScheduleRepository;
import com.Hamalog.repository.medication.MedicationTimeRepository;
import com.Hamalog.repository.member.MemberRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("MedicationRecordService Tests")
class MedicationRecordServiceTest {

    @Mock
    private MedicationRecordRepository medicationRecordRepository;
    
    @Mock
    private MedicationScheduleRepository medicationScheduleRepository;
    
    @Mock
    private MedicationTimeRepository medicationTimeRepository;
    
    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MedicationRecordService medicationRecordService;

    private Member mockMember;
    private MedicationSchedule mockSchedule;
    private MedicationTime mockTime;
    private MedicationRecord mockRecord;
    private MedicationRecordCreateRequest createRequest;
    private MedicationRecordUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        // Create mock objects - no stubbing here to avoid unnecessary stubbing exceptions
        mockMember = mock(Member.class);
        mockSchedule = mock(MedicationSchedule.class);
        mockTime = mock(MedicationTime.class);
        mockRecord = mock(MedicationRecord.class);

        // Create request objects
        createRequest = new MedicationRecordCreateRequest(1L, 1L, true, LocalDateTime.now());
        updateRequest = new MedicationRecordUpdateRequest(true, LocalDateTime.now());
    }

    @Test
    @DisplayName("Should get medication records by schedule ID successfully")
    void getMedicationRecords_WithValidScheduleId_ShouldReturnRecords() {
        // given
        Long scheduleId = 1L;
        List<MedicationRecord> expectedRecords = Arrays.asList(mockRecord);
        when(medicationScheduleRepository.existsById(scheduleId)).thenReturn(true);
        when(medicationRecordRepository.findAllByMedicationSchedule_MedicationScheduleId(scheduleId))
                .thenReturn(expectedRecords);

        // when
        List<MedicationRecord> result = medicationRecordService.getMedicationRecords(scheduleId);

        // then
        assertThat(result).isEqualTo(expectedRecords);
        verify(medicationScheduleRepository).existsById(scheduleId);
        verify(medicationRecordRepository).findAllByMedicationSchedule_MedicationScheduleId(scheduleId);
    }

    @Test
    @DisplayName("Should get medication record by ID successfully")
    void getMedicationRecord_WithValidId_ShouldReturnRecord() {
        // given
        Long recordId = 1L;
        when(medicationRecordRepository.findById(recordId)).thenReturn(Optional.of(mockRecord));

        // when
        MedicationRecord result = medicationRecordService.getMedicationRecord(recordId);

        // then
        assertThat(result).isEqualTo(mockRecord);
        verify(medicationRecordRepository).findById(recordId);
    }

    @Test
    @DisplayName("Should throw exception when medication record not found")
    void getMedicationRecord_WithInvalidId_ShouldThrowException() {
        // given
        Long recordId = 999L;
        when(medicationRecordRepository.findById(recordId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> medicationRecordService.getMedicationRecord(recordId))
                .isInstanceOf(MedicationRecordNotFoundException.class);
        
        verify(medicationRecordRepository).findById(recordId);
    }

    @Test
    @DisplayName("Should create medication record successfully")
    void createMedicationRecord_WithValidRequest_ShouldReturnSavedRecord() {
        // given
        when(medicationScheduleRepository.findById(createRequest.medicationScheduleId()))
                .thenReturn(Optional.of(mockSchedule));
        when(medicationTimeRepository.findById(createRequest.medicationTimeId()))
                .thenReturn(Optional.of(mockTime));

        // Mock the relationship between MedicationTime and MedicationSchedule
        when(mockTime.getMedicationSchedule()).thenReturn(mockSchedule);
        when(mockSchedule.getMedicationScheduleId()).thenReturn(1L);

        when(medicationRecordRepository.save(any(MedicationRecord.class))).thenReturn(mockRecord);

        // when
        MedicationRecord result = medicationRecordService.createMedicationRecord(createRequest);

        // then
        assertThat(result).isEqualTo(mockRecord);
        verify(medicationScheduleRepository).findById(createRequest.medicationScheduleId());
        verify(medicationTimeRepository).findById(createRequest.medicationTimeId());
        verify(medicationRecordRepository).save(any(MedicationRecord.class));
    }

    @Test
    @DisplayName("Should throw exception when medication schedule not found during creation")
    void createMedicationRecord_WithInvalidScheduleId_ShouldThrowException() {
        // given
        when(medicationScheduleRepository.findById(createRequest.medicationScheduleId()))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> medicationRecordService.createMedicationRecord(createRequest))
                .isInstanceOf(MedicationScheduleNotFoundException.class);
        
        verify(medicationScheduleRepository).findById(createRequest.medicationScheduleId());
        verify(medicationTimeRepository, never()).findById(anyLong());
        verify(medicationRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when medication time not found during creation")
    void createMedicationRecord_WithInvalidTimeId_ShouldThrowException() {
        // given
        when(medicationScheduleRepository.findById(createRequest.medicationScheduleId()))
                .thenReturn(Optional.of(mockSchedule));
        when(medicationTimeRepository.findById(createRequest.medicationTimeId()))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> medicationRecordService.createMedicationRecord(createRequest))
                .isInstanceOf(MedicationTimeNotFoundException.class);
        
        verify(medicationScheduleRepository).findById(createRequest.medicationScheduleId());
        verify(medicationTimeRepository).findById(createRequest.medicationTimeId());
        verify(medicationRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update medication record successfully")
    void updateMedicationRecord_WithValidData_ShouldReturnUpdatedRecord() {
        // given
        Long recordId = 1L;
        when(medicationRecordRepository.findById(recordId)).thenReturn(Optional.of(mockRecord));
        when(medicationRecordRepository.save(mockRecord)).thenReturn(mockRecord);

        // when
        MedicationRecord result = medicationRecordService.updateMedicationRecord(recordId, updateRequest);

        // then
        assertThat(result).isEqualTo(mockRecord);
        verify(medicationRecordRepository).findById(recordId);
        verify(mockRecord).update(updateRequest.isTakeMedication(), updateRequest.realTakeTime());
        verify(medicationRecordRepository).save(mockRecord);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent medication record")
    void updateMedicationRecord_WithInvalidId_ShouldThrowException() {
        // given
        Long recordId = 999L;
        when(medicationRecordRepository.findById(recordId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> medicationRecordService.updateMedicationRecord(recordId, updateRequest))
                .isInstanceOf(MedicationRecordNotFoundException.class);
        
        verify(medicationRecordRepository).findById(recordId);
        verify(medicationRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete medication record successfully")
    void deleteMedicationRecord_WithValidId_ShouldDeleteRecord() {
        // given
        Long recordId = 1L;
        when(medicationRecordRepository.findById(recordId)).thenReturn(Optional.of(mockRecord));

        // when
        medicationRecordService.deleteMedicationRecord(recordId);

        // then
        verify(medicationRecordRepository).findById(recordId);
        verify(medicationRecordRepository).delete(mockRecord);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent medication record")
    void deleteMedicationRecord_WithInvalidId_ShouldThrowException() {
        // given
        Long recordId = 999L;
        when(medicationRecordRepository.findById(recordId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> medicationRecordService.deleteMedicationRecord(recordId))
                .isInstanceOf(MedicationRecordNotFoundException.class);
        
        verify(medicationRecordRepository).findById(recordId);
        verify(medicationRecordRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should return true when user is owner of schedule")
    void isOwnerOfSchedule_WithValidOwner_ShouldReturnTrue() {
        // given
        Long scheduleId = 1L;
        String loginId = "testUser";
        when(medicationScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(mockSchedule));
        when(mockSchedule.getMember()).thenReturn(mockMember);
        when(mockMember.getLoginId()).thenReturn(loginId);

        // when
        boolean result = medicationRecordService.isOwnerOfSchedule(scheduleId, loginId);

        // then
        assertThat(result).isTrue();
        verify(medicationScheduleRepository).findById(scheduleId);
        verify(mockSchedule).getMember();
        verify(mockMember).getLoginId();
    }

    @Test
    @DisplayName("Should return false when user is not owner of schedule")
    void isOwnerOfSchedule_WithInvalidOwner_ShouldReturnFalse() {
        // given
        Long scheduleId = 1L;
        String loginId = "otherUser";
        when(medicationScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(mockSchedule));
        when(mockSchedule.getMember()).thenReturn(mockMember);
        when(mockMember.getLoginId()).thenReturn("testUser");

        // when
        boolean result = medicationRecordService.isOwnerOfSchedule(scheduleId, loginId);

        // then
        assertThat(result).isFalse();
        verify(medicationScheduleRepository).findById(scheduleId);
    }

    @Test
    @DisplayName("Should return false when schedule not found")
    void isOwnerOfSchedule_WithNonExistentSchedule_ShouldReturnFalse() {
        // given
        Long scheduleId = 999L;
        String loginId = "testUser";
        when(medicationScheduleRepository.findById(scheduleId)).thenReturn(Optional.empty());

        // when
        boolean result = medicationRecordService.isOwnerOfSchedule(scheduleId, loginId);

        // then
        assertThat(result).isFalse();
        verify(medicationScheduleRepository).findById(scheduleId);
    }

    @Test
    @DisplayName("Should return true when user is owner of record")
    void isOwnerOfRecord_WithValidOwner_ShouldReturnTrue() {
        // given
        Long recordId = 1L;
        String loginId = "testUser";
        when(medicationRecordRepository.findById(recordId)).thenReturn(Optional.of(mockRecord));
        when(mockRecord.getMedicationSchedule()).thenReturn(mockSchedule);
        when(mockSchedule.getMember()).thenReturn(mockMember);
        when(mockMember.getLoginId()).thenReturn(loginId);

        // when
        boolean result = medicationRecordService.isOwnerOfRecord(recordId, loginId);

        // then
        assertThat(result).isTrue();
        verify(medicationRecordRepository).findById(recordId);
    }

    @Test
    @DisplayName("Should return false when user is not owner of record")
    void isOwnerOfRecord_WithInvalidOwner_ShouldReturnFalse() {
        // given
        Long recordId = 1L;
        String loginId = "otherUser";
        when(medicationRecordRepository.findById(recordId)).thenReturn(Optional.of(mockRecord));
        when(mockRecord.getMedicationSchedule()).thenReturn(mockSchedule);
        when(mockSchedule.getMember()).thenReturn(mockMember);
        when(mockMember.getLoginId()).thenReturn("testUser");

        // when
        boolean result = medicationRecordService.isOwnerOfRecord(recordId, loginId);

        // then
        assertThat(result).isFalse();
        verify(medicationRecordRepository).findById(recordId);
    }

    @Test
    @DisplayName("Should return false when record not found")
    void isOwnerOfRecord_WithNonExistentRecord_ShouldReturnFalse() {
        // given
        Long recordId = 999L;
        String loginId = "testUser";
        when(medicationRecordRepository.findById(recordId)).thenReturn(Optional.empty());

        // when
        boolean result = medicationRecordService.isOwnerOfRecord(recordId, loginId);

        // then
        assertThat(result).isFalse();
        verify(medicationRecordRepository).findById(recordId);
    }
}