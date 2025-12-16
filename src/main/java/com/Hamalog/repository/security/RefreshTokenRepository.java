package com.Hamalog.repository.security;

import com.Hamalog.domain.security.RefreshToken;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenValue(String tokenValue);

    List<RefreshToken> findByMemberId(Long memberId);

    @Modifying
    @Query("DELETE FROM RefreshToken WHERE expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE RefreshToken SET revoked = true WHERE memberId = :memberId")
    void revokeAllByMemberId(@Param("memberId") Long memberId);

    @Modifying
    @Query("DELETE FROM RefreshToken WHERE memberId = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);
}
