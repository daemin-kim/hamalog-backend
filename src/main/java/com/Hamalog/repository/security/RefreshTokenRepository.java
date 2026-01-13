package com.Hamalog.repository.security;

import com.Hamalog.domain.security.RefreshToken;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @EntityGraph(attributePaths = {"member"})
    Optional<RefreshToken> findByTokenValue(String tokenValue);

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.member.memberId = :memberId")
    List<RefreshToken> findByMemberId(@Param("memberId") Long memberId);

    @Modifying
    @Query("DELETE FROM RefreshToken WHERE expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.member.memberId = :memberId")
    void revokeAllByMemberId(@Param("memberId") Long memberId);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.member.memberId = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);
}
