package com.Hamalog.security.encryption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("EncryptedLocalDateConverter Tests")
class EncryptedLocalDateConverterTest {

    @Mock
    private DataEncryptionUtil encryptionUtil;

    @InjectMocks
    private EncryptedLocalDateConverter converter;

    private LocalDate testDate;
    private String encryptedDateString;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2023, 12, 25);
        encryptedDateString = "encrypted_2023-12-25";
    }

    @Test
    @DisplayName("Should encrypt LocalDate when converting to database column")
    void convertToDatabaseColumn_ValidDate_EncryptsDate() {
        // given
        when(encryptionUtil.encrypt("2023-12-25")).thenReturn(encryptedDateString);

        // when
        String result = converter.convertToDatabaseColumn(testDate);

        // then
        assertThat(result).isEqualTo(encryptedDateString);
        verify(encryptionUtil).encrypt("2023-12-25");
    }

    @Test
    @DisplayName("Should return null when converting null date to database column")
    void convertToDatabaseColumn_NullDate_ReturnsNull() {
        // when
        String result = converter.convertToDatabaseColumn(null);

        // then
        assertThat(result).isNull();
        verify(encryptionUtil, never()).encrypt(anyString());
    }

    @Test
    @DisplayName("Should decrypt date when converting to entity attribute")
    void convertToEntityAttribute_ValidEncryptedData_DecryptsDate() {
        // given
        when(encryptionUtil.decrypt(encryptedDateString)).thenReturn("2023-12-25");

        // when
        LocalDate result = converter.convertToEntityAttribute(encryptedDateString);

        // then
        assertThat(result).isEqualTo(testDate);
        verify(encryptionUtil).decrypt(encryptedDateString);
    }

    @Test
    @DisplayName("Should return null when converting null encrypted data to entity attribute")
    void convertToEntityAttribute_NullData_ReturnsNull() {
        // when
        LocalDate result = converter.convertToEntityAttribute(null);

        // then
        assertThat(result).isNull();
        verify(encryptionUtil, never()).decrypt(anyString());
    }

    @Test
    @DisplayName("Should handle leap year date correctly")
    void convertToDatabaseColumn_LeapYearDate_HandlesCorrectly() {
        // given
        LocalDate leapYearDate = LocalDate.of(2020, 2, 29);
        String expectedEncrypted = "encrypted_2020-02-29";
        when(encryptionUtil.encrypt("2020-02-29")).thenReturn(expectedEncrypted);

        // when
        String result = converter.convertToDatabaseColumn(leapYearDate);

        // then
        assertThat(result).isEqualTo(expectedEncrypted);
        verify(encryptionUtil).encrypt("2020-02-29");
    }

    @Test
    @DisplayName("Should handle leap year date decryption correctly")
    void convertToEntityAttribute_LeapYearDate_HandlesCorrectly() {
        // given
        LocalDate expectedDate = LocalDate.of(2020, 2, 29);
        when(encryptionUtil.decrypt("encrypted_leap_year")).thenReturn("2020-02-29");

        // when
        LocalDate result = converter.convertToEntityAttribute("encrypted_leap_year");

        // then
        assertThat(result).isEqualTo(expectedDate);
        verify(encryptionUtil).decrypt("encrypted_leap_year");
    }

    @Test
    @DisplayName("Should handle minimum LocalDate value")
    void convertToDatabaseColumn_MinimumDate_HandlesCorrectly() {
        // given
        LocalDate minDate = LocalDate.MIN;
        String expectedEncrypted = "encrypted_min_date";
        when(encryptionUtil.encrypt(minDate.toString())).thenReturn(expectedEncrypted);

        // when
        String result = converter.convertToDatabaseColumn(minDate);

        // then
        assertThat(result).isEqualTo(expectedEncrypted);
        verify(encryptionUtil).encrypt(minDate.toString());
    }

    @Test
    @DisplayName("Should handle maximum LocalDate value")
    void convertToDatabaseColumn_MaximumDate_HandlesCorrectly() {
        // given
        LocalDate maxDate = LocalDate.MAX;
        String expectedEncrypted = "encrypted_max_date";
        when(encryptionUtil.encrypt(maxDate.toString())).thenReturn(expectedEncrypted);

        // when
        String result = converter.convertToDatabaseColumn(maxDate);

        // then
        assertThat(result).isEqualTo(expectedEncrypted);
        verify(encryptionUtil).encrypt(maxDate.toString());
    }

    @Test
    @DisplayName("Should handle current date")
    void convertToDatabaseColumn_CurrentDate_HandlesCorrectly() {
        // given
        LocalDate currentDate = LocalDate.now();
        String expectedEncrypted = "encrypted_current_date";
        when(encryptionUtil.encrypt(currentDate.toString())).thenReturn(expectedEncrypted);

        // when
        String result = converter.convertToDatabaseColumn(currentDate);

        // then
        assertThat(result).isEqualTo(expectedEncrypted);
        verify(encryptionUtil).encrypt(currentDate.toString());
    }

    @Test
    @DisplayName("Should handle year 2000 date correctly")
    void convertToDatabaseColumn_Year2000Date_HandlesCorrectly() {
        // given
        LocalDate y2kDate = LocalDate.of(2000, 1, 1);
        String expectedEncrypted = "encrypted_y2k";
        when(encryptionUtil.encrypt("2000-01-01")).thenReturn(expectedEncrypted);

        // when
        String result = converter.convertToDatabaseColumn(y2kDate);

        // then
        assertThat(result).isEqualTo(expectedEncrypted);
        verify(encryptionUtil).encrypt("2000-01-01");
    }

    @Test
    @DisplayName("Should handle decryption with various date formats")
    void convertToEntityAttribute_VariousDates_HandlesCorrectly() {
        // given
        LocalDate expectedDate1 = LocalDate.of(1995, 6, 15);
        LocalDate expectedDate2 = LocalDate.of(2030, 12, 31);
        
        when(encryptionUtil.decrypt("encrypted_1995")).thenReturn("1995-06-15");
        when(encryptionUtil.decrypt("encrypted_2030")).thenReturn("2030-12-31");

        // when
        LocalDate result1 = converter.convertToEntityAttribute("encrypted_1995");
        LocalDate result2 = converter.convertToEntityAttribute("encrypted_2030");

        // then
        assertThat(result1).isEqualTo(expectedDate1);
        assertThat(result2).isEqualTo(expectedDate2);
        verify(encryptionUtil).decrypt("encrypted_1995");
        verify(encryptionUtil).decrypt("encrypted_2030");
    }

    @Test
    @DisplayName("Should throw exception when decrypted data is not valid date format")
    void convertToEntityAttribute_InvalidDateFormat_ThrowsException() {
        // given
        when(encryptionUtil.decrypt("encrypted_invalid")).thenReturn("invalid-date-format");

        // when/then
        assertThatThrownBy(() -> converter.convertToEntityAttribute("encrypted_invalid"))
            .isInstanceOf(Exception.class);
        
        verify(encryptionUtil).decrypt("encrypted_invalid");
    }
}