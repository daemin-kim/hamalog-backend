package com.Hamalog.repository.sideEffect;

import com.Hamalog.domain.sideEffect.SideEffect;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SideEffectRepository extends JpaRepository<SideEffect, Long> {

    @Query(value = """
        SELECT s.name
        FROM side_effect s
        JOIN side_effect_side_effect_record ssr ON s.side_effect_id = ssr.side_effect_id
        JOIN side_effect_record sr ON ssr.side_effect_record_id = sr.side_effect_record_id
        WHERE sr.member_id = :memberId
        ORDER BY sr.created_at DESC
        LIMIT 5
        """, nativeQuery = true)
    List<String> findRecentSideEffectNames(@Param("memberId") Long memberId);
}
