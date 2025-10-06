package com.Hamalog.security;

import com.Hamalog.domain.member.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CustomUserDetails Tests")
class CustomUserDetailsTest {

    private Member testMember;
    private CustomUserDetails customUserDetails;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .memberId(1L)
                .loginId("test@example.com")
                .password("hashedPassword123")
                .name("Test User")
                .phoneNumber("01012345678")
                .nickName("testuser")
                .birth(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.now())
                .version(0L)
                .build();
                
        customUserDetails = new CustomUserDetails(testMember);
    }

    @Test
    @DisplayName("Should create CustomUserDetails with valid member")
    void constructor_ValidMember_CreatesSuccessfully() {
        // when
        CustomUserDetails userDetails = new CustomUserDetails(testMember);

        // then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getMember()).isEqualTo(testMember);
    }

    @Test
    @DisplayName("Should allow null member in constructor")
    void constructor_NullMember_AllowsCreation() {
        // when/then - Should not throw exception but will cause NPE in methods
        CustomUserDetails userDetails = new CustomUserDetails(null);
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getMember()).isNull();
    }

    @Test
    @DisplayName("Should return empty authorities collection")
    void getAuthorities_Always_ReturnsEmptyCollection() {
        // when
        Collection<? extends GrantedAuthority> authorities = customUserDetails.getAuthorities();

        // then
        assertThat(authorities).isNotNull();
        assertThat(authorities).isEmpty();
    }

    @Test
    @DisplayName("Should return member password")
    void getPassword_ValidMember_ReturnsMemberPassword() {
        // when
        String password = customUserDetails.getPassword();

        // then
        assertThat(password).isEqualTo("hashedPassword123");
        assertThat(password).isEqualTo(testMember.getPassword());
    }

    @Test
    @DisplayName("Should throw NPE when getting password from null member")
    void getPassword_NullMember_ThrowsNullPointerException() {
        // given
        CustomUserDetails userDetailsWithNullMember = new CustomUserDetails(null);

        // when/then
        assertThatThrownBy(() -> userDetailsWithNullMember.getPassword())
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should return member login ID as username")
    void getUsername_ValidMember_ReturnsMemberLoginId() {
        // when
        String username = customUserDetails.getUsername();

        // then
        assertThat(username).isEqualTo("test@example.com");
        assertThat(username).isEqualTo(testMember.getLoginId());
    }

    @Test
    @DisplayName("Should throw NPE when getting username from null member")
    void getUsername_NullMember_ThrowsNullPointerException() {
        // given
        CustomUserDetails userDetailsWithNullMember = new CustomUserDetails(null);

        // when/then
        assertThatThrownBy(() -> userDetailsWithNullMember.getUsername())
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should return true for account non-expired")
    void isAccountNonExpired_Always_ReturnsTrue() {
        // when
        boolean isNonExpired = customUserDetails.isAccountNonExpired();

        // then
        assertThat(isNonExpired).isTrue();
    }

    @Test
    @DisplayName("Should return true for account non-locked")
    void isAccountNonLocked_Always_ReturnsTrue() {
        // when
        boolean isNonLocked = customUserDetails.isAccountNonLocked();

        // then
        assertThat(isNonLocked).isTrue();
    }

    @Test
    @DisplayName("Should return true for credentials non-expired")
    void isCredentialsNonExpired_Always_ReturnsTrue() {
        // when
        boolean isCredentialsNonExpired = customUserDetails.isCredentialsNonExpired();

        // then
        assertThat(isCredentialsNonExpired).isTrue();
    }

    @Test
    @DisplayName("Should return true for enabled")
    void isEnabled_Always_ReturnsTrue() {
        // when
        boolean isEnabled = customUserDetails.isEnabled();

        // then
        assertThat(isEnabled).isTrue();
    }

    @Test
    @DisplayName("Should provide access to underlying member")
    void getMember_Always_ReturnsWrappedMember() {
        // when
        Member retrievedMember = customUserDetails.getMember();

        // then
        assertThat(retrievedMember).isEqualTo(testMember);
        assertThat(retrievedMember).isSameAs(testMember);
    }

    @Test
    @DisplayName("Should handle member with different field values")
    void customUserDetails_DifferentMemberFields_HandlesCorrectly() {
        // given
        Member differentMember = Member.builder()
                .memberId(999L)
                .loginId("different@example.com")
                .password("differentPassword")
                .name("Different User")
                .phoneNumber("01087654321")
                .nickName("different")
                .birth(LocalDate.of(1985, 12, 31))
                .createdAt(LocalDateTime.now().minusDays(10))
                .version(5L)
                .build();

        // when
        CustomUserDetails differentUserDetails = new CustomUserDetails(differentMember);

        // then
        assertThat(differentUserDetails.getUsername()).isEqualTo("different@example.com");
        assertThat(differentUserDetails.getPassword()).isEqualTo("differentPassword");
        assertThat(differentUserDetails.getMember()).isEqualTo(differentMember);
        assertThat(differentUserDetails.getAuthorities()).isEmpty();
        assertThat(differentUserDetails.isAccountNonExpired()).isTrue();
        assertThat(differentUserDetails.isAccountNonLocked()).isTrue();
        assertThat(differentUserDetails.isCredentialsNonExpired()).isTrue();
        assertThat(differentUserDetails.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should handle member with null password")
    void customUserDetails_MemberWithNullPassword_HandlesCorrectly() {
        // given
        Member memberWithNullPassword = Member.builder()
                .memberId(1L)
                .loginId("test@example.com")
                .password(null)  // null password
                .name("Test User")
                .phoneNumber("01012345678")
                .nickName("testuser")
                .birth(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.now())
                .version(0L)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(memberWithNullPassword);

        // when/then
        assertThat(userDetails.getPassword()).isNull();
        assertThat(userDetails.getUsername()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should handle member with null login ID")
    void customUserDetails_MemberWithNullLoginId_HandlesCorrectly() {
        // given
        Member memberWithNullLoginId = Member.builder()
                .memberId(1L)
                .loginId(null)  // null login ID
                .password("hashedPassword123")
                .name("Test User")
                .phoneNumber("01012345678")
                .nickName("testuser")
                .birth(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.now())
                .version(0L)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(memberWithNullLoginId);

        // when/then
        assertThat(userDetails.getUsername()).isNull();
        assertThat(userDetails.getPassword()).isEqualTo("hashedPassword123");
    }

    @Test
    @DisplayName("Should maintain consistent behavior across multiple calls")
    void customUserDetails_MultipleCalls_ConsistentBehavior() {
        // when - Call methods multiple times
        String username1 = customUserDetails.getUsername();
        String username2 = customUserDetails.getUsername();
        String password1 = customUserDetails.getPassword();
        String password2 = customUserDetails.getPassword();
        
        Collection<? extends GrantedAuthority> authorities1 = customUserDetails.getAuthorities();
        Collection<? extends GrantedAuthority> authorities2 = customUserDetails.getAuthorities();

        // then - Should return consistent values
        assertThat(username1).isEqualTo(username2);
        assertThat(password1).isEqualTo(password2);
        assertThat(authorities1).isEqualTo(authorities2);
        assertThat(customUserDetails.isEnabled()).isTrue();
        assertThat(customUserDetails.isEnabled()).isTrue(); // Multiple calls
    }
}