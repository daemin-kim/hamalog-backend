package com.Hamalog.security.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EncryptedStringConverter Tests")
class EncryptedStringConverterTest {

    @Mock
    private DataEncryptionUtil dataEncryptionUtil;

    private EncryptedStringConverter encryptedStringConverter;

    @BeforeEach
    void setUp() {
        encryptedStringConverter = new EncryptedStringConverter(dataEncryptionUtil);
    }

    @Test
    @DisplayName("Should encrypt string when converting to database column")
    void convertToDatabaseColumn_ValidString_ReturnsEncryptedString() {
        // given
        String plainText = "sensitive-data";
        String encryptedText = "encrypted-sensitive-data";
        
        when(dataEncryptionUtil.encrypt(plainText)).thenReturn(encryptedText);

        // when
        String result = encryptedStringConverter.convertToDatabaseColumn(plainText);

        // then
        assertThat(result).isEqualTo(encryptedText);
        verify(dataEncryptionUtil).encrypt(plainText);
    }

    @Test
    @DisplayName("Should return null when converting null string to database column")
    void convertToDatabaseColumn_NullString_ReturnsNull() {
        // when
        String result = encryptedStringConverter.convertToDatabaseColumn(null);

        // then
        assertThat(result).isNull();
        verify(dataEncryptionUtil, never()).encrypt(anyString());
    }

    @Test
    @DisplayName("Should encrypt empty string when converting to database column")
    void convertToDatabaseColumn_EmptyString_ReturnsEncryptedString() {
        // given
        String emptyString = "";
        String encryptedEmptyString = "encrypted-empty";
        
        when(dataEncryptionUtil.encrypt(emptyString)).thenReturn(encryptedEmptyString);

        // when
        String result = encryptedStringConverter.convertToDatabaseColumn(emptyString);

        // then
        assertThat(result).isEqualTo(encryptedEmptyString);
        verify(dataEncryptionUtil).encrypt(emptyString);
    }

    @Test
    @DisplayName("Should decrypt string when converting to entity attribute")
    void convertToEntityAttribute_ValidEncryptedString_ReturnsDecryptedString() {
        // given
        String encryptedText = "encrypted-sensitive-data";
        String plainText = "sensitive-data";
        
        when(dataEncryptionUtil.decrypt(encryptedText)).thenReturn(plainText);

        // when
        String result = encryptedStringConverter.convertToEntityAttribute(encryptedText);

        // then
        assertThat(result).isEqualTo(plainText);
        verify(dataEncryptionUtil).decrypt(encryptedText);
    }

    @Test
    @DisplayName("Should return null when converting null encrypted string to entity attribute")
    void convertToEntityAttribute_NullString_ReturnsNull() {
        // when
        String result = encryptedStringConverter.convertToEntityAttribute(null);

        // then
        assertThat(result).isNull();
        verify(dataEncryptionUtil, never()).decrypt(anyString());
    }

    @Test
    @DisplayName("Should decrypt empty encrypted string when converting to entity attribute")
    void convertToEntityAttribute_EmptyEncryptedString_ReturnsDecryptedString() {
        // given
        String encryptedEmptyString = "encrypted-empty";
        String emptyString = "";
        
        when(dataEncryptionUtil.decrypt(encryptedEmptyString)).thenReturn(emptyString);

        // when
        String result = encryptedStringConverter.convertToEntityAttribute(encryptedEmptyString);

        // then
        assertThat(result).isEqualTo(emptyString);
        verify(dataEncryptionUtil).decrypt(encryptedEmptyString);
    }

    @Test
    @DisplayName("Should handle special characters in string encryption")
    void convertToDatabaseColumn_SpecialCharacters_ReturnsEncryptedString() {
        // given
        String specialChars = "Hello @#$%^&*()_+ 한글 тест";
        String encryptedSpecialChars = "encrypted-special-chars";
        
        when(dataEncryptionUtil.encrypt(specialChars)).thenReturn(encryptedSpecialChars);

        // when
        String result = encryptedStringConverter.convertToDatabaseColumn(specialChars);

        // then
        assertThat(result).isEqualTo(encryptedSpecialChars);
        verify(dataEncryptionUtil).encrypt(specialChars);
    }

    @Test
    @DisplayName("Should handle special characters in string decryption")
    void convertToEntityAttribute_SpecialCharacters_ReturnsDecryptedString() {
        // given
        String encryptedSpecialChars = "encrypted-special-chars";
        String specialChars = "Hello @#$%^&*()_+ 한글 тест";
        
        when(dataEncryptionUtil.decrypt(encryptedSpecialChars)).thenReturn(specialChars);

        // when
        String result = encryptedStringConverter.convertToEntityAttribute(encryptedSpecialChars);

        // then
        assertThat(result).isEqualTo(specialChars);
        verify(dataEncryptionUtil).decrypt(encryptedSpecialChars);
    }

    @Test
    @DisplayName("Should handle long string encryption")
    void convertToDatabaseColumn_LongString_ReturnsEncryptedString() {
        // given
        String longString = "a".repeat(1000); // 1000 character string
        String encryptedLongString = "encrypted-long-string";
        
        when(dataEncryptionUtil.encrypt(longString)).thenReturn(encryptedLongString);

        // when
        String result = encryptedStringConverter.convertToDatabaseColumn(longString);

        // then
        assertThat(result).isEqualTo(encryptedLongString);
        verify(dataEncryptionUtil).encrypt(longString);
    }

    @Test
    @DisplayName("Should handle long string decryption")
    void convertToEntityAttribute_LongString_ReturnsDecryptedString() {
        // given
        String encryptedLongString = "encrypted-long-string";
        String longString = "a".repeat(1000); // 1000 character string
        
        when(dataEncryptionUtil.decrypt(encryptedLongString)).thenReturn(longString);

        // when
        String result = encryptedStringConverter.convertToEntityAttribute(encryptedLongString);

        // then
        assertThat(result).isEqualTo(longString);
        verify(dataEncryptionUtil).decrypt(encryptedLongString);
    }

    @Test
    @DisplayName("Should handle whitespace-only string encryption")
    void convertToDatabaseColumn_WhitespaceOnlyString_ReturnsEncryptedString() {
        // given
        String whitespaceString = "   \t\n   ";
        String encryptedWhitespaceString = "encrypted-whitespace";
        
        when(dataEncryptionUtil.encrypt(whitespaceString)).thenReturn(encryptedWhitespaceString);

        // when
        String result = encryptedStringConverter.convertToDatabaseColumn(whitespaceString);

        // then
        assertThat(result).isEqualTo(encryptedWhitespaceString);
        verify(dataEncryptionUtil).encrypt(whitespaceString);
    }

    @Test
    @DisplayName("Should handle whitespace-only string decryption")
    void convertToEntityAttribute_WhitespaceOnlyString_ReturnsDecryptedString() {
        // given
        String encryptedWhitespaceString = "encrypted-whitespace";
        String whitespaceString = "   \t\n   ";
        
        when(dataEncryptionUtil.decrypt(encryptedWhitespaceString)).thenReturn(whitespaceString);

        // when
        String result = encryptedStringConverter.convertToEntityAttribute(encryptedWhitespaceString);

        // then
        assertThat(result).isEqualTo(whitespaceString);
        verify(dataEncryptionUtil).decrypt(encryptedWhitespaceString);
    }
}