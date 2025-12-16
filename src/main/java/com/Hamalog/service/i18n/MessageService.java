package com.Hamalog.service.i18n;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

/**
 * Service for internationalization message handling.
 * Provides convenient methods to retrieve localized messages.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageSource messageSource;

    /**
     * Gets a localized message using the current locale context.
     *
     * @param code the message code
     * @param args optional arguments for message formatting
     * @return the localized message
     */
    public String getMessage(String code, Object... args) {
        try {
            Locale locale = LocaleContextHolder.getLocale();
            return messageSource.getMessage(code, args, locale);
        } catch (Exception e) {
            log.warn("Failed to get message for code '{}' with locale '{}': {}", 
                    code, LocaleContextHolder.getLocale(), e.getMessage());
            return code; // Return the code itself as fallback
        }
    }

    /**
     * Gets a localized message with a specific locale.
     *
     * @param code the message code
     * @param locale the target locale
     * @param args optional arguments for message formatting
     * @return the localized message
     */
    public String getMessage(String code, Locale locale, Object... args) {
        try {
            return messageSource.getMessage(code, args, locale);
        } catch (Exception e) {
            log.warn("Failed to get message for code '{}' with locale '{}': {}", 
                    code, locale, e.getMessage());
            return code; // Return the code itself as fallback
        }
    }

    /**
     * Gets a localized message with a default value if the code is not found.
     *
     * @param code the message code
     * @param defaultMessage the default message to return if code is not found
     * @param args optional arguments for message formatting
     * @return the localized message or default message
     */
    public String getMessageWithDefault(String code, String defaultMessage, Object... args) {
        try {
            Locale locale = LocaleContextHolder.getLocale();
            return messageSource.getMessage(code, args, defaultMessage, locale);
        } catch (Exception e) {
            log.warn("Failed to get message for code '{}' with locale '{}': {}", 
                    code, LocaleContextHolder.getLocale(), e.getMessage());
            return defaultMessage;
        }
    }

    /**
     * Gets a Korean message explicitly.
     * Useful for ensuring Korean locale in specific contexts.
     *
     * @param code the message code
     * @param args optional arguments for message formatting
     * @return the Korean localized message
     */
    public String getKoreanMessage(String code, Object... args) {
        return getMessage(code, Locale.KOREAN, args);
    }

    /**
     * Gets an English message explicitly.
     * Useful for ensuring English locale in specific contexts.
     *
     * @param code the message code
     * @param args optional arguments for message formatting
     * @return the English localized message
     */
    public String getEnglishMessage(String code, Object... args) {
        return getMessage(code, Locale.ENGLISH, args);
    }

    /**
     * Checks if a message code exists in the message source.
     *
     * @param code the message code to check
     * @return true if the code exists, false otherwise
     */
    public boolean hasMessage(String code) {
        try {
            Locale locale = LocaleContextHolder.getLocale();
            messageSource.getMessage(code, null, locale);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}