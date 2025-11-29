package com.Hamalog.logging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utility for masking sensitive values before they are written to logs.
 */
public final class SensitiveDataMasker {

    private static final String MASK = "***";
    private static final Pattern SENSITIVE_KEY_PATTERN = Pattern.compile("(?i).*(password|secret|token|credential|authorization|session|cookie|ssn|phone|email|account|birth|resident|card|pin).*");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("(^[^@]+)@(.+$)");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(010|011|016|017|018|019)\\d{7,8}$");
    private static final Pattern NUMERIC_ID_PATTERN = Pattern.compile("\\d{6,}");
    private static final Set<String> SENSITIVE_HEADER_NAMES = Set.of(
            "authorization", "cookie", "set-cookie", "x-api-key", "x-forwarded-for",
            "x-refresh-token", "refresh-token", "x-id-token", "x-request-token"
    );

    private SensitiveDataMasker() {
        // Utility class
    }

    public static void mask(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return;
        }
        maskMap(payload);
    }

    @SuppressWarnings("unchecked")
    private static void maskMap(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : new ArrayList<>(map.entrySet())) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (key != null && isSensitiveKey(key)) {
                entry.setValue(MASK);
                continue;
            }

            if (value instanceof String stringValue && isSensitiveValue(stringValue)) {
                entry.setValue(MASK);
                continue;
            }

            if (value instanceof Map<?, ?> childMap) {
                // Defensive copy to avoid ClassCastException when original value isn't Map<String, Object>
                Map<String, Object> nested = new HashMap<>();
                childMap.forEach((childKey, childValue) -> nested.put(String.valueOf(childKey), childValue));
                maskMap(nested);
                entry.setValue(nested);
            } else if (value instanceof Collection<?> collection) {
                entry.setValue(maskCollection(collection));
            } else if (value instanceof Object[] array) {
                entry.setValue(maskArray(array));
            }
        }
    }

    private static List<Object> maskCollection(Collection<?> collection) {
        List<Object> sanitized = new ArrayList<>(collection.size());
        for (Object element : collection) {
            sanitized.add(maskNestedValue(element));
        }
        return sanitized;
    }

    private static List<Object> maskArray(Object[] array) {
        List<Object> sanitized = new ArrayList<>(array.length);
        for (Object element : array) {
            sanitized.add(maskNestedValue(element));
        }
        return sanitized;
    }

    private static Object maskNestedValue(Object value) {
        if (value instanceof Map<?, ?> mapValue) {
            Map<String, Object> nested = new HashMap<>();
            mapValue.forEach((k, v) -> nested.put(String.valueOf(k), v));
            maskMap(nested);
            return nested;
        }
        if (value instanceof Collection<?> collection) {
            return maskCollection(collection);
        }
        if (value instanceof Object[] array) {
            return maskArray(array);
        }
        return value;
    }

    public static String maskEmail(String email) {
        if (email == null) {
            return MASK;
        }
        var matcher = EMAIL_PATTERN.matcher(email);
        if (matcher.matches()) {
            String local = matcher.group(1);
            String domain = matcher.group(2);
            String maskedLocal = local.length() <= 2 ? MASK : local.substring(0, 2) + "***";
            return maskedLocal + "@" + domain;
        }
        return MASK;
    }

    public static String maskUserId(Long userId) {
        if (userId == null) {
            return MASK;
        }
        String idStr = String.valueOf(userId);
        if (idStr.length() <= 2) {
            return MASK;
        }
        return idStr.substring(0, idStr.length() - 2) + "**";
    }

    private static boolean isSensitiveKey(String key) {
        return key != null && SENSITIVE_KEY_PATTERN.matcher(key).matches();
    }

    private static boolean isSensitiveValue(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.length() > 256) {
            trimmed = trimmed.substring(0, 256);
        }
        if (trimmed.contains("@")) {
            return true;
        }
        if (PHONE_PATTERN.matcher(trimmed).matches()) {
            return true;
        }
        return NUMERIC_ID_PATTERN.matcher(trimmed).matches();
    }

    public static Map<String, String> maskHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return headers;
        }
        Map<String, String> sanitized = new HashMap<>(headers.size());
        headers.forEach((key, value) -> {
            if (key == null) {
                return;
            }
            String lower = key.toLowerCase();
            if (SENSITIVE_HEADER_NAMES.contains(lower) || isSensitiveKey(lower)) {
                sanitized.put(key, MASK);
            } else if (value != null && isSensitiveValue(value)) {
                sanitized.put(key, MASK);
            } else {
                sanitized.put(key, value);
            }
        });
        return sanitized;
    }

    public static Map<String, Object> maskParameters(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return parameters;
        }
        Map<String, Object> copy = new HashMap<>(parameters);
        mask(copy);
        return copy;
    }

    public static Map<String, Object> maskHeadersIfPresent(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return parameters;
        }
        if (parameters.containsKey("headers") && parameters.get("headers") instanceof Map<?, ?> headerMap) {
            Map<String, String> headers = new HashMap<>();
            headerMap.forEach((k, v) -> headers.put(String.valueOf(k), v == null ? null : String.valueOf(v)));
            parameters.put("headers", maskHeaders(headers));
        }
        mask(parameters);
        return parameters;
    }
}
