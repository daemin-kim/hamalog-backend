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
        assertThat(aopConfiguration.getAudit()).isNotNull();
        assertThat(aopConfiguration.getCache()).isNotNull();
        assertThat(aopConfiguration.isGlobalEnabled()).isTrue();
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
        assertThat(cache.isCollectStats()).isTrue();
        assertThat(cache.isFallbackOnRedisFailure()).isTrue();
    }

    @Test
    @DisplayName("Should bind properties from configuration")
    void propertyBinding_WithCustomValues_BindsCorrectly() {
        // given
        Map<String, Object> properties = new HashMap<>();
        properties.put("app.aop.audit.enabled", "false");
        properties.put("app.aop.cache.enabled", "false");
        properties.put("app.aop.global-enabled", "false");

        ConfigurationPropertySource source = new MapConfigurationPropertySource(properties);
        Binder binder = new Binder(source);

        // when
        AopConfiguration boundConfig = binder.bind("app.aop", AopConfiguration.class).get();

        // then
        assertThat(boundConfig.getAudit().isEnabled()).isFalse();
        assertThat(boundConfig.getCache().isEnabled()).isFalse();
        assertThat(boundConfig.isGlobalEnabled()).isFalse();
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
        assertThat(cache.isFallbackOnRedisFailure()).isFalse();
    }

    @Test
    @DisplayName("Should allow modification of nested configuration objects")
    void settersAndGetters_NestedObjects_WorkCorrectly() {
        // given
        AopConfiguration.Audit newAudit = new AopConfiguration.Audit();
        newAudit.setEnabled(false);
        newAudit.setMaxParameterLength(200);

        AopConfiguration.Cache newCache = new AopConfiguration.Cache();
        newCache.setEnabled(false);
        newCache.setDefaultTtl(600);

        // when
        aopConfiguration.setAudit(newAudit);
        aopConfiguration.setCache(newCache);

        // then
        assertThat(aopConfiguration.getAudit().isEnabled()).isFalse();
        assertThat(aopConfiguration.getAudit().getMaxParameterLength()).isEqualTo(200);
        assertThat(aopConfiguration.getCache().isEnabled()).isFalse();
        assertThat(aopConfiguration.getCache().getDefaultTtl()).isEqualTo(600);
    }

    @Test
    @DisplayName("Should handle null values gracefully in setters")
    void setters_NullValues_HandlesGracefully() {
        // when/then - Should not throw exceptions
        aopConfiguration.setAudit(null);
        aopConfiguration.setCache(null);

        assertThat(aopConfiguration.getAudit()).isNull();
        assertThat(aopConfiguration.getCache()).isNull();
    }

    @Test
    @DisplayName("Should support equals and hashCode for nested objects")
    void equalsAndHashCode_NestedObjects_WorkCorrectly() {
        // given
        AopConfiguration.Audit audit1 = new AopConfiguration.Audit();
        AopConfiguration.Audit audit2 = new AopConfiguration.Audit();

        // when/then - Default objects should be equal
        assertThat(audit1).isEqualTo(audit2);
        assertThat(audit1.hashCode()).isEqualTo(audit2.hashCode());

        // when - Modify one object
        audit1.setEnabled(false);

        // then - Should no longer be equal
        assertThat(audit1).isNotEqualTo(audit2);
    }

    @Test
    @DisplayName("Should provide meaningful toString for debugging")
    void toString_NestedObjects_ProvidesMeaningfulString() {
        // when
        String configString = aopConfiguration.toString();
        String auditString = aopConfiguration.getAudit().toString();

        // then
        assertThat(configString).contains("Audit");
        assertThat(configString).contains("Cache");
        assertThat(auditString).contains("enabled=true");
    }
}
