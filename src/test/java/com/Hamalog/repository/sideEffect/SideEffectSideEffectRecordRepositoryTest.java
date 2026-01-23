package com.Hamalog.repository.sideEffect;

import static org.assertj.core.api.Assertions.assertThat;

import com.Hamalog.config.TestEncryptionConfig;
import com.Hamalog.domain.idClass.SideEffectSideEffectRecordId;
import com.Hamalog.domain.member.Member;
import com.Hamalog.domain.sideEffect.SideEffect;
import com.Hamalog.domain.sideEffect.SideEffectRecord;
import com.Hamalog.domain.sideEffect.SideEffectSideEffectRecord;
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
@DisplayName("SideEffectSideEffectRecordRepository Tests")
class SideEffectSideEffectRecordRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SideEffectSideEffectRecordRepository sideEffectSideEffectRecordRepository;

    private Member testMember;
    private SideEffect headache;
    private SideEffect nausea;
    private SideEffectRecord sideEffectRecord1;
    private SideEffectRecord sideEffectRecord2;

    @BeforeEach
    void setUp() {
        testMember = createTestMember("test@example.com");
        entityManager.persistAndFlush(testMember);
        
        // Create side effects
        headache = createTestSideEffect("neurological", "headache");
        nausea = createTestSideEffect("gastrointestinal", "nausea");
        entityManager.persistAndFlush(headache);
        entityManager.persistAndFlush(nausea);
        
        // Create side effect records
        sideEffectRecord1 = SideEffectRecord.builder()
                .member(testMember)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
        sideEffectRecord2 = SideEffectRecord.builder()
                .member(testMember)
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();
        entityManager.persistAndFlush(sideEffectRecord1);
        entityManager.persistAndFlush(sideEffectRecord2);
    }

    @Test
    @DisplayName("Should save side effect junction record successfully")
    void save_ValidJunctionRecord_SavesSuccessfully() {
        // given
        SideEffectSideEffectRecord junctionRecord = SideEffectSideEffectRecord.builder()
                .sideEffectRecordId(sideEffectRecord1.getSideEffectRecordId())
                .sideEffectId(headache.getSideEffectId())
                .degree(3)
                .build();

        // when
        SideEffectSideEffectRecord savedRecord = sideEffectSideEffectRecordRepository.save(junctionRecord);

        // then
        assertThat(savedRecord).isNotNull();
        assertThat(savedRecord.getSideEffectRecordId()).isEqualTo(sideEffectRecord1.getSideEffectRecordId());
        assertThat(savedRecord.getSideEffectId()).isEqualTo(headache.getSideEffectId());
        assertThat(savedRecord.getDegree()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should find junction record by composite ID when exists")
    void findById_ExistingJunctionRecord_ReturnsRecord() {
        // given
        SideEffectSideEffectRecord junctionRecord = SideEffectSideEffectRecord.builder()
                .sideEffectRecordId(sideEffectRecord1.getSideEffectRecordId())
                .sideEffectId(headache.getSideEffectId())
                .degree(2)
                .build();
        entityManager.persistAndFlush(junctionRecord);

        SideEffectSideEffectRecordId compositeId = createCompositeId(
                sideEffectRecord1.getSideEffectRecordId(), 
                headache.getSideEffectId()
        );

        // when
        Optional<SideEffectSideEffectRecord> foundRecord = sideEffectSideEffectRecordRepository.findById(compositeId);

        // then
        assertThat(foundRecord).isPresent();
        assertThat(foundRecord.get().getSideEffectRecordId()).isEqualTo(sideEffectRecord1.getSideEffectRecordId());
        assertThat(foundRecord.get().getSideEffectId()).isEqualTo(headache.getSideEffectId());
        assertThat(foundRecord.get().getDegree()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return empty when junction record does not exist")
    void findById_NonExistentJunctionRecord_ReturnsEmpty() {
        // given
        SideEffectSideEffectRecordId nonExistentId = createCompositeId(999L, 999L);

        // when
        Optional<SideEffectSideEffectRecord> foundRecord = sideEffectSideEffectRecordRepository.findById(nonExistentId);

        // then
        assertThat(foundRecord).isEmpty();
    }

    @Test
    @DisplayName("Should find all junction records")
    void findAll_WithJunctionRecords_ReturnsAllRecords() {
        // given
        SideEffectSideEffectRecord junction1 = SideEffectSideEffectRecord.builder()
                .sideEffectRecordId(sideEffectRecord1.getSideEffectRecordId())
                .sideEffectId(headache.getSideEffectId())
                .degree(3)
                .build();
        SideEffectSideEffectRecord junction2 = SideEffectSideEffectRecord.builder()
                .sideEffectRecordId(sideEffectRecord1.getSideEffectRecordId())
                .sideEffectId(nausea.getSideEffectId())
                .degree(2)
                .build();
        SideEffectSideEffectRecord junction3 = SideEffectSideEffectRecord.builder()
                .sideEffectRecordId(sideEffectRecord2.getSideEffectRecordId())
                .sideEffectId(headache.getSideEffectId())
                .degree(1)
                .build();
        
        entityManager.persistAndFlush(junction1);
        entityManager.persistAndFlush(junction2);
        entityManager.persistAndFlush(junction3);

        // when
        List<SideEffectSideEffectRecord> junctionRecords = sideEffectSideEffectRecordRepository.findAll();

        // then
        assertThat(junctionRecords).hasSize(3);
        assertThat(junctionRecords).extracting("degree")
            .containsExactlyInAnyOrder(3, 2, 1);
    }

    @Test
    @DisplayName("Should count junction records correctly")
    void count_WithJunctionRecords_ReturnsCorrectCount() {
        // given
        SideEffectSideEffectRecord junction1 = SideEffectSideEffectRecord.builder()
                .sideEffectRecordId(sideEffectRecord1.getSideEffectRecordId())
                .sideEffectId(headache.getSideEffectId())
                .degree(3)
                .build();
        SideEffectSideEffectRecord junction2 = SideEffectSideEffectRecord.builder()
                .sideEffectRecordId(sideEffectRecord2.getSideEffectRecordId())
                .sideEffectId(nausea.getSideEffectId())
                .degree(2)
                .build();
        
        entityManager.persistAndFlush(junction1);
        entityManager.persistAndFlush(junction2);

        // when
        long count = sideEffectSideEffectRecordRepository.count();

        // then
        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("Should delete junction record successfully")
    void delete_ExistingJunctionRecord_DeletesSuccessfully() {
        // given
        SideEffectSideEffectRecord junctionRecord = SideEffectSideEffectRecord.builder()
                .sideEffectRecordId(sideEffectRecord1.getSideEffectRecordId())
                .sideEffectId(headache.getSideEffectId())
                .degree(3)
                .build();
        SideEffectSideEffectRecord savedRecord = entityManager.persistAndFlush(junctionRecord);

        // when
        sideEffectSideEffectRecordRepository.delete(savedRecord);
        entityManager.flush();

        // then
        SideEffectSideEffectRecordId compositeId = createCompositeId(
                sideEffectRecord1.getSideEffectRecordId(), 
                headache.getSideEffectId()
        );
        Optional<SideEffectSideEffectRecord> foundRecord = sideEffectSideEffectRecordRepository.findById(compositeId);
        assertThat(foundRecord).isEmpty();
    }

    @Test
    @DisplayName("Should delete junction record by composite ID successfully")
    void deleteById_ExistingJunctionRecord_DeletesSuccessfully() {
        // given
        SideEffectSideEffectRecord junctionRecord = SideEffectSideEffectRecord.builder()
                .sideEffectRecordId(sideEffectRecord1.getSideEffectRecordId())
                .sideEffectId(headache.getSideEffectId())
                .degree(3)
                .build();
        entityManager.persistAndFlush(junctionRecord);

        SideEffectSideEffectRecordId compositeId = createCompositeId(
                sideEffectRecord1.getSideEffectRecordId(), 
                headache.getSideEffectId()
        );

        // when
        sideEffectSideEffectRecordRepository.deleteById(compositeId);
        entityManager.flush();

        // then
        Optional<SideEffectSideEffectRecord> foundRecord = sideEffectSideEffectRecordRepository.findById(compositeId);
        assertThat(foundRecord).isEmpty();
    }

    @Test
    @DisplayName("Should handle multiple junction records for same side effect record")
    void save_MultipleSideEffectsForSameRecord_SavesCorrectly() {
        // given
        SideEffectSideEffectRecord junction1 = SideEffectSideEffectRecord.builder()
                .sideEffectRecordId(sideEffectRecord1.getSideEffectRecordId())
                .sideEffectId(headache.getSideEffectId())
                .degree(3)
                .build();
        SideEffectSideEffectRecord junction2 = SideEffectSideEffectRecord.builder()
                .sideEffectRecordId(sideEffectRecord1.getSideEffectRecordId())
                .sideEffectId(nausea.getSideEffectId())
                .degree(2)
                .build();

        // when
        SideEffectSideEffectRecord savedJunction1 = sideEffectSideEffectRecordRepository.save(junction1);
        SideEffectSideEffectRecord savedJunction2 = sideEffectSideEffectRecordRepository.save(junction2);

        // then
        assertThat(savedJunction1.getSideEffectRecordId()).isEqualTo(sideEffectRecord1.getSideEffectRecordId());
        assertThat(savedJunction2.getSideEffectRecordId()).isEqualTo(sideEffectRecord1.getSideEffectRecordId());
        assertThat(savedJunction1.getSideEffectId()).isNotEqualTo(savedJunction2.getSideEffectId());
        assertThat(savedJunction1.getDegree()).isEqualTo(3);
        assertThat(savedJunction2.getDegree()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should handle multiple junction records for same side effect")
    void save_SameSideEffectForMultipleRecords_SavesCorrectly() {
        // given
        SideEffectSideEffectRecord junction1 = SideEffectSideEffectRecord.builder()
                .sideEffectRecordId(sideEffectRecord1.getSideEffectRecordId())
                .sideEffectId(headache.getSideEffectId())
                .degree(3)
                .build();
        SideEffectSideEffectRecord junction2 = SideEffectSideEffectRecord.builder()
                .sideEffectRecordId(sideEffectRecord2.getSideEffectRecordId())
                .sideEffectId(headache.getSideEffectId())
                .degree(1)
                .build();

        // when
        SideEffectSideEffectRecord savedJunction1 = sideEffectSideEffectRecordRepository.save(junction1);
        SideEffectSideEffectRecord savedJunction2 = sideEffectSideEffectRecordRepository.save(junction2);

        // then
        assertThat(savedJunction1.getSideEffectId()).isEqualTo(headache.getSideEffectId());
        assertThat(savedJunction2.getSideEffectId()).isEqualTo(headache.getSideEffectId());
        assertThat(savedJunction1.getSideEffectRecordId()).isNotEqualTo(savedJunction2.getSideEffectRecordId());
        assertThat(savedJunction1.getDegree()).isEqualTo(3);
        assertThat(savedJunction2.getDegree()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle different degree values correctly")
    void save_VariousDegreeValues_SavesCorrectly() {
        // given
        SideEffectSideEffectRecord lowDegree = SideEffectSideEffectRecord.builder()
                .sideEffectRecordId(sideEffectRecord1.getSideEffectRecordId())
                .sideEffectId(headache.getSideEffectId())
                .degree(1)
                .build();
        SideEffectSideEffectRecord highDegree = SideEffectSideEffectRecord.builder()
                .sideEffectRecordId(sideEffectRecord2.getSideEffectRecordId())
                .sideEffectId(nausea.getSideEffectId())
                .degree(5)
                .build();

        // when
        SideEffectSideEffectRecord savedLowDegree = sideEffectSideEffectRecordRepository.save(lowDegree);
        SideEffectSideEffectRecord savedHighDegree = sideEffectSideEffectRecordRepository.save(highDegree);

        // then
        assertThat(savedLowDegree.getDegree()).isEqualTo(1);
        assertThat(savedHighDegree.getDegree()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should verify composite ID equality works correctly")
    void compositeId_EqualityComparison_WorksCorrectly() {
        // given
        SideEffectSideEffectRecordId id1 = createCompositeId(1L, 2L);
        SideEffectSideEffectRecordId id2 = createCompositeId(1L, 2L);
        SideEffectSideEffectRecordId id3 = createCompositeId(1L, 3L);

        // then
        assertThat(id1).isEqualTo(id2);
        assertThat(id1).isNotEqualTo(id3);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        assertThat(id1.hashCode()).isNotEqualTo(id3.hashCode());
    }

    @Test
    @DisplayName("Should load related entities through relationships")
    void findById_WithRelationships_LoadsRelatedEntities() {
        // given
        SideEffectSideEffectRecord junctionRecord = SideEffectSideEffectRecord.builder()
                .sideEffectRecordId(sideEffectRecord1.getSideEffectRecordId())
                .sideEffectId(headache.getSideEffectId())
                .sideEffectRecord(sideEffectRecord1) // Set relationships
                .sideEffect(headache)
                .degree(3)
                .build();
        entityManager.persistAndFlush(junctionRecord);

        SideEffectSideEffectRecordId compositeId = createCompositeId(
                sideEffectRecord1.getSideEffectRecordId(), 
                headache.getSideEffectId()
        );

        // when
        Optional<SideEffectSideEffectRecord> foundRecord = sideEffectSideEffectRecordRepository.findById(compositeId);

        // then
        assertThat(foundRecord).isPresent();
        assertThat(foundRecord.get().getSideEffectRecord()).isNotNull();
        assertThat(foundRecord.get().getSideEffect()).isNotNull();
        assertThat(foundRecord.get().getSideEffectRecord().getMember()).isNotNull();
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

    private SideEffect createTestSideEffect(String type, String name) {
        // Use reflection to instantiate SideEffect since it has protected no-args constructor
        try {
            java.lang.reflect.Constructor<SideEffect> constructor = SideEffect.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            SideEffect sideEffect = constructor.newInstance();

            java.lang.reflect.Field typeField = SideEffect.class.getDeclaredField("type");
            typeField.setAccessible(true);
            typeField.set(sideEffect, type);
            
            java.lang.reflect.Field nameField = SideEffect.class.getDeclaredField("name");
            nameField.setAccessible(true);
            nameField.set(sideEffect, name);

            return sideEffect;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test SideEffect", e);
        }
    }

    private SideEffectSideEffectRecordId createCompositeId(Long sideEffectRecordId, Long sideEffectId) {
        SideEffectSideEffectRecordId compositeId = new SideEffectSideEffectRecordId();
        
        // Use reflection to set fields since the ID class only has private fields
        try {
            java.lang.reflect.Field recordIdField = SideEffectSideEffectRecordId.class.getDeclaredField("sideEffectRecordId");
            recordIdField.setAccessible(true);
            recordIdField.set(compositeId, sideEffectRecordId);
            
            java.lang.reflect.Field effectIdField = SideEffectSideEffectRecordId.class.getDeclaredField("sideEffectId");
            effectIdField.setAccessible(true);
            effectIdField.set(compositeId, sideEffectId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create composite ID", e);
        }
        
        return compositeId;
    }
}