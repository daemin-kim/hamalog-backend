package com.Hamalog.security.encryption;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Converter
public class EncryptedLocalDateConverter implements AttributeConverter<LocalDate, String> {

    private final DataEncryptionUtil encryptionUtil;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    @Autowired
    public EncryptedLocalDateConverter(DataEncryptionUtil encryptionUtil) {
        this.encryptionUtil = encryptionUtil;
    }

    @Override
    public String convertToDatabaseColumn(LocalDate attribute) {
        if (attribute == null) {
            return null;
        }
        String dateString = attribute.format(FORMATTER);
        return encryptionUtil.encrypt(dateString);
    }

    @Override
    public LocalDate convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        String decryptedDateString = encryptionUtil.decrypt(dbData);
        return LocalDate.parse(decryptedDateString, FORMATTER);
    }
}