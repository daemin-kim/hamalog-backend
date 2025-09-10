package com.Hamalog;

import com.Hamalog.config.TestRedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@Import(TestRedisConfig.class)
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "logging.level.org.hibernate.SQL=warn",
    "hamalog.encryption.key=+ZFRGoRl5CElrJfikdx1TmzQ3U8OJ+J6im5OMjuvsqE="
})
class HamalogApplicationTests {

    @Test
    void contextLoads() {
        // This test verifies that the Spring application context loads successfully
        // with the H2 database configuration, confirming the MySQL connection error is fixed
    }
}