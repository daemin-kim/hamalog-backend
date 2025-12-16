package com.Hamalog.domain.medication;

import static org.assertj.core.api.Assertions.assertThat;

import com.Hamalog.domain.member.Member;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("MedicationRecord Domain Entity Tests")
class MedicationRecordTest {

    @Mock
    private MedicationSchedule mockMedicationSchedule;
    
    @Mock
    private MedicationTime mockMedicationTime;

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
            .loginId("testUser@example.com")
            .password("encryptedPassword")
            .name("testName")
            .phoneNumber("01012345678")
            .nickName("testNick")
            .birth(LocalDate.of(1990, 1, 1))
            .createdAt(LocalDateTime.now())
            .build();
        
        // No mock stubbings needed - MedicationRecord just stores references
    }

    @Test
    @DisplayName("Should create medication record with valid parameters")
    void createMedicationRecord_ValidParameters_Success() {
        // given
        Boolean isTakeMedication = true;
        LocalDateTime realTakeTime = LocalDateTime.now();

        // when
        MedicationRecord record = new MedicationRecord(
            mockMedicationSchedule,
            mockMedicationTime,
            isTakeMedication,
            realTakeTime
        );

        // then
        assertThat(record.getMedicationSchedule()).isEqualTo(mockMedicationSchedule);
        assertThat(record.getMedicationTime()).isEqualTo(mockMedicationTime);
        assertThat(record.getIsTakeMedication()).isEqualTo(isTakeMedication);
        assertThat(record.getRealTakeTime()).isEqualTo(realTakeTime);
        assertThat(record.getMedicationRecordId()).isNull(); // Not persisted yet
        assertThat(record.getVersion()).isNull(); // Not persisted yet
    }

    @Test
    @DisplayName("Should create medication record when medication was taken")
    void createMedicationRecord_MedicationTaken_Success() {
        // given
        Boolean isTakeMedication = true;
        LocalDateTime realTakeTime = LocalDateTime.now();

        // when
        MedicationRecord record = new MedicationRecord(
            mockMedicationSchedule,
            mockMedicationTime,
            isTakeMedication,
            realTakeTime
        );

        // then
        assertThat(record.getIsTakeMedication()).isTrue();
        assertThat(record.getRealTakeTime()).isNotNull();
        assertThat(record.getRealTakeTime()).isEqualTo(realTakeTime);
    }

    @Test
    @DisplayName("Should create medication record when medication was not taken")
    void createMedicationRecord_MedicationNotTaken_Success() {
        // given
        Boolean isTakeMedication = false;
        LocalDateTime realTakeTime = null; // No actual take time when not taken

        // when
        MedicationRecord record = new MedicationRecord(
            mockMedicationSchedule,
            mockMedicationTime,
            isTakeMedication,
            realTakeTime
        );

        // then
        assertThat(record.getIsTakeMedication()).isFalse();
        assertThat(record.getRealTakeTime()).isNull();
    }

    @Test
    @DisplayName("Should create medication record with late take time")
    void createMedicationRecord_LateTakeTime_Success() {
        // given
        Boolean isTakeMedication = true;
        LocalDateTime realTakeTime = LocalDateTime.now().plusHours(2); // 2 hours late

        // when
        MedicationRecord record = new MedicationRecord(
            mockMedicationSchedule,
            mockMedicationTime,
            isTakeMedication,
            realTakeTime
        );

        // then
        assertThat(record.getIsTakeMedication()).isTrue();
        assertThat(record.getRealTakeTime()).isEqualTo(realTakeTime);
    }

    @Test
    @DisplayName("Should create medication record with early take time")
    void createMedicationRecord_EarlyTakeTime_Success() {
        // given
        Boolean isTakeMedication = true;
        LocalDateTime realTakeTime = LocalDateTime.now().minusMinutes(30); // 30 minutes early

        // when
        MedicationRecord record = new MedicationRecord(
            mockMedicationSchedule,
            mockMedicationTime,
            isTakeMedication,
            realTakeTime
        );

        // then
        assertThat(record.getIsTakeMedication()).isTrue();
        assertThat(record.getRealTakeTime()).isEqualTo(realTakeTime);
    }

    @Test
    @DisplayName("Should update medication record from not taken to taken")
    void updateMedicationRecord_FromNotTakenToTaken_Success() {
        // given
        MedicationRecord record = new MedicationRecord(
            mockMedicationSchedule,
            mockMedicationTime,
            false,
            null
        );

        Boolean newIsTakeMedication = true;
        LocalDateTime newRealTakeTime = LocalDateTime.now();

        // when
        record.update(newIsTakeMedication, newRealTakeTime);

        // then
        assertThat(record.getIsTakeMedication()).isTrue();
        assertThat(record.getRealTakeTime()).isEqualTo(newRealTakeTime);
        assertThat(record.getMedicationSchedule()).isEqualTo(mockMedicationSchedule); // Should not change
        assertThat(record.getMedicationTime()).isEqualTo(mockMedicationTime); // Should not change
    }

    @Test
    @DisplayName("Should update medication record from taken to not taken")
    void updateMedicationRecord_FromTakenToNotTaken_Success() {
        // given
        MedicationRecord record = new MedicationRecord(
            mockMedicationSchedule,
            mockMedicationTime,
            true,
            LocalDateTime.now().minusHours(1)
        );

        Boolean newIsTakeMedication = false;
        LocalDateTime newRealTakeTime = null;

        // when
        record.update(newIsTakeMedication, newRealTakeTime);

        // then
        assertThat(record.getIsTakeMedication()).isFalse();
        assertThat(record.getRealTakeTime()).isNull();
    }

    @Test
    @DisplayName("Should update medication record with new take time")
    void updateMedicationRecord_WithNewTakeTime_Success() {
        // given
        LocalDateTime originalTakeTime = LocalDateTime.now().minusHours(2);
        MedicationRecord record = new MedicationRecord(
            mockMedicationSchedule,
            mockMedicationTime,
            true,
            originalTakeTime
        );

        Boolean newIsTakeMedication = true;
        LocalDateTime newRealTakeTime = LocalDateTime.now();

        // when
        record.update(newIsTakeMedication, newRealTakeTime);

        // then
        assertThat(record.getIsTakeMedication()).isTrue();
        assertThat(record.getRealTakeTime()).isEqualTo(newRealTakeTime);
        assertThat(record.getRealTakeTime()).isNotEqualTo(originalTakeTime);
    }

    @Test
    @DisplayName("Should update medication record multiple times")
    void updateMedicationRecord_MultipleTimes_Success() {
        // given
        MedicationRecord record = new MedicationRecord(
            mockMedicationSchedule,
            mockMedicationTime,
            false,
            null
        );

        // First update: mark as taken
        LocalDateTime firstTakeTime = LocalDateTime.now().minusMinutes(30);
        record.update(true, firstTakeTime);
        
        assertThat(record.getIsTakeMedication()).isTrue();
        assertThat(record.getRealTakeTime()).isEqualTo(firstTakeTime);

        // Second update: correct the take time
        LocalDateTime correctedTakeTime = LocalDateTime.now();
        record.update(true, correctedTakeTime);

        // then
        assertThat(record.getIsTakeMedication()).isTrue();
        assertThat(record.getRealTakeTime()).isEqualTo(correctedTakeTime);
        assertThat(record.getRealTakeTime()).isNotEqualTo(firstTakeTime);
    }

    @Test
    @DisplayName("Should handle null real take time when medication not taken")
    void createMedicationRecord_NullRealTakeTimeWhenNotTaken_Success() {
        // given
        Boolean isTakeMedication = false;
        LocalDateTime realTakeTime = null;

        // when
        MedicationRecord record = new MedicationRecord(
            mockMedicationSchedule,
            mockMedicationTime,
            isTakeMedication,
            realTakeTime
        );

        // then
        assertThat(record.getIsTakeMedication()).isFalse();
        assertThat(record.getRealTakeTime()).isNull();
    }

    @Test
    @DisplayName("Should preserve relationships after updates")
    void updateMedicationRecord_PreservesRelationships_Success() {
        // given
        MedicationRecord record = new MedicationRecord(
            mockMedicationSchedule,
            mockMedicationTime,
            true,
            LocalDateTime.now()
        );

        // when
        record.update(false, null);

        // then
        assertThat(record.getMedicationSchedule()).isEqualTo(mockMedicationSchedule);
        assertThat(record.getMedicationTime()).isEqualTo(mockMedicationTime);
        // Only isTakeMedication and realTakeTime should change
        assertThat(record.getIsTakeMedication()).isFalse();
        assertThat(record.getRealTakeTime()).isNull();
    }
}