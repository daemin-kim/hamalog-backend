package com.Hamalog.dto.medication.response;

import com.Hamalog.domain.medication.MedicationScheduleGroup;
import java.time.LocalDateTime;

public record MedicationScheduleGroupResponse(
    Long groupId,
    Long memberId,
    String name,
    String description,
    String color,
    LocalDateTime createdAt
) {
    public static MedicationScheduleGroupResponse from(MedicationScheduleGroup group) {
        return new MedicationScheduleGroupResponse(
            group.getMedicationScheduleGroupId(),
            group.getMember().getMemberId(),
            group.getName(),
            group.getDescription(),
            group.getColor(),
            group.getCreatedAt()
        );
    }
}
