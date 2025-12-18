package com.Hamalog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "hamalog.rate-limit")
@Data
public class RateLimitProperties {
    private Auth auth = new Auth();
    private Api api = new Api();
    private long degradeSeconds = 300;
    private boolean metricsEnabled = true;

    @Data
    public static class Auth {
        private int perMinute = 5;
        private int perHour = 20;
    }

    @Data
    public static class Api {
        private int perMinute = 60;
        private int perHour = 1000;
    }
}
