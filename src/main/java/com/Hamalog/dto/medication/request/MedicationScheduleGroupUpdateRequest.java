package com.Hamalog.dto.medication.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MedicationScheduleGroupUpdateRequest(
    @Size(max = 20, message = "그룹 이름은 20자 이하여야 합니다")
    String name,

    @Size(max = 255, message = "설명은 255자 이하여야 합니다")
    String description,

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "색상은 #RRGGBB 형식이어야 합니다")
    String color
) {}

