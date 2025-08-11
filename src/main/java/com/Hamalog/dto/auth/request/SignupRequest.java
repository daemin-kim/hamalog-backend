package com.Hamalog.dto.auth.request;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record SignupRequest(
        @NotBlank @Size(min = 4, max = 20) String loginId,
        @NotBlank @Size(min = 6, max = 100) String password,
        @NotBlank @Size(min = 1, max = 50) String name,
        @NotBlank @Pattern(regexp = "^\\d{10,13}$") String phoneNumber,
        @NotNull LocalDate birth
) {}
