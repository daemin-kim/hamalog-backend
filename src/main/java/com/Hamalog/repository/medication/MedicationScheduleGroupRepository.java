package com.Hamalog.repository.medication;

import com.Hamalog.domain.medication.MedicationScheduleGroup;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MedicationScheduleGroupRepository extends JpaRepository<MedicationScheduleGroup, Long> {

    // 회원별 그룹 목록 조회
    @EntityGraph(attributePaths = {"member"})
    List<MedicationScheduleGroup> findByMember_MemberIdOrderByCreatedAtDesc(Long memberId);

    // 회원별 그룹 목록 조회 (페이지네이션)
    @EntityGraph(attributePaths = {"member"})
    Page<MedicationScheduleGroup> findByMember_MemberId(Long memberId, Pageable pageable);

    // 그룹 ID와 회원 ID로 조회 (권한 검증용)
    Optional<MedicationScheduleGroup> findByMedicationScheduleGroupIdAndMember_MemberId(Long groupId, Long memberId);

    // 그룹명 중복 확인
    boolean existsByMember_MemberIdAndName(Long memberId, String name);

    // 회원 탈퇴 시 삭제
    @Modifying
    @Query("DELETE FROM MedicationScheduleGroup g WHERE g.member.memberId = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);

    // 그룹명으로 검색
    @Query("SELECT g FROM MedicationScheduleGroup g WHERE g.member.memberId = :memberId " +
           "AND LOWER(g.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<MedicationScheduleGroup> searchByName(@Param("memberId") Long memberId, @Param("keyword") String keyword);
}

