package com.Hamalog.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = RestTemplateConfigTest.TestConfig.class)
class RestTemplateConfigTest {
    @TestConfiguration
    @Import(RestTemplateConfig.class)
    @ImportAutoConfiguration(classes = {
            org.springframework.boot.autoconfigure.http.client.HttpClientAutoConfiguration.class,
            org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration.class
    })
    static class TestConfig { }

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RestTemplateConfig restTemplateConfig;

    @Autowired(required = false)
    private ClientHttpRequestFactory clientHttpRequestFactory;

    @Test
    @DisplayName("RestTemplate 빈이 정상적으로 생성되는지 확인")
    void restTemplateBean_ShouldBeCreated() {
        assertThat(restTemplate).isNotNull();
        if (clientHttpRequestFactory != null) {
            assertThat(restTemplate.getRequestFactory()).isSameAs(clientHttpRequestFactory);
        }
    }

    @Test
    @DisplayName("RestTemplateConfig 빈이 정상적으로 생성되는지 확인")
    void restTemplateConfig_ShouldBeCreated() {
        assertThat(restTemplateConfig).isNotNull();
    }
}
