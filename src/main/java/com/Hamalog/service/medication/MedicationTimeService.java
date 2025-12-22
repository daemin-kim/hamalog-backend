package com.Hamalog.service.medication;

import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.domain.medication.MedicationTime;
import com.Hamalog.dto.medication.request.MedicationTimeCreateRequest;
import com.Hamalog.dto.medication.request.MedicationTimeUpdateRequest;
import com.Hamalog.dto.medication.response.MedicationTimeResponse;
import com.Hamalog.exception.medication.MedicationScheduleNotFoundException;
import com.Hamalog.exception.medication.MedicationTimeNotFoundException;
import com.Hamalog.repository.medication.MedicationScheduleRepository;
import com.Hamalog.repository.medication.MedicationTimeRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class MedicationTimeService {

    private final MedicationTimeRepository medicationTimeRepository;
    private final MedicationScheduleRepository medicationScheduleRepository;

    /**
     * 스케줄별 알림 시간 목록 조회
     */
    public List<MedicationTimeResponse> getMedicationTimes(Long scheduleId) {
        log.info("복약 알림 시간 목록 조회 - scheduleId: {}", scheduleId);

        if (!medicationScheduleRepository.existsById(scheduleId)) {
            throw new MedicationScheduleNotFoundException();
        }

        List<MedicationTime> times = medicationTimeRepository
                .findByMedicationSchedule_MedicationScheduleIdOrderByTakeTimeAsc(scheduleId);

        return times.stream()
                .map(MedicationTimeResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 알림 시간 단건 조회
     */
    public MedicationTimeResponse getMedicationTime(Long timeId) {
        log.info("복약 알림 시간 조회 - timeId: {}", timeId);

        MedicationTime time = medicationTimeRepository.findByIdWithScheduleAndMember(timeId)
                .orElseThrow(MedicationTimeNotFoundException::new);

        return MedicationTimeResponse.from(time);
    }

    /**
     * 알림 시간 추가
     */
    @Transactional
    public MedicationTimeResponse createMedicationTime(Long scheduleId, MedicationTimeCreateRequest request) {
        log.info("복약 알림 시간 생성 - scheduleId: {}, time: {}", scheduleId, request.takeTime());

        MedicationSchedule schedule = medicationScheduleRepository.findById(scheduleId)
                .orElseThrow(MedicationScheduleNotFoundException::new);

        MedicationTime time = new MedicationTime(schedule, request.takeTime());
        MedicationTime savedTime = medicationTimeRepository.save(time);

        log.info("복약 알림 시간 생성 완료 - timeId: {}", savedTime.getMedicationTimeId());
        return MedicationTimeResponse.from(savedTime);
    }

    /**
     * 알림 시간 수정
     */
    @Transactional
    public MedicationTimeResponse updateMedicationTime(Long timeId, MedicationTimeUpdateRequest request) {
        log.info("복약 알림 시간 수정 - timeId: {}, newTime: {}", timeId, request.takeTime());

        MedicationTime time = medicationTimeRepository.findByIdWithScheduleAndMember(timeId)
                .orElseThrow(MedicationTimeNotFoundException::new);

        time.updateTime(request.takeTime());

        log.info("복약 알림 시간 수정 완료 - timeId: {}", timeId);
        return MedicationTimeResponse.from(time);
    }

    /**
     * 알림 시간 삭제
     */
    @Transactional
    public void deleteMedicationTime(Long timeId) {
        log.info("복약 알림 시간 삭제 - timeId: {}", timeId);

        if (!medicationTimeRepository.existsById(timeId)) {
            throw new MedicationTimeNotFoundException();
        }

        medicationTimeRepository.deleteById(timeId);
        log.info("복약 알림 시간 삭제 완료 - timeId: {}", timeId);
    }

    /**
     * 알림 시간이 특정 회원의 것인지 확인
     */
    public boolean isOwnedByMember(Long timeId, Long memberId) {
        return medicationTimeRepository.existsByIdAndMemberId(timeId, memberId);
    }
}
