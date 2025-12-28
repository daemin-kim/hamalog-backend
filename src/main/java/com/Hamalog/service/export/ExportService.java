package com.Hamalog.service.export;

import com.Hamalog.domain.diary.MoodDiary;
import com.Hamalog.domain.medication.MedicationRecord;
import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.domain.member.Member;
import com.Hamalog.domain.sideEffect.SideEffectRecord;
import com.Hamalog.dto.export.ExportDataResponse;
import com.Hamalog.dto.export.ExportDataResponse.*;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.repository.diary.MoodDiaryRepository;
import com.Hamalog.repository.medication.MedicationRecordRepository;
import com.Hamalog.repository.medication.MedicationScheduleRepository;
import com.Hamalog.repository.member.MemberRepository;
import com.Hamalog.repository.sideEffect.SideEffectRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ExportService {

    private final MemberRepository memberRepository;
    private final MedicationScheduleRepository scheduleRepository;
    private final MedicationRecordRepository recordRepository;
    private final MoodDiaryRepository moodDiaryRepository;
    private final SideEffectRecordRepository sideEffectRecordRepository;
    private final ObjectMapper objectMapper;

    /**
     * 사용자의 모든 데이터 내보내기 (JSON 형식)
     */
    public ExportDataResponse exportAllData(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 메타 정보
        ExportMeta meta = new ExportMeta(
                LocalDateTime.now(),
                "JSON",
                "1.0"
        );

        // 회원 정보
        MemberExportData memberData = new MemberExportData(
                member.getMemberId(),
                member.getName(),
                member.getNickName(),
                member.getBirth(),
                member.getCreatedAt()
        );

        // 복약 스케줄
        List<MedicationSchedule> schedules = scheduleRepository.findAllByMember_MemberId(memberId);
        List<MedicationScheduleExportData> scheduleData = schedules.stream()
                .map(s -> new MedicationScheduleExportData(
                        s.getMedicationScheduleId(),
                        s.getName(),
                        s.getHospitalName(),
                        s.getPrescriptionDate(),
                        s.getMemo(),
                        s.getStartOfAd(),
                        s.getPrescriptionDays(),
                        s.getPerDay(),
                        s.getAlarmType().name()
                ))
                .toList();

        // 복약 기록 - 배치 조회로 N+1 문제 해결
        List<Long> scheduleIds = schedules.stream()
                .map(MedicationSchedule::getMedicationScheduleId)
                .toList();

        List<MedicationRecordExportData> recordData;
        if (scheduleIds.isEmpty()) {
            recordData = List.of();
        } else {
            // 한 번의 쿼리로 모든 스케줄의 복약 기록을 조회
            List<MedicationRecord> allRecords = recordRepository.findAllByScheduleIds(scheduleIds);
            recordData = allRecords.stream()
                    .map(r -> new MedicationRecordExportData(
                            r.getMedicationRecordId(),
                            r.getMedicationSchedule().getMedicationScheduleId(),
                            r.getMedicationSchedule().getName(),
                            r.getIsTakeMedication(),
                            r.getRealTakeTime()
                    ))
                    .toList();
        }

        // 마음 일기 (최근 1년)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusYears(1);
        List<MoodDiary> diaries = moodDiaryRepository.findByMemberIdAndDateRange(memberId, startDate, endDate);
        List<MoodDiaryExportData> diaryData = diaries.stream()
                .map(d -> new MoodDiaryExportData(
                        d.getMoodDiaryId(),
                        d.getDiaryDate(),
                        d.getMoodType().name(),
                        d.getDiaryType().name(),
                        d.getDiaryType().name().equals("FREE_FORM") ? d.getFreeContent() :
                                String.join("\n",
                                        nullSafe(d.getTemplateAnswer1()),
                                        nullSafe(d.getTemplateAnswer2()),
                                        nullSafe(d.getTemplateAnswer3()),
                                        nullSafe(d.getTemplateAnswer4()))
                ))
                .toList();

        // 부작용 기록
        List<SideEffectRecord> sideEffectRecords = sideEffectRecordRepository.findByMember_MemberIdOrderByCreatedAtDesc(
                memberId, PageRequest.of(0, 1000)).getContent();
        List<SideEffectRecordExportData> sideEffectData = sideEffectRecords.stream()
                .map(r -> new SideEffectRecordExportData(
                        r.getSideEffectRecordId(),
                        r.getCreatedAt(),
                        List.of() // TODO: 부작용 상세 정보 추가
                ))
                .toList();

        log.info("데이터 내보내기 완료 - memberId: {}, schedules: {}, records: {}, diaries: {}",
                memberId, scheduleData.size(), recordData.size(), diaryData.size());

        return new ExportDataResponse(meta, memberData, scheduleData, recordData, diaryData, sideEffectData);
    }

    /**
     * JSON 형식으로 변환
     */
    public String exportAsJson(Long memberId) {
        try {
            ExportDataResponse data = exportAllData(memberId);
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        } catch (Exception e) {
            log.error("JSON 내보내기 실패 - memberId: {}", memberId, e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * CSV 형식으로 복약 기록 내보내기 (의사 상담용)
     */
    public String exportMedicationRecordsAsCsv(Long memberId, LocalDate startDate, LocalDate endDate) {
        List<MedicationSchedule> schedules = scheduleRepository.findAllByMember_MemberId(memberId);

        StringBuilder csv = new StringBuilder();
        csv.append("날짜,약 이름,복용 여부,복용 시간\n");

        // 배치 조회로 N+1 문제 해결
        List<Long> scheduleIds = schedules.stream()
                .map(MedicationSchedule::getMedicationScheduleId)
                .toList();

        if (scheduleIds.isEmpty()) {
            return csv.toString();
        }

        // 한 번의 쿼리로 모든 스케줄의 복약 기록을 조회
        List<MedicationRecord> allRecords = recordRepository.findAllByScheduleIds(scheduleIds);

        for (MedicationRecord record : allRecords) {
            if (record.getRealTakeTime() != null) {
                LocalDate recordDate = record.getRealTakeTime().toLocalDate();
                if (!recordDate.isBefore(startDate) && !recordDate.isAfter(endDate)) {
                    csv.append(String.format("%s,%s,%s,%s\n",
                            recordDate,
                            escapeCsv(record.getMedicationSchedule().getName()),
                            record.getIsTakeMedication() ? "복용" : "미복용",
                            record.getRealTakeTime().toLocalTime()
                    ));
                }
            }
        }

        return csv.toString();
    }

    private String nullSafe(String str) {
        return str != null ? str : "";
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
