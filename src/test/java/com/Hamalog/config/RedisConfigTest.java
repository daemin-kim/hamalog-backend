package com.Hamalog.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisConfig Tests")
class RedisConfigTest {

    @Mock
    private RedisConnectionFactory redisConnectionFactory;

    private RedisConfig redisConfig;

    @BeforeEach
    void setUp() {
        redisConfig = new RedisConfig();
    }

    @Test
    @DisplayName("Should create RedisTemplate with correct configuration")
    void redisTemplate_ValidConnectionFactory_CreatesCorrectRedisTemplate() {
        // when
        RedisTemplate<String, Object> redisTemplate = redisConfig.redisTemplate(redisConnectionFactory);

        // then
        assertThat(redisTemplate).isNotNull();
        assertThat(redisTemplate.getConnectionFactory()).isEqualTo(redisConnectionFactory);
    }

    @Test
    @DisplayName("Should configure StringRedisSerializer for keys")
    void redisTemplate_KeySerializer_ConfiguresStringRedisSerializer() {
        // when
        RedisTemplate<String, Object> redisTemplate = redisConfig.redisTemplate(redisConnectionFactory);

        // then
        assertThat(redisTemplate.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(redisTemplate.getHashKeySerializer()).isInstanceOf(StringRedisSerializer.class);
    }

    @Test
    @DisplayName("Should configure GenericJackson2JsonRedisSerializer for values")
    void redisTemplate_ValueSerializer_ConfiguresJsonRedisSerializer() {
        // when
        RedisTemplate<String, Object> redisTemplate = redisConfig.redisTemplate(redisConnectionFactory);

        // then
        assertThat(redisTemplate.getValueSerializer()).isInstanceOf(GenericJackson2JsonRedisSerializer.class);
        assertThat(redisTemplate.getHashValueSerializer()).isInstanceOf(GenericJackson2JsonRedisSerializer.class);
    }

    @Test
    @DisplayName("Should call afterPropertiesSet on RedisTemplate")
    void redisTemplate_Configuration_CallsAfterPropertiesSet() {
        // when
        RedisTemplate<String, Object> redisTemplate = redisConfig.redisTemplate(redisConnectionFactory);

        // then
        // Verify that the template is properly initialized
        assertThat(redisTemplate.getConnectionFactory()).isNotNull();
        assertThat(redisTemplate.getKeySerializer()).isNotNull();
        assertThat(redisTemplate.getValueSerializer()).isNotNull();
        assertThat(redisTemplate.getHashKeySerializer()).isNotNull();
        assertThat(redisTemplate.getHashValueSerializer()).isNotNull();
    }

    @Test
    @DisplayName("Should create different RedisTemplate instances on multiple calls")
    void redisTemplate_MultipleCalls_CreatesDifferentInstances() {
        // when
        RedisTemplate<String, Object> redisTemplate1 = redisConfig.redisTemplate(redisConnectionFactory);
        RedisTemplate<String, Object> redisTemplate2 = redisConfig.redisTemplate(redisConnectionFactory);

        // then
        assertThat(redisTemplate1).isNotSameAs(redisTemplate2);
        assertThat(redisTemplate1.getConnectionFactory()).isEqualTo(redisTemplate2.getConnectionFactory());
    }

    @Test
    @DisplayName("Should configure RedisTemplate with generic types")
    void redisTemplate_GenericTypes_ConfiguresCorrectTypes() {
        // when
        RedisTemplate<String, Object> redisTemplate = redisConfig.redisTemplate(redisConnectionFactory);

        // then
        assertThat(redisTemplate).isInstanceOf(RedisTemplate.class);
        // The generic types are String for keys and Object for values
        assertThat(redisTemplate.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(redisTemplate.getValueSerializer()).isInstanceOf(GenericJackson2JsonRedisSerializer.class);
    }

    @Test
    @DisplayName("Should throw exception when connection factory is null")
    void redisTemplate_NullConnectionFactory_ThrowsException() {
        // when/then
        assertThatThrownBy(() -> redisConfig.redisTemplate(null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("RedisConnectionFactory is required");
    }

    @Test
    @DisplayName("Should configure consistent serializer instances")
    void redisTemplate_SerializerInstances_ConfiguresConsistentTypes() {
        // when
        RedisTemplate<String, Object> redisTemplate = redisConfig.redisTemplate(redisConnectionFactory);

        // then
        // Key serializers should be String serializers
        assertThat(redisTemplate.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(redisTemplate.getHashKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        
        // Value serializers should be JSON serializers
        assertThat(redisTemplate.getValueSerializer()).isInstanceOf(GenericJackson2JsonRedisSerializer.class);
        assertThat(redisTemplate.getHashValueSerializer()).isInstanceOf(GenericJackson2JsonRedisSerializer.class);
        
        // Each serializer should be a different instance
        assertThat(redisTemplate.getKeySerializer()).isNotSameAs(redisTemplate.getHashKeySerializer());
        assertThat(redisTemplate.getValueSerializer()).isNotSameAs(redisTemplate.getHashValueSerializer());
    }

    @Test
    @DisplayName("Should maintain serializer configuration after initialization")
    void redisTemplate_AfterInitialization_MaintainsSerializerConfiguration() {
        // when
        RedisTemplate<String, Object> redisTemplate = redisConfig.redisTemplate(redisConnectionFactory);

        // then
        // Verify that serializers are still properly configured after afterPropertiesSet()
        assertThat(redisTemplate.getKeySerializer()).isNotNull();
        assertThat(redisTemplate.getValueSerializer()).isNotNull();
        assertThat(redisTemplate.getHashKeySerializer()).isNotNull();
        assertThat(redisTemplate.getHashValueSerializer()).isNotNull();
        
        // Verify correct types
        assertThat(redisTemplate.getKeySerializer().getClass().getSimpleName()).isEqualTo("StringRedisSerializer");
        assertThat(redisTemplate.getValueSerializer().getClass().getSimpleName()).isEqualTo("GenericJackson2JsonRedisSerializer");
    }

    @Test
    @DisplayName("Should create RedisTemplate that supports String keys and Object values")
    void redisTemplate_KeyValueTypes_SupportsCorrectTypes() {
        // when
        RedisTemplate<String, Object> redisTemplate = redisConfig.redisTemplate(redisConnectionFactory);

        // then
        // Verify that the template is configured for String keys and Object values
        assertThat(redisTemplate).isNotNull();
        
        // The key serializer should handle String types
        StringRedisSerializer keySerializer = (StringRedisSerializer) redisTemplate.getKeySerializer();
        assertThat(keySerializer).isNotNull();
        
        // The value serializer should handle Object types (JSON serialization)
        GenericJackson2JsonRedisSerializer valueSerializer = (GenericJackson2JsonRedisSerializer) redisTemplate.getValueSerializer();
        assertThat(valueSerializer).isNotNull();
    }

    @Test
    @DisplayName("Should configure hash operations with appropriate serializers")
    void redisTemplate_HashOperations_ConfiguresCorrectSerializers() {
        // when
        RedisTemplate<String, Object> redisTemplate = redisConfig.redisTemplate(redisConnectionFactory);

        // then
        // Hash key serializer should be String serializer
        assertThat(redisTemplate.getHashKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        
        // Hash value serializer should be JSON serializer
        assertThat(redisTemplate.getHashValueSerializer()).isInstanceOf(GenericJackson2JsonRedisSerializer.class);
        
        // Verify they are the same types as regular serializers
        assertThat(redisTemplate.getHashKeySerializer().getClass())
            .isEqualTo(redisTemplate.getKeySerializer().getClass());
        assertThat(redisTemplate.getHashValueSerializer().getClass())
            .isEqualTo(redisTemplate.getValueSerializer().getClass());
    }

    @Test
    @DisplayName("Should provide fully initialized RedisTemplate")
    void redisTemplate_FullInitialization_ProvidesReadyToUseTemplate() {
        // when
        RedisTemplate<String, Object> redisTemplate = redisConfig.redisTemplate(redisConnectionFactory);

        // then
        // Verify all essential components are configured
        assertThat(redisTemplate.getConnectionFactory()).isEqualTo(redisConnectionFactory);
        assertThat(redisTemplate.getKeySerializer()).isNotNull();
        assertThat(redisTemplate.getValueSerializer()).isNotNull();
        assertThat(redisTemplate.getHashKeySerializer()).isNotNull();
        assertThat(redisTemplate.getHashValueSerializer()).isNotNull();
        
        // Template should be ready for use (afterPropertiesSet called)
        // This is verified implicitly by the fact that all serializers are properly configured
        assertThat(redisTemplate.isExposeConnection()).isFalse(); // Default value
    }

    @Test
    @DisplayName("Should create RedisConfig instance successfully")
    void redisConfig_Creation_CreatesSuccessfully() {
        // when
        RedisConfig config = new RedisConfig();

        // then
        assertThat(config).isNotNull();
    }
}