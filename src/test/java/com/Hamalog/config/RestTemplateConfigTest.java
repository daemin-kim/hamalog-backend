package com.Hamalog.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RestTemplateConfigTest {

    @Autowired
    private RestTemplateConfig restTemplateConfig;

    @Test
    @DisplayName("RestTemplate 빈이 정상적으로 생성되는지 확인")
    void restTemplateBean_ShouldBeCreated() {
        // when
        RestTemplate restTemplate = restTemplateConfig.restTemplate();

        // then
        assertThat(restTemplate).isNotNull();
    }

    @Test
    @DisplayName("RestTemplateConfig 빈이 정상적으로 생성되는지 확인")
    void restTemplateConfig_ShouldBeCreated() {
        assertThat(restTemplateConfig).isNotNull();
    }
}
