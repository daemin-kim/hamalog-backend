package com.Hamalog.repository.medication;

import com.Hamalog.domain.medication.MedicationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicationRecordRepository extends JpaRepository<MedicationRecord, Long> {

    List<MedicationRecord> findAllByMedicationSchedule_MedicationScheduleId(Long medicationScheduleId);
}
