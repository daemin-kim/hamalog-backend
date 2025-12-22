package com.Hamalog.service.diary;

import com.Hamalog.domain.diary.MoodDiary;
import com.Hamalog.domain.diary.MoodType;
import com.Hamalog.dto.diary.response.MoodDiaryCalendarResponse;
import com.Hamalog.dto.diary.response.MoodDiaryCalendarResponse.CalendarDayRecord;
import com.Hamalog.dto.diary.response.MoodDiaryStatsResponse;
import com.Hamalog.dto.diary.response.MoodDiaryStatsResponse.DailyMoodRecord;
import com.Hamalog.exception.member.MemberNotFoundException;
import com.Hamalog.repository.diary.MoodDiaryRepository;
import com.Hamalog.repository.member.MemberRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
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
public class MoodDiaryStatsService {

    private final MoodDiaryRepository moodDiaryRepository;
    private final MemberRepository memberRepository;

    /**
     * 기간별 마음 일기 통계 조회
     */
    public MoodDiaryStatsResponse getStats(Long memberId, LocalDate startDate, LocalDate endDate) {
        log.info("마음 일기 통계 조회 - memberId: {}, period: {} ~ {}", memberId, startDate, endDate);

        validateMemberExists(memberId);

        // 기간 내 일기 조회
        List<MoodDiary> diaries = moodDiaryRepository.findByMemberIdAndDateRange(memberId, startDate, endDate);

        // 기분 분포 계산
        Map<MoodType, Integer> moodDistribution = new EnumMap<>(MoodType.class);
        for (MoodType type : MoodType.values()) {
            moodDistribution.put(type, 0);
        }
        for (MoodDiary diary : diaries) {
            moodDistribution.merge(diary.getMoodType(), 1, Integer::sum);
        }

        // 일별 기록 생성
        List<DailyMoodRecord> dailyRecords = diaries.stream()
                .map(d -> new DailyMoodRecord(
                        d.getDiaryDate(),
                        d.getMoodType(),
                        d.getDiaryType().name()
                ))
                .collect(Collectors.toList());

        // 연속 작성일 계산
        int consecutiveDays = calculateConsecutiveDays(diaries, endDate);

        // 총 일수 계산
        int totalDays = (int) (endDate.toEpochDay() - startDate.toEpochDay() + 1);

        return MoodDiaryStatsResponse.of(
                startDate, endDate, diaries.size(), totalDays, consecutiveDays,
                moodDistribution, dailyRecords
        );
    }

    /**
     * 월별 캘린더 조회
     */
    public MoodDiaryCalendarResponse getCalendar(Long memberId, int year, int month) {
        log.info("마음 일기 캘린더 조회 - memberId: {}, year: {}, month: {}", memberId, year, month);

        validateMemberExists(memberId);

        YearMonth yearMonth = YearMonth.of(year, month);
        List<MoodDiary> diaries = moodDiaryRepository.findByMemberIdAndYearMonth(memberId, year, month);

        // 날짜별 일기 매핑
        Map<LocalDate, MoodDiary> diaryMap = diaries.stream()
                .collect(Collectors.toMap(MoodDiary::getDiaryDate, d -> d));

        // 캘린더 레코드 생성
        List<CalendarDayRecord> records = new ArrayList<>();
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = yearMonth.atDay(day);
            MoodDiary diary = diaryMap.get(date);

            records.add(new CalendarDayRecord(
                    day,
                    date,
                    diary != null,
                    diary != null ? diary.getMoodType() : null,
                    diary != null ? diary.getMoodDiaryId() : null
            ));
        }

        return MoodDiaryCalendarResponse.of(yearMonth, diaries.size(), records);
    }

    /**
     * 연속 작성일 계산 (오늘 기준으로 역순 계산)
     */
    private int calculateConsecutiveDays(List<MoodDiary> diaries, LocalDate endDate) {
        if (diaries.isEmpty()) {
            return 0;
        }

        // 날짜 집합 생성
        var dateSet = diaries.stream()
                .map(MoodDiary::getDiaryDate)
                .collect(Collectors.toSet());

        int consecutive = 0;
        LocalDate checkDate = endDate;

        while (dateSet.contains(checkDate)) {
            consecutive++;
            checkDate = checkDate.minusDays(1);
        }

        return consecutive;
    }

    private void validateMemberExists(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new MemberNotFoundException();
        }
    }
}
