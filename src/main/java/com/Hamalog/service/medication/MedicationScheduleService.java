package com.Hamalog.service.medication;

import com.Hamalog.domain.events.DomainEventPublisher;
import com.Hamalog.domain.events.medication.MedicationScheduleCreated;
import com.Hamalog.domain.events.medication.MedicationScheduleDeleted;
import com.Hamalog.domain.events.medication.MedicationScheduleUpdated;
import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.domain.member.Member;
import com.Hamalog.dto.medication.request.MedicationScheduleCreateRequest;
import com.Hamalog.dto.medication.request.MedicationScheduleUpdateRequest;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.exception.medication.MedicationScheduleNotFoundException;
import com.Hamalog.exception.member.MemberNotFoundException;
import com.Hamalog.exception.validation.InvalidInputException;
import com.Hamalog.repository.medication.MedicationScheduleRepository;
import com.Hamalog.repository.member.MemberRepository;
import com.Hamalog.security.annotation.RequireResourceOwnership;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MedicationScheduleService {

    // 비즈니스 검증 상수
    private static final int MAX_PRESCRIPTION_DAYS = 365;
    private static final int MAX_PER_DAY = 10;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 100;

    private final MemberRepository memberRepository;
    private final MedicationScheduleRepository medicationScheduleRepository;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional(readOnly = true)
    public List<MedicationSchedule> getMedicationSchedules(Long memberId) {
        // 회원 존재 여부 검증
        if (!memberRepository.existsById(memberId)) {
            throw new MemberNotFoundException();
        }
        return medicationScheduleRepository.findAllByMember_MemberId(memberId);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<MedicationSchedule> getMedicationSchedules(Long memberId, org.springframework.data.domain.Pageable pageable) {
        // 회원 존재 여부 검증
        if (!memberRepository.existsById(memberId)) {
            throw new MemberNotFoundException();
        }

        // 페이지네이션 파라미터 검증
        validatePaginationParams(pageable);

        return medicationScheduleRepository.findByMember_MemberId(memberId, pageable);
    }

    @RequireResourceOwnership(
            resourceType = RequireResourceOwnership.ResourceType.MEDICATION_SCHEDULE,
            paramName = "medicationScheduleId",
            strategy = RequireResourceOwnership.OwnershipStrategy.DIRECT
    )
    @Transactional(readOnly = true)
    public MedicationSchedule getMedicationSchedule(Long medicationScheduleId) {
        if (medicationScheduleId == null || medicationScheduleId <= 0) {
            throw new InvalidInputException(ErrorCode.INVALID_PARAMETER);
        }
        return medicationScheduleRepository.findById(medicationScheduleId)
                .orElseThrow(MedicationScheduleNotFoundException::new);
    }

    @Transactional(rollbackFor = {Exception.class})
    public MedicationSchedule createMedicationSchedule(
            MedicationScheduleCreateRequest medicationScheduleCreateRequest
    ) {
        // 입력값 검증
        validateMedicationScheduleRequest(medicationScheduleCreateRequest);

        Member member = memberRepository.findById(medicationScheduleCreateRequest.memberId())
                .orElseThrow(MemberNotFoundException::new);

        // 비즈니스 로직 검증
        validateDateRange(medicationScheduleCreateRequest.prescriptionDate(), medicationScheduleCreateRequest.startOfAd());
        validatePrescriptionDays(medicationScheduleCreateRequest.prescriptionDays());
        validatePerDay(medicationScheduleCreateRequest.perDay());

        MedicationSchedule medicationSchedule = new MedicationSchedule(
                member,
                medicationScheduleCreateRequest.name(),
                medicationScheduleCreateRequest.hospitalName(),
                medicationScheduleCreateRequest.prescriptionDate(),
                medicationScheduleCreateRequest.memo(),
                medicationScheduleCreateRequest.startOfAd(),
                medicationScheduleCreateRequest.prescriptionDays(),
                medicationScheduleCreateRequest.perDay(),
                medicationScheduleCreateRequest.alarmType()
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

    @RequireResourceOwnership(
            resourceType = RequireResourceOwnership.ResourceType.MEDICATION_SCHEDULE,
            paramName = "medicationScheduleId",
            strategy = RequireResourceOwnership.OwnershipStrategy.DIRECT
    )
    @Transactional(rollbackFor = {Exception.class})
    public MedicationSchedule updateMedicationSchedule(
            Long medicationScheduleId,
            MedicationScheduleUpdateRequest medicationSchedule
    ) {
        if (medicationScheduleId == null || medicationScheduleId <= 0) {
            throw new InvalidInputException(ErrorCode.INVALID_PARAMETER);
        }

        // 입력값 검증
        validateMedicationScheduleUpdateRequest(medicationSchedule);

        MedicationSchedule existingMedicationSchedule = getMedicationSchedule(medicationScheduleId);

        // 비즈니스 로직 검증
        validateDateRange(medicationSchedule.prescriptionDate(), medicationSchedule.startOfAd());
        validatePrescriptionDays(medicationSchedule.prescriptionDays());
        validatePerDay(medicationSchedule.perDay());

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

    @RequireResourceOwnership(
            resourceType = RequireResourceOwnership.ResourceType.MEDICATION_SCHEDULE,
            paramName = "medicationScheduleId",
            strategy = RequireResourceOwnership.OwnershipStrategy.DIRECT
    )
    @Transactional(rollbackFor = {Exception.class})
    public void deleteMedicationSchedule(Long medicationScheduleId) {
        if (medicationScheduleId == null || medicationScheduleId <= 0) {
            throw new InvalidInputException(ErrorCode.INVALID_PARAMETER);
        }

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

    // ========== Private Validation Methods ==========

    /**
     * 복약 스케줄 생성 요청 데이터 검증
     */
    private void validateMedicationScheduleRequest(MedicationScheduleCreateRequest request) {
        if (request == null) {
            throw new InvalidInputException(ErrorCode.BAD_REQUEST);
        }

        if (request.memberId() == null || request.memberId() <= 0) {
            throw new InvalidInputException(ErrorCode.INVALID_PARAMETER);
        }

        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new InvalidInputException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        if (request.prescriptionDate() == null || request.startOfAd() == null) {
            throw new InvalidInputException(ErrorCode.MISSING_REQUIRED_FIELD);
        }
    }

    /**
     * 복약 스케줄 수정 요청 데이터 검증
     */
    private void validateMedicationScheduleUpdateRequest(MedicationScheduleUpdateRequest request) {
        if (request == null) {
            throw new InvalidInputException(ErrorCode.BAD_REQUEST);
        }

        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new InvalidInputException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        if (request.prescriptionDate() == null || request.startOfAd() == null) {
            throw new InvalidInputException(ErrorCode.MISSING_REQUIRED_FIELD);
        }
    }

    /**
     * 날짜 범위 검증 - 시작일은 처방일 이후여야 함
     */
    private void validateDateRange(LocalDate prescriptionDate, LocalDate startOfAd) {
        if (startOfAd.isBefore(prescriptionDate)) {
            log.warn("Invalid date range: startOfAd {} is before prescriptionDate {}", startOfAd, prescriptionDate);
            throw new InvalidInputException(ErrorCode.INVALID_DATE_RANGE);
        }
    }

    /**
     * 처방 일수 검증 - 1일 이상이어야 함
     */
    private void validatePrescriptionDays(Integer prescriptionDays) {
        if (prescriptionDays == null || prescriptionDays < 1) {
            log.warn("Invalid prescription days: {}", prescriptionDays);
            throw new InvalidInputException(ErrorCode.INVALID_PRESCRIPTION_DAYS);
        }

        if (prescriptionDays > MAX_PRESCRIPTION_DAYS) {
            log.warn("Prescription days {} exceeds maximum allowed ({})", prescriptionDays, MAX_PRESCRIPTION_DAYS);
            throw new InvalidInputException(ErrorCode.INVALID_PRESCRIPTION_DAYS);
        }
    }

    /**
     * 1일 복용 횟수 검증 - 1회 이상이어야 함
     */
    private void validatePerDay(Integer perDay) {
        if (perDay == null || perDay < 1) {
            log.warn("Invalid per day: {}", perDay);
            throw new InvalidInputException(ErrorCode.INVALID_PER_DAY);
        }

        if (perDay > MAX_PER_DAY) {
            log.warn("Per day {} exceeds maximum allowed ({})", perDay, MAX_PER_DAY);
            throw new InvalidInputException(ErrorCode.INVALID_PER_DAY);
        }
    }

    /**
     * 페이지네이션 파라미터 검증
     */
    private void validatePaginationParams(org.springframework.data.domain.Pageable pageable) {
        if (pageable.getPageNumber() < 0) {
            throw new InvalidInputException(ErrorCode.INVALID_PAGE_NUMBER);
        }

        if (pageable.getPageSize() < MIN_PAGE_SIZE || pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new InvalidInputException(ErrorCode.INVALID_PAGE_SIZE);
        }
    }
}
