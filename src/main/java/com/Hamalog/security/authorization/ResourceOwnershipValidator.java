package com.Hamalog.security.authorization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * 리소스 소유권 검증 유틸리티
 * Method-level 보안을 위한 @PreAuthorize에서 사용
 */
@Component("resourceOwnershipValidator")
public class ResourceOwnershipValidator {

    private static final Logger log = LoggerFactory.getLogger(ResourceOwnershipValidator.class);

    /**
     * 현재 인증된 사용자가 리소스의 소유자인지 확인
     * @PreAuthorize("@resourceOwnershipValidator.isResourceOwner(#userId)")에서 사용
     */
    public boolean isResourceOwner(String resourceUserId) {
        try {
            String currentUserId = getCurrentUserId();
            
            if (!StringUtils.hasText(currentUserId) || !StringUtils.hasText(resourceUserId)) {
                log.warn("소유권 검증 실패: 사용자 ID가 비어있음. Current: {}, Resource: {}", currentUserId, resourceUserId);
                return false;
            }

            boolean isOwner = Objects.equals(currentUserId, resourceUserId);
            
            if (!isOwner) {
                log.warn("리소스 소유권 검증 실패. 요청자: {}, 리소스 소유자: {}", currentUserId, resourceUserId);
            } else {
                log.debug("리소스 소유권 검증 성공. 사용자: {}", currentUserId);
            }
            
            return isOwner;
            
        } catch (Exception e) {
            log.error("리소스 소유권 검증 중 오류 발생", e);
            return false;
        }
    }

    /**
     * 현재 인증된 사용자가 여러 리소스 중 하나의 소유자인지 확인
     * @PreAuthorize("@resourceOwnershipValidator.isAnyResourceOwner(#userIds)")에서 사용
     */
    public boolean isAnyResourceOwner(String... resourceUserIds) {
        if (resourceUserIds == null || resourceUserIds.length == 0) {
            return false;
        }

        String currentUserId = getCurrentUserId();
        if (!StringUtils.hasText(currentUserId)) {
            return false;
        }

        for (String resourceUserId : resourceUserIds) {
            if (Objects.equals(currentUserId, resourceUserId)) {
                log.debug("다중 리소스 소유권 검증 성공. 사용자: {}, 소유 리소스: {}", currentUserId, resourceUserId);
                return true;
            }
        }

        log.warn("다중 리소스 소유권 검증 실패. 요청자: {}", currentUserId);
        return false;
    }

    /**
     * 현재 사용자가 자신의 프로필을 수정하려는지 확인
     * @PreAuthorize("@resourceOwnershipValidator.canModifyProfile(#targetUserId)")에서 사용
     */
    public boolean canModifyProfile(String targetUserId) {
        boolean canModify = isResourceOwner(targetUserId);
        
        if (canModify) {
            log.info("프로필 수정 권한 검증 성공. 사용자: {}", getCurrentUserId());
        } else {
            log.warn("프로필 수정 권한 검증 실패. 요청자: {}, 대상: {}", getCurrentUserId(), targetUserId);
        }
        
        return canModify;
    }

    /**
     * 현재 사용자가 특정 약물 스케줄의 소유자인지 확인
     * @PreAuthorize("@resourceOwnershipValidator.canAccessMedicationSchedule(#scheduleUserId)")에서 사용
     */
    public boolean canAccessMedicationSchedule(String scheduleUserId) {
        boolean canAccess = isResourceOwner(scheduleUserId);
        
        if (canAccess) {
            log.debug("약물 스케줄 접근 권한 검증 성공. 사용자: {}", getCurrentUserId());
        } else {
            log.warn("약물 스케줄 접근 권한 검증 실패. 요청자: {}, 스케줄 소유자: {}", getCurrentUserId(), scheduleUserId);
        }
        
        return canAccess;
    }

    /**
     * 현재 사용자가 특정 약물 기록의 소유자인지 확인
     * @PreAuthorize("@resourceOwnershipValidator.canAccessMedicationRecord(#recordUserId)")에서 사용
     */
    public boolean canAccessMedicationRecord(String recordUserId) {
        boolean canAccess = isResourceOwner(recordUserId);
        
        if (canAccess) {
            log.debug("약물 기록 접근 권한 검증 성공. 사용자: {}", getCurrentUserId());
        } else {
            log.warn("약물 기록 접근 권한 검증 실패. 요청자: {}, 기록 소유자: {}", getCurrentUserId(), recordUserId);
        }
        
        return canAccess;
    }

    /**
     * 현재 사용자가 특정 부작용 기록의 소유자인지 확인
     * @PreAuthorize("@resourceOwnershipValidator.canAccessSideEffectRecord(#recordUserId)")에서 사용
     */
    public boolean canAccessSideEffectRecord(String recordUserId) {
        boolean canAccess = isResourceOwner(recordUserId);
        
        if (canAccess) {
            log.debug("부작용 기록 접근 권한 검증 성공. 사용자: {}", getCurrentUserId());
        } else {
            log.warn("부작용 기록 접근 권한 검증 실패. 요청자: {}, 기록 소유자: {}", getCurrentUserId(), recordUserId);
        }
        
        return canAccess;
    }

    /**
     * 관리자 권한 확인 (향후 역할 기반 권한이 추가될 경우 사용)
     * @PreAuthorize("@resourceOwnershipValidator.isAdmin()")에서 사용
     */
    public boolean isAdmin() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return false;
            }

            // 현재는 간단한 구현, 향후 역할 시스템 도입 시 확장
            boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
            
            if (isAdmin) {
                log.info("관리자 권한 검증 성공. 사용자: {}", getCurrentUserId());
            }
            
            return isAdmin;
            
        } catch (Exception e) {
            log.error("관리자 권한 검증 중 오류 발생", e);
            return false;
        }
    }

    /**
     * 현재 인증된 사용자 ID 반환
     */
    public String getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                log.debug("인증 정보가 없음");
                return null;
            }

            Object principal = authentication.getPrincipal();
            if (principal == null) {
                log.debug("Principal이 null");
                return null;
            }
            
            if (principal instanceof String) {
                return (String) principal;
            }
            
            // CustomUserDetails가 있다면 해당 객체에서 사용자 ID 추출
            if (principal.toString().equals("anonymousUser")) {
                log.debug("익명 사용자");
                return null;
            }
            
            return principal.toString();
            
        } catch (Exception e) {
            log.error("현재 사용자 ID 추출 중 오류 발생", e);
            return null;
        }
    }

    /**
     * 인증된 사용자인지 확인
     * @PreAuthorize("@resourceOwnershipValidator.isAuthenticated()")에서 사용
     */
    public boolean isAuthenticated() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            boolean authenticated = authentication != null && 
                                  authentication.isAuthenticated() && 
                                  authentication.getPrincipal() != null &&
                                  !"anonymousUser".equals(authentication.getPrincipal().toString());
            
            if (!authenticated) {
                log.debug("인증되지 않은 사용자의 접근 시도");
            }
            
            return authenticated;
            
        } catch (Exception e) {
            log.error("인증 상태 확인 중 오류 발생", e);
            return false;
        }
    }

    /**
     * 특정 조건에서만 접근 허용 (예: 특정 시간대, 특정 IP 등)
     * @PreAuthorize("@resourceOwnershipValidator.isAccessAllowed()")에서 사용
     */
    public boolean isAccessAllowed() {
        try {
            // 현재는 기본적으로 인증된 사용자만 허용
            // 향후 추가 조건 (시간대, IP 제한 등) 구현 가능
            return isAuthenticated();
            
        } catch (Exception e) {
            log.error("접근 허용 검증 중 오류 발생", e);
            return false;
        }
    }
}