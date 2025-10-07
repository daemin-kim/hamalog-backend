package com.Hamalog.config;

import com.Hamalog.security.encryption.DataEncryptionUtil;
import com.Hamalog.security.encryption.EncryptedLocalDateConverter;
import com.Hamalog.security.encryption.EncryptedStringConverter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Test configuration for encryption utilities needed in repository tests
 */
@TestConfiguration
public class TestEncryptionConfig {

    @Bean
    public DataEncryptionUtil dataEncryptionUtil(Environment environment) {
        // Use the test encryption key from application-test.properties
        String testEncryptionKey = "+ZFRGoRl5CElrJfikdx1TmzQ3U8OJ+J6im5OMjuvsqE=";
        return new DataEncryptionUtil(testEncryptionKey, environment);
    }

    @Bean
    public EncryptedStringConverter encryptedStringConverter(DataEncryptionUtil dataEncryptionUtil) {
        return new EncryptedStringConverter(dataEncryptionUtil);
    }

    @Bean
    public EncryptedLocalDateConverter encryptedLocalDateConverter(DataEncryptionUtil dataEncryptionUtil) {
        return new EncryptedLocalDateConverter(dataEncryptionUtil);
    }
}