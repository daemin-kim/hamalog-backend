package com.Hamalog.domain.medication;

import com.Hamalog.domain.member.Member;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicationScheduleGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "medication_schedule_group_id")
    private Long medicationScheduleGroupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /**
     * 그룹에 속한 스케줄 연결 목록 (애그리게이트 내부 엔티티)
     * MedicationScheduleGroup이 애그리게이트 루트로서 중간 테이블의 생명주기를 관리
     */
    @OneToMany(mappedBy = "medicationScheduleGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MedicationScheduleMedicationScheduleGroup> scheduleLinks = new ArrayList<>();

    @Column(length = 20, nullable = false)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(length = 7)
    private String color;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public MedicationScheduleGroup(Member member, String name, String description, String color) {
        this.member = member;
        this.name = name;
        this.description = description;
        this.color = color;
    }

    public void update(String name, String description, String color) {
        this.name = name;
        this.description = description;
        this.color = color;
    }

    // ========== 스케줄 연결 관리 메서드 (애그리게이트 루트를 통한 접근) ==========

    /**
     * 스케줄을 그룹에 추가
     * @param schedule 추가할 스케줄
     */
    public void addSchedule(MedicationSchedule schedule) {
        if (containsSchedule(schedule.getMedicationScheduleId())) {
            return; // 이미 포함된 경우 무시
        }
        MedicationScheduleMedicationScheduleGroup link = new MedicationScheduleMedicationScheduleGroup(
                schedule.getMedicationScheduleId(),
                this.medicationScheduleGroupId,
                schedule,
                this
        );
        this.scheduleLinks.add(link);
    }

    /**
     * 스케줄을 그룹에서 제거
     * @param scheduleId 제거할 스케줄 ID
     * @return 제거 성공 여부
     */
    public boolean removeSchedule(Long scheduleId) {
        return this.scheduleLinks.removeIf(
                link -> link.getMedicationScheduleId() != null && link.getMedicationScheduleId().equals(scheduleId)
        );
    }

    /**
     * 그룹의 모든 스케줄 연결 제거
     */
    public void clearSchedules() {
        this.scheduleLinks.clear();
    }

    /**
     * 스케줄 연결 목록 조회 (불변 리스트 반환)
     * @return 스케줄 연결 불변 리스트
     */
    public List<MedicationScheduleMedicationScheduleGroup> getScheduleLinksReadOnly() {
        return Collections.unmodifiableList(this.scheduleLinks);
    }

    /**
     * 그룹에 포함된 스케줄 개수
     * @return 스케줄 개수
     */
    public int getScheduleCount() {
        return this.scheduleLinks.size();
    }

    /**
     * 특정 스케줄이 그룹에 포함되어 있는지 확인
     * @param scheduleId 스케줄 ID
     * @return 포함 여부
     */
    public boolean containsSchedule(Long scheduleId) {
        return this.scheduleLinks.stream()
                .anyMatch(link -> link.getMedicationScheduleId() != null
                        && link.getMedicationScheduleId().equals(scheduleId));
    }
}
