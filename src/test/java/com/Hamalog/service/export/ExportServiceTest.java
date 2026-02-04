package com.Hamalog.service.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.Hamalog.domain.diary.MoodDiary;
import com.Hamalog.domain.diary.MoodType;
import com.Hamalog.domain.medication.AlarmType;
import com.Hamalog.domain.medication.MedicationRecord;
import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.domain.medication.MedicationTime;
import com.Hamalog.domain.member.Member;
import com.Hamalog.dto.export.ExportDataResponse;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.repository.diary.MoodDiaryRepository;
import com.Hamalog.repository.medication.MedicationRecordRepository;
import com.Hamalog.repository.medication.MedicationScheduleRepository;
import com.Hamalog.repository.member.MemberRepository;
import com.Hamalog.repository.sideEffect.SideEffectRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * ExportService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExportService 테스트")
class ExportServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MedicationScheduleRepository scheduleRepository;

    @Mock
    private MedicationRecordRepository recordRepository;

    @Mock
    private MoodDiaryRepository moodDiaryRepository;

    @Mock
    private SideEffectRecordRepository sideEffectRecordRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ExportService exportService;

    private static final Long TEST_MEMBER_ID = 1L;
    private Member testMember;
    private MedicationSchedule testSchedule;
    private MedicationTime testMedicationTime;
    private MedicationRecord testRecord;
    private MoodDiary testDiary;

    @BeforeEach
    void setUp() {
        // 테스트 회원 생성
        testMember = Member.builder()
                .loginId("test@test.com")
                .password("password123")
                .name("테스트 사용자")
                .nickName("테스트")
                .phoneNumber("01012345678")
                .birth(LocalDate.of(1990, 1, 1))
                .build();
        ReflectionTestUtils.setField(testMember, "memberId", TEST_MEMBER_ID);

        // 테스트 복약 스케줄 생성
        testSchedule = new MedicationSchedule(
                testMember,
                "테스트 약",
                "테스트 병원",
                LocalDate.now(),
                "메모",
                LocalDate.now(),
                30,
                3,
                AlarmType.SOUND
        );
        ReflectionTestUtils.setField(testSchedule, "medicationScheduleId", 1L);

        // 테스트 복약 시간 생성
        testMedicationTime = new MedicationTime(testSchedule, LocalTime.of(9, 0));
        ReflectionTestUtils.setField(testMedicationTime, "medicationTimeId", 1L);

        // 테스트 복약 기록 생성
        testRecord = new MedicationRecord(
                testSchedule,
                testMedicationTime,
                true,
                LocalDateTime.now()
        );
        ReflectionTestUtils.setField(testRecord, "medicationRecordId", 1L);

        // 테스트 마음 일기 생성
        testDiary = MoodDiary.createFreeFormType(
                testMember,
                LocalDate.now(),
                MoodType.HAPPY,
                "오늘은 좋은 하루였다."
        );
        ReflectionTestUtils.setField(testDiary, "moodDiaryId", 1L);
    }

    @Nested
    @DisplayName("exportAllData")
    class ExportAllData {

        @Test
        @DisplayName("성공: 회원 데이터 전체 내보내기")
        void success() {
            // given
            when(memberRepository.findById(TEST_MEMBER_ID)).thenReturn(Optional.of(testMember));
            when(scheduleRepository.findAllByMember_MemberId(TEST_MEMBER_ID)).thenReturn(List.of(testSchedule));
            when(recordRepository.findAllByScheduleIds(any())).thenReturn(List.of(testRecord));
            when(moodDiaryRepository.findByMemberIdAndDateRange(eq(TEST_MEMBER_ID), any(), any()))
                    .thenReturn(List.of(testDiary));
            when(sideEffectRecordRepository.findByMember_MemberIdOrderByCreatedAtDesc(
                    eq(TEST_MEMBER_ID), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            // when
            ExportDataResponse result = exportService.exportAllData(TEST_MEMBER_ID);

            // then
            assertThat(result).isNotNull();
            assertThat(result.member().memberId()).isEqualTo(TEST_MEMBER_ID);
            assertThat(result.medicationSchedules()).hasSize(1);
            assertThat(result.medicationRecords()).hasSize(1);
            assertThat(result.moodDiaries()).hasSize(1);
            assertThat(result.meta().format()).isEqualTo("JSON");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 회원")
        void fail_memberNotFound() {
            // given
            when(memberRepository.findById(TEST_MEMBER_ID)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> exportService.exportAllData(TEST_MEMBER_ID))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException ce = (CustomException) ex;
                        assertThat(ce.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("성공: 데이터가 없는 회원")
        void success_noData() {
            // given
            when(memberRepository.findById(TEST_MEMBER_ID)).thenReturn(Optional.of(testMember));
            when(scheduleRepository.findAllByMember_MemberId(TEST_MEMBER_ID)).thenReturn(List.of());
            when(moodDiaryRepository.findByMemberIdAndDateRange(eq(TEST_MEMBER_ID), any(), any()))
                    .thenReturn(List.of());
            when(sideEffectRecordRepository.findByMember_MemberIdOrderByCreatedAtDesc(
                    eq(TEST_MEMBER_ID), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            // when
            ExportDataResponse result = exportService.exportAllData(TEST_MEMBER_ID);

            // then
            assertThat(result).isNotNull();
            assertThat(result.medicationSchedules()).isEmpty();
            assertThat(result.medicationRecords()).isEmpty();
            assertThat(result.moodDiaries()).isEmpty();
        }
    }

    @Nested
    @DisplayName("exportAsJson")
    class ExportAsJson {

        @Test
        @DisplayName("성공: JSON 형식으로 내보내기")
        void success() {
            // given
            when(memberRepository.findById(TEST_MEMBER_ID)).thenReturn(Optional.of(testMember));
            when(scheduleRepository.findAllByMember_MemberId(TEST_MEMBER_ID)).thenReturn(List.of());
            when(moodDiaryRepository.findByMemberIdAndDateRange(eq(TEST_MEMBER_ID), any(), any()))
                    .thenReturn(List.of());
            when(sideEffectRecordRepository.findByMember_MemberIdOrderByCreatedAtDesc(
                    eq(TEST_MEMBER_ID), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            // when
            String result = exportService.exportAsJson(TEST_MEMBER_ID);

            // then
            assertThat(result).isNotNull();
            assertThat(result).contains("meta");
            assertThat(result).contains("member");
        }
    }

    @Nested
    @DisplayName("exportMedicationRecordsAsCsv")
    class ExportMedicationRecordsAsCsv {

        @Test
        @DisplayName("성공: CSV 형식으로 복약 기록 내보내기")
        void success() {
            // given
            LocalDate startDate = LocalDate.now().minusDays(7);
            LocalDate endDate = LocalDate.now();

            when(scheduleRepository.findAllByMember_MemberId(TEST_MEMBER_ID)).thenReturn(List.of(testSchedule));
            when(recordRepository.findAllByScheduleIds(any())).thenReturn(List.of(testRecord));

            // when
            String result = exportService.exportMedicationRecordsAsCsv(TEST_MEMBER_ID, startDate, endDate);

            // then
            assertThat(result).isNotNull();
            assertThat(result).contains("날짜,약 이름,복용 여부,복용 시간");
            assertThat(result).contains("테스트 약");
            assertThat(result).contains("복용");
        }

        @Test
        @DisplayName("성공: 복약 스케줄이 없는 경우")
        void success_noSchedules() {
            // given
            LocalDate startDate = LocalDate.now().minusDays(7);
            LocalDate endDate = LocalDate.now();

            when(scheduleRepository.findAllByMember_MemberId(TEST_MEMBER_ID)).thenReturn(List.of());

            // when
            String result = exportService.exportMedicationRecordsAsCsv(TEST_MEMBER_ID, startDate, endDate);

            // then
            assertThat(result).isEqualTo("날짜,약 이름,복용 여부,복용 시간\n");
        }

        @Test
        @DisplayName("성공: 쉼표가 포함된 약 이름 이스케이프 처리")
        void success_escapeComma() {
            // given
            LocalDate startDate = LocalDate.now().minusDays(7);
            LocalDate endDate = LocalDate.now();

            // 쉼표가 포함된 약 이름
            MedicationSchedule scheduleWithComma = new MedicationSchedule(
                    testMember,
                    "테스트, 복합약",
                    "테스트 병원",
                    LocalDate.now(),
                    "메모",
                    LocalDate.now(),
                    30,
                    3,
                    AlarmType.SOUND
            );
            ReflectionTestUtils.setField(scheduleWithComma, "medicationScheduleId", 2L);

            MedicationTime medicationTimeWithComma = new MedicationTime(scheduleWithComma, LocalTime.of(9, 0));
            ReflectionTestUtils.setField(medicationTimeWithComma, "medicationTimeId", 2L);

            MedicationRecord recordWithComma = new MedicationRecord(
                    scheduleWithComma,
                    medicationTimeWithComma,
                    true,
                    LocalDateTime.now()
            );
            ReflectionTestUtils.setField(recordWithComma, "medicationRecordId", 2L);

            when(scheduleRepository.findAllByMember_MemberId(TEST_MEMBER_ID))
                    .thenReturn(List.of(scheduleWithComma));
            when(recordRepository.findAllByScheduleIds(any())).thenReturn(List.of(recordWithComma));

            // when
            String result = exportService.exportMedicationRecordsAsCsv(TEST_MEMBER_ID, startDate, endDate);

            // then
            assertThat(result).contains("\"테스트, 복합약\"");
        }
    }
}
