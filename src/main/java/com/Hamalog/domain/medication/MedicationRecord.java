package com.Hamalog.domain.medication;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class MedicationRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "medication_record_id")
    private Long medicationRecordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_schedule_id", nullable = false)
    private MedicationSchedule medicationSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_time_id", nullable = false)
    private MedicationTime medicationTime;

    @Column(name = "is_take_medication", nullable = false)
    private Boolean isTakeMedication;

    @Column(name = "real_take_time")
    private LocalDateTime realTakeTime;

    @Version
    @Column(name = "version")
    private Long version;

    // 지연 복용 판단 기준 (분)
    private static final int DELAYED_THRESHOLD_MINUTES = 30;

    public MedicationRecord(
            MedicationSchedule medicationSchedule,
            MedicationTime medicationTime,
            Boolean isTakeMedication,
            LocalDateTime realTakeTime
    ) {
        this.medicationSchedule = medicationSchedule;
        this.medicationTime = medicationTime;
        this.isTakeMedication = isTakeMedication;
        this.realTakeTime = realTakeTime;
    }

    // ========== 도메인 로직 (비즈니스 규칙) ==========

    /**
     * 복용 완료 여부 확인
     * @return 복용했으면 true
     */
    public boolean isTaken() {
        return Boolean.TRUE.equals(this.isTakeMedication);
    }

    /**
     * 복용 미완료 여부 확인
     * @return 복용하지 않았으면 true
     */
    public boolean isSkipped() {
        return Boolean.FALSE.equals(this.isTakeMedication);
    }

    /**
     * 지연 복용 여부 확인
     * 예정 시간보다 30분 이상 늦게 복용한 경우 지연으로 판단
     * @return 지연 복용이면 true
     */
    public boolean isDelayed() {
        if (!isTaken() || this.realTakeTime == null || this.medicationTime == null) {
            return false;
        }

        LocalTime scheduledTime = this.medicationTime.getTakeTime();
        LocalTime actualTime = this.realTakeTime.toLocalTime();

        long delayMinutes = Duration.between(scheduledTime, actualTime).toMinutes();
        return delayMinutes > DELAYED_THRESHOLD_MINUTES;
    }

    /**
     * 조기 복용 여부 확인
     * 예정 시간보다 30분 이상 일찍 복용한 경우
     * @return 조기 복용이면 true
     */
    public boolean isEarly() {
        if (!isTaken() || this.realTakeTime == null || this.medicationTime == null) {
            return false;
        }

        LocalTime scheduledTime = this.medicationTime.getTakeTime();
        LocalTime actualTime = this.realTakeTime.toLocalTime();

        long earlyMinutes = Duration.between(actualTime, scheduledTime).toMinutes();
        return earlyMinutes > DELAYED_THRESHOLD_MINUTES;
    }

    /**
     * 정시 복용 여부 확인
     * 예정 시간 전후 30분 이내에 복용한 경우
     * @return 정시 복용이면 true
     */
    public boolean isOnTime() {
        return isTaken() && !isDelayed() && !isEarly();
    }

    /**
     * 복용 시간 차이 계산 (분 단위)
     * 양수: 지연, 음수: 조기
     * @return 예정 시간과 실제 복용 시간의 차이 (분)
     */
    public long getTimeDifferenceMinutes() {
        if (this.realTakeTime == null || this.medicationTime == null) {
            return 0;
        }

        LocalTime scheduledTime = this.medicationTime.getTakeTime();
        LocalTime actualTime = this.realTakeTime.toLocalTime();

        return Duration.between(scheduledTime, actualTime).toMinutes();
    }

    /**
     * 실제 복용 시간이 기록되었는지 확인
     * @return 실제 복용 시간이 있으면 true
     */
    public boolean hasRealTakeTime() {
        return this.realTakeTime != null;
    }

    // ========== 상태 변경 메서드 ==========

    public void update(
            Boolean isTakeMedication,
            LocalDateTime realTakeTime
    ) {
        this.isTakeMedication = isTakeMedication;
        this.realTakeTime = realTakeTime;
    }

    /**
     * 복용 완료로 상태 변경
     * @param takeTime 실제 복용 시간
     */
    public void markAsTaken(LocalDateTime takeTime) {
        this.isTakeMedication = true;
        this.realTakeTime = takeTime;
    }

    /**
     * 복용 미완료로 상태 변경
     */
    public void markAsSkipped() {
        this.isTakeMedication = false;
        this.realTakeTime = null;
    }
}
