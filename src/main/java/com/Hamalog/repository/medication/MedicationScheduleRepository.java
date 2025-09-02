package com.Hamalog.repository.medication;

import com.Hamalog.domain.medication.MedicationSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MedicationScheduleRepository extends JpaRepository<MedicationSchedule, Long> {

    // N+1 문제 해결: @EntityGraph를 사용하여 Member를 EAGER로 함께 조회
    @EntityGraph(attributePaths = {"member"})
    List<MedicationSchedule> findAllByMember_MemberId(Long memberId);

    // N+1 문제 해결: @EntityGraph를 사용하여 Member를 EAGER로 함께 조회 (페이징)
    @EntityGraph(attributePaths = {"member"})
    Page<MedicationSchedule> findByMember_MemberId(Long memberId, Pageable pageable);
    
    // N+1 문제 해결: 단일 조회 시에도 Member를 함께 조회
    @EntityGraph(attributePaths = {"member"})
    Optional<MedicationSchedule> findById(Long id);
    
    // N+1 문제 해결: JOIN FETCH를 사용한 대안 메서드
    @Query("SELECT ms FROM MedicationSchedule ms JOIN FETCH ms.member WHERE ms.member.memberId = :memberId")
    List<MedicationSchedule> findAllByMemberIdWithMember(@Param("memberId") Long memberId);
}
