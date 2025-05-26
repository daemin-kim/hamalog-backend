package com.Hamalog.repository.medication;

import com.Hamalog.domain.medication.MedicationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicationScheduleRepository extends JpaRepository<MedicationSchedule, Long> {

    List<MedicationSchedule> findAllByMember_MemberId(Long memberId);
}
