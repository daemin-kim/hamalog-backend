package com.Hamalog.logging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Utility for masking sensitive values before they are written to logs.
 */
public final class SensitiveDataMasker {

    private static final String MASK = "***";
    private static final Pattern SENSITIVE_KEY_PATTERN = Pattern.compile("(?i).*(password|secret|token|credential|authorization|session|cookie|ssn|phone|email|account).*");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("(^[^@]+)@(.+$)");

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

            if (key != null && SENSITIVE_KEY_PATTERN.matcher(key).matches()) {
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
}
