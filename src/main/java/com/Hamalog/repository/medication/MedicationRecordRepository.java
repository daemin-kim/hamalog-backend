package com.Hamalog.repository.medication;

import com.Hamalog.domain.medication.MedicationRecord;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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
}
