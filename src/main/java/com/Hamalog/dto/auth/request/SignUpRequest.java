package com.Hamalog.dto.auth.request;

import java.time.LocalDate;

public record SignUpRequest(
        String loginId,
        String password,
        String name,
        String phoneNumber,
        LocalDate birth
) {}
