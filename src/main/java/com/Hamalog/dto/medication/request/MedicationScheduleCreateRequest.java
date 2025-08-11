package com.Hamalog.dto.medication.request;

import jakarta.validation.constraints.*;

public record MedicationScheduleCreateRequest(
        @NotNull(message = "memberId는 필수입니다") Long memberId,
        @NotBlank(message = "약물명은 필수입니다") @Size(max = 20, message = "약물명은 최대 20자입니다") String name,
        @NotBlank(message = "병원명은 필수입니다") @Size(max = 20, message = "병원명은 최대 20자입니다") String hospitalName,
        @NotBlank(message = "처방일은 필수입니다")
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "처방일 형식은 yyyy-MM-dd 여야 합니다") String prescriptionDate,
        @Size(max = 500, message = "메모는 최대 500자입니다") String memo,
        @NotBlank(message = "복용 시작일은 필수입니다")
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "복용 시작일 형식은 yyyy-MM-dd 여야 합니다") String startOfAd,
        @NotNull(message = "처방 일수는 필수입니다") @Min(value = 1, message = "처방 일수는 최소 1 이상이어야 합니다") Integer prescriptionDays,
        @NotNull(message = "하루 복용 횟수는 필수입니다") @Min(value = 1, message = "하루 복용 횟수는 최소 1 이상이어야 합니다") Integer perDay,
        @NotBlank(message = "알람 유형은 필수입니다") String alarmType
) {}
