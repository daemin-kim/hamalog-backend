package com.Hamalog.repository.sideEffect;

import static org.assertj.core.api.Assertions.assertThat;

import com.Hamalog.config.TestEncryptionConfig;
import com.Hamalog.domain.member.Member;
import com.Hamalog.domain.sideEffect.SideEffectRecord;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
@DisplayName("SideEffectRecordRepository Tests")
class SideEffectRecordRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SideEffectRecordRepository sideEffectRecordRepository;

    private Member testMember;
    private Member anotherMember;

    @BeforeEach
    void setUp() {
        testMember = createTestMember("test@example.com");
        anotherMember = createTestMember("another@example.com");
        entityManager.persistAndFlush(testMember);
        entityManager.persistAndFlush(anotherMember);
    }

    @Test
    @DisplayName("Should save side effect record successfully")
    void save_ValidSideEffectRecord_SavesSuccessfully() {
        // given
        SideEffectRecord record = SideEffectRecord.builder()
                .member(testMember)
                .createdAt(LocalDateTime.now())
                .build();

        // when
        SideEffectRecord savedRecord = sideEffectRecordRepository.save(record);

        // then
        assertThat(savedRecord).isNotNull();
        assertThat(savedRecord.getSideEffectRecordId()).isNotNull();
        assertThat(savedRecord.getMember()).isEqualTo(testMember);
        assertThat(savedRecord.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find side effect record by ID when exists")
    void findById_ExistingSideEffectRecord_ReturnsRecord() {
        // given
        SideEffectRecord record = SideEffectRecord.builder()
                .member(testMember)
                .createdAt(LocalDateTime.now())
                .build();
        SideEffectRecord savedRecord = entityManager.persistAndFlush(record);

        // when
        Optional<SideEffectRecord> foundRecord = sideEffectRecordRepository.findById(savedRecord.getSideEffectRecordId());

        // then
        assertThat(foundRecord).isPresent();
        assertThat(foundRecord.get().getSideEffectRecordId()).isEqualTo(savedRecord.getSideEffectRecordId());
        assertThat(foundRecord.get().getMember().getMemberId()).isEqualTo(testMember.getMemberId());
        assertThat(foundRecord.get().getCreatedAt()).isEqualTo(savedRecord.getCreatedAt());
    }

    @Test
    @DisplayName("Should return empty when side effect record does not exist")
    void findById_NonExistentSideEffectRecord_ReturnsEmpty() {
        // given
        Long nonExistentId = 999L;

        // when
        Optional<SideEffectRecord> foundRecord = sideEffectRecordRepository.findById(nonExistentId);

        // then
        assertThat(foundRecord).isEmpty();
    }

    @Test
    @DisplayName("Should find all side effect records")
    void findAll_WithSideEffectRecords_ReturnsAllRecords() {
        // given
        LocalDateTime now = LocalDateTime.now();
        SideEffectRecord record1 = SideEffectRecord.builder()
                .member(testMember)
                .createdAt(now.minusDays(1))
                .build();
        SideEffectRecord record2 = SideEffectRecord.builder()
                .member(testMember)
                .createdAt(now.minusDays(2))
                .build();
        SideEffectRecord record3 = SideEffectRecord.builder()
                .member(anotherMember)
                .createdAt(now.minusDays(3))
                .build();
        
        entityManager.persistAndFlush(record1);
        entityManager.persistAndFlush(record2);
        entityManager.persistAndFlush(record3);

        // when
        List<SideEffectRecord> records = sideEffectRecordRepository.findAll();

        // then
        assertThat(records).hasSize(3);
        assertThat(records).extracting("member.loginId")
            .containsExactlyInAnyOrder("test@example.com", "test@example.com", "another@example.com");
    }

    @Test
    @DisplayName("Should count side effect records correctly")
    void count_WithSideEffectRecords_ReturnsCorrectCount() {
        // given
        SideEffectRecord record1 = SideEffectRecord.builder()
                .member(testMember)
                .createdAt(LocalDateTime.now())
                .build();
        SideEffectRecord record2 = SideEffectRecord.builder()
                .member(anotherMember)
                .createdAt(LocalDateTime.now())
                .build();
        
        entityManager.persistAndFlush(record1);
        entityManager.persistAndFlush(record2);

        // when
        long count = sideEffectRecordRepository.count();

        // then
        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("Should delete side effect record successfully")
    void delete_ExistingSideEffectRecord_DeletesSuccessfully() {
        // given
        SideEffectRecord record = SideEffectRecord.builder()
                .member(testMember)
                .createdAt(LocalDateTime.now())
                .build();
        SideEffectRecord savedRecord = entityManager.persistAndFlush(record);

        // when
        sideEffectRecordRepository.delete(savedRecord);
        entityManager.flush();

        // then
        Optional<SideEffectRecord> foundRecord = sideEffectRecordRepository.findById(savedRecord.getSideEffectRecordId());
        assertThat(foundRecord).isEmpty();
    }

    @Test
    @DisplayName("Should delete side effect record by ID successfully")
    void deleteById_ExistingSideEffectRecord_DeletesSuccessfully() {
        // given
        SideEffectRecord record = SideEffectRecord.builder()
                .member(testMember)
                .createdAt(LocalDateTime.now())
                .build();
        SideEffectRecord savedRecord = entityManager.persistAndFlush(record);

        // when
        sideEffectRecordRepository.deleteById(savedRecord.getSideEffectRecordId());
        entityManager.flush();

        // then
        Optional<SideEffectRecord> foundRecord = sideEffectRecordRepository.findById(savedRecord.getSideEffectRecordId());
        assertThat(foundRecord).isEmpty();
    }

    @Test
    @DisplayName("Should delete all side effect records by member ID")
    void deleteByMemberId_ExistingRecords_DeletesAllMemberRecords() {
        // given
        LocalDateTime now = LocalDateTime.now();
        SideEffectRecord record1 = SideEffectRecord.builder()
                .member(testMember)
                .createdAt(now.minusDays(1))
                .build();
        SideEffectRecord record2 = SideEffectRecord.builder()
                .member(testMember)
                .createdAt(now.minusDays(2))
                .build();
        SideEffectRecord record3 = SideEffectRecord.builder()
                .member(anotherMember)
                .createdAt(now.minusDays(3))
                .build();
        
        entityManager.persistAndFlush(record1);
        entityManager.persistAndFlush(record2);
        entityManager.persistAndFlush(record3);

        // when
        sideEffectRecordRepository.deleteByMemberId(testMember.getMemberId());
        entityManager.flush();

        // then
        List<SideEffectRecord> remainingRecords = sideEffectRecordRepository.findAll();
        assertThat(remainingRecords).hasSize(1);
        assertThat(remainingRecords.get(0).getMember().getMemberId()).isEqualTo(anotherMember.getMemberId());
    }

    @Test
    @DisplayName("Should handle empty result when deleting by non-existent member ID")
    void deleteByMemberId_NonExistentMember_DoesNotDeleteAnyRecords() {
        // given
        SideEffectRecord record = SideEffectRecord.builder()
                .member(testMember)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persistAndFlush(record);

        Long nonExistentMemberId = 999L;

        // when
        sideEffectRecordRepository.deleteByMemberId(nonExistentMemberId);
        entityManager.flush();

        // then
        List<SideEffectRecord> remainingRecords = sideEffectRecordRepository.findAll();
        assertThat(remainingRecords).hasSize(1);
        assertThat(remainingRecords.get(0).getMember().getMemberId()).isEqualTo(testMember.getMemberId());
    }

    @Test
    @DisplayName("Should handle delete by member ID when no records exist")
    void deleteByMemberId_NoRecords_HandlesGracefully() {
        // when
        sideEffectRecordRepository.deleteByMemberId(testMember.getMemberId());
        entityManager.flush();

        // then
        List<SideEffectRecord> records = sideEffectRecordRepository.findAll();
        assertThat(records).isEmpty();
    }

    @Test
    @DisplayName("Should handle side effect records with different timestamps")
    void save_MultipleRecordsWithDifferentTimestamps_SavesCorrectly() {
        // given
        LocalDateTime now = LocalDateTime.now();
        SideEffectRecord record1 = SideEffectRecord.builder()
                .member(testMember)
                .createdAt(now.minusDays(5))
                .build();
        SideEffectRecord record2 = SideEffectRecord.builder()
                .member(testMember)
                .createdAt(now.minusHours(2))
                .build();
        SideEffectRecord record3 = SideEffectRecord.builder()
                .member(testMember)
                .createdAt(now.minusMinutes(30))
                .build();

        // when
        SideEffectRecord savedRecord1 = sideEffectRecordRepository.save(record1);
        SideEffectRecord savedRecord2 = sideEffectRecordRepository.save(record2);
        SideEffectRecord savedRecord3 = sideEffectRecordRepository.save(record3);

        // then
        assertThat(savedRecord1.getCreatedAt()).isEqualTo(record1.getCreatedAt());
        assertThat(savedRecord2.getCreatedAt()).isEqualTo(record2.getCreatedAt());
        assertThat(savedRecord3.getCreatedAt()).isEqualTo(record3.getCreatedAt());
        
        // Verify chronological order
        assertThat(savedRecord1.getCreatedAt()).isBefore(savedRecord2.getCreatedAt());
        assertThat(savedRecord2.getCreatedAt()).isBefore(savedRecord3.getCreatedAt());
    }

    @Test
    @DisplayName("Should handle batch deletion correctly with multiple members")
    void deleteByMemberId_MultipleMembersBatchDeletion_DeletesCorrectRecords() {
        // given
        LocalDateTime now = LocalDateTime.now();
        
        // Create records for first member
        SideEffectRecord record1 = SideEffectRecord.builder()
                .member(testMember)
                .createdAt(now.minusDays(1))
                .build();
        SideEffectRecord record2 = SideEffectRecord.builder()
                .member(testMember)
                .createdAt(now.minusDays(2))
                .build();
        
        // Create records for second member
        SideEffectRecord record3 = SideEffectRecord.builder()
                .member(anotherMember)
                .createdAt(now.minusDays(3))
                .build();
        SideEffectRecord record4 = SideEffectRecord.builder()
                .member(anotherMember)
                .createdAt(now.minusDays(4))
                .build();
        
        entityManager.persistAndFlush(record1);
        entityManager.persistAndFlush(record2);
        entityManager.persistAndFlush(record3);
        entityManager.persistAndFlush(record4);

        // when - delete first member's records
        sideEffectRecordRepository.deleteByMemberId(testMember.getMemberId());
        entityManager.flush();

        // then
        List<SideEffectRecord> remainingRecords = sideEffectRecordRepository.findAll();
        assertThat(remainingRecords).hasSize(2);
        assertThat(remainingRecords).allMatch(record -> 
            record.getMember().getMemberId().equals(anotherMember.getMemberId()));
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
}