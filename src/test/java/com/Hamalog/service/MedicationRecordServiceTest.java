package com.Hamalog.service;

import com.Hamalog.domain.medication.MedicationRecord;
import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.domain.medication.MedicationTime;
import com.Hamalog.dto.medication.request.MedicationRecordCreateRequest;
import com.Hamalog.dto.medication.request.MedicationRecordUpdateRequest;
import com.Hamalog.exception.medication.MedicationRecordNotFoundException;
import com.Hamalog.exception.medication.MedicationScheduleNotFoundException;
import com.Hamalog.exception.medication.MedicationTimeNotFoundException;
import com.Hamalog.repository.medication.MedicationRecordRepository;
import com.Hamalog.repository.medication.MedicationScheduleRepository;
import com.Hamalog.repository.medication.MedicationTimeRepository;
import com.Hamalog.service.medication.MedicationRecordService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;


import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MedicationRecordServiceTest {

    private MedicationRecordRepository medicationRecordRepository = mock(MedicationRecordRepository.class);
    private MedicationScheduleRepository medicationScheduleRepository = mock(MedicationScheduleRepository.class);
    private MedicationTimeRepository medicationTimeRepository = mock(MedicationTimeRepository.class);
    private MedicationRecordService service =
            new MedicationRecordService(medicationRecordRepository, medicationScheduleRepository, medicationTimeRepository);

    private MedicationSchedule mockSchedule = mock(MedicationSchedule.class);
    private MedicationTime mockTime = mock(MedicationTime.class);

    @Test
    void 복약기록_생성_성공() {
        // given
        MedicationRecordCreateRequest req = new MedicationRecordCreateRequest(1L, 2L, true, LocalDateTime.now());

        when(medicationScheduleRepository.findById(1L)).thenReturn(Optional.of(mockSchedule));
        when(medicationTimeRepository.findById(2L)).thenReturn(Optional.of(mockTime));

        MedicationRecord savedRecord = new MedicationRecord(mockSchedule, mockTime, true, req.realTakeTime());
        when(medicationRecordRepository.save(any())).thenReturn(savedRecord);

        // when
        MedicationRecord result = service.createMedicationRecord(req);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getIsTakeMedication()).isTrue();
        verify(medicationRecordRepository).save(any());
    }

    @Test
    void 복약기록_생성_실패_스케줄없음() {
        MedicationRecordCreateRequest req = new MedicationRecordCreateRequest(99L, 2L, true, LocalDateTime.now());
        when(medicationScheduleRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createMedicationRecord(req))
            .isInstanceOf(MedicationScheduleNotFoundException.class);
    }

    @Test
    void 복약기록_생성_실패_타임없음() {
        MedicationRecordCreateRequest req = new MedicationRecordCreateRequest(1L, 99L, true, LocalDateTime.now());
        when(medicationScheduleRepository.findById(1L)).thenReturn(Optional.of(mockSchedule));
        when(medicationTimeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createMedicationRecord(req))
            .isInstanceOf(MedicationTimeNotFoundException.class);
    }

    @Test
    void 복약기록_수정_성공() {
        MedicationRecord origin = new MedicationRecord(mockSchedule, mockTime, false, LocalDateTime.now());
        origin.update(false, LocalDateTime.now().minusHours(1));

        MedicationRecordUpdateRequest req = new MedicationRecordUpdateRequest(true, LocalDateTime.now());
        when(medicationRecordRepository.findById(any())).thenReturn(Optional.of(origin));
        when(medicationRecordRepository.save(any())).thenReturn(origin);

        MedicationRecord updated = service.updateMedicationRecord(1L, req);

        assertThat(updated.getIsTakeMedication()).isTrue();
        verify(medicationRecordRepository).save(any());
    }

    @Test
    void 복약기록_수정_실패_없는ID() {
        MedicationRecordUpdateRequest req = new MedicationRecordUpdateRequest(true, LocalDateTime.now());
        when(medicationRecordRepository.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateMedicationRecord(1L, req))
            .isInstanceOf(MedicationRecordNotFoundException.class);
    }

    @Test
    void 복약기록_단건조회_실패() {
        when(medicationRecordRepository.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getMedicationRecord(123L))
            .isInstanceOf(MedicationRecordNotFoundException.class);
    }
}
