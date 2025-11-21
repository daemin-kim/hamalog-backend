package com.Hamalog.logging;

import lombok.experimental.UtilityClass;

/**
 * 로그에서 민감정보를 마스킹하는 유틸리티 클래스
 */
@UtilityClass
public class SensitiveDataMasker {

    private static final String MASK_CHAR = "*";
    private static final int VISIBLE_PREFIX_LENGTH = 3;
    private static final int VISIBLE_SUFFIX_LENGTH = 2;

    /**
     * 이메일 주소 마스킹
     * 예: user@example.com -> use***@ex***le.com
     */
    public static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "N/A";
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return maskString(email);
        }

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex + 1);

        String maskedLocal = maskString(localPart);
        String maskedDomain = maskString(domain);

        return maskedLocal + "@" + maskedDomain;
    }

    /**
     * 전화번호 마스킹
     * 예: 01012345678 -> 010****5678
     */
    public static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return "N/A";
        }

        if (phoneNumber.length() < 7) {
            return MASK_CHAR.repeat(phoneNumber.length());
        }

        String prefix = phoneNumber.substring(0, 3);
        String suffix = phoneNumber.substring(phoneNumber.length() - 4);
        String masked = MASK_CHAR.repeat(phoneNumber.length() - 7);

        return prefix + masked + suffix;
    }

    /**
     * 사용자 ID 마스킹
     * 예: 123456 -> 12***6
     */
    public static String maskUserId(Long userId) {
        if (userId == null) {
            return "N/A";
        }
        return maskString(userId.toString());
    }

    /**
     * 일반 문자열 마스킹
     * 예: abcdefgh -> abc***gh
     */
    public static String maskString(String str) {
        if (str == null || str.isEmpty()) {
            return "N/A";
        }

        int length = str.length();

        if (length <= VISIBLE_PREFIX_LENGTH + VISIBLE_SUFFIX_LENGTH) {
            return MASK_CHAR.repeat(length);
        }

        String prefix = str.substring(0, VISIBLE_PREFIX_LENGTH);
        String suffix = str.substring(length - VISIBLE_SUFFIX_LENGTH);
        String masked = MASK_CHAR.repeat(Math.min(3, length - VISIBLE_PREFIX_LENGTH - VISIBLE_SUFFIX_LENGTH));

        return prefix + masked + suffix;
    }

    /**
     * 토큰 마스킹
     * 예: eyJhbGciOiJIUzI1NiJ9... -> eyJ***9 (앞 3자, 뒤 1자만 표시)
     */
    public static String maskToken(String token) {
        if (token == null || token.isEmpty()) {
            return "N/A";
        }

        if (token.length() <= 4) {
            return MASK_CHAR.repeat(token.length());
        }

        return token.substring(0, 3) + MASK_CHAR.repeat(3) + token.substring(token.length() - 1);
    }

    /**
     * 비밀번호 완전 마스킹
     */
    public static String maskPassword() {
        return "********";
    }

    /**
     * IP 주소 마스킹
     * 예: 192.168.1.100 -> 192.168.***.***
     */
    public static String maskIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return "N/A";
        }

        String[] parts = ipAddress.split("\\.");
        if (parts.length != 4) {
            return maskString(ipAddress);
        }

        return parts[0] + "." + parts[1] + ".***. ***";
    }
}

