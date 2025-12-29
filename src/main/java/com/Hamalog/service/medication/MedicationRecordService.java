package com.Hamalog.service.medication;

import com.Hamalog.domain.events.DomainEventPublisher;
import com.Hamalog.domain.events.medication.MedicationRecordCreated;
import com.Hamalog.domain.medication.MedicationRecord;
import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.domain.medication.MedicationTime;
import com.Hamalog.dto.medication.request.MedicationRecordCreateRequest;
import com.Hamalog.dto.medication.request.MedicationRecordUpdateRequest;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.exception.medication.MedicationRecordNotFoundException;
import com.Hamalog.exception.medication.MedicationScheduleNotFoundException;
import com.Hamalog.exception.medication.MedicationTimeNotFoundException;
import com.Hamalog.exception.validation.InvalidInputException;
import com.Hamalog.repository.medication.MedicationRecordRepository;
import com.Hamalog.repository.medication.MedicationScheduleRepository;
import com.Hamalog.repository.medication.MedicationTimeRepository;
import com.Hamalog.repository.member.MemberRepository;
import com.Hamalog.security.annotation.RequireResourceOwnership;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class MedicationRecordService {

    private final MedicationRecordRepository medicationRecordRepository;
    private final MedicationScheduleRepository medicationScheduleRepository;
    private final MedicationTimeRepository medicationTimeRepository;
    private final MemberRepository memberRepository;
    private final DomainEventPublisher domainEventPublisher;

    public List<MedicationRecord> getMedicationRecords(Long medicationScheduleId) {
        if (medicationScheduleId == null || medicationScheduleId <= 0) {
            throw new InvalidInputException(ErrorCode.INVALID_PARAMETER);
        }

        // 복약 스케줄 존재 여부 검증
        if (!medicationScheduleRepository.existsById(medicationScheduleId)) {
            throw new MedicationScheduleNotFoundException();
        }

        return medicationRecordRepository.findAllByMedicationSchedule_MedicationScheduleId(medicationScheduleId);
    }

    @RequireResourceOwnership(
            resourceType = RequireResourceOwnership.ResourceType.MEDICATION_RECORD,
            paramName = "medicationRecordId",
            strategy = RequireResourceOwnership.OwnershipStrategy.DIRECT
    )
    public MedicationRecord getMedicationRecord(
            Long medicationRecordId
    ) {
        if (medicationRecordId == null || medicationRecordId <= 0) {
            throw new InvalidInputException(ErrorCode.INVALID_PARAMETER);
        }

        return medicationRecordRepository.findById(medicationRecordId)
                .orElseThrow(MedicationRecordNotFoundException::new);
    }

    @Transactional(rollbackFor = {Exception.class})
    public MedicationRecord createMedicationRecord(
            MedicationRecordCreateRequest medicationRecordCreateRequest
    ) {
        // 입력값 검증
        validateMedicationRecordCreateRequest(medicationRecordCreateRequest);

        MedicationSchedule medicationSchedule = medicationScheduleRepository.findById(medicationRecordCreateRequest.medicationScheduleId())
                .orElseThrow(MedicationScheduleNotFoundException::new);

        MedicationTime medicationTime = medicationTimeRepository.findById(medicationRecordCreateRequest.medicationTimeId())
                .orElseThrow(MedicationTimeNotFoundException::new);

        // 비즈니스 로직 검증: MedicationTime이 해당 MedicationSchedule에 속하는지 확인
        validateMedicationTimeBelongsToSchedule(medicationTime, medicationSchedule);

        // 복용 시간 검증
        if (medicationRecordCreateRequest.isTakeMedication() && medicationRecordCreateRequest.realTakeTime() != null) {
            validateRealTakeTime(medicationRecordCreateRequest.realTakeTime());
        }

        MedicationRecord medicationRecord = new MedicationRecord(
                medicationSchedule,
                medicationTime,
                medicationRecordCreateRequest.isTakeMedication(),
                medicationRecordCreateRequest.realTakeTime()
        );

        MedicationRecord savedRecord = medicationRecordRepository.save(medicationRecord);

        // 도메인 이벤트 발행
        MedicationRecordCreated event = new MedicationRecordCreated(
                savedRecord.getMedicationRecordId(),
                medicationSchedule.getMedicationScheduleId(),
                medicationTime.getMedicationTimeId(),
                medicationSchedule.getMember().getMemberId(),
                medicationSchedule.getMember().getLoginId(),
                savedRecord.getIsTakeMedication(),
                savedRecord.getRealTakeTime()
        );
        domainEventPublisher.publish(event);
        log.debug("Published MedicationRecordCreated event for record ID: {}", savedRecord.getMedicationRecordId());

        return savedRecord;
    }

    @RequireResourceOwnership(
            resourceType = RequireResourceOwnership.ResourceType.MEDICATION_RECORD,
            paramName = "medicationRecordId",
            strategy = RequireResourceOwnership.OwnershipStrategy.DIRECT
    )
    @Transactional(rollbackFor = {Exception.class})
    public MedicationRecord updateMedicationRecord(
            Long medicationRecordId,
            MedicationRecordUpdateRequest medicationRecordUpdateRequest
    ) {
        if (medicationRecordId == null || medicationRecordId <= 0) {
            throw new InvalidInputException(ErrorCode.INVALID_PARAMETER);
        }

        // 입력값 검증
        validateMedicationRecordUpdateRequest(medicationRecordUpdateRequest);

        MedicationRecord medicationRecord = medicationRecordRepository.findById(medicationRecordId)
                .orElseThrow(MedicationRecordNotFoundException::new);

        // 복용 시간 검증
        if (medicationRecordUpdateRequest.isTakeMedication() && medicationRecordUpdateRequest.realTakeTime() != null) {
            validateRealTakeTime(medicationRecordUpdateRequest.realTakeTime());
        }

        medicationRecord.update(
                medicationRecordUpdateRequest.isTakeMedication(),
                medicationRecordUpdateRequest.realTakeTime()
        );

        return medicationRecordRepository.save(medicationRecord);
    }

    @RequireResourceOwnership(
            resourceType = RequireResourceOwnership.ResourceType.MEDICATION_RECORD,
            paramName = "medicationRecordId",
            strategy = RequireResourceOwnership.OwnershipStrategy.DIRECT
    )
    @Transactional(rollbackFor = {Exception.class})
    public void deleteMedicationRecord(Long medicationRecordId) {
        if (medicationRecordId == null || medicationRecordId <= 0) {
            throw new InvalidInputException(ErrorCode.INVALID_PARAMETER);
        }

        MedicationRecord medicationRecord = medicationRecordRepository.findById(medicationRecordId)
                .orElseThrow(MedicationRecordNotFoundException::new);
        medicationRecordRepository.delete(medicationRecord);
    }
    
    public boolean isOwnerOfSchedule(Long medicationScheduleId, String loginId) {
        if (medicationScheduleId == null || loginId == null) {
            return false;
        }
        return medicationScheduleRepository.findById(medicationScheduleId)
                .map(schedule -> schedule.getMember().getLoginId().equals(loginId))
                .orElse(false);
    }
    
    public boolean isOwnerOfRecord(Long medicationRecordId, String loginId) {
        if (medicationRecordId == null || loginId == null) {
            return false;
        }
        return medicationRecordRepository.findById(medicationRecordId)
                .map(record -> record.getMedicationSchedule().getMember().getLoginId().equals(loginId))
                .orElse(false);
    }

    // ========== Private Validation Methods ==========

    /**
     * 복약 기록 생성 요청 데이터 검증
     */
    private void validateMedicationRecordCreateRequest(MedicationRecordCreateRequest request) {
        if (request == null) {
            throw new InvalidInputException(ErrorCode.BAD_REQUEST);
        }

        if (request.medicationScheduleId() == null || request.medicationScheduleId() <= 0) {
            throw new InvalidInputException(ErrorCode.INVALID_PARAMETER);
        }

        if (request.medicationTimeId() == null || request.medicationTimeId() <= 0) {
            throw new InvalidInputException(ErrorCode.INVALID_PARAMETER);
        }

        if (request.isTakeMedication() == null) {
            throw new InvalidInputException(ErrorCode.MISSING_REQUIRED_FIELD);
        }
    }

    /**
     * 복약 기록 수정 요청 데이터 검증
     */
    private void validateMedicationRecordUpdateRequest(MedicationRecordUpdateRequest request) {
        if (request == null) {
            throw new InvalidInputException(ErrorCode.BAD_REQUEST);
        }

        if (request.isTakeMedication() == null) {
            throw new InvalidInputException(ErrorCode.MISSING_REQUIRED_FIELD);
        }
    }

    /**
     * MedicationTime이 해당 MedicationSchedule에 속하는지 검증
     */
    private void validateMedicationTimeBelongsToSchedule(MedicationTime medicationTime, MedicationSchedule medicationSchedule) {
        if (!medicationTime.getMedicationSchedule().getMedicationScheduleId()
                .equals(medicationSchedule.getMedicationScheduleId())) {
            log.warn("MedicationTime {} does not belong to MedicationSchedule {}",
                    medicationTime.getMedicationTimeId(),
                    medicationSchedule.getMedicationScheduleId());
            throw new InvalidInputException(ErrorCode.INVALID_MEDICATION_SCHEDULE);
        }
    }

    /**
     * 실제 복용 시간 검증 - 미래 시간은 불가
     */
    private void validateRealTakeTime(LocalDateTime realTakeTime) {
        if (realTakeTime.isAfter(LocalDateTime.now())) {
            log.warn("Real take time {} is in the future", realTakeTime);
            throw new InvalidInputException(ErrorCode.INVALID_DATE_RANGE);
        }
    }

    // ========== Batch Methods ==========

    /**
     * 복약 기록 일괄 생성
     */
    @Transactional(rollbackFor = {Exception.class})
    public List<MedicationRecord> createMedicationRecordsBatch(List<MedicationRecordCreateRequest> requests) {
        return requests.stream()
                .map(this::createMedicationRecordInternal)
                .toList();
    }

    /**
     * 복약 기록 일괄 수정
     */
    @Transactional(rollbackFor = {Exception.class})
    public List<MedicationRecord> updateMedicationRecordsBatch(
            List<com.Hamalog.dto.medication.request.MedicationRecordBatchUpdateItem> items
    ) {
        return items.stream()
                .map(item -> {
                    MedicationRecord record = medicationRecordRepository.findById(item.medicationRecordId())
                            .orElseThrow(MedicationRecordNotFoundException::new);

                    if (item.isTakeMedication() != null) {
                        record.update(item.isTakeMedication(), item.realTakeTime());
                    }

                    return medicationRecordRepository.save(record);
                })
                .toList();
    }

    private MedicationRecord createMedicationRecordInternal(MedicationRecordCreateRequest request) {
        MedicationSchedule medicationSchedule = medicationScheduleRepository.findById(request.medicationScheduleId())
                .orElseThrow(MedicationScheduleNotFoundException::new);

        MedicationTime medicationTime = medicationTimeRepository.findById(request.medicationTimeId())
                .orElseThrow(MedicationTimeNotFoundException::new);

        validateMedicationTimeBelongsToSchedule(medicationTime, medicationSchedule);

        MedicationRecord medicationRecord = new MedicationRecord(
                medicationSchedule,
                medicationTime,
                request.isTakeMedication(),
                request.realTakeTime()
        );

        return medicationRecordRepository.save(medicationRecord);
    }
}
