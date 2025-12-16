package com.Hamalog.repository.medication;

import static org.assertj.core.api.Assertions.assertThat;

import com.Hamalog.config.TestEncryptionConfig;
import com.Hamalog.domain.medication.AlarmType;
import com.Hamalog.domain.medication.MedicationRecord;
import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.domain.medication.MedicationTime;
import com.Hamalog.domain.member.Member;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(TestEncryptionConfig.class)
@DisplayName("MedicationRecordRepository Tests")
class MedicationRecordRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MedicationRecordRepository medicationRecordRepository;

    private Member testMember;
    private Member anotherMember;
    private MedicationSchedule testSchedule;
    private MedicationSchedule anotherSchedule;
    private MedicationTime morningTime;
    private MedicationTime eveningTime;

    @BeforeEach
    void setUp() {
        testMember = createTestMember("test@example.com");
        anotherMember = createTestMember("another@example.com");
        entityManager.persistAndFlush(testMember);
        entityManager.persistAndFlush(anotherMember);
        
        testSchedule = createTestMedicationSchedule(testMember, "Test Medicine");
        anotherSchedule = createTestMedicationSchedule(anotherMember, "Another Medicine");
        entityManager.persistAndFlush(testSchedule);
        entityManager.persistAndFlush(anotherSchedule);
        
        morningTime = createTestMedicationTime(testSchedule, LocalTime.of(8, 0));
        eveningTime = createTestMedicationTime(testSchedule, LocalTime.of(20, 0));
        entityManager.persistAndFlush(morningTime);
        entityManager.persistAndFlush(eveningTime);
    }

    @Test
    @DisplayName("Should save medication record successfully")
    void save_ValidMedicationRecord_SavesSuccessfully() {
        // given
        MedicationRecord record = new MedicationRecord(
                testSchedule, 
                morningTime, 
                true, 
                LocalDateTime.now()
        );

        // when
        MedicationRecord savedRecord = medicationRecordRepository.save(record);

        // then
        assertThat(savedRecord).isNotNull();
        assertThat(savedRecord.getMedicationRecordId()).isNotNull();
        assertThat(savedRecord.getIsTakeMedication()).isTrue();
        assertThat(savedRecord.getMedicationSchedule()).isEqualTo(testSchedule);
        assertThat(savedRecord.getMedicationTime()).isEqualTo(morningTime);
        assertThat(savedRecord.getVersion()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Should find medication record by ID with EntityGraph")
    void findById_ExistingRecord_ReturnsRecordWithEntityGraph() {
        // given
        MedicationRecord record = new MedicationRecord(
                testSchedule, 
                morningTime, 
                false, 
                null
        );
        MedicationRecord savedRecord = entityManager.persistAndFlush(record);

        // when
        Optional<MedicationRecord> foundRecord = medicationRecordRepository.findById(savedRecord.getMedicationRecordId());

        // then
        assertThat(foundRecord).isPresent();
        assertThat(foundRecord.get().getMedicationRecordId()).isEqualTo(savedRecord.getMedicationRecordId());
        assertThat(foundRecord.get().getIsTakeMedication()).isFalse();
        assertThat(foundRecord.get().getRealTakeTime()).isNull();
        
        // Verify EntityGraph loads related entities
        assertThat(foundRecord.get().getMedicationSchedule()).isNotNull();
        assertThat(foundRecord.get().getMedicationSchedule().getMember()).isNotNull();
        assertThat(foundRecord.get().getMedicationTime()).isNotNull();
    }

    @Test
    @DisplayName("Should return empty when medication record does not exist")
    void findById_NonExistentRecord_ReturnsEmpty() {
        // given
        Long nonExistentId = 999L;

        // when
        Optional<MedicationRecord> foundRecord = medicationRecordRepository.findById(nonExistentId);

        // then
        assertThat(foundRecord).isEmpty();
    }

    @Test
    @DisplayName("Should find all records by medication schedule ID with EntityGraph")
    void findAllByMedicationSchedule_MedicationScheduleId_WithEntityGraph_ReturnsRecords() {
        // given
        MedicationRecord record1 = new MedicationRecord(testSchedule, morningTime, true, LocalDateTime.now());
        MedicationRecord record2 = new MedicationRecord(testSchedule, eveningTime, false, null);
        MedicationRecord record3 = new MedicationRecord(anotherSchedule, morningTime, true, LocalDateTime.now());
        
        entityManager.persistAndFlush(record1);
        entityManager.persistAndFlush(record2);
        entityManager.persistAndFlush(record3);

        // when
        List<MedicationRecord> records = medicationRecordRepository
                .findAllByMedicationSchedule_MedicationScheduleId(testSchedule.getMedicationScheduleId());

        // then
        assertThat(records).hasSize(2);
        assertThat(records).allMatch(record -> 
                record.getMedicationSchedule().getMedicationScheduleId().equals(testSchedule.getMedicationScheduleId()));
        
        // Verify EntityGraph loaded related entities
        assertThat(records).allMatch(record -> record.getMedicationSchedule() != null);
        assertThat(records).allMatch(record -> record.getMedicationSchedule().getMember() != null);
        assertThat(records).allMatch(record -> record.getMedicationTime() != null);
    }

    @Test
    @DisplayName("Should find record by ID with member for ownership check using JOIN FETCH")
    void findByIdWithMemberForOwnershipCheck_ExistingRecord_ReturnsRecordWithMember() {
        // given
        MedicationRecord record = new MedicationRecord(
                testSchedule, 
                morningTime, 
                true, 
                LocalDateTime.now()
        );
        MedicationRecord savedRecord = entityManager.persistAndFlush(record);

        // when
        Optional<MedicationRecord> foundRecord = medicationRecordRepository
                .findByIdWithMemberForOwnershipCheck(savedRecord.getMedicationRecordId());

        // then
        assertThat(foundRecord).isPresent();
        assertThat(foundRecord.get().getMedicationRecordId()).isEqualTo(savedRecord.getMedicationRecordId());
        
        // Verify JOIN FETCH loaded related entities for ownership check
        assertThat(foundRecord.get().getMedicationSchedule()).isNotNull();
        assertThat(foundRecord.get().getMedicationSchedule().getMember()).isNotNull();
        assertThat(foundRecord.get().getMedicationSchedule().getMember().getLoginId()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should return empty when record not found for ownership check")
    void findByIdWithMemberForOwnershipCheck_NonExistentRecord_ReturnsEmpty() {
        // given
        Long nonExistentId = 999L;

        // when
        Optional<MedicationRecord> foundRecord = medicationRecordRepository
                .findByIdWithMemberForOwnershipCheck(nonExistentId);

        // then
        assertThat(foundRecord).isEmpty();
    }

    @Test
    @DisplayName("Should find all records by schedule ID with JOIN FETCH")
    void findAllByScheduleIdWithJoinFetch_ExistingSchedule_ReturnsRecordsWithJoinFetch() {
        // given
        MedicationRecord record1 = new MedicationRecord(testSchedule, morningTime, true, LocalDateTime.now());
        MedicationRecord record2 = new MedicationRecord(testSchedule, eveningTime, false, null);
        
        entityManager.persistAndFlush(record1);
        entityManager.persistAndFlush(record2);

        // when
        List<MedicationRecord> records = medicationRecordRepository
                .findAllByScheduleIdWithJoinFetch(testSchedule.getMedicationScheduleId());

        // then
        assertThat(records).hasSize(2);
        
        // Verify JOIN FETCH loaded related entities
        assertThat(records).allMatch(record -> record.getMedicationSchedule() != null);
        assertThat(records).allMatch(record -> record.getMedicationTime() != null);
        assertThat(records).extracting("isTakeMedication").containsExactlyInAnyOrder(true, false);
    }

    @Test
    @DisplayName("Should return empty list when no records exist for schedule")
    void findAllByScheduleIdWithJoinFetch_NoRecords_ReturnsEmptyList() {
        // when
        List<MedicationRecord> records = medicationRecordRepository
                .findAllByScheduleIdWithJoinFetch(testSchedule.getMedicationScheduleId());

        // then
        assertThat(records).isEmpty();
    }

    @Test
    @DisplayName("Should delete records by schedule IDs in batch")
    void deleteByScheduleIds_ExistingRecords_DeletesRecordsInBatch() {
        // given
        MedicationRecord record1 = new MedicationRecord(testSchedule, morningTime, true, LocalDateTime.now());
        MedicationRecord record2 = new MedicationRecord(testSchedule, eveningTime, false, null);
        MedicationRecord record3 = new MedicationRecord(anotherSchedule, morningTime, true, LocalDateTime.now());
        
        entityManager.persistAndFlush(record1);
        entityManager.persistAndFlush(record2);
        entityManager.persistAndFlush(record3);

        List<Long> scheduleIdsToDelete = List.of(testSchedule.getMedicationScheduleId());

        // when
        medicationRecordRepository.deleteByScheduleIds(scheduleIdsToDelete);
        entityManager.flush();

        // then
        List<MedicationRecord> remainingRecords = medicationRecordRepository.findAll();
        assertThat(remainingRecords).hasSize(1);
        assertThat(remainingRecords.get(0).getMedicationSchedule().getMedicationScheduleId())
                .isEqualTo(anotherSchedule.getMedicationScheduleId());
    }

    @Test
    @DisplayName("Should delete multiple schedule records in batch")
    void deleteByScheduleIds_MultipleScheduleIds_DeletesAllMatchingRecords() {
        // given
        MedicationRecord record1 = new MedicationRecord(testSchedule, morningTime, true, LocalDateTime.now());
        MedicationRecord record2 = new MedicationRecord(anotherSchedule, morningTime, false, null);
        
        entityManager.persistAndFlush(record1);
        entityManager.persistAndFlush(record2);

        List<Long> scheduleIdsToDelete = List.of(
                testSchedule.getMedicationScheduleId(), 
                anotherSchedule.getMedicationScheduleId()
        );

        // when
        medicationRecordRepository.deleteByScheduleIds(scheduleIdsToDelete);
        entityManager.flush();

        // then
        List<MedicationRecord> remainingRecords = medicationRecordRepository.findAll();
        assertThat(remainingRecords).isEmpty();
    }

    @Test
    @DisplayName("Should handle empty schedule IDs list in batch delete")
    void deleteByScheduleIds_EmptyList_DoesNotDeleteAnyRecords() {
        // given
        MedicationRecord record = new MedicationRecord(testSchedule, morningTime, true, LocalDateTime.now());
        entityManager.persistAndFlush(record);

        List<Long> emptyScheduleIds = List.of();

        // when
        medicationRecordRepository.deleteByScheduleIds(emptyScheduleIds);
        entityManager.flush();

        // then
        List<MedicationRecord> remainingRecords = medicationRecordRepository.findAll();
        assertThat(remainingRecords).hasSize(1);
    }

    @Test
    @DisplayName("Should update medication record successfully")
    void save_UpdatedMedicationRecord_UpdatesSuccessfully() {
        // given
        MedicationRecord record = new MedicationRecord(testSchedule, morningTime, false, null);
        MedicationRecord savedRecord = entityManager.persistAndFlush(record);
        Long initialVersion = savedRecord.getVersion();

        // when
        LocalDateTime realTakeTime = LocalDateTime.now();
        savedRecord.update(true, realTakeTime);
        MedicationRecord updatedRecord = medicationRecordRepository.save(savedRecord);
        entityManager.flush(); // Force version increment

        // then
        assertThat(updatedRecord.getIsTakeMedication()).isTrue();
        assertThat(updatedRecord.getRealTakeTime()).isEqualTo(realTakeTime);
        assertThat(updatedRecord.getVersion()).isGreaterThan(initialVersion);
    }

    @Test
    @DisplayName("Should handle version control correctly")
    void save_ConcurrentUpdate_HandlesVersionControl() {
        // given
        MedicationRecord record = new MedicationRecord(testSchedule, morningTime, false, null);
        MedicationRecord savedRecord = entityManager.persistAndFlush(record);
        Long initialVersion = savedRecord.getVersion();

        // when
        savedRecord.update(true, LocalDateTime.now());
        MedicationRecord updatedRecord = medicationRecordRepository.save(savedRecord);
        entityManager.flush();

        // then
        assertThat(updatedRecord.getVersion()).isGreaterThan(initialVersion);
    }

    @Test
    @DisplayName("Should count medication records correctly")
    void count_WithMedicationRecords_ReturnsCorrectCount() {
        // given
        MedicationRecord record1 = new MedicationRecord(testSchedule, morningTime, true, LocalDateTime.now());
        MedicationRecord record2 = new MedicationRecord(testSchedule, eveningTime, false, null);
        
        entityManager.persistAndFlush(record1);
        entityManager.persistAndFlush(record2);

        // when
        long count = medicationRecordRepository.count();

        // then
        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("Should delete medication record successfully")
    void delete_ExistingMedicationRecord_DeletesSuccessfully() {
        // given
        MedicationRecord record = new MedicationRecord(testSchedule, morningTime, true, LocalDateTime.now());
        MedicationRecord savedRecord = entityManager.persistAndFlush(record);

        // when
        medicationRecordRepository.delete(savedRecord);
        entityManager.flush();

        // then
        Optional<MedicationRecord> foundRecord = medicationRecordRepository.findById(savedRecord.getMedicationRecordId());
        assertThat(foundRecord).isEmpty();
    }

    private Member createTestMember(String loginId) {
        return Member.builder()
                .loginId(loginId)
                .password("$2a$10$encrypted.password.hash")
                .name("TestUser")
                .phoneNumber("01012345678")
                .nickName("TestNick")
                .birth(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private MedicationSchedule createTestMedicationSchedule(Member member, String medicineName) {
        return new MedicationSchedule(
                member,
                medicineName,
                "Test Hospital",
                LocalDate.now().minusDays(7),
                "Test memo",
                LocalDate.now(),
                30,
                2,
                AlarmType.SOUND
        );
    }

    private MedicationTime createTestMedicationTime(MedicationSchedule schedule, LocalTime takeTime) {
        MedicationTime medicationTime = new MedicationTime();
        
        // Use reflection to set the fields since there are no setters
        try {
            java.lang.reflect.Field scheduleField = MedicationTime.class.getDeclaredField("medicationSchedule");
            scheduleField.setAccessible(true);
            scheduleField.set(medicationTime, schedule);
            
            java.lang.reflect.Field timeField = MedicationTime.class.getDeclaredField("takeTime");
            timeField.setAccessible(true);
            timeField.set(medicationTime, takeTime);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test MedicationTime", e);
        }
        
        return medicationTime;
    }
}