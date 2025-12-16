package com.Hamalog.domain.medication;

import static org.assertj.core.api.Assertions.assertThat;

import com.Hamalog.domain.member.Member;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MedicationSchedule Domain Entity Tests")
class MedicationScheduleTest {

    private Validator validator;
    private Member testMember;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        testMember = Member.builder()
            .loginId("testUser@example.com")
            .password("encryptedPassword")
            .name("testName")
            .phoneNumber("01012345678")
            .nickName("testNick")
            .birth(LocalDate.of(1990, 1, 1))
            .createdAt(LocalDateTime.now())
            .build();
    }

    @Test
    @DisplayName("Should create medication schedule with valid parameters")
    void createMedicationSchedule_ValidParameters_Success() {
        // given
        String name = "약물명";
        String hospitalName = "테스트병원";
        LocalDate prescriptionDate = LocalDate.now();
        String memo = "복용 시 주의사항";
        LocalDate startOfAd = LocalDate.now();
        Integer prescriptionDays = 7;
        Integer perDay = 3;
        AlarmType alarmType = AlarmType.SOUND;

        // when
        MedicationSchedule schedule = new MedicationSchedule(
            testMember, name, hospitalName, prescriptionDate,
            memo, startOfAd, prescriptionDays, perDay, alarmType
        );

        // then
        assertThat(schedule.getMember()).isEqualTo(testMember);
        assertThat(schedule.getName()).isEqualTo(name);
        assertThat(schedule.getHospitalName()).isEqualTo(hospitalName);
        assertThat(schedule.getPrescriptionDate()).isEqualTo(prescriptionDate);
        assertThat(schedule.getMemo()).isEqualTo(memo);
        assertThat(schedule.getStartOfAd()).isEqualTo(startOfAd);
        assertThat(schedule.getPrescriptionDays()).isEqualTo(prescriptionDays);
        assertThat(schedule.getPerDay()).isEqualTo(perDay);
        assertThat(schedule.getAlarmType()).isEqualTo(alarmType);
        assertThat(schedule.getMedicationScheduleId()).isNull(); // Not persisted yet
        assertThat(schedule.getVersion()).isNull(); // Not persisted yet
    }

    @Test
    @DisplayName("Should create medication schedule with null memo")
    void createMedicationSchedule_NullMemo_Success() {
        // given
        String name = "약물명";
        String hospitalName = "테스트병원";
        LocalDate prescriptionDate = LocalDate.now();
        String memo = null;
        LocalDate startOfAd = LocalDate.now();
        Integer prescriptionDays = 7;
        Integer perDay = 3;
        AlarmType alarmType = AlarmType.VIBE;

        // when
        MedicationSchedule schedule = new MedicationSchedule(
            testMember, name, hospitalName, prescriptionDate,
            memo, startOfAd, prescriptionDays, perDay, alarmType
        );

        // then
        assertThat(schedule.getMemo()).isNull();
        assertThat(schedule.getName()).isEqualTo(name);
        assertThat(schedule.getAlarmType()).isEqualTo(alarmType);
    }

    @Test
    @DisplayName("Should update medication schedule with new values")
    void updateMedicationSchedule_ValidParameters_Success() {
        // given
        MedicationSchedule schedule = new MedicationSchedule(
            testMember, "기존약물명", "기존병원", LocalDate.now().minusDays(1),
            "기존메모", LocalDate.now(), 5, 2, AlarmType.SOUND
        );

        String newName = "새로운약물명";
        String newHospitalName = "새로운병원";
        LocalDate newPrescriptionDate = LocalDate.now();
        String newMemo = "새로운 복용 주의사항";
        LocalDate newStartOfAd = LocalDate.now().plusDays(1);
        Integer newPrescriptionDays = 14;
        Integer newPerDay = 1;
        AlarmType newAlarmType = AlarmType.VIBE;

        // when
        schedule.update(
            newName, newHospitalName, newPrescriptionDate,
            newMemo, newStartOfAd, newPrescriptionDays, newPerDay, newAlarmType
        );

        // then
        assertThat(schedule.getName()).isEqualTo(newName);
        assertThat(schedule.getHospitalName()).isEqualTo(newHospitalName);
        assertThat(schedule.getPrescriptionDate()).isEqualTo(newPrescriptionDate);
        assertThat(schedule.getMemo()).isEqualTo(newMemo);
        assertThat(schedule.getStartOfAd()).isEqualTo(newStartOfAd);
        assertThat(schedule.getPrescriptionDays()).isEqualTo(newPrescriptionDays);
        assertThat(schedule.getPerDay()).isEqualTo(newPerDay);
        assertThat(schedule.getAlarmType()).isEqualTo(newAlarmType);
        assertThat(schedule.getMember()).isEqualTo(testMember); // Member should not change
    }

    @Test
    @DisplayName("Should update medication schedule with null memo")
    void updateMedicationSchedule_NullMemo_Success() {
        // given
        MedicationSchedule schedule = new MedicationSchedule(
            testMember, "약물명", "병원명", LocalDate.now(),
            "기존메모", LocalDate.now(), 7, 3, AlarmType.SOUND
        );

        // when
        schedule.update(
            "약물명", "병원명", LocalDate.now(),
            null, LocalDate.now(), 7, 3, AlarmType.SOUND
        );

        // then
        assertThat(schedule.getMemo()).isNull();
    }

    @Test
    @DisplayName("Should create medication schedule with minimum valid prescription days")
    void createMedicationSchedule_MinimumPrescriptionDays_Success() {
        // given
        Integer prescriptionDays = 1;

        // when
        MedicationSchedule schedule = new MedicationSchedule(
            testMember, "약물명", "병원명", LocalDate.now(),
            "메모", LocalDate.now(), prescriptionDays, 1, AlarmType.SOUND
        );

        // then
        assertThat(schedule.getPrescriptionDays()).isEqualTo(prescriptionDays);
    }

    @Test
    @DisplayName("Should create medication schedule with maximum valid per day")
    void createMedicationSchedule_MaximumPerDay_Success() {
        // given
        Integer perDay = 10; // Assuming reasonable maximum

        // when
        MedicationSchedule schedule = new MedicationSchedule(
            testMember, "약물명", "병원명", LocalDate.now(),
            "메모", LocalDate.now(), 7, perDay, AlarmType.SOUND
        );

        // then
        assertThat(schedule.getPerDay()).isEqualTo(perDay);
    }

    @Test
    @DisplayName("Should create medication schedule with all alarm types")
    void createMedicationSchedule_AllAlarmTypes_Success() {
        // Test SOUND alarm type
        MedicationSchedule soundSchedule = new MedicationSchedule(
            testMember, "약물명", "병원명", LocalDate.now(),
            "메모", LocalDate.now(), 7, 3, AlarmType.SOUND
        );
        assertThat(soundSchedule.getAlarmType()).isEqualTo(AlarmType.SOUND);

        // Test VIBE alarm type
        MedicationSchedule vibeSchedule = new MedicationSchedule(
            testMember, "약물명", "병원명", LocalDate.now(),
            "메모", LocalDate.now(), 7, 3, AlarmType.VIBE
        );
        assertThat(vibeSchedule.getAlarmType()).isEqualTo(AlarmType.VIBE);
    }

    @Test
    @DisplayName("Should handle past prescription date")
    void createMedicationSchedule_PastPrescriptionDate_Success() {
        // given
        LocalDate pastDate = LocalDate.now().minusDays(30);

        // when
        MedicationSchedule schedule = new MedicationSchedule(
            testMember, "약물명", "병원명", pastDate,
            "메모", LocalDate.now(), 7, 3, AlarmType.SOUND
        );

        // then
        assertThat(schedule.getPrescriptionDate()).isEqualTo(pastDate);
    }

    @Test
    @DisplayName("Should handle future start date")
    void createMedicationSchedule_FutureStartDate_Success() {
        // given
        LocalDate futureDate = LocalDate.now().plusDays(7);

        // when
        MedicationSchedule schedule = new MedicationSchedule(
            testMember, "약물명", "병원명", LocalDate.now(),
            "메모", futureDate, 7, 3, AlarmType.SOUND
        );

        // then
        assertThat(schedule.getStartOfAd()).isEqualTo(futureDate);
    }

    @Test
    @DisplayName("Should create medication schedule with long memo")
    void createMedicationSchedule_LongMemo_Success() {
        // given
        String longMemo = "이것은 매우 긴 메모입니다. ".repeat(50); // About 1000 characters

        // when
        MedicationSchedule schedule = new MedicationSchedule(
            testMember, "약물명", "병원명", LocalDate.now(),
            longMemo, LocalDate.now(), 7, 3, AlarmType.SOUND
        );

        // then
        assertThat(schedule.getMemo()).isEqualTo(longMemo);
        assertThat(schedule.getMemo().length()).isGreaterThan(500);
    }
}