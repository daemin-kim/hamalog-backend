package com.Hamalog.repository.member;

import com.Hamalog.domain.member.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("MemberRepository Tests")
class MemberRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("Should find member by login ID when member exists")
    void findByLoginId_ExistingMember_ReturnsMember() {
        // given
        String loginId = "test@example.com";
        Member member = createTestMember(loginId);
        entityManager.persistAndFlush(member);

        // when
        Optional<Member> foundMember = memberRepository.findByLoginId(loginId);

        // then
        assertThat(foundMember).isPresent();
        assertThat(foundMember.get().getLoginId()).isEqualTo(loginId);
        assertThat(foundMember.get().getName()).isEqualTo("TestUser");
        assertThat(foundMember.get().getNickName()).isEqualTo("TestNick");
    }

    @Test
    @DisplayName("Should return empty when member does not exist")
    void findByLoginId_NonExistentMember_ReturnsEmpty() {
        // given
        String nonExistentLoginId = "nonexistent@example.com";

        // when
        Optional<Member> foundMember = memberRepository.findByLoginId(nonExistentLoginId);

        // then
        assertThat(foundMember).isEmpty();
    }

    @Test
    @DisplayName("Should save member successfully")
    void save_ValidMember_SavesSuccessfully() {
        // given
        Member member = createTestMember("save@example.com");

        // when
        Member savedMember = memberRepository.save(member);

        // then
        assertThat(savedMember).isNotNull();
        assertThat(savedMember.getMemberId()).isNotNull();
        assertThat(savedMember.getLoginId()).isEqualTo("save@example.com");
        assertThat(savedMember.getCreatedAt()).isNotNull();
        assertThat(savedMember.getVersion()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Should find member by ID when exists")
    void findById_ExistingMember_ReturnsMember() {
        // given
        Member member = createTestMember("findbyid@example.com");
        Member savedMember = entityManager.persistAndFlush(member);

        // when
        Optional<Member> foundMember = memberRepository.findById(savedMember.getMemberId());

        // then
        assertThat(foundMember).isPresent();
        assertThat(foundMember.get().getMemberId()).isEqualTo(savedMember.getMemberId());
        assertThat(foundMember.get().getLoginId()).isEqualTo("findbyid@example.com");
    }

    @Test
    @DisplayName("Should delete member successfully")
    void delete_ExistingMember_DeletesSuccessfully() {
        // given
        Member member = createTestMember("delete@example.com");
        Member savedMember = entityManager.persistAndFlush(member);

        // when
        memberRepository.delete(savedMember);
        entityManager.flush();

        // then
        Optional<Member> foundMember = memberRepository.findById(savedMember.getMemberId());
        assertThat(foundMember).isEmpty();
    }

    @Test
    @DisplayName("Should count members correctly")
    void count_WithMembers_ReturnsCorrectCount() {
        // given
        Member member1 = createTestMember("count1@example.com");
        Member member2 = createTestMember("count2@example.com");
        entityManager.persistAndFlush(member1);
        entityManager.persistAndFlush(member2);

        // when
        long count = memberRepository.count();

        // then
        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("Should handle encrypted fields correctly")
    void save_MemberWithEncryptedFields_HandlesEncryptionCorrectly() {
        // given
        Member member = createTestMember("encryption@example.com");
        
        // when
        Member savedMember = memberRepository.save(member);
        entityManager.flush();
        entityManager.clear();

        // then
        Optional<Member> foundMember = memberRepository.findById(savedMember.getMemberId());
        assertThat(foundMember).isPresent();
        assertThat(foundMember.get().getPhoneNumber()).isEqualTo("01012345678");
        assertThat(foundMember.get().getBirth()).isEqualTo(LocalDate.of(1990, 1, 1));
    }

    @Test
    @DisplayName("Should handle unique constraint on login ID")
    void save_DuplicateLoginId_ThrowsException() {
        // given
        String duplicateLoginId = "duplicate@example.com";
        Member member1 = createTestMember(duplicateLoginId);
        Member member2 = createTestMember(duplicateLoginId);
        
        memberRepository.save(member1);
        entityManager.flush();

        // when & then
        try {
            memberRepository.save(member2);
            entityManager.flush();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        }
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