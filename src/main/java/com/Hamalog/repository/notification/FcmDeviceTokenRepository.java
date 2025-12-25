package com.Hamalog.repository.notification;

import com.Hamalog.domain.notification.FcmDeviceToken;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * FCM 디바이스 토큰 Repository
 */
@Repository
public interface FcmDeviceTokenRepository extends JpaRepository<FcmDeviceToken, Long> {

    /**
     * 회원 ID로 활성화된 디바이스 토큰 목록 조회
     */
    List<FcmDeviceToken> findByMember_MemberIdAndIsActiveTrue(Long memberId);

    /**
     * 회원 ID로 모든 디바이스 토큰 조회
     */
    List<FcmDeviceToken> findByMember_MemberId(Long memberId);

    /**
     * 토큰 값으로 디바이스 토큰 조회
     */
    Optional<FcmDeviceToken> findByToken(String token);

    /**
     * 토큰 값 존재 여부 확인
     */
    boolean existsByToken(String token);

    /**
     * 회원 ID와 토큰으로 디바이스 토큰 조회
     */
    Optional<FcmDeviceToken> findByMember_MemberIdAndToken(Long memberId, String token);

    /**
     * 특정 기간 동안 사용되지 않은 토큰 조회 (정리용)
     */
    @Query("SELECT f FROM FcmDeviceToken f WHERE f.lastUsedAt < :threshold AND f.isActive = true")
    List<FcmDeviceToken> findInactiveTokens(@Param("threshold") LocalDateTime threshold);

    /**
     * 회원 ID로 모든 토큰 비활성화
     */
    @Modifying
    @Query("UPDATE FcmDeviceToken f SET f.isActive = false, f.updatedAt = :now WHERE f.member.memberId = :memberId")
    void deactivateAllByMemberId(@Param("memberId") Long memberId, @Param("now") LocalDateTime now);

    /**
     * 회원 ID로 모든 토큰 삭제
     */
    void deleteByMember_MemberId(Long memberId);

    /**
     * 활성화된 토큰 수 조회
     */
    long countByMember_MemberIdAndIsActiveTrue(Long memberId);
}
