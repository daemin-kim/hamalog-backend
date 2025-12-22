package com.Hamalog.repository.medication;

import com.Hamalog.domain.medication.MedicationRecord;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicationRecordRepository extends JpaRepository<MedicationRecord, Long> {

    // N+1 문제 해결: @EntityGraph를 사용하여 연관된 엔티티들을 함께 조회
    @EntityGraph(attributePaths = {"medicationSchedule", "medicationSchedule.member", "medicationTime"})
    List<MedicationRecord> findAllByMedicationSchedule_MedicationScheduleId(Long medicationScheduleId);
    
    // N+1 문제 해결: 단일 조회 시에도 연관된 엔티티들을 함께 조회
    @EntityGraph(attributePaths = {"medicationSchedule", "medicationSchedule.member", "medicationTime"})
    Optional<MedicationRecord> findById(Long id);
    
    // N+1 문제 해결: JOIN FETCH를 사용한 대안 메서드 (권한 확인용)
    @Query("SELECT mr FROM MedicationRecord mr " +
           "JOIN FETCH mr.medicationSchedule ms " +
           "JOIN FETCH ms.member " +
           "WHERE mr.medicationRecordId = :recordId")
    Optional<MedicationRecord> findByIdWithMemberForOwnershipCheck(@Param("recordId") Long recordId);
    
    // N+1 문제 해결: 스케줄별 레코드 조회 시 JOIN FETCH 사용
    @Query("SELECT mr FROM MedicationRecord mr " +
           "JOIN FETCH mr.medicationSchedule ms " +
           "JOIN FETCH mr.medicationTime mt " +
           "WHERE ms.medicationScheduleId = :scheduleId")
    List<MedicationRecord> findAllByScheduleIdWithJoinFetch(@Param("scheduleId") Long scheduleId);
    
    // Efficient batch delete for member deletion (by schedule IDs)
    @Modifying
    @Query("DELETE FROM MedicationRecord mr WHERE mr.medicationSchedule.medicationScheduleId IN :scheduleIds")
    void deleteByScheduleIds(@Param("scheduleIds") List<Long> scheduleIds);

    // 회원별 기간 내 복약 기록 조회 (통계용)
    @Query("SELECT mr FROM MedicationRecord mr " +
           "JOIN FETCH mr.medicationSchedule ms " +
           "JOIN FETCH mr.medicationTime mt " +
           "WHERE ms.member.memberId = :memberId " +
           "AND mr.realTakeTime BETWEEN :startDateTime AND :endDateTime")
    List<MedicationRecord> findByMemberIdAndDateRange(
            @Param("memberId") Long memberId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    // 회원별 전체 복약 기록 수 조회
    @Query("SELECT COUNT(mr) FROM MedicationRecord mr " +
           "JOIN mr.medicationSchedule ms " +
           "WHERE ms.member.memberId = :memberId")
    long countByMemberId(@Param("memberId") Long memberId);

    // 회원별 복용 완료 기록 수 조회
    @Query("SELECT COUNT(mr) FROM MedicationRecord mr " +
           "JOIN mr.medicationSchedule ms " +
           "WHERE ms.member.memberId = :memberId AND mr.isTakeMedication = true")
    long countTakenByMemberId(@Param("memberId") Long memberId);

    // 회원별 기간 내 복약 기록 수 조회
    @Query("SELECT COUNT(mr) FROM MedicationRecord mr " +
           "JOIN mr.medicationSchedule ms " +
           "WHERE ms.member.memberId = :memberId " +
           "AND mr.realTakeTime BETWEEN :startDateTime AND :endDateTime")
    long countByMemberIdAndDateRange(
            @Param("memberId") Long memberId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    // 회원별 기간 내 복용 완료 기록 수 조회
    @Query("SELECT COUNT(mr) FROM MedicationRecord mr " +
           "JOIN mr.medicationSchedule ms " +
           "WHERE ms.member.memberId = :memberId " +
           "AND mr.isTakeMedication = true " +
           "AND mr.realTakeTime BETWEEN :startDateTime AND :endDateTime")
    long countTakenByMemberIdAndDateRange(
            @Param("memberId") Long memberId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    // 스케줄별 복약 기록 통계
    @Query("SELECT mr.medicationSchedule.medicationScheduleId, COUNT(mr), " +
           "SUM(CASE WHEN mr.isTakeMedication = true THEN 1 ELSE 0 END) " +
           "FROM MedicationRecord mr " +
           "WHERE mr.medicationSchedule.member.memberId = :memberId " +
           "GROUP BY mr.medicationSchedule.medicationScheduleId")
    List<Object[]> getScheduleStatsByMemberId(@Param("memberId") Long memberId);
}
