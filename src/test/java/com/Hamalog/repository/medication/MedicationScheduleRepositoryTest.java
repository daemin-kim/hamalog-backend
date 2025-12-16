package com.Hamalog.repository.medication;

import static org.assertj.core.api.Assertions.assertThat;

import com.Hamalog.config.TestEncryptionConfig;
import com.Hamalog.domain.medication.AlarmType;
import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.domain.member.Member;
import com.Hamalog.repository.member.MemberRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(TestEncryptionConfig.class)
@DisplayName("MedicationScheduleRepository Tests")
class MedicationScheduleRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MedicationScheduleRepository medicationScheduleRepository;

    @Autowired
    private MemberRepository memberRepository;

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
    @DisplayName("Should find all medication schedules by member ID")
    void findAllByMember_MemberId_ExistingSchedules_ReturnsSchedules() {
        // given
        MedicationSchedule schedule1 = createTestMedicationSchedule(testMember, "Medicine A");
        MedicationSchedule schedule2 = createTestMedicationSchedule(testMember, "Medicine B");
        MedicationSchedule schedule3 = createTestMedicationSchedule(anotherMember, "Medicine C");
        
        entityManager.persistAndFlush(schedule1);
        entityManager.persistAndFlush(schedule2);
        entityManager.persistAndFlush(schedule3);

        // when
        List<MedicationSchedule> schedules = medicationScheduleRepository.findAllByMember_MemberId(testMember.getMemberId());

        // then
        assertThat(schedules).hasSize(2);
        assertThat(schedules).extracting("name").containsExactlyInAnyOrder("Medicine A", "Medicine B");
        assertThat(schedules).allMatch(schedule -> schedule.getMember().getMemberId().equals(testMember.getMemberId()));
    }

    @Test
    @DisplayName("Should return empty list when no schedules exist for member")
    void findAllByMember_MemberId_NoSchedules_ReturnsEmptyList() {
        // when
        List<MedicationSchedule> schedules = medicationScheduleRepository.findAllByMember_MemberId(testMember.getMemberId());

        // then
        assertThat(schedules).isEmpty();
    }

    @Test
    @DisplayName("Should find medication schedules by member ID with pagination")
    void findByMember_MemberId_WithPagination_ReturnsPagedSchedules() {
        // given
        for (int i = 1; i <= 5; i++) {
            MedicationSchedule schedule = createTestMedicationSchedule(testMember, "Medicine " + i);
            entityManager.persistAndFlush(schedule);
        }

        Pageable pageable = PageRequest.of(0, 3);

        // when
        Page<MedicationSchedule> schedulePage = medicationScheduleRepository.findByMember_MemberId(testMember.getMemberId(), pageable);

        // then
        assertThat(schedulePage.getContent()).hasSize(3);
        assertThat(schedulePage.getTotalElements()).isEqualTo(5);
        assertThat(schedulePage.getTotalPages()).isEqualTo(2);
        assertThat(schedulePage.isFirst()).isTrue();
        assertThat(schedulePage.hasNext()).isTrue();
    }

    @Test
    @DisplayName("Should find medication schedule by ID with member entity graph")
    void findById_ExistingSchedule_ReturnsScheduleWithMember() {
        // given
        MedicationSchedule schedule = createTestMedicationSchedule(testMember, "Medicine A");
        MedicationSchedule savedSchedule = entityManager.persistAndFlush(schedule);

        // when
        Optional<MedicationSchedule> foundSchedule = medicationScheduleRepository.findById(savedSchedule.getMedicationScheduleId());

        // then
        assertThat(foundSchedule).isPresent();
        assertThat(foundSchedule.get().getName()).isEqualTo("Medicine A");
        assertThat(foundSchedule.get().getMember()).isNotNull();
        assertThat(foundSchedule.get().getMember().getLoginId()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should find all schedules by member ID with JOIN FETCH")
    void findAllByMemberIdWithMember_ExistingSchedules_ReturnsSchedulesWithMember() {
        // given
        MedicationSchedule schedule1 = createTestMedicationSchedule(testMember, "Medicine A");
        MedicationSchedule schedule2 = createTestMedicationSchedule(testMember, "Medicine B");
        
        entityManager.persistAndFlush(schedule1);
        entityManager.persistAndFlush(schedule2);

        // when
        List<MedicationSchedule> schedules = medicationScheduleRepository.findAllByMemberIdWithMember(testMember.getMemberId());

        // then
        assertThat(schedules).hasSize(2);
        assertThat(schedules).extracting("name").containsExactlyInAnyOrder("Medicine A", "Medicine B");
        assertThat(schedules).allMatch(schedule -> schedule.getMember() != null);
        assertThat(schedules).allMatch(schedule -> schedule.getMember().getLoginId().equals("test@example.com"));
    }

    @Test
    @DisplayName("Should delete all schedules by member ID")
    void deleteByMemberId_ExistingSchedules_DeletesAllSchedules() {
        // given
        MedicationSchedule schedule1 = createTestMedicationSchedule(testMember, "Medicine A");
        MedicationSchedule schedule2 = createTestMedicationSchedule(testMember, "Medicine B");
        MedicationSchedule schedule3 = createTestMedicationSchedule(anotherMember, "Medicine C");
        
        entityManager.persistAndFlush(schedule1);
        entityManager.persistAndFlush(schedule2);
        entityManager.persistAndFlush(schedule3);

        // when
        medicationScheduleRepository.deleteByMemberId(testMember.getMemberId());
        entityManager.flush();

        // then
        List<MedicationSchedule> remainingSchedules = medicationScheduleRepository.findAll();
        assertThat(remainingSchedules).hasSize(1);
        assertThat(remainingSchedules.get(0).getName()).isEqualTo("Medicine C");
        assertThat(remainingSchedules.get(0).getMember().getMemberId()).isEqualTo(anotherMember.getMemberId());
    }

    @Test
    @DisplayName("Should save medication schedule successfully")
    void save_ValidMedicationSchedule_SavesSuccessfully() {
        // given
        MedicationSchedule schedule = createTestMedicationSchedule(testMember, "Test Medicine");

        // when
        MedicationSchedule savedSchedule = medicationScheduleRepository.save(schedule);

        // then
        assertThat(savedSchedule).isNotNull();
        assertThat(savedSchedule.getMedicationScheduleId()).isNotNull();
        assertThat(savedSchedule.getName()).isEqualTo("Test Medicine");
        assertThat(savedSchedule.getHospitalName()).isEqualTo("Test Hospital");
        assertThat(savedSchedule.getPrescriptionDays()).isEqualTo(30);
        assertThat(savedSchedule.getPerDay()).isEqualTo(2);
        assertThat(savedSchedule.getAlarmType()).isEqualTo(AlarmType.SOUND);
        assertThat(savedSchedule.getVersion()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Should update medication schedule successfully")
    void save_UpdatedMedicationSchedule_UpdatesSuccessfully() {
        // given
        MedicationSchedule schedule = createTestMedicationSchedule(testMember, "Original Medicine");
        MedicationSchedule savedSchedule = entityManager.persistAndFlush(schedule);

        // when
        savedSchedule.update(
                "Updated Medicine",
                "Updated Hospital",
                LocalDate.now().minusDays(1),
                "Updated memo",
                LocalDate.now(),
                60,
                3,
                AlarmType.VIBE
        );
        MedicationSchedule updatedSchedule = medicationScheduleRepository.save(savedSchedule);

        // then
        assertThat(updatedSchedule.getName()).isEqualTo("Updated Medicine");
        assertThat(updatedSchedule.getHospitalName()).isEqualTo("Updated Hospital");
        assertThat(updatedSchedule.getPrescriptionDays()).isEqualTo(60);
        assertThat(updatedSchedule.getPerDay()).isEqualTo(3);
        assertThat(updatedSchedule.getAlarmType()).isEqualTo(AlarmType.VIBE);
        assertThat(updatedSchedule.getMemo()).isEqualTo("Updated memo");
    }

    @Test
    @DisplayName("Should handle version control correctly")
    void save_ConcurrentUpdate_HandlesVersionControl() {
        // given
        MedicationSchedule schedule = createTestMedicationSchedule(testMember, "Version Test");
        MedicationSchedule savedSchedule = entityManager.persistAndFlush(schedule);
        Long initialVersion = savedSchedule.getVersion();

        // when
        savedSchedule.update(
                "Updated Medicine",
                savedSchedule.getHospitalName(),
                savedSchedule.getPrescriptionDate(),
                savedSchedule.getMemo(),
                savedSchedule.getStartOfAd(),
                savedSchedule.getPrescriptionDays(),
                savedSchedule.getPerDay(),
                savedSchedule.getAlarmType()
        );
        MedicationSchedule updatedSchedule = medicationScheduleRepository.save(savedSchedule);
        entityManager.flush(); // Ensure version increment is triggered

        // then
        assertThat(updatedSchedule.getVersion()).isGreaterThan(initialVersion);
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
}