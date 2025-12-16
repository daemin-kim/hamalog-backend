package com.Hamalog.security.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResourceOwnershipValidator Tests")
class ResourceOwnershipValidatorTest {

    @Mock
    private SecurityContext securityContext;
    
    @Mock
    private Authentication authentication;

    @InjectMocks
    private ResourceOwnershipValidator validator;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("동일한 사용자 ID일 때 리소스 소유권 검증이 성공해야 함")
    void isResourceOwner_SameUserId_ShouldReturnTrue() {
        // given
        String currentUserId = "user123";
        String resourceUserId = "user123";
        
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getPrincipal()).willReturn(currentUserId);

        // when
        boolean result = validator.isResourceOwner(resourceUserId);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("다른 사용자 ID일 때 리소스 소유권 검증이 실패해야 함")
    void isResourceOwner_DifferentUserId_ShouldReturnFalse() {
        // given
        String currentUserId = "user123";
        String resourceUserId = "user456";
        
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getPrincipal()).willReturn(currentUserId);

        // when
        boolean result = validator.isResourceOwner(resourceUserId);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("현재 사용자 ID가 null일 때 리소스 소유권 검증이 실패해야 함")
    void isResourceOwner_NullCurrentUserId_ShouldReturnFalse() {
        // given
        String resourceUserId = "user456";
        
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getPrincipal()).willReturn(null);

        // when
        boolean result = validator.isResourceOwner(resourceUserId);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("리소스 사용자 ID가 null일 때 리소스 소유권 검증이 실패해야 함")
    void isResourceOwner_NullResourceUserId_ShouldReturnFalse() {
        // given
        String currentUserId = "user123";
        String resourceUserId = null;
        
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getPrincipal()).willReturn(currentUserId);

        // when
        boolean result = validator.isResourceOwner(resourceUserId);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("빈 문자열 사용자 ID일 때 리소스 소유권 검증이 실패해야 함")
    void isResourceOwner_EmptyUserId_ShouldReturnFalse() {
        // given
        String currentUserId = "";
        String resourceUserId = "user456";
        
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getPrincipal()).willReturn(currentUserId);

        // when
        boolean result = validator.isResourceOwner(resourceUserId);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("인증 정보가 null일 때 리소스 소유권 검증이 실패해야 함")
    void isResourceOwner_NullAuthentication_ShouldReturnFalse() {
        // given
        String resourceUserId = "user456";
        
        given(securityContext.getAuthentication()).willReturn(null);

        // when
        boolean result = validator.isResourceOwner(resourceUserId);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("예외 발생 시 리소스 소유권 검증이 실패해야 함")
    void isResourceOwner_ExceptionOccurs_ShouldReturnFalse() {
        // given
        String resourceUserId = "user456";
        
        given(securityContext.getAuthentication()).willThrow(new RuntimeException("Security error"));

        // when
        boolean result = validator.isResourceOwner(resourceUserId);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("여러 리소스 중 하나가 소유자와 일치할 때 검증이 성공해야 함")
    void isAnyResourceOwner_OneMatchingUserId_ShouldReturnTrue() {
        // given
        String currentUserId = "user123";
        String[] resourceUserIds = {"user456", "user123", "user789"};
        
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getPrincipal()).willReturn(currentUserId);

        // when
        boolean result = validator.isAnyResourceOwner(resourceUserIds);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("여러 리소스 중 소유자와 일치하는 것이 없을 때 검증이 실패해야 함")
    void isAnyResourceOwner_NoMatchingUserId_ShouldReturnFalse() {
        // given
        String currentUserId = "user123";
        String[] resourceUserIds = {"user456", "user789", "user111"};
        
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getPrincipal()).willReturn(currentUserId);

        // when
        boolean result = validator.isAnyResourceOwner(resourceUserIds);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("리소스 사용자 ID 배열이 null일 때 검증이 실패해야 함")
    void isAnyResourceOwner_NullArray_ShouldReturnFalse() {
        // given
        String[] resourceUserIds = null;
        
        // No mocking needed since method returns early for null array

        // when
        boolean result = validator.isAnyResourceOwner(resourceUserIds);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("빈 리소스 사용자 ID 배열일 때 검증이 실패해야 함")
    void isAnyResourceOwner_EmptyArray_ShouldReturnFalse() {
        // given
        String[] resourceUserIds = {};
        
        // No mocking needed since method returns early for empty array

        // when
        boolean result = validator.isAnyResourceOwner(resourceUserIds);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("프로필 수정 권한 검증이 올바르게 동작해야 함")
    void canModifyProfile_SameUser_ShouldReturnTrue() {
        // given
        String currentUserId = "user123";
        String targetUserId = "user123";
        
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getPrincipal()).willReturn(currentUserId);

        // when
        boolean result = validator.canModifyProfile(targetUserId);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("다른 사용자의 프로필 수정 시 권한 검증이 실패해야 함")
    void canModifyProfile_DifferentUser_ShouldReturnFalse() {
        // given
        String currentUserId = "user123";
        String targetUserId = "user456";
        
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getPrincipal()).willReturn(currentUserId);

        // when
        boolean result = validator.canModifyProfile(targetUserId);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("약물 스케줄 접근 권한 검증이 올바르게 동작해야 함")
    void canAccessMedicationSchedule_Owner_ShouldReturnTrue() {
        // given
        String currentUserId = "user123";
        String scheduleUserId = "user123";
        
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getPrincipal()).willReturn(currentUserId);

        // when
        boolean result = validator.canAccessMedicationSchedule(scheduleUserId);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("다른 사용자의 약물 스케줄 접근 시 권한 검증이 실패해야 함")
    void canAccessMedicationSchedule_NonOwner_ShouldReturnFalse() {
        // given
        String currentUserId = "user123";
        String scheduleUserId = "user456";
        
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getPrincipal()).willReturn(currentUserId);

        // when
        boolean result = validator.canAccessMedicationSchedule(scheduleUserId);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("약물 기록 접근 권한 검증이 올바르게 동작해야 함")
    void canAccessMedicationRecord_Owner_ShouldReturnTrue() {
        // given
        String currentUserId = "user123";
        String recordUserId = "user123";
        
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getPrincipal()).willReturn(currentUserId);

        // when
        boolean result = validator.canAccessMedicationRecord(recordUserId);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("부작용 기록 접근 권한 검증이 올바르게 동작해야 함")
    void canAccessSideEffectRecord_Owner_ShouldReturnTrue() {
        // given
        String currentUserId = "user123";
        String recordUserId = "user123";
        
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getPrincipal()).willReturn(currentUserId);

        // when
        boolean result = validator.canAccessSideEffectRecord(recordUserId);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("관리자 권한 검증이 올바르게 동작해야 함")
    void isAdmin_WithAdminRole_ShouldReturnTrue() {
        // given
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_USER"),
            new SimpleGrantedAuthority("ROLE_ADMIN")
        );
        
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getAuthorities()).willReturn((Collection) authorities);

        // when
        boolean result = validator.isAdmin();

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("일반 사용자의 관리자 권한 검증이 실패해야 함")
    void isAdmin_WithoutAdminRole_ShouldReturnFalse() {
        // given
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_USER")
        );
        
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getAuthorities()).willReturn((Collection) authorities);

        // when
        boolean result = validator.isAdmin();

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("인증되지 않은 사용자의 관리자 권한 검증이 실패해야 함")
    void isAdmin_NotAuthenticated_ShouldReturnFalse() {
        // given
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(false);

        // when
        boolean result = validator.isAdmin();

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("인증 상태 검증이 올바르게 동작해야 함")
    void isAuthenticated_AuthenticatedUser_ShouldReturnTrue() {
        // given
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getPrincipal()).willReturn("user123");

        // when
        boolean result = validator.isAuthenticated();

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("인증되지 않은 사용자의 인증 상태 검증이 실패해야 함")
    void isAuthenticated_NotAuthenticatedUser_ShouldReturnFalse() {
        // given
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(false);

        // when
        boolean result = validator.isAuthenticated();

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("인증 정보가 null일 때 인증 상태 검증이 실패해야 함")
    void isAuthenticated_NullAuthentication_ShouldReturnFalse() {
        // given
        given(securityContext.getAuthentication()).willReturn(null);

        // when
        boolean result = validator.isAuthenticated();

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("접근 허용 검증이 올바르게 동작해야 함")
    void isAccessAllowed_AuthenticatedUser_ShouldReturnTrue() {
        // given
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getPrincipal()).willReturn("user123");

        // when
        boolean result = validator.isAccessAllowed();

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("현재 사용자 ID를 올바르게 반환해야 함")
    void getCurrentUserId_AuthenticatedUser_ShouldReturnUserId() {
        // given
        String expectedUserId = "user123";
        
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getPrincipal()).willReturn(expectedUserId);

        // when
        String result = validator.getCurrentUserId();

        // then
        assertThat(result).isEqualTo(expectedUserId);
    }

    @Test
    @DisplayName("인증되지 않은 사용자의 현재 사용자 ID가 null을 반환해야 함")
    void getCurrentUserId_NotAuthenticatedUser_ShouldReturnNull() {
        // given
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(false);

        // when
        String result = validator.getCurrentUserId();

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getCurrentUserId에서 예외 발생 시 null을 반환해야 함")
    void getCurrentUserId_ExceptionOccurs_ShouldReturnNull() {
        // given
        given(securityContext.getAuthentication()).willThrow(new RuntimeException("Security error"));

        // when
        String result = validator.getCurrentUserId();

        // then
        assertThat(result).isNull();
    }
}