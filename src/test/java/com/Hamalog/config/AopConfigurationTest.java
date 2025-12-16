package com.Hamalog.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

@DisplayName("AopConfiguration Tests")
class AopConfigurationTest {

    private AopConfiguration aopConfiguration;

    @BeforeEach
    void setUp() {
        aopConfiguration = new AopConfiguration();
    }

    @Test
    @DisplayName("Should create AopConfiguration with default values")
    void constructor_DefaultValues_SetsCorrectDefaults() {
        // when/then
        assertThat(aopConfiguration).isNotNull();
        assertThat(aopConfiguration.getApiLogging()).isNotNull();
        assertThat(aopConfiguration.getPerformance()).isNotNull();
        assertThat(aopConfiguration.getAudit()).isNotNull();
        assertThat(aopConfiguration.getRetry()).isNotNull();
        assertThat(aopConfiguration.getCache()).isNotNull();
    }

    @Test
    @DisplayName("Should have correct default values for ApiLogging")
    void apiLogging_DefaultValues_SetsCorrectDefaults() {
        // given
        AopConfiguration.ApiLogging apiLogging = aopConfiguration.getApiLogging();

        // then
        assertThat(apiLogging.isEnabled()).isTrue();
        assertThat(apiLogging.getMaxResponseLength()).isEqualTo(200);
        assertThat(apiLogging.getMaxParameterLength()).isEqualTo(200);
        assertThat(apiLogging.getSensitiveKeywords()).containsExactly(
            "password", "token", "authorization", "secret", "key"
        );
        assertThat(apiLogging.getPerformance()).isNotNull();
        assertThat(apiLogging.getPerformance().getVeryFastThreshold()).isEqualTo(100);
        assertThat(apiLogging.getPerformance().getFastThreshold()).isEqualTo(500);
        assertThat(apiLogging.getPerformance().getModerateThreshold()).isEqualTo(1000);
        assertThat(apiLogging.getPerformance().getSlowThreshold()).isEqualTo(3000);
    }

    @Test
    @DisplayName("Should have correct default values for Performance")
    void performance_DefaultValues_SetsCorrectDefaults() {
        // given
        AopConfiguration.Performance performance = aopConfiguration.getPerformance();

        // then
        assertThat(performance.isEnabled()).isTrue();
        assertThat(performance.getSlowThreshold()).isEqualTo(1000);
        assertThat(performance.getVerySlowThreshold()).isEqualTo(3000);
        assertThat(performance.isCollectStats()).isTrue();
        assertThat(performance.getStatsResetInterval()).isEqualTo(24);
        assertThat(performance.getLogLevel()).isEqualTo("DEBUG");
        assertThat(performance.isEnableNanoPrecision()).isTrue();
    }

    @Test
    @DisplayName("Should have correct default values for Audit")
    void audit_DefaultValues_SetsCorrectDefaults() {
        // given
        AopConfiguration.Audit audit = aopConfiguration.getAudit();

        // then
        assertThat(audit.isEnabled()).isTrue();
        assertThat(audit.isAuditLoginFailures()).isTrue();
        assertThat(audit.isAuditDataChanges()).isTrue();
        assertThat(audit.isCollectIpAddress()).isTrue();
        assertThat(audit.isCollectUserAgent()).isTrue();
        assertThat(audit.getMaxParameterLength()).isEqualTo(100);
        assertThat(audit.getMaxResultLength()).isEqualTo(100);
        assertThat(audit.isMaskSensitiveParams()).isTrue();
        assertThat(audit.getLogLevel()).isEqualTo("INFO");
    }

    @Test
    @DisplayName("Should have correct default values for Retry")
    void retry_DefaultValues_SetsCorrectDefaults() {
        // given
        AopConfiguration.Retry retry = aopConfiguration.getRetry();

        // then
        assertThat(retry.isEnabled()).isTrue();
        assertThat(retry.getDefaultMaxAttempts()).isEqualTo(3);
        assertThat(retry.getDefaultDelay()).isEqualTo(1000);
        assertThat(retry.getMaxDelay()).isEqualTo(10000);
        assertThat(retry.getDefaultBackoffMultiplier()).isEqualTo(2.0);
        assertThat(retry.isDefaultRandomizeDelay()).isTrue();
        assertThat(retry.isCollectStats()).isTrue();
        assertThat(retry.getLogLevel()).isEqualTo("WARN");
    }

    @Test
    @DisplayName("Should have correct default values for Cache")
    void cache_DefaultValues_SetsCorrectDefaults() {
        // given
        AopConfiguration.Cache cache = aopConfiguration.getCache();

        // then
        assertThat(cache.isEnabled()).isTrue();
        assertThat(cache.getDefaultTtl()).isEqualTo(300);
        assertThat(cache.getMaxTtl()).isEqualTo(86400);
        assertThat(cache.isDefaultUseLocalFallback()).isTrue();
        assertThat(cache.getLocalCacheMaxSize()).isEqualTo(1000);
        assertThat(cache.getLocalCacheCleanupInterval()).isEqualTo(30);
        assertThat(cache.isDefaultCacheNull()).isFalse();
        assertThat(cache.isCollectStats()).isTrue();
        assertThat(cache.getMaxKeyLength()).isEqualTo(250);
        assertThat(cache.getLogLevel()).isEqualTo("DEBUG");
        assertThat(cache.isFallbackOnRedisFailure()).isTrue();
    }

    @Test
    @DisplayName("Should bind properties from configuration")
    void propertyBinding_WithCustomValues_BindsCorrectly() {
        // given
        Map<String, Object> properties = new HashMap<>();
        properties.put("app.aop.api-logging.enabled", "false");
        properties.put("app.aop.api-logging.max-response-length", "500");
        properties.put("app.aop.api-logging.max-parameter-length", "300");
        properties.put("app.aop.api-logging.sensitive-keywords", "pwd,secret,auth");
        properties.put("app.aop.performance.enabled", "false");
        properties.put("app.aop.performance.slow-threshold", "2000");
        properties.put("app.aop.audit.enabled", "false");
        properties.put("app.aop.retry.enabled", "false");
        properties.put("app.aop.cache.enabled", "false");

        ConfigurationPropertySource source = new MapConfigurationPropertySource(properties);
        Binder binder = new Binder(source);

        // when
        AopConfiguration boundConfig = binder.bind("app.aop", AopConfiguration.class).get();

        // then
        assertThat(boundConfig.getApiLogging().isEnabled()).isFalse();
        assertThat(boundConfig.getApiLogging().getMaxResponseLength()).isEqualTo(500);
        assertThat(boundConfig.getApiLogging().getMaxParameterLength()).isEqualTo(300);
        assertThat(boundConfig.getApiLogging().getSensitiveKeywords()).containsExactly("pwd", "secret", "auth");
        assertThat(boundConfig.getPerformance().isEnabled()).isFalse();
        assertThat(boundConfig.getPerformance().getSlowThreshold()).isEqualTo(2000);
        assertThat(boundConfig.getAudit().isEnabled()).isFalse();
        assertThat(boundConfig.getRetry().isEnabled()).isFalse();
        assertThat(boundConfig.getCache().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Should bind nested performance thresholds in ApiLogging")
    void propertyBinding_ApiLoggingPerformance_BindsCorrectly() {
        // given
        Map<String, Object> properties = new HashMap<>();
        properties.put("app.aop.api-logging.performance.very-fast-threshold", "50");
        properties.put("app.aop.api-logging.performance.fast-threshold", "200");
        properties.put("app.aop.api-logging.performance.moderate-threshold", "800");
        properties.put("app.aop.api-logging.performance.slow-threshold", "2500");

        ConfigurationPropertySource source = new MapConfigurationPropertySource(properties);
        Binder binder = new Binder(source);

        // when
        AopConfiguration boundConfig = binder.bind("app.aop", AopConfiguration.class).get();

        // then
        AopConfiguration.ApiLogging.Performance performance = boundConfig.getApiLogging().getPerformance();
        assertThat(performance.getVeryFastThreshold()).isEqualTo(50);
        assertThat(performance.getFastThreshold()).isEqualTo(200);
        assertThat(performance.getModerateThreshold()).isEqualTo(800);
        assertThat(performance.getSlowThreshold()).isEqualTo(2500);
    }

    @Test
    @DisplayName("Should bind complex Retry configuration")
    void propertyBinding_RetryConfiguration_BindsCorrectly() {
        // given
        Map<String, Object> properties = new HashMap<>();
        properties.put("app.aop.retry.default-max-attempts", "5");
        properties.put("app.aop.retry.default-delay", "2000");
        properties.put("app.aop.retry.max-delay", "20000");
        properties.put("app.aop.retry.default-backoff-multiplier", "1.5");
        properties.put("app.aop.retry.default-randomize-delay", "false");
        properties.put("app.aop.retry.collect-stats", "false");
        properties.put("app.aop.retry.log-level", "ERROR");

        ConfigurationPropertySource source = new MapConfigurationPropertySource(properties);
        Binder binder = new Binder(source);

        // when
        AopConfiguration boundConfig = binder.bind("app.aop", AopConfiguration.class).get();

        // then
        AopConfiguration.Retry retry = boundConfig.getRetry();
        assertThat(retry.getDefaultMaxAttempts()).isEqualTo(5);
        assertThat(retry.getDefaultDelay()).isEqualTo(2000);
        assertThat(retry.getMaxDelay()).isEqualTo(20000);
        assertThat(retry.getDefaultBackoffMultiplier()).isEqualTo(1.5);
        assertThat(retry.isDefaultRandomizeDelay()).isFalse();
        assertThat(retry.isCollectStats()).isFalse();
        assertThat(retry.getLogLevel()).isEqualTo("ERROR");
    }

    @Test
    @DisplayName("Should bind complex Cache configuration")
    void propertyBinding_CacheConfiguration_BindsCorrectly() {
        // given
        Map<String, Object> properties = new HashMap<>();
        properties.put("app.aop.cache.default-ttl", "600");
        properties.put("app.aop.cache.max-ttl", "172800");
        properties.put("app.aop.cache.default-use-local-fallback", "false");
        properties.put("app.aop.cache.local-cache-max-size", "2000");
        properties.put("app.aop.cache.local-cache-cleanup-interval", "60");
        properties.put("app.aop.cache.default-cache-null", "true");
        properties.put("app.aop.cache.max-key-length", "500");
        properties.put("app.aop.cache.log-level", "INFO");
        properties.put("app.aop.cache.fallback-on-redis-failure", "false");

        ConfigurationPropertySource source = new MapConfigurationPropertySource(properties);
        Binder binder = new Binder(source);

        // when
        AopConfiguration boundConfig = binder.bind("app.aop", AopConfiguration.class).get();

        // then
        AopConfiguration.Cache cache = boundConfig.getCache();
        assertThat(cache.getDefaultTtl()).isEqualTo(600);
        assertThat(cache.getMaxTtl()).isEqualTo(172800);
        assertThat(cache.isDefaultUseLocalFallback()).isFalse();
        assertThat(cache.getLocalCacheMaxSize()).isEqualTo(2000);
        assertThat(cache.getLocalCacheCleanupInterval()).isEqualTo(60);
        assertThat(cache.isDefaultCacheNull()).isTrue();
        assertThat(cache.getMaxKeyLength()).isEqualTo(500);
        assertThat(cache.getLogLevel()).isEqualTo("INFO");
        assertThat(cache.isFallbackOnRedisFailure()).isFalse();
    }

    @Test
    @DisplayName("Should allow modification of nested configuration objects")
    void settersAndGetters_NestedObjects_WorkCorrectly() {
        // given
        AopConfiguration.ApiLogging newApiLogging = new AopConfiguration.ApiLogging();
        newApiLogging.setEnabled(false);
        newApiLogging.setMaxResponseLength(1000);

        AopConfiguration.Performance newPerformance = new AopConfiguration.Performance();
        newPerformance.setEnabled(false);
        newPerformance.setSlowThreshold(5000);

        // when
        aopConfiguration.setApiLogging(newApiLogging);
        aopConfiguration.setPerformance(newPerformance);

        // then
        assertThat(aopConfiguration.getApiLogging().isEnabled()).isFalse();
        assertThat(aopConfiguration.getApiLogging().getMaxResponseLength()).isEqualTo(1000);
        assertThat(aopConfiguration.getPerformance().isEnabled()).isFalse();
        assertThat(aopConfiguration.getPerformance().getSlowThreshold()).isEqualTo(5000);
    }

    @Test
    @DisplayName("Should handle null values gracefully in setters")
    void setters_NullValues_HandlesGracefully() {
        // when/then - Should not throw exceptions
        aopConfiguration.setApiLogging(null);
        aopConfiguration.setPerformance(null);
        aopConfiguration.setAudit(null);
        aopConfiguration.setRetry(null);
        aopConfiguration.setCache(null);

        assertThat(aopConfiguration.getApiLogging()).isNull();
        assertThat(aopConfiguration.getPerformance()).isNull();
        assertThat(aopConfiguration.getAudit()).isNull();
        assertThat(aopConfiguration.getRetry()).isNull();
        assertThat(aopConfiguration.getCache()).isNull();
    }

    @Test
    @DisplayName("Should support equals and hashCode for nested objects")
    void equalsAndHashCode_NestedObjects_WorkCorrectly() {
        // given
        AopConfiguration.ApiLogging apiLogging1 = new AopConfiguration.ApiLogging();
        AopConfiguration.ApiLogging apiLogging2 = new AopConfiguration.ApiLogging();
        
        // when/then - Default objects should be equal
        assertThat(apiLogging1).isEqualTo(apiLogging2);
        assertThat(apiLogging1.hashCode()).isEqualTo(apiLogging2.hashCode());

        // when - Modify one object
        apiLogging1.setEnabled(false);

        // then - Should no longer be equal
        assertThat(apiLogging1).isNotEqualTo(apiLogging2);
    }

    @Test
    @DisplayName("Should provide meaningful toString for debugging")
    void toString_NestedObjects_ProvidesMeaningfulString() {
        // when
        String configString = aopConfiguration.toString();
        String apiLoggingString = aopConfiguration.getApiLogging().toString();
        
        // then
        assertThat(configString).contains("ApiLogging");
        assertThat(configString).contains("Performance");
        assertThat(configString).contains("Audit");
        assertThat(configString).contains("Retry");
        assertThat(configString).contains("Cache");
        
        assertThat(apiLoggingString).contains("enabled=true");
        assertThat(apiLoggingString).contains("maxResponseLength=200");
    }
}