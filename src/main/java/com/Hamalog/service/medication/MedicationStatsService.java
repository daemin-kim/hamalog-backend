package com.Hamalog.service.medication;

import com.Hamalog.domain.medication.MedicationRecord;
import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.dto.medication.response.MedicationAdherenceResponse;
import com.Hamalog.dto.medication.response.MedicationAdherenceResponse.DailyAdherenceStat;
import com.Hamalog.dto.medication.response.MedicationSummaryResponse;
import com.Hamalog.dto.medication.response.MedicationSummaryResponse.ScheduleSummary;
import com.Hamalog.exception.member.MemberNotFoundException;
import com.Hamalog.repository.medication.MedicationRecordRepository;
import com.Hamalog.repository.medication.MedicationScheduleRepository;
import com.Hamalog.repository.member.MemberRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class MedicationStatsService {

    private final MedicationRecordRepository medicationRecordRepository;
    private final MedicationScheduleRepository medicationScheduleRepository;
    private final MemberRepository memberRepository;

    /**
     * 기간별 복약 이행률 조회
     */
    public MedicationAdherenceResponse getAdherence(Long memberId, LocalDate startDate, LocalDate endDate) {
        log.info("복약 이행률 조회 - memberId: {}, period: {} ~ {}", memberId, startDate, endDate);

        validateMemberExists(memberId);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // 기간 내 복약 기록 조회
        List<MedicationRecord> records = medicationRecordRepository
                .findByMemberIdAndDateRange(memberId, startDateTime, endDateTime);

        // 일별 통계 계산
        Map<LocalDate, List<MedicationRecord>> recordsByDate = records.stream()
                .filter(r -> r.getRealTakeTime() != null)
                .collect(Collectors.groupingBy(r -> r.getRealTakeTime().toLocalDate()));

        List<DailyAdherenceStat> dailyStats = new ArrayList<>();
        List<LocalDate> missedDates = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<MedicationRecord> dayRecords = recordsByDate.getOrDefault(date, List.of());
            int scheduled = dayRecords.size();
            int taken = (int) dayRecords.stream().filter(MedicationRecord::getIsTakeMedication).count();
            double rate = scheduled > 0 ? Math.round((double) taken / scheduled * 1000) / 10.0 : 0.0;

            dailyStats.add(new DailyAdherenceStat(date, scheduled, taken, rate));

            // 예정된 복약이 있는데 하나도 안 먹은 날
            if (scheduled > 0 && taken == 0) {
                missedDates.add(date);
            }
        }

        long totalScheduled = records.size();
        long totalTaken = records.stream().filter(MedicationRecord::getIsTakeMedication).count();

        return MedicationAdherenceResponse.of(
                startDate, endDate, totalScheduled, totalTaken, missedDates, dailyStats
        );
    }

    /**
     * 복약 현황 요약 조회
     */
    public MedicationSummaryResponse getSummary(Long memberId) {
        log.info("복약 현황 요약 조회 - memberId: {}", memberId);

        validateMemberExists(memberId);

        // 활성 스케줄 조회
        List<MedicationSchedule> schedules = medicationScheduleRepository.findAllByMember_MemberId(memberId);
        int totalActiveSchedules = schedules.size();

        // 오늘 날짜 기준 계산
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);

        // 오늘 복약 통계
        long todayTotal = medicationRecordRepository.countByMemberIdAndDateRange(memberId, todayStart, todayEnd);
        long todayTaken = medicationRecordRepository.countTakenByMemberIdAndDateRange(memberId, todayStart, todayEnd);

        // 이번 주 통계 (월요일 ~ 오늘)
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
        double weeklyRate = calculateAdherenceRate(memberId, weekStart.atStartOfDay(), todayEnd);

        // 이번 달 통계
        LocalDate monthStart = today.withDayOfMonth(1);
        double monthlyRate = calculateAdherenceRate(memberId, monthStart.atStartOfDay(), todayEnd);

        // 스케줄별 통계
        List<ScheduleSummary> scheduleSummaries = getScheduleSummaries(memberId, schedules);

        return MedicationSummaryResponse.of(
                totalActiveSchedules,
                (int) todayTotal,
                (int) todayTaken,
                weeklyRate,
                monthlyRate,
                scheduleSummaries
        );
    }

    private double calculateAdherenceRate(Long memberId, LocalDateTime start, LocalDateTime end) {
        long total = medicationRecordRepository.countByMemberIdAndDateRange(memberId, start, end);
        long taken = medicationRecordRepository.countTakenByMemberIdAndDateRange(memberId, start, end);
        return total > 0 ? Math.round((double) taken / total * 1000) / 10.0 : 0.0;
    }

    private List<ScheduleSummary> getScheduleSummaries(Long memberId, List<MedicationSchedule> schedules) {
        // 스케줄별 통계 조회
        List<Object[]> stats = medicationRecordRepository.getScheduleStatsByMemberId(memberId);
        Map<Long, Object[]> statsMap = new HashMap<>();
        for (Object[] row : stats) {
            Long scheduleId = (Long) row[0];
            statsMap.put(scheduleId, row);
        }

        return schedules.stream()
                .map(schedule -> {
                    Object[] stat = statsMap.get(schedule.getMedicationScheduleId());
                    long totalRecords = stat != null ? ((Number) stat[1]).longValue() : 0;
                    long takenCount = stat != null ? ((Number) stat[2]).longValue() : 0;
                    double rate = totalRecords > 0
                            ? Math.round((double) takenCount / totalRecords * 1000) / 10.0
                            : 0.0;

                    return new ScheduleSummary(
                            schedule.getMedicationScheduleId(),
                            schedule.getName(),
                            null, // 별명 필드 없음
                            totalRecords,
                            takenCount,
                            rate
                    );
                })
                .collect(Collectors.toList());
    }

    private void validateMemberExists(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new MemberNotFoundException();
        }
    }
}
