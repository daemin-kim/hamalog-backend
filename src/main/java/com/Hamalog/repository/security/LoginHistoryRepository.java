package com.Hamalog.repository.security;

import com.Hamalog.domain.security.LoginHistory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    // 회원별 로그인 이력 조회 (최신순)
    Page<LoginHistory> findByMember_MemberIdOrderByLoginTimeDesc(Long memberId, Pageable pageable);

    // 회원별 최근 로그인 이력 조회 (기본 10개)
    List<LoginHistory> findTop10ByMember_MemberIdOrderByLoginTimeDesc(Long memberId);

    // 활성 세션 조회
    @Query("SELECT lh FROM LoginHistory lh WHERE lh.member.memberId = :memberId AND lh.isActive = true ORDER BY lh.loginTime DESC")
    List<LoginHistory> findActiveSessionsByMemberId(@Param("memberId") Long memberId);

    // 세션 ID로 조회
    Optional<LoginHistory> findBySessionId(String sessionId);

    // 세션 ID와 회원 ID로 조회 (보안 검증용)
    Optional<LoginHistory> findBySessionIdAndMember_MemberId(String sessionId, Long memberId);

    // 특정 세션 비활성화
    @Modifying
    @Query("UPDATE LoginHistory lh SET lh.isActive = false, lh.logoutTime = :logoutTime WHERE lh.sessionId = :sessionId")
    int deactivateSession(@Param("sessionId") String sessionId, @Param("logoutTime") LocalDateTime logoutTime);

    // 회원의 모든 세션 비활성화 (로그아웃 시)
    @Modifying
    @Query("UPDATE LoginHistory lh SET lh.isActive = false, lh.logoutTime = :logoutTime WHERE lh.member.memberId = :memberId AND lh.isActive = true")
    int deactivateAllSessionsByMemberId(@Param("memberId") Long memberId, @Param("logoutTime") LocalDateTime logoutTime);

    // 로그인 실패 이력 조회 (최근 N분간)
    @Query("SELECT COUNT(lh) FROM LoginHistory lh WHERE lh.member.memberId = :memberId AND lh.loginStatus = 'FAILED' AND lh.loginTime > :since")
    long countRecentFailedLoginAttempts(@Param("memberId") Long memberId, @Param("since") LocalDateTime since);

    // 회원 탈퇴 시 이력 삭제
    @Modifying
    @Query("DELETE FROM LoginHistory lh WHERE lh.member.memberId = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);

    // 오래된 이력 정리 (30일 이상)
    @Modifying
    @Query("DELETE FROM LoginHistory lh WHERE lh.loginTime < :before")
    int deleteOldHistory(@Param("before") LocalDateTime before);
}

