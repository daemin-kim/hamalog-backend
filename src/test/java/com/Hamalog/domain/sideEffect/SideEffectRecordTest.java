package com.Hamalog.domain.sideEffect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.domain.member.Member;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("SideEffectRecord 도메인 테스트")
class SideEffectRecordTest {

    private Member mockMember;
    private SideEffectRecord sideEffectRecord;

    @BeforeEach
    void setUp() {
        mockMember = mock(Member.class);
    }

    @Nested
    @DisplayName("생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("SideEffectRecord 생성 테스트")
        void createSideEffectRecord() {
            // given
            LocalDateTime now = LocalDateTime.now();

            // when
            sideEffectRecord = SideEffectRecord.builder()
                    .member(mockMember)
                    .createdAt(now)
                    .build();

            // then
            assertThat(sideEffectRecord).isNotNull();
            assertThat(sideEffectRecord.getMember()).isEqualTo(mockMember);
            assertThat(sideEffectRecord.getCreatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("SideEffectRecord의 빈 빌더 테스트")
        void emptyBuilderTest() {
            // when
            sideEffectRecord = SideEffectRecord.builder().build();

            // then
            assertThat(sideEffectRecord).isNotNull();
            assertThat(sideEffectRecord.getSideEffectRecordId()).isNull();
            assertThat(sideEffectRecord.getMember()).isNull();
            assertThat(sideEffectRecord.getCreatedAt()).isNull();
        }

        @Test
        @DisplayName("SideEffectRecord의 전체 생성자 테스트")
        void allArgsConstructorTest() {
            // given
            Long id = 1L;
            LocalDateTime now = LocalDateTime.now();
            String desc = "test";

            // when
            sideEffectRecord = SideEffectRecord.builder()
                    .sideEffectRecordId(id)
                    .member(mockMember)
                    .linkedMedicationSchedule(null)
                    .createdAt(now)
                    .description(desc)
                    .build();

            // then
            assertThat(sideEffectRecord.getSideEffectRecordId()).isEqualTo(id);
            assertThat(sideEffectRecord.getMember()).isEqualTo(mockMember);
            assertThat(sideEffectRecord.getLinkedMedicationSchedule()).isNull();
            assertThat(sideEffectRecord.getCreatedAt()).isEqualTo(now);
            assertThat(sideEffectRecord.getDescription()).isEqualTo(desc);
        }
    }

    @Nested
    @DisplayName("복약 스케줄 연계 테스트")
    class MedicationLinkTest {

        @Test
        @DisplayName("복약 스케줄 연계 여부 확인 - 연계되지 않은 경우")
        void isLinkedToMedication_WhenNotLinked() {
            // given
            sideEffectRecord = SideEffectRecord.builder()
                    .member(mockMember)
                    .createdAt(LocalDateTime.now())
                    .build();

            // then
            assertThat(sideEffectRecord.isLinkedToMedication()).isFalse();
        }

        @Test
        @DisplayName("복약 스케줄 연계 여부 확인 - 연계된 경우")
        void isLinkedToMedication_WhenLinked() {
            // given
            MedicationSchedule mockSchedule = mock(MedicationSchedule.class);
            sideEffectRecord = SideEffectRecord.builder()
                    .member(mockMember)
                    .linkedMedicationSchedule(mockSchedule)
                    .createdAt(LocalDateTime.now())
                    .build();

            // then
            assertThat(sideEffectRecord.isLinkedToMedication()).isTrue();
        }

        @Test
        @DisplayName("복약 스케줄 연계 설정")
        void linkToMedicationSchedule() {
            // given
            MedicationSchedule mockSchedule = mock(MedicationSchedule.class);
            sideEffectRecord = SideEffectRecord.builder()
                    .member(mockMember)
                    .createdAt(LocalDateTime.now())
                    .build();

            // when
            sideEffectRecord.linkToMedicationSchedule(mockSchedule);

            // then
            assertThat(sideEffectRecord.isLinkedToMedication()).isTrue();
            assertThat(sideEffectRecord.getLinkedMedicationSchedule()).isEqualTo(mockSchedule);
        }

        @Test
        @DisplayName("복약 스케줄 연계 해제")
        void unlinkFromMedicationSchedule() {
            // given
            MedicationSchedule mockSchedule = mock(MedicationSchedule.class);
            sideEffectRecord = SideEffectRecord.builder()
                    .member(mockMember)
                    .linkedMedicationSchedule(mockSchedule)
                    .createdAt(LocalDateTime.now())
                    .build();

            // when
            sideEffectRecord.unlinkFromMedicationSchedule();

            // then
            assertThat(sideEffectRecord.isLinkedToMedication()).isFalse();
        }

        @Test
        @DisplayName("연계된 약물명 조회 - 연계된 경우")
        void getLinkedMedicationName_WhenLinked() {
            // given
            MedicationSchedule mockSchedule = mock(MedicationSchedule.class);
            when(mockSchedule.getName()).thenReturn("테스트 약물");
            sideEffectRecord = SideEffectRecord.builder()
                    .member(mockMember)
                    .linkedMedicationSchedule(mockSchedule)
                    .createdAt(LocalDateTime.now())
                    .build();

            // then
            assertThat(sideEffectRecord.getLinkedMedicationName()).isEqualTo("테스트 약물");
        }

        @Test
        @DisplayName("연계된 약물명 조회 - 연계되지 않은 경우")
        void getLinkedMedicationName_WhenNotLinked() {
            // given
            sideEffectRecord = SideEffectRecord.builder()
                    .member(mockMember)
                    .createdAt(LocalDateTime.now())
                    .build();

            // then
            assertThat(sideEffectRecord.getLinkedMedicationName()).isNull();
        }
    }

    @Nested
    @DisplayName("시간 관련 테스트")
    class TimeTest {

        @Test
        @DisplayName("생성 후 경과 일수 계산")
        void getDaysSinceCreated() {
            // given
            LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
            sideEffectRecord = SideEffectRecord.builder()
                    .member(mockMember)
                    .createdAt(threeDaysAgo)
                    .build();

            // then
            assertThat(sideEffectRecord.getDaysSinceCreated()).isEqualTo(3);
        }

        @Test
        @DisplayName("최근 기록인지 확인 - 7일 이내")
        void isRecent_WhenWithin7Days() {
            // given
            LocalDateTime fiveDaysAgo = LocalDateTime.now().minusDays(5);
            sideEffectRecord = SideEffectRecord.builder()
                    .member(mockMember)
                    .createdAt(fiveDaysAgo)
                    .build();

            // then
            assertThat(sideEffectRecord.isRecent()).isTrue();
        }

        @Test
        @DisplayName("최근 기록인지 확인 - 7일 초과")
        void isRecent_WhenAfter7Days() {
            // given
            LocalDateTime tenDaysAgo = LocalDateTime.now().minusDays(10);
            sideEffectRecord = SideEffectRecord.builder()
                    .member(mockMember)
                    .createdAt(tenDaysAgo)
                    .build();

            // then
            assertThat(sideEffectRecord.isRecent()).isFalse();
        }

        @Test
        @DisplayName("오늘 기록인지 확인 - 오늘인 경우")
        void isToday_WhenToday() {
            // given
            sideEffectRecord = SideEffectRecord.builder()
                    .member(mockMember)
                    .createdAt(LocalDateTime.now())
                    .build();

            // then
            assertThat(sideEffectRecord.isToday()).isTrue();
        }

        @Test
        @DisplayName("오늘 기록인지 확인 - 어제인 경우")
        void isToday_WhenYesterday() {
            // given
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            sideEffectRecord = SideEffectRecord.builder()
                    .member(mockMember)
                    .createdAt(yesterday)
                    .build();

            // then
            assertThat(sideEffectRecord.isToday()).isFalse();
        }
    }

    @Nested
    @DisplayName("설명 관련 테스트")
    class DescriptionTest {

        @Test
        @DisplayName("설명이 있는지 확인 - 있는 경우")
        void hasDescription_WhenPresent() {
            // given
            sideEffectRecord = SideEffectRecord.builder()
                    .member(mockMember)
                    .createdAt(LocalDateTime.now())
                    .description("테스트 설명")
                    .build();

            // then
            assertThat(sideEffectRecord.hasDescription()).isTrue();
        }

        @Test
        @DisplayName("설명이 있는지 확인 - null인 경우")
        void hasDescription_WhenNull() {
            // given
            sideEffectRecord = SideEffectRecord.builder()
                    .member(mockMember)
                    .createdAt(LocalDateTime.now())
                    .build();

            // then
            assertThat(sideEffectRecord.hasDescription()).isFalse();
        }

        @Test
        @DisplayName("설명이 있는지 확인 - 빈 문자열인 경우")
        void hasDescription_WhenBlank() {
            // given
            sideEffectRecord = SideEffectRecord.builder()
                    .member(mockMember)
                    .createdAt(LocalDateTime.now())
                    .description("   ")
                    .build();

            // then
            assertThat(sideEffectRecord.hasDescription()).isFalse();
        }

        @Test
        @DisplayName("설명 업데이트")
        void updateDescription() {
            // given
            sideEffectRecord = SideEffectRecord.builder()
                    .member(mockMember)
                    .createdAt(LocalDateTime.now())
                    .description("원래 설명")
                    .build();

            // when
            sideEffectRecord.updateDescription("새로운 설명");

            // then
            assertThat(sideEffectRecord.getDescription()).isEqualTo("새로운 설명");
        }
    }

    @Nested
    @DisplayName("부작용 항목 관리 테스트")
    class SideEffectItemTest {

        @Test
        @DisplayName("부작용 항목 추가")
        void addSideEffectItem() {
            // given
            SideEffect mockSideEffect = mock(SideEffect.class);
            when(mockSideEffect.getSideEffectId()).thenReturn(1L);

            sideEffectRecord = SideEffectRecord.builder()
                    .member(mockMember)
                    .createdAt(LocalDateTime.now())
                    .build();
            ReflectionTestUtils.setField(sideEffectRecord, "sideEffectRecordId", 1L);

            // when
            SideEffectSideEffectRecord item = sideEffectRecord.addSideEffectItem(mockSideEffect, 3);

            // then
            assertThat(item).isNotNull();
            assertThat(item.getDegree()).isEqualTo(3);
            assertThat(sideEffectRecord.getSideEffectItemCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("부작용 항목 제거")
        void removeSideEffectItem() {
            // given
            SideEffect mockSideEffect = mock(SideEffect.class);
            when(mockSideEffect.getSideEffectId()).thenReturn(1L);

            sideEffectRecord = SideEffectRecord.builder()
                    .member(mockMember)
                    .createdAt(LocalDateTime.now())
                    .build();
            ReflectionTestUtils.setField(sideEffectRecord, "sideEffectRecordId", 1L);
            sideEffectRecord.addSideEffectItem(mockSideEffect, 3);

            // when
            boolean removed = sideEffectRecord.removeSideEffectItem(1L);

            // then
            assertThat(removed).isTrue();
            assertThat(sideEffectRecord.getSideEffectItemCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("모든 부작용 항목 제거")
        void clearSideEffectItems() {
            // given
            SideEffect mockSideEffect1 = mock(SideEffect.class);
            SideEffect mockSideEffect2 = mock(SideEffect.class);
            when(mockSideEffect1.getSideEffectId()).thenReturn(1L);
            when(mockSideEffect2.getSideEffectId()).thenReturn(2L);

            sideEffectRecord = SideEffectRecord.builder()
                    .member(mockMember)
                    .createdAt(LocalDateTime.now())
                    .build();
            ReflectionTestUtils.setField(sideEffectRecord, "sideEffectRecordId", 1L);
            sideEffectRecord.addSideEffectItem(mockSideEffect1, 2);
            sideEffectRecord.addSideEffectItem(mockSideEffect2, 4);

            // when
            sideEffectRecord.clearSideEffectItems();

            // then
            assertThat(sideEffectRecord.getSideEffectItemCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("특정 부작용 포함 여부 확인")
        void containsSideEffect() {
            // given
            SideEffect mockSideEffect = mock(SideEffect.class);
            when(mockSideEffect.getSideEffectId()).thenReturn(1L);

            sideEffectRecord = SideEffectRecord.builder()
                    .member(mockMember)
                    .createdAt(LocalDateTime.now())
                    .build();
            ReflectionTestUtils.setField(sideEffectRecord, "sideEffectRecordId", 1L);
            sideEffectRecord.addSideEffectItem(mockSideEffect, 3);

            // then
            assertThat(sideEffectRecord.containsSideEffect(1L)).isTrue();
            assertThat(sideEffectRecord.containsSideEffect(999L)).isFalse();
        }

        @Test
        @DisplayName("부작용 항목 불변 리스트 반환")
        void getSideEffectItemsReadOnly() {
            // given
            sideEffectRecord = SideEffectRecord.builder()
                    .member(mockMember)
                    .createdAt(LocalDateTime.now())
                    .build();

            // when & then
            assertThat(sideEffectRecord.getSideEffectItemsReadOnly()).isNotNull();
            assertThat(sideEffectRecord.getSideEffectItemsReadOnly()).isEmpty();
        }
    }
}
