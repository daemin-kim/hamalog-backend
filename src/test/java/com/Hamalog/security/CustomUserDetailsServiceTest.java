package com.Hamalog.security;

import com.Hamalog.domain.member.Member;
import com.Hamalog.repository.member.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("Custom User Details Service Tests")
class CustomUserDetailsServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = createTestMember("testuser");
    }

    @Test
    @DisplayName("Should load user by username successfully when user exists")
    void loadUserByUsername_UserExists_ShouldReturnUserDetails() {
        // given
        String username = "testuser";
        when(memberRepository.findByLoginId(username)).thenReturn(Optional.of(testMember));

        // when
        UserDetails result = customUserDetailsService.loadUserByUsername(username);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(CustomUserDetails.class);
        assertThat(result.getUsername()).isEqualTo(username);
        
        verify(memberRepository).findByLoginId(username);
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when user does not exist")
    void loadUserByUsername_UserNotExists_ShouldThrowException() {
        // given
        String username = "nonexistent";
        when(memberRepository.findByLoginId(username)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(username))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("존재하지 않는 회원입니다.");
        
        verify(memberRepository).findByLoginId(username);
    }

    @Test
    @DisplayName("Should handle null username gracefully")
    void loadUserByUsername_NullUsername_ShouldCallRepository() {
        // given
        when(memberRepository.findByLoginId(null)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(null))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("존재하지 않는 회원입니다.");
        
        verify(memberRepository).findByLoginId(null);
    }

    @Test
    @DisplayName("Should handle empty username gracefully")
    void loadUserByUsername_EmptyUsername_ShouldCallRepository() {
        // given
        String emptyUsername = "";
        when(memberRepository.findByLoginId(emptyUsername)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(emptyUsername))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("존재하지 않는 회원입니다.");
        
        verify(memberRepository).findByLoginId(emptyUsername);
    }

    @Test
    @DisplayName("Should handle whitespace-only username gracefully")
    void loadUserByUsername_WhitespaceUsername_ShouldCallRepository() {
        // given
        String whitespaceUsername = "   ";
        when(memberRepository.findByLoginId(whitespaceUsername)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(whitespaceUsername))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("존재하지 않는 회원입니다.");
        
        verify(memberRepository).findByLoginId(whitespaceUsername);
    }

    @Test
    @DisplayName("Should handle repository exceptions gracefully")
    void loadUserByUsername_RepositoryException_ShouldPropagateException() {
        // given
        String username = "testuser";
        RuntimeException repositoryException = new RuntimeException("Database connection failed");
        when(memberRepository.findByLoginId(username)).thenThrow(repositoryException);

        // when & then
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(username))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database connection failed");
        
        verify(memberRepository).findByLoginId(username);
    }

    @Test
    @DisplayName("Should create CustomUserDetails with correct member data")
    void loadUserByUsername_ValidUser_ShouldCreateCorrectUserDetails() {
        // given
        String username = "detaileduser";
        Member memberWithDetails = createTestMember(username);
        when(memberRepository.findByLoginId(username)).thenReturn(Optional.of(memberWithDetails));

        // when
        UserDetails result = customUserDetailsService.loadUserByUsername(username);

        // then
        assertThat(result).isInstanceOf(CustomUserDetails.class);
        CustomUserDetails customDetails = (CustomUserDetails) result;
        assertThat(customDetails.getUsername()).isEqualTo(username);
        assertThat(customDetails.isEnabled()).isTrue();
        assertThat(customDetails.isAccountNonExpired()).isTrue();
        assertThat(customDetails.isAccountNonLocked()).isTrue();
        assertThat(customDetails.isCredentialsNonExpired()).isTrue();
        
        verify(memberRepository).findByLoginId(username);
    }

    @Test
    @DisplayName("Should handle case-sensitive username lookup")
    void loadUserByUsername_CaseSensitiveUsername_ShouldRespectCase() {
        // given
        String lowerCaseUsername = "testuser";
        String upperCaseUsername = "TESTUSER";
        
        when(memberRepository.findByLoginId(lowerCaseUsername)).thenReturn(Optional.of(testMember));
        when(memberRepository.findByLoginId(upperCaseUsername)).thenReturn(Optional.empty());

        // when
        UserDetails lowerCaseResult = customUserDetailsService.loadUserByUsername(lowerCaseUsername);
        
        // then
        assertThat(lowerCaseResult).isNotNull();
        
        // when & then for uppercase
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(upperCaseUsername))
                .isInstanceOf(UsernameNotFoundException.class);
        
        verify(memberRepository).findByLoginId(lowerCaseUsername);
        verify(memberRepository).findByLoginId(upperCaseUsername);
    }

    @Test
    @DisplayName("Should not cache results between calls")
    void loadUserByUsername_MultipleCalls_ShouldCallRepositoryEachTime() {
        // given
        String username = "testuser";
        when(memberRepository.findByLoginId(username)).thenReturn(Optional.of(testMember));

        // when
        customUserDetailsService.loadUserByUsername(username);
        customUserDetailsService.loadUserByUsername(username);
        customUserDetailsService.loadUserByUsername(username);

        // then
        verify(memberRepository, times(3)).findByLoginId(username);
    }

    /**
     * Helper method to create test Member objects
     */
    private Member createTestMember(String loginId) {
        Member member = mock(Member.class);
        lenient().when(member.getLoginId()).thenReturn(loginId);
        return member;
    }
}