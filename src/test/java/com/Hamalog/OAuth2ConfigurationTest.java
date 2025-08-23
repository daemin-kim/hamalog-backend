package com.Hamalog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
    "spring.profiles.active=local",
    "spring.main.web-application-type=none"
})
public class OAuth2ConfigurationTest {
    
    @Test
    public void contextLoads() {
        // This test passes if the Spring context loads successfully
        // without any OAuth2 "client id must not be empty" errors
        System.out.println("[DEBUG_LOG] OAuth2 configuration test passed - no client ID errors");
    }
}
