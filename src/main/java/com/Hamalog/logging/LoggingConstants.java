package com.Hamalog.logging;

import java.util.Set;

/**
 * Shared logging-related constants to keep sensitive-key lists in sync across components.
 */
public final class LoggingConstants {

    private LoggingConstants() {
    }

    public static final String RESPONSE_STATUS_ATTRIBUTE = "hamalog.response.status";
    public static final String API_EVENT_LOGGED_ATTRIBUTE = "hamalog.logging.api.event.logged";
    public static final String API_LOGGING_OWNER_ATTRIBUTE = "hamalog.logging.api.owner";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    public static final Set<String> SENSITIVE_PARAM_KEYWORDS = Set.of(
            "password", "pass", "pwd", "token", "refresh", "authorization",
            "secret", "credential", "auth", "card", "pin", "birth", "resident"
    );
}
