package com.Hamalog.dto.auth.request;

public record LoginRequest(
        String loginId,
        String password
) {}
