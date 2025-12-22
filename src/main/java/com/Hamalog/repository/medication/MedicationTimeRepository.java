package com.Hamalog.repository.medication;

import com.Hamalog.domain.medication.MedicationTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicationTimeRepository extends JpaRepository<MedicationTime, Long> {

    // 스케줄별 알림 시간 목록 조회
    List<MedicationTime> findByMedicationSchedule_MedicationScheduleIdOrderByTakeTimeAsc(Long scheduleId);

    // 알림 시간 단건 조회 (스케줄 정보 포함)
    @Query("SELECT mt FROM MedicationTime mt " +
           "JOIN FETCH mt.medicationSchedule ms " +
           "JOIN FETCH ms.member " +
           "WHERE mt.medicationTimeId = :timeId")
    Optional<MedicationTime> findByIdWithScheduleAndMember(@Param("timeId") Long timeId);

    // 스케줄별 알림 시간 삭제
    @Modifying
    @Query("DELETE FROM MedicationTime mt WHERE mt.medicationSchedule.medicationScheduleId = :scheduleId")
    void deleteByScheduleId(@Param("scheduleId") Long scheduleId);

    // 특정 알림 시간이 특정 회원의 것인지 확인
    @Query("SELECT CASE WHEN COUNT(mt) > 0 THEN true ELSE false END " +
           "FROM MedicationTime mt " +
           "WHERE mt.medicationTimeId = :timeId " +
           "AND mt.medicationSchedule.member.memberId = :memberId")
    boolean existsByIdAndMemberId(@Param("timeId") Long timeId, @Param("memberId") Long memberId);
}
