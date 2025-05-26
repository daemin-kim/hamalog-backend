package com.Hamalog.service.medication;

import com.Hamalog.domain.medication.AlarmType;
import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.domain.member.Member;
import com.Hamalog.dto.medication.request.MedicationScheduleCreateRequest;
import com.Hamalog.dto.medication.request.MedicationScheduleUpdateRequest;
import com.Hamalog.repository.medication.MedicationScheduleRepository;
import com.Hamalog.repository.member.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class MedicationScheduleService {

    private final MemberRepository memberRepository;
    private final MedicationScheduleRepository medicationScheduleRepository;

    public MedicationScheduleService(
            MemberRepository memberRepository,
            MedicationScheduleRepository medicationScheduleRepository
    ) {
        this.memberRepository = memberRepository;
        this.medicationScheduleRepository = medicationScheduleRepository;
    }

    @Transactional(readOnly = true)
    public List<MedicationSchedule> getMedicationSchedules(Long memberId) {
        return medicationScheduleRepository.findAllByMember_MemberId(memberId);
    }

    @Transactional(readOnly = true)
    public MedicationSchedule getMedicationSchedule(Long medicationScheduleId) {
        return medicationScheduleRepository.findById(medicationScheduleId)
                .orElseThrow(() -> new RuntimeException("Medication schedule not found"));
    }

    public MedicationSchedule createMedicationSchedule(
            MedicationScheduleCreateRequest medicationScheduleCreateRequest
    ) {
        Member member = memberRepository.findById(medicationScheduleCreateRequest.memberId())
                .orElseThrow(() -> new RuntimeException("Member not found"));

        MedicationSchedule medicationSchedule = new MedicationSchedule(
                member,
                medicationScheduleCreateRequest.name(),
                medicationScheduleCreateRequest.hospitalName(),
                LocalDate.parse(medicationScheduleCreateRequest.prescriptionDate()),
                medicationScheduleCreateRequest.memo(),
                LocalDate.parse(medicationScheduleCreateRequest.startOfAd()),
                medicationScheduleCreateRequest.prescriptionDays(),
                medicationScheduleCreateRequest.perDay(),
                AlarmType.valueOf(medicationScheduleCreateRequest.alarmType())
        );

        return medicationScheduleRepository.save(medicationSchedule);
    }

    public MedicationSchedule updateMedicationSchedule(
            Long medicationScheduleId,
            MedicationScheduleUpdateRequest medicationSchedule
    ) {
        MedicationSchedule existingMedicationSchedule = getMedicationSchedule(medicationScheduleId);

        existingMedicationSchedule.update(
                medicationSchedule.name(),
                medicationSchedule.hospitalName(),
                medicationSchedule.prescriptionDate(),
                medicationSchedule.memo(),
                medicationSchedule.startOfAd(),
                medicationSchedule.prescriptionDays(),
                medicationSchedule.perDay(),
                medicationSchedule.alarmType()
        );

        return medicationScheduleRepository.save(existingMedicationSchedule);
    }

    public void deleteMedicationSchedule(Long medicationScheduleId) {
        MedicationSchedule medicationSchedule = getMedicationSchedule(medicationScheduleId);
        medicationScheduleRepository.delete(medicationSchedule);
    }
}
