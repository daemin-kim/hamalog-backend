package com.Hamalog.domain.sideEffect;

import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.domain.member.Member;
import com.Hamalog.security.encryption.EncryptedStringConverter;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SideEffectRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "side_effect_record_id")
    private Long sideEffectRecordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /**
     * 연계된 복약 스케줄 (선택)
     * 부작용이 특정 약물 복용 후 발생한 경우 연결하여 추적
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_medication_schedule_id")
    private MedicationSchedule linkedMedicationSchedule;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "description", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String description;

    // ========== 도메인 로직 (비즈니스 규칙) ==========

    /**
     * 복약 스케줄과 연계되어 있는지 확인
     * @return 연계된 스케줄이 있으면 true
     */
    public boolean isLinkedToMedication() {
        return this.linkedMedicationSchedule != null;
    }

    /**
     * 기록 생성 후 경과 일수 계산
     * @return 생성일로부터 경과한 일수
     */
    public long getDaysSinceCreated() {
        return ChronoUnit.DAYS.between(this.createdAt.toLocalDate(), LocalDate.now());
    }

    /**
     * 최근 기록인지 확인 (7일 이내)
     * @return 7일 이내 기록이면 true
     */
    public boolean isRecent() {
        return getDaysSinceCreated() <= 7;
    }

    /**
     * 오늘 기록인지 확인
     * @return 오늘 생성된 기록이면 true
     */
    public boolean isToday() {
        return this.createdAt.toLocalDate().equals(LocalDate.now());
    }

    /**
     * 설명이 있는지 확인
     * @return 설명이 있으면 true
     */
    public boolean hasDescription() {
        return this.description != null && !this.description.isBlank();
    }

    /**
     * 연계된 약물명 조회 (안전한 조회)
     * @return 연계된 약물명, 없으면 null
     */
    public String getLinkedMedicationName() {
        return isLinkedToMedication() ? this.linkedMedicationSchedule.getName() : null;
    }

    // ========== 상태 변경 메서드 ==========

    /**
     * 복약 스케줄 연계 설정
     */
    public void linkToMedicationSchedule(MedicationSchedule schedule) {
        this.linkedMedicationSchedule = schedule;
    }

    /**
     * 복약 스케줄 연계 해제
     */
    public void unlinkFromMedicationSchedule() {
        this.linkedMedicationSchedule = null;
    }

    /**
     * 설명 업데이트
     */
    public void updateDescription(String description) {
        this.description = description;
    }
}
