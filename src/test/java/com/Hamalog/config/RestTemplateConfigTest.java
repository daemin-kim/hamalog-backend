package com.Hamalog.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RestTemplateConfig Tests")
class RestTemplateConfigTest {

    private RestTemplateConfig restTemplateConfig;

    @BeforeEach
    void setUp() {
        restTemplateConfig = new RestTemplateConfig();
    }

    @Test
    @DisplayName("Should create RestTemplate bean successfully")
    void restTemplate_Creation_CreatesRestTemplateSuccessfully() {
        // when
        RestTemplate restTemplate = restTemplateConfig.restTemplate();

        // then
        assertThat(restTemplate).isNotNull();
        assertThat(restTemplate).isInstanceOf(RestTemplate.class);
    }

    @Test
    @DisplayName("Should create different RestTemplate instances on multiple calls")
    void restTemplate_MultipleCalls_CreatesDifferentInstances() {
        // when
        RestTemplate restTemplate1 = restTemplateConfig.restTemplate();
        RestTemplate restTemplate2 = restTemplateConfig.restTemplate();

        // then
        assertThat(restTemplate1).isNotNull();
        assertThat(restTemplate2).isNotNull();
        assertThat(restTemplate1).isNotSameAs(restTemplate2);
    }

    @Test
    @DisplayName("Should create RestTemplate with default configuration")
    void restTemplate_DefaultConfiguration_HasCorrectDefaults() {
        // when
        RestTemplate restTemplate = restTemplateConfig.restTemplate();

        // then
        assertThat(restTemplate).isNotNull();
        assertThat(restTemplate.getMessageConverters()).isNotEmpty();
        assertThat(restTemplate.getRequestFactory()).isNotNull();
        assertThat(restTemplate.getErrorHandler()).isNotNull();
        assertThat(restTemplate.getInterceptors()).isEmpty(); // Default has no interceptors
    }

    @Test
    @DisplayName("Should create RestTemplate with standard message converters")
    void restTemplate_MessageConverters_HasStandardConverters() {
        // when
        RestTemplate restTemplate = restTemplateConfig.restTemplate();

        // then
        assertThat(restTemplate.getMessageConverters()).isNotEmpty();
        // RestTemplate comes with default message converters for various content types
        assertThat(restTemplate.getMessageConverters().size()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should create RestTemplate with default request factory")
    void restTemplate_RequestFactory_HasDefaultRequestFactory() {
        // when
        RestTemplate restTemplate = restTemplateConfig.restTemplate();

        // then
        assertThat(restTemplate.getRequestFactory()).isNotNull();
        // Default request factory should be SimpleClientHttpRequestFactory
        assertThat(restTemplate.getRequestFactory().getClass().getSimpleName())
                .isEqualTo("SimpleClientHttpRequestFactory");
    }

    @Test
    @DisplayName("Should create RestTemplate with default error handler")
    void restTemplate_ErrorHandler_HasDefaultErrorHandler() {
        // when
        RestTemplate restTemplate = restTemplateConfig.restTemplate();

        // then
        assertThat(restTemplate.getErrorHandler()).isNotNull();
        // Default error handler should be DefaultResponseErrorHandler
        assertThat(restTemplate.getErrorHandler().getClass().getSimpleName())
                .isEqualTo("DefaultResponseErrorHandler");
    }

    @Test
    @DisplayName("Should create RestTemplate with empty interceptors list")
    void restTemplate_Interceptors_HasEmptyInterceptorsList() {
        // when
        RestTemplate restTemplate = restTemplateConfig.restTemplate();

        // then
        assertThat(restTemplate.getInterceptors()).isNotNull();
        assertThat(restTemplate.getInterceptors()).isEmpty();
    }

    @Test
    @DisplayName("Should create RestTemplate with UriTemplateHandler")
    void restTemplate_UriTemplateHandler_HasDefaultUriTemplateHandler() {
        // when
        RestTemplate restTemplate = restTemplateConfig.restTemplate();

        // then
        assertThat(restTemplate.getUriTemplateHandler()).isNotNull();
        // Default URI template handler should be DefaultUriBuilderFactory
        assertThat(restTemplate.getUriTemplateHandler().getClass().getSimpleName())
                .isEqualTo("DefaultUriBuilderFactory");
    }

    @Test
    @DisplayName("Should create RestTemplateConfig instance successfully")
    void restTemplateConfig_Creation_CreatesSuccessfully() {
        // when
        RestTemplateConfig config = new RestTemplateConfig();

        // then
        assertThat(config).isNotNull();
    }

    @Test
    @DisplayName("Should create RestTemplate that is ready for use")
    void restTemplate_ReadyForUse_IsProperlyInitialized() {
        // when
        RestTemplate restTemplate = restTemplateConfig.restTemplate();

        // then
        // Verify that RestTemplate is fully initialized and ready for HTTP operations
        assertThat(restTemplate).isNotNull();
        assertThat(restTemplate.getMessageConverters()).isNotEmpty();
        assertThat(restTemplate.getRequestFactory()).isNotNull();
        assertThat(restTemplate.getErrorHandler()).isNotNull();
        assertThat(restTemplate.getUriTemplateHandler()).isNotNull();
        
        // RestTemplate should be able to handle basic HTTP operations
        // (We're not testing actual HTTP calls here, just that the template is configured)
        assertThat(restTemplate.getClass().getMethods())
                .anyMatch(method -> method.getName().equals("getForObject"))
                .anyMatch(method -> method.getName().equals("postForObject"))
                .anyMatch(method -> method.getName().equals("exchange"));
    }

    @Test
    @DisplayName("Should maintain consistent configuration across multiple RestTemplate instances")
    void restTemplate_ConsistentConfiguration_MaintainsConsistency() {
        // when
        RestTemplate restTemplate1 = restTemplateConfig.restTemplate();
        RestTemplate restTemplate2 = restTemplateConfig.restTemplate();

        // then
        // Both instances should have the same configuration (but be different objects)
        assertThat(restTemplate1).isNotSameAs(restTemplate2);
        
        // Same types of components
        assertThat(restTemplate1.getRequestFactory().getClass())
                .isEqualTo(restTemplate2.getRequestFactory().getClass());
        assertThat(restTemplate1.getErrorHandler().getClass())
                .isEqualTo(restTemplate2.getErrorHandler().getClass());
        assertThat(restTemplate1.getUriTemplateHandler().getClass())
                .isEqualTo(restTemplate2.getUriTemplateHandler().getClass());
        
        // Same number of message converters
        assertThat(restTemplate1.getMessageConverters().size())
                .isEqualTo(restTemplate2.getMessageConverters().size());
    }

    @Test
    @DisplayName("Should create RestTemplate with all standard HTTP methods available")
    void restTemplate_HttpMethods_HasAllStandardMethods() {
        // when
        RestTemplate restTemplate = restTemplateConfig.restTemplate();

        // then
        assertThat(restTemplate).isNotNull();
        
        // Verify that RestTemplate has all the standard HTTP method support
        // by checking that the methods exist (without actually calling them)
        Class<RestTemplate> restTemplateClass = RestTemplate.class;
        
        try {
            restTemplateClass.getMethod("getForObject", String.class, Class.class, Object[].class);
            restTemplateClass.getMethod("postForObject", String.class, Object.class, Class.class, Object[].class);
            restTemplateClass.getMethod("put", String.class, Object.class, Object[].class);
            restTemplateClass.getMethod("delete", String.class, Object[].class);
            restTemplateClass.getMethod("headForHeaders", String.class, Object[].class);
            restTemplateClass.getMethod("optionsForAllow", String.class, Object[].class);
            
            // If we get here, all methods exist
            assertThat(true).isTrue();
        } catch (NoSuchMethodException e) {
            // This should not happen with a properly configured RestTemplate
            assertThat(e).isNull();
        }
    }
}