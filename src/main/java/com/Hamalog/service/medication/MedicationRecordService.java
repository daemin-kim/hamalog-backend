package com.Hamalog.service.medication;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MedicationRecordService {

    private final MedicationRecordRepository medicationRecordRepository;
    private final MedicationScheduleRepository medicationScheduleRepository;
    private final MedicationTimeRepository medicationTimeRepository;

    public MedicationRecordService(
            MedicationRecordRepository medicationRecordRepository,
            MedicationScheduleRepository medicationScheduleRepository,
            MedicationTimeRepository medicationTimeRepository
    ) {
        this.medicationRecordRepository = medicationRecordRepository;
        this.medicationScheduleRepository = medicationScheduleRepository;
        this.medicationTimeRepository = medicationTimeRepository;
    }

    @Transactional(readOnly = true)
    public List<MedicationRecord> getMedicationRecords(Long medicationScheduleId) {
        return medicationRecordRepository.findAllByMedicationSchedule_MedicationScheduleId(medicationScheduleId);
    }

    @Transactional(readOnly = true)
    public MedicationRecord getMedicationRecord(
            Long medicationRecordId
    ) {
        return medicationRecordRepository.findById(medicationRecordId)
                .orElseThrow(MedicationRecordNotFoundException::new);
    }

    public MedicationRecord createMedicationRecord(
            MedicationRecordCreateRequest medicationRecordCreateRequest
    ) {
        MedicationSchedule medicationSchedule = medicationScheduleRepository.findById(medicationRecordCreateRequest.medicationScheduleId())
                .orElseThrow(MedicationScheduleNotFoundException::new);

        MedicationTime medicationTime = medicationTimeRepository.findById(medicationRecordCreateRequest.medicationTimeId())
                .orElseThrow(MedicationTimeNotFoundException::new);

        MedicationRecord medicationRecord = new MedicationRecord(
                medicationSchedule,
                medicationTime,
                medicationRecordCreateRequest.isTakeMedication(),
                medicationRecordCreateRequest.realTakeTime()
        );

        return medicationRecordRepository.save(medicationRecord);
    }

    public MedicationRecord updateMedicationRecord(
            Long medicationRecordId,
            MedicationRecordUpdateRequest medicationRecordUpdateRequest
    ) {
        MedicationRecord medicationRecord = medicationRecordRepository.findById(medicationRecordId)
                .orElseThrow(MedicationRecordNotFoundException::new);

        medicationRecord.update(
                medicationRecordUpdateRequest.isTakeMedication(),
                medicationRecordUpdateRequest.realTakeTime()
        );

        return medicationRecordRepository.save(medicationRecord);
    }

    public void deleteMedicationRecord(Long medicationRecordId) {
        MedicationRecord medicationRecord = medicationRecordRepository.findById(medicationRecordId)
                .orElseThrow(MedicationRecordNotFoundException::new);
        medicationRecordRepository.delete(medicationRecord);
    }
}
