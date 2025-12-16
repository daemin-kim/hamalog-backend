package com.Hamalog.service.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageService Tests")
class MessageServiceTest {

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private MessageService messageService;

    @BeforeEach
    void setUp() {
        // Reset locale context to default for consistent test behavior
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("Should return localized message when code exists")
    void getMessage_ValidCode_ReturnsMessage() {
        // given
        String code = "test.message";
        String expectedMessage = "Test message";
        Object[] args = {"arg1", "arg2"};
        
        given(messageSource.getMessage(eq(code), eq(args), any(Locale.class)))
                .willReturn(expectedMessage);

        // when
        String result = messageService.getMessage(code, args);

        // then
        assertThat(result).isEqualTo(expectedMessage);
        verify(messageSource).getMessage(eq(code), eq(args), any(Locale.class));
    }

    @Test
    @DisplayName("Should return code as fallback when message not found")
    void getMessage_InvalidCode_ReturnsCodeAsFallback() {
        // given
        String code = "invalid.code";
        Object[] args = {};
        
        given(messageSource.getMessage(eq(code), eq(args), any(Locale.class)))
                .willThrow(new NoSuchMessageException(code));

        // when
        String result = messageService.getMessage(code, args);

        // then
        assertThat(result).isEqualTo(code);
        verify(messageSource).getMessage(eq(code), eq(args), any(Locale.class));
    }

    @Test
    @DisplayName("Should return localized message with specific locale")
    void getMessageWithLocale_ValidCode_ReturnsMessage() {
        // given
        String code = "test.message";
        Locale locale = Locale.FRENCH;
        String expectedMessage = "Message de test";
        Object[] args = {"arg1"};
        
        given(messageSource.getMessage(code, args, locale))
                .willReturn(expectedMessage);

        // when
        String result = messageService.getMessage(code, locale, args);

        // then
        assertThat(result).isEqualTo(expectedMessage);
        verify(messageSource).getMessage(code, args, locale);
    }

    @Test
    @DisplayName("Should return code as fallback when message not found with specific locale")
    void getMessageWithLocale_InvalidCode_ReturnsCodeAsFallback() {
        // given
        String code = "invalid.code";
        Locale locale = Locale.GERMAN;
        Object[] args = {};
        
        given(messageSource.getMessage(code, args, locale))
                .willThrow(new NoSuchMessageException(code));

        // when
        String result = messageService.getMessage(code, locale, args);

        // then
        assertThat(result).isEqualTo(code);
        verify(messageSource).getMessage(code, args, locale);
    }

    @Test
    @DisplayName("Should return message when using default message method")
    void getMessageWithDefault_ValidCode_ReturnsMessage() {
        // given
        String code = "test.message";
        String defaultMessage = "Default message";
        String expectedMessage = "Actual message";
        Object[] args = {"arg1"};
        
        given(messageSource.getMessage(eq(code), eq(args), eq(defaultMessage), any(Locale.class)))
                .willReturn(expectedMessage);

        // when
        String result = messageService.getMessageWithDefault(code, defaultMessage, args);

        // then
        assertThat(result).isEqualTo(expectedMessage);
        verify(messageSource).getMessage(eq(code), eq(args), eq(defaultMessage), any(Locale.class));
    }

    @Test
    @DisplayName("Should return default message when code not found")
    void getMessageWithDefault_InvalidCode_ReturnsDefaultMessage() {
        // given
        String code = "invalid.code";
        String defaultMessage = "Default message";
        Object[] args = {};
        
        given(messageSource.getMessage(eq(code), eq(args), eq(defaultMessage), any(Locale.class)))
                .willThrow(new RuntimeException("Message not found"));

        // when
        String result = messageService.getMessageWithDefault(code, defaultMessage, args);

        // then
        assertThat(result).isEqualTo(defaultMessage);
        verify(messageSource).getMessage(eq(code), eq(args), eq(defaultMessage), any(Locale.class));
    }

    @Test
    @DisplayName("Should return Korean message")
    void getKoreanMessage_ValidCode_ReturnsKoreanMessage() {
        // given
        String code = "greeting.hello";
        String expectedMessage = "안녕하세요";
        Object[] args = {"홍길동"};
        
        given(messageSource.getMessage(code, args, Locale.KOREAN))
                .willReturn(expectedMessage);

        // when
        String result = messageService.getKoreanMessage(code, args);

        // then
        assertThat(result).isEqualTo(expectedMessage);
        verify(messageSource).getMessage(code, args, Locale.KOREAN);
    }

    @Test
    @DisplayName("Should return English message")
    void getEnglishMessage_ValidCode_ReturnsEnglishMessage() {
        // given
        String code = "greeting.hello";
        String expectedMessage = "Hello";
        Object[] args = {"John"};
        
        given(messageSource.getMessage(code, args, Locale.ENGLISH))
                .willReturn(expectedMessage);

        // when
        String result = messageService.getEnglishMessage(code, args);

        // then
        assertThat(result).isEqualTo(expectedMessage);
        verify(messageSource).getMessage(code, args, Locale.ENGLISH);
    }

    @Test
    @DisplayName("Should return true when message code exists")
    void hasMessage_ValidCode_ReturnsTrue() {
        // given
        String code = "existing.message";
        
        given(messageSource.getMessage(eq(code), eq(null), any(Locale.class)))
                .willReturn("Some message");

        // when
        boolean result = messageService.hasMessage(code);

        // then
        assertThat(result).isTrue();
        verify(messageSource).getMessage(eq(code), eq(null), any(Locale.class));
    }

    @Test
    @DisplayName("Should return false when message code does not exist")
    void hasMessage_InvalidCode_ReturnsFalse() {
        // given
        String code = "non.existing.message";
        
        given(messageSource.getMessage(eq(code), eq(null), any(Locale.class)))
                .willThrow(new NoSuchMessageException(code));

        // when
        boolean result = messageService.hasMessage(code);

        // then
        assertThat(result).isFalse();
        verify(messageSource).getMessage(eq(code), eq(null), any(Locale.class));
    }

    @Test
    @DisplayName("Should handle null arguments gracefully")
    void getMessage_NullArguments_HandlesGracefully() {
        // given
        String code = "test.message";
        String expectedMessage = "Test message";
        
        given(messageSource.getMessage(eq(code), eq(null), any(Locale.class)))
                .willReturn(expectedMessage);

        // when
        String result = messageService.getMessage(code, (Object[]) null);

        // then
        assertThat(result).isEqualTo(expectedMessage);
        verify(messageSource).getMessage(eq(code), eq(null), any(Locale.class));
    }

    @Test
    @DisplayName("Should handle empty message code")
    void getMessage_EmptyCode_ReturnsCodeAsFallback() {
        // given
        String code = "";
        
        given(messageSource.getMessage(eq(code), any(), any(Locale.class)))
                .willThrow(new NoSuchMessageException(code));

        // when
        String result = messageService.getMessage(code);

        // then
        assertThat(result).isEqualTo(code);
        verify(messageSource).getMessage(eq(code), any(), any(Locale.class));
    }

    @Test
    @DisplayName("Should use current locale context when no locale specified")
    void getMessage_NoLocaleSpecified_UsesCurrentLocale() {
        // given
        String code = "test.message";
        String expectedMessage = "Test message";
        Locale testLocale = Locale.JAPANESE;
        LocaleContextHolder.setLocale(testLocale);
        
        given(messageSource.getMessage(code, new Object[0], testLocale))
                .willReturn(expectedMessage);

        // when
        String result = messageService.getMessage(code);

        // then
        assertThat(result).isEqualTo(expectedMessage);
        verify(messageSource).getMessage(code, new Object[0], testLocale);
        
        // cleanup
        LocaleContextHolder.resetLocaleContext();
    }
}