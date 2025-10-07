package com.Hamalog.repository.sideEffect;

import com.Hamalog.config.TestEncryptionConfig;
import com.Hamalog.domain.member.Member;
import com.Hamalog.domain.sideEffect.SideEffect;
import com.Hamalog.domain.sideEffect.SideEffectRecord;
import com.Hamalog.domain.sideEffect.SideEffectSideEffectRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(TestEncryptionConfig.class)
@DisplayName("SideEffectRepository Tests")
class SideEffectRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SideEffectRepository sideEffectRepository;

    private Member testMember;
    private Member anotherMember;
    private SideEffect headache;
    private SideEffect nausea;
    private SideEffect dizziness;
    private SideEffect fatigue;
    private SideEffect stomachPain;

    @BeforeEach
    void setUp() {
        testMember = createTestMember("test@example.com");
        anotherMember = createTestMember("another@example.com");
        entityManager.persistAndFlush(testMember);
        entityManager.persistAndFlush(anotherMember);
        
        // Create sample side effects
        headache = createTestSideEffect("neurological", "headache");
        nausea = createTestSideEffect("gastrointestinal", "nausea");
        dizziness = createTestSideEffect("neurological", "dizziness");
        fatigue = createTestSideEffect("general", "fatigue");
        stomachPain = createTestSideEffect("gastrointestinal", "stomach pain");
        
        entityManager.persistAndFlush(headache);
        entityManager.persistAndFlush(nausea);
        entityManager.persistAndFlush(dizziness);
        entityManager.persistAndFlush(fatigue);
        entityManager.persistAndFlush(stomachPain);
    }

    @Test
    @DisplayName("Should save side effect successfully")
    void save_ValidSideEffect_SavesSuccessfully() {
        // given
        SideEffect sideEffect = createTestSideEffect("skin", "rash");

        // when
        SideEffect savedSideEffect = sideEffectRepository.save(sideEffect);

        // then
        assertThat(savedSideEffect).isNotNull();
        assertThat(savedSideEffect.getSideEffectId()).isNotNull();
        assertThat(savedSideEffect.getType()).isEqualTo("skin");
        assertThat(savedSideEffect.getName()).isEqualTo("rash");
    }

    @Test
    @DisplayName("Should find side effect by ID when exists")
    void findById_ExistingSideEffect_ReturnsSideEffect() {
        // given
        SideEffect sideEffect = createTestSideEffect("cardiac", "palpitations");
        SideEffect savedSideEffect = entityManager.persistAndFlush(sideEffect);

        // when
        Optional<SideEffect> foundSideEffect = sideEffectRepository.findById(savedSideEffect.getSideEffectId());

        // then
        assertThat(foundSideEffect).isPresent();
        assertThat(foundSideEffect.get().getSideEffectId()).isEqualTo(savedSideEffect.getSideEffectId());
        assertThat(foundSideEffect.get().getType()).isEqualTo("cardiac");
        assertThat(foundSideEffect.get().getName()).isEqualTo("palpitations");
    }

    @Test
    @DisplayName("Should return empty when side effect does not exist")
    void findById_NonExistentSideEffect_ReturnsEmpty() {
        // given
        Long nonExistentId = 999L;

        // when
        Optional<SideEffect> foundSideEffect = sideEffectRepository.findById(nonExistentId);

        // then
        assertThat(foundSideEffect).isEmpty();
    }

    @Test
    @DisplayName("Should find all side effects")
    void findAll_WithSideEffects_ReturnsAllSideEffects() {
        // when
        List<SideEffect> sideEffects = sideEffectRepository.findAll();

        // then
        assertThat(sideEffects).hasSize(5);
        assertThat(sideEffects).extracting("name")
            .containsExactlyInAnyOrder("headache", "nausea", "dizziness", "fatigue", "stomach pain");
    }

    @Test
    @DisplayName("Should count side effects correctly")
    void count_WithSideEffects_ReturnsCorrectCount() {
        // when
        long count = sideEffectRepository.count();

        // then
        assertThat(count).isEqualTo(5L);
    }

    @Test
    @DisplayName("Should delete side effect successfully")
    void delete_ExistingSideEffect_DeletesSuccessfully() {
        // given
        SideEffect sideEffect = createTestSideEffect("respiratory", "cough");
        SideEffect savedSideEffect = entityManager.persistAndFlush(sideEffect);

        // when
        sideEffectRepository.delete(savedSideEffect);
        entityManager.flush();

        // then
        Optional<SideEffect> foundSideEffect = sideEffectRepository.findById(savedSideEffect.getSideEffectId());
        assertThat(foundSideEffect).isEmpty();
    }

    @Test
    @DisplayName("Should find recent side effect names for member with records")
    void findRecentSideEffectNames_MemberWithRecords_ReturnsRecentNames() {
        // given
        LocalDateTime now = LocalDateTime.now();
        
        // Create side effect records for testMember (in reverse chronological order)
        SideEffectRecord record1 = createSideEffectRecord(testMember, now.minusDays(1)); // Most recent
        SideEffectRecord record2 = createSideEffectRecord(testMember, now.minusDays(2));
        SideEffectRecord record3 = createSideEffectRecord(testMember, now.minusDays(3));
        SideEffectRecord record4 = createSideEffectRecord(testMember, now.minusDays(4));
        SideEffectRecord record5 = createSideEffectRecord(testMember, now.minusDays(5));
        SideEffectRecord record6 = createSideEffectRecord(testMember, now.minusDays(6)); // Oldest, should be excluded
        
        entityManager.persistAndFlush(record1);
        entityManager.persistAndFlush(record2);
        entityManager.persistAndFlush(record3);
        entityManager.persistAndFlush(record4);
        entityManager.persistAndFlush(record5);
        entityManager.persistAndFlush(record6);
        
        // Create junction records linking side effects to records
        createAndPersistJunctionRecord(record1.getSideEffectRecordId(), headache.getSideEffectId(), 3);
        createAndPersistJunctionRecord(record2.getSideEffectRecordId(), nausea.getSideEffectId(), 2);
        createAndPersistJunctionRecord(record3.getSideEffectRecordId(), dizziness.getSideEffectId(), 1);
        createAndPersistJunctionRecord(record4.getSideEffectRecordId(), fatigue.getSideEffectId(), 2);
        createAndPersistJunctionRecord(record5.getSideEffectRecordId(), stomachPain.getSideEffectId(), 3);
        createAndPersistJunctionRecord(record6.getSideEffectRecordId(), headache.getSideEffectId(), 1); // Should be excluded

        // when
        List<String> recentSideEffectNames = sideEffectRepository.findRecentSideEffectNames(testMember.getMemberId());

        // then
        assertThat(recentSideEffectNames).hasSize(5);
        assertThat(recentSideEffectNames)
            .containsExactly("headache", "nausea", "dizziness", "fatigue", "stomach pain"); // In chronological order
    }

    @Test
    @DisplayName("Should return empty list when member has no side effect records")
    void findRecentSideEffectNames_MemberWithNoRecords_ReturnsEmptyList() {
        // when
        List<String> recentSideEffectNames = sideEffectRepository.findRecentSideEffectNames(testMember.getMemberId());

        // then
        assertThat(recentSideEffectNames).isEmpty();
    }

    @Test
    @DisplayName("Should return only member's side effects, not other members'")
    void findRecentSideEffectNames_MultipleMembers_ReturnsOnlyMembersSideEffects() {
        // given
        LocalDateTime now = LocalDateTime.now();
        
        // Create records for testMember
        SideEffectRecord testMemberRecord = createSideEffectRecord(testMember, now.minusDays(1));
        entityManager.persistAndFlush(testMemberRecord);
        createAndPersistJunctionRecord(testMemberRecord.getSideEffectRecordId(), headache.getSideEffectId(), 2);
        
        // Create records for anotherMember (should not be included)
        SideEffectRecord anotherMemberRecord = createSideEffectRecord(anotherMember, now.minusHours(1)); // More recent
        entityManager.persistAndFlush(anotherMemberRecord);
        createAndPersistJunctionRecord(anotherMemberRecord.getSideEffectRecordId(), nausea.getSideEffectId(), 3);

        // when
        List<String> testMemberSideEffects = sideEffectRepository.findRecentSideEffectNames(testMember.getMemberId());
        List<String> anotherMemberSideEffects = sideEffectRepository.findRecentSideEffectNames(anotherMember.getMemberId());

        // then
        assertThat(testMemberSideEffects).containsExactly("headache");
        assertThat(anotherMemberSideEffects).containsExactly("nausea");
    }

    @Test
    @DisplayName("Should limit results to 5 most recent side effects")
    void findRecentSideEffectNames_MoreThanFiveRecords_ReturnsOnlyFiveMostRecent() {
        // given
        LocalDateTime now = LocalDateTime.now();
        
        // Create 7 side effect records
        SideEffect[] sideEffects = {headache, nausea, dizziness, fatigue, stomachPain};
        for (int i = 0; i < 7; i++) {
            SideEffectRecord record = createSideEffectRecord(testMember, now.minusDays(i + 1));
            entityManager.persistAndFlush(record);
            
            // Use side effects cyclically
            SideEffect sideEffect = sideEffects[i % sideEffects.length];
            createAndPersistJunctionRecord(record.getSideEffectRecordId(), sideEffect.getSideEffectId(), 1);
        }

        // when
        List<String> recentSideEffectNames = sideEffectRepository.findRecentSideEffectNames(testMember.getMemberId());

        // then
        assertThat(recentSideEffectNames).hasSize(5); // Limited to 5
    }

    @Test
    @DisplayName("Should handle non-existent member ID gracefully")
    void findRecentSideEffectNames_NonExistentMember_ReturnsEmptyList() {
        // given
        Long nonExistentMemberId = 999L;

        // when
        List<String> recentSideEffectNames = sideEffectRepository.findRecentSideEffectNames(nonExistentMemberId);

        // then
        assertThat(recentSideEffectNames).isEmpty();
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
        SideEffect sideEffect = new SideEffect();
        
        // Use reflection to set fields since SideEffect only has @Getter
        try {
            java.lang.reflect.Field typeField = SideEffect.class.getDeclaredField("type");
            typeField.setAccessible(true);
            typeField.set(sideEffect, type);
            
            java.lang.reflect.Field nameField = SideEffect.class.getDeclaredField("name");
            nameField.setAccessible(true);
            nameField.set(sideEffect, name);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test SideEffect", e);
        }
        
        return sideEffect;
    }

    private SideEffectRecord createSideEffectRecord(Member member, LocalDateTime createdAt) {
        return SideEffectRecord.builder()
                .member(member)
                .createdAt(createdAt)
                .build();
    }

    private void createAndPersistJunctionRecord(Long sideEffectRecordId, Long sideEffectId, Integer degree) {
        SideEffectSideEffectRecord junctionRecord = SideEffectSideEffectRecord.builder()
                .sideEffectRecordId(sideEffectRecordId)
                .sideEffectId(sideEffectId)
                .degree(degree)
                .build();
        entityManager.persistAndFlush(junctionRecord);
    }
}