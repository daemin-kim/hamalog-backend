package com.Hamalog.domain.sideEffect;

import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.domain.member.Member;
import com.Hamalog.security.encryption.EncryptedStringConverter;
import jakarta.persistence.*;
import java.time.LocalDateTime;
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

    /**
     * 복약 스케줄 연계 설정
     */
    public void linkToMedicationSchedule(MedicationSchedule schedule) {
        this.linkedMedicationSchedule = schedule;
    }
}
