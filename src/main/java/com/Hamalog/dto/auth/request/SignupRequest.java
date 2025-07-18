package com.Hamalog.dto.auth.request;

import java.time.LocalDate;

public record SignupRequest(
        String loginId,
        String password,
        String name,
        String phoneNumber,
        LocalDate birth
) {}
