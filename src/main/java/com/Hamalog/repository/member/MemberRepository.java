package com.Hamalog.repository.member;

import com.Hamalog.domain.member.Member;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByLoginId(String loginId);
    List<Member> findAllByDeletionScheduledTrueAndDeletionDueAtBefore(LocalDateTime threshold);
}
