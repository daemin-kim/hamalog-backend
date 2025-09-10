package com.Hamalog.service.medication;

import com.Hamalog.domain.events.DomainEventPublisher;
import com.Hamalog.domain.events.medication.MedicationScheduleCreated;
import com.Hamalog.domain.events.medication.MedicationScheduleDeleted;
import com.Hamalog.domain.events.medication.MedicationScheduleUpdated;
import com.Hamalog.domain.medication.AlarmType;
import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.domain.member.Member;
import com.Hamalog.dto.medication.request.MedicationScheduleCreateRequest;
import com.Hamalog.dto.medication.request.MedicationScheduleUpdateRequest;
import com.Hamalog.exception.medication.MedicationScheduleNotFoundException;
import com.Hamalog.exception.member.MemberNotFoundException;
import com.Hamalog.repository.medication.MedicationScheduleRepository;
import com.Hamalog.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MedicationScheduleService {

    private final MemberRepository memberRepository;
    private final MedicationScheduleRepository medicationScheduleRepository;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional(readOnly = true)
    public List<MedicationSchedule> getMedicationSchedules(Long memberId) {
        return medicationScheduleRepository.findAllByMember_MemberId(memberId);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<MedicationSchedule> getMedicationSchedules(Long memberId, org.springframework.data.domain.Pageable pageable) {
        return medicationScheduleRepository.findByMember_MemberId(memberId, pageable);
    }

    @Transactional(readOnly = true)
    public MedicationSchedule getMedicationSchedule(Long medicationScheduleId) {
        return medicationScheduleRepository.findById(medicationScheduleId)
                .orElseThrow(MedicationScheduleNotFoundException::new);
    }

    public MedicationSchedule createMedicationSchedule(
            MedicationScheduleCreateRequest medicationScheduleCreateRequest
    ) {
        Member member = memberRepository.findById(medicationScheduleCreateRequest.memberId())
                .orElseThrow(MemberNotFoundException::new);

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

        MedicationSchedule savedSchedule = medicationScheduleRepository.save(medicationSchedule);
        
        // Publish domain event for decoupled business logic
        MedicationScheduleCreated event = new MedicationScheduleCreated(
                savedSchedule.getMedicationScheduleId(),
                savedSchedule.getMember().getMemberId(),
                savedSchedule.getMember().getLoginId(),
                savedSchedule.getName(),
                savedSchedule.getHospitalName(),
                savedSchedule.getPrescriptionDate(),
                savedSchedule.getMemo(),
                savedSchedule.getStartOfAd(),
                savedSchedule.getPrescriptionDays(),
                savedSchedule.getPerDay(),
                savedSchedule.getAlarmType()
        );
        
        domainEventPublisher.publish(event);
        log.debug("Published MedicationScheduleCreated event for schedule ID: {}", savedSchedule.getMedicationScheduleId());

        return savedSchedule;
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

        MedicationSchedule updatedSchedule = medicationScheduleRepository.save(existingMedicationSchedule);
        
        // Publish domain event for decoupled business logic
        MedicationScheduleUpdated event = new MedicationScheduleUpdated(
                updatedSchedule.getMedicationScheduleId(),
                updatedSchedule.getMember().getMemberId(),
                updatedSchedule.getMember().getLoginId(),
                updatedSchedule.getName(),
                updatedSchedule.getHospitalName(),
                updatedSchedule.getPrescriptionDate(),
                updatedSchedule.getMemo(),
                updatedSchedule.getStartOfAd(),
                updatedSchedule.getPrescriptionDays(),
                updatedSchedule.getPerDay(),
                updatedSchedule.getAlarmType()
        );
        
        domainEventPublisher.publish(event);
        log.debug("Published MedicationScheduleUpdated event for schedule ID: {}", updatedSchedule.getMedicationScheduleId());

        return updatedSchedule;
    }

    public void deleteMedicationSchedule(Long medicationScheduleId) {
        MedicationSchedule medicationSchedule = getMedicationSchedule(medicationScheduleId);
        
        // Capture information before deletion for the event
        Long scheduleId = medicationSchedule.getMedicationScheduleId();
        Long memberId = medicationSchedule.getMember().getMemberId();
        String memberLoginId = medicationSchedule.getMember().getLoginId();
        String scheduleName = medicationSchedule.getName();
        
        medicationScheduleRepository.delete(medicationSchedule);
        
        // Publish domain event for decoupled business logic
        MedicationScheduleDeleted event = new MedicationScheduleDeleted(
                scheduleId,
                memberId,
                memberLoginId,
                scheduleName
        );
        
        domainEventPublisher.publish(event);
        log.debug("Published MedicationScheduleDeleted event for schedule ID: {}", scheduleId);
    }
    
    @Transactional(readOnly = true)
    public boolean isOwner(Long memberId, String loginId) {
        return memberRepository.findById(memberId)
                .map(member -> member.getLoginId().equals(loginId))
                .orElse(false);
    }
}
