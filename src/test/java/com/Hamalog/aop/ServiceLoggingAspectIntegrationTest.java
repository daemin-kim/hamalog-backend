package com.Hamalog.aop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.BusinessEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "app.aop.global-enabled=true",
        "app.aop.service-logging.enabled=true",
        // 다른 AOP는 비활성화하여 간섭 최소화
        "app.aop.api-logging.enabled=false",
        "app.aop.performance.enabled=false",
        "app.aop.audit.enabled=false",
        "app.aop.retry.enabled=false",
        "app.aop.cache.enabled=false"
})
@ActiveProfiles("test")
class ServiceLoggingAspectIntegrationTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        DummyService dummyService() {
            return new DummyService();
        }

        @Bean
        @org.springframework.context.annotation.Primary
        StructuredLogger structuredLoggerMock() {
            return org.mockito.Mockito.mock(StructuredLogger.class);
        }
    }

    static class DummyService {
        public String doWork(String input) {
            return "OK:" + input;
        }
    }

    @Autowired
    private DummyService dummyService;

    @Autowired
    private StructuredLogger structuredLogger;

    @Test
    @DisplayName("ServiceLoggingAspect가 서비스 호출 시작/성공 이벤트를 로깅해야 한다")
    void shouldLogStartAndSuccess() {
        String out = dummyService.doWork("ping");
        assertThat(out).isEqualTo("OK:ping");

        ArgumentCaptor<BusinessEvent> captor = ArgumentCaptor.forClass(BusinessEvent.class);
        verify(structuredLogger, times(2)).business(captor.capture());

        assertThat(captor.getAllValues())
            .extracting(BusinessEvent::getResult)
            .contains("START", "SUCCESS");

        verify(structuredLogger, times(2)).business(any(BusinessEvent.class));
    }
}
