package com.Hamalog.domain.medication;

import com.Hamalog.domain.member.Member;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class MedicationSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "medication_schedule_id")
    private Long medicationScheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /**
     * 복약 알림 시간 목록 (애그리게이트 내부 엔티티)
     * MedicationSchedule이 애그리게이트 루트로서 MedicationTime의 생명주기를 관리
     */
    @OneToMany(mappedBy = "medicationSchedule", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("takeTime ASC")
    private List<MedicationTime> medicationTimes = new ArrayList<>();

    @Column(length = 20, nullable = false)
    private String name;

    @Column(name = "hospital_name", length = 20, nullable = false)
    private String hospitalName;

    @Column(name = "prescription_date", nullable = false)
    private LocalDate prescriptionDate;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(name = "start_of_ad", nullable = false)
    private LocalDate startOfAd;

    @Column(name = "prescription_days", nullable = false)
    private Integer prescriptionDays;

    @Column(name = "per_day", nullable = false)
    private Integer perDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "alarm_type", nullable = false)
    private AlarmType alarmType;

    @Column(name = "image_path", length = 500)
    private String imagePath;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Version
    @Column(name = "version")
    private Long version;

    public MedicationSchedule(
            Member member,
            String name,
            String hospitalName,
            LocalDate prescriptionDate,
            String memo,
            LocalDate startOfAd,
            Integer prescriptionDays,
            Integer perDay,
            AlarmType alarmType
    ) {
        this.member = member;
        this.name = name;
        this.hospitalName = hospitalName;
        this.prescriptionDate = prescriptionDate;
        this.memo = memo;
        this.startOfAd = startOfAd;
        this.prescriptionDays = prescriptionDays;
        this.perDay = perDay;
        this.alarmType = alarmType;
    }

    // ========== 도메인 로직 (비즈니스 규칙) ==========

    /**
     * 복약 스케줄 종료일 계산
     * @return 복약 종료 예정일
     */
    public LocalDate getEndDate() {
        return this.startOfAd.plusDays(this.prescriptionDays - 1);
    }

    /**
     * 복약 스케줄 만료 여부 확인
     * @return 오늘 기준 복약 기간이 종료되었으면 true
     */
    public boolean isExpired() {
        return LocalDate.now().isAfter(getEndDate());
    }

    /**
     * 복약 스케줄 만료 여부 확인 (특정 날짜 기준)
     * @param referenceDate 기준 날짜
     * @return 기준 날짜 기준 복약 기간이 종료되었으면 true
     */
    public boolean isExpiredAt(LocalDate referenceDate) {
        return referenceDate.isAfter(getEndDate());
    }

    /**
     * 남은 복약 일수 계산
     * @return 남은 일수 (만료 시 0 반환)
     */
    public int getRemainingDays() {
        long remaining = ChronoUnit.DAYS.between(LocalDate.now(), getEndDate()) + 1;
        return Math.max(0, (int) remaining);
    }

    /**
     * 복약 진행률 계산 (백분율)
     * @return 0~100 사이의 진행률
     */
    public int getProgressPercentage() {
        long totalDays = this.prescriptionDays;
        long elapsedDays = ChronoUnit.DAYS.between(this.startOfAd, LocalDate.now()) + 1;

        if (elapsedDays <= 0) return 0;
        if (elapsedDays >= totalDays) return 100;

        return (int) ((elapsedDays * 100) / totalDays);
    }

    /**
     * 복약이 시작되었는지 확인
     * @return 오늘이 복약 시작일 이후면 true
     */
    public boolean hasStarted() {
        return !LocalDate.now().isBefore(this.startOfAd);
    }

    /**
     * 현재 복약 중인지 확인 (시작됨 & 만료되지 않음 & 활성 상태)
     * @return 현재 복약 중이면 true
     */
    public boolean isOngoing() {
        return hasStarted() && !isExpired() && Boolean.TRUE.equals(this.isActive);
    }

    /**
     * 총 복용 횟수 계산
     * @return 처방 기간 동안의 총 복용 횟수
     */
    public int getTotalDosageCount() {
        return this.prescriptionDays * this.perDay;
    }

    /**
     * 알람 활성화 여부 확인
     * @return 알람 타입이 설정되어 있으면 true
     */
    public boolean isAlarmEnabled() {
        return this.alarmType != null;
    }

    // ========== 상태 변경 메서드 ==========

    public void update(
            String name,
            String hospitalName,
            LocalDate prescriptionDate,
            String memo,
            LocalDate startOfAd,
            Integer prescriptionDays,
            Integer perDay,
            AlarmType alarmType
    ) {
        this.name = name;
        this.hospitalName = hospitalName;
        this.prescriptionDate = prescriptionDate;
        this.memo = memo;
        this.startOfAd = startOfAd;
        this.prescriptionDays = prescriptionDays;
        this.perDay = perDay;
        this.alarmType = alarmType;
    }

    public void updateImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public void removeImage() {
        this.imagePath = null;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public boolean hasImage() {
        return this.imagePath != null && !this.imagePath.isBlank();
    }

    // ========== MedicationTime 관리 메서드 (애그리게이트 루트를 통한 접근) ==========

    /**
     * 복약 알림 시간 추가
     * 애그리게이트 루트를 통해서만 MedicationTime을 추가할 수 있음
     * @param takeTime 복용 시간
     * @return 생성된 MedicationTime
     */
    public MedicationTime addMedicationTime(LocalTime takeTime) {
        MedicationTime medicationTime = new MedicationTime(this, takeTime);
        this.medicationTimes.add(medicationTime);
        return medicationTime;
    }

    /**
     * 복약 알림 시간 제거
     * @param medicationTime 제거할 MedicationTime
     */
    public void removeMedicationTime(MedicationTime medicationTime) {
        this.medicationTimes.remove(medicationTime);
    }

    /**
     * 특정 ID의 복약 알림 시간 제거
     * @param medicationTimeId 제거할 MedicationTime의 ID
     * @return 제거 성공 여부
     */
    public boolean removeMedicationTimeById(Long medicationTimeId) {
        return this.medicationTimes.removeIf(
                mt -> mt.getMedicationTimeId() != null && mt.getMedicationTimeId().equals(medicationTimeId)
        );
    }

    /**
     * 모든 복약 알림 시간 제거
     */
    public void clearMedicationTimes() {
        this.medicationTimes.clear();
    }

    /**
     * 복약 알림 시간 목록 조회 (불변 리스트 반환)
     * @return 복약 알림 시간 불변 리스트
     */
    public List<MedicationTime> getMedicationTimesReadOnly() {
        return Collections.unmodifiableList(this.medicationTimes);
    }

    /**
     * 특정 ID의 복약 알림 시간 조회
     * @param medicationTimeId 조회할 MedicationTime의 ID
     * @return MedicationTime (없으면 null)
     */
    public MedicationTime findMedicationTimeById(Long medicationTimeId) {
        return this.medicationTimes.stream()
                .filter(mt -> mt.getMedicationTimeId() != null && mt.getMedicationTimeId().equals(medicationTimeId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 복약 알림 시간 개수 조회
     * @return 알림 시간 개수
     */
    public int getMedicationTimeCount() {
        return this.medicationTimes.size();
    }
}
