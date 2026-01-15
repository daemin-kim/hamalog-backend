package com.Hamalog.repository.medication;

import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.dto.medication.projection.MedicationScheduleProjection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicationScheduleRepository extends JpaRepository<MedicationSchedule, Long> {

    // 벤치마크용: 회원별 스케줄 개수 카운트
    long countByMember_MemberId(Long memberId);

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

    // ⚠️ 벤치마크용: N+1 문제가 발생하는 naive 쿼리 (성능 비교 테스트 전용)
    // 프로덕션에서는 사용 금지 - findAllByMember_MemberId() 사용할 것
    @Query("SELECT ms FROM MedicationSchedule ms WHERE ms.member.memberId = :memberId")
    List<MedicationSchedule> findAllByMemberIdNaive(@Param("memberId") Long memberId);

    // DTO Projection: 엔티티 전체가 아닌 필요한 필드만 조회하여 성능 최적화
    @Query("SELECT new com.Hamalog.dto.medication.projection.MedicationScheduleProjection(" +
           "ms.medicationScheduleId, ms.member.memberId, ms.name, ms.hospitalName, " +
           "ms.prescriptionDate, ms.memo, ms.startOfAd, ms.prescriptionDays, ms.perDay, " +
           "ms.alarmType, ms.isActive) " +
           "FROM MedicationSchedule ms WHERE ms.member.memberId = :memberId")
    List<MedicationScheduleProjection> findProjectionsByMemberId(@Param("memberId") Long memberId);

    // DTO Projection with Paging: 페이징 지원
    @Query("SELECT new com.Hamalog.dto.medication.projection.MedicationScheduleProjection(" +
           "ms.medicationScheduleId, ms.member.memberId, ms.name, ms.hospitalName, " +
           "ms.prescriptionDate, ms.memo, ms.startOfAd, ms.prescriptionDays, ms.perDay, " +
           "ms.alarmType, ms.isActive) " +
           "FROM MedicationSchedule ms WHERE ms.member.memberId = :memberId")
    Page<MedicationScheduleProjection> findProjectionsByMemberId(@Param("memberId") Long memberId, Pageable pageable);

    // Efficient batch delete for member deletion
    @Modifying
    @Query("DELETE FROM MedicationSchedule ms WHERE ms.member.memberId = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);

    // 약 이름으로 검색
    @EntityGraph(attributePaths = {"member"})
    @Query("SELECT ms FROM MedicationSchedule ms " +
           "WHERE ms.member.memberId = :memberId " +
           "AND LOWER(ms.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<MedicationSchedule> searchByName(
            @Param("memberId") Long memberId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // DTO Projection: 약 이름으로 검색 (성능 최적화)
    @Query("SELECT new com.Hamalog.dto.medication.projection.MedicationScheduleProjection(" +
           "ms.medicationScheduleId, ms.member.memberId, ms.name, ms.hospitalName, " +
           "ms.prescriptionDate, ms.memo, ms.startOfAd, ms.prescriptionDays, ms.perDay, " +
           "ms.alarmType, ms.isActive) " +
           "FROM MedicationSchedule ms WHERE ms.member.memberId = :memberId " +
           "AND LOWER(ms.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<MedicationScheduleProjection> searchProjectionsByName(
            @Param("memberId") Long memberId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // 활성 상태로 필터링
    @EntityGraph(attributePaths = {"member"})
    Page<MedicationSchedule> findByMember_MemberIdAndIsActive(Long memberId, Boolean isActive, Pageable pageable);

    // DTO Projection: 활성 상태로 필터링 (성능 최적화)
    @Query("SELECT new com.Hamalog.dto.medication.projection.MedicationScheduleProjection(" +
           "ms.medicationScheduleId, ms.member.memberId, ms.name, ms.hospitalName, " +
           "ms.prescriptionDate, ms.memo, ms.startOfAd, ms.prescriptionDays, ms.perDay, " +
           "ms.alarmType, ms.isActive) " +
           "FROM MedicationSchedule ms WHERE ms.member.memberId = :memberId AND ms.isActive = :isActive")
    Page<MedicationScheduleProjection> findProjectionsByMemberIdAndIsActive(
            @Param("memberId") Long memberId,
            @Param("isActive") Boolean isActive,
            Pageable pageable
    );
}
