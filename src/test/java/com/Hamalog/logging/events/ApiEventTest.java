package com.Hamalog.logging.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiEvent Tests")
class ApiEventTest {

    @Test
    @DisplayName("빌더 패턴을 사용하여 ApiEvent를 생성할 수 있어야 함")
    void builder_ShouldCreateApiEventWithAllFields() {
        // given
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("param1", "value1");
        parameters.put("param2", 123);

        // when
        ApiEvent event = ApiEvent.builder()
                .httpMethod("GET")
                .path("/api/test")
                .controller("TestController")
                .action("testAction")
                .userId("user123")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .durationMs(250L)
                .statusCode(200)
                .requestSize(1024L)
                .responseSize(2048L)
                .requestType("내부 요청")
                .parameters(parameters)
                .build();

        // then
        assertThat(event.getHttpMethod()).isEqualTo("GET");
        assertThat(event.getPath()).isEqualTo("/api/test");
        assertThat(event.getController()).isEqualTo("TestController");
        assertThat(event.getAction()).isEqualTo("testAction");
        assertThat(event.getUserId()).isEqualTo("user123");
        assertThat(event.getIpAddress()).isEqualTo("192.168.1.1");
        assertThat(event.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(event.getDurationMs()).isEqualTo(250L);
        assertThat(event.getStatusCode()).isEqualTo(200);
        assertThat(event.getRequestSize()).isEqualTo(1024L);
        assertThat(event.getResponseSize()).isEqualTo(2048L);
        assertThat(event.getRequestType()).isEqualTo("내부 요청");
        assertThat(event.getParameters()).containsExactlyInAnyOrderEntriesOf(parameters);
    }

    @Test
    @DisplayName("빈 ApiEvent를 생성할 수 있어야 함")
    void builder_ShouldCreateEmptyApiEvent() {
        // given
        // when
        ApiEvent event = ApiEvent.builder().build();

        // then
        assertThat(event.getHttpMethod()).isNull();
        assertThat(event.getPath()).isNull();
        assertThat(event.getController()).isNull();
        assertThat(event.getAction()).isNull();
        assertThat(event.getUserId()).isNull();
        assertThat(event.getIpAddress()).isNull();
        assertThat(event.getUserAgent()).isNull();
        assertThat(event.getDurationMs()).isEqualTo(0L);
        assertThat(event.getStatusCode()).isEqualTo(0);
        assertThat(event.getRequestSize()).isNull();
        assertThat(event.getResponseSize()).isNull();
        assertThat(event.getRequestType()).isNull();
        assertThat(event.getParameters()).isNull();
    }

    @Test
    @DisplayName("빌더 패턴은 체이닝을 지원해야 함")
    void builder_ShouldSupportMethodChaining() {
        // given
        // when
        ApiEvent event = ApiEvent.builder()
                .httpMethod("POST")
                .path("/api/users")
                .statusCode(201)
                .build();

        // then
        assertThat(event.getHttpMethod()).isEqualTo("POST");
        assertThat(event.getPath()).isEqualTo("/api/users");
        assertThat(event.getStatusCode()).isEqualTo(201);
    }

    @Test
    @DisplayName("외부 요청 타입을 설정할 수 있어야 함")
    void builder_ShouldHandleExternalRequestType() {
        // given
        // when
        ApiEvent event = ApiEvent.builder()
                .requestType("외부 요청")
                .build();

        // then
        assertThat(event.getRequestType()).isEqualTo("외부 요청");
    }

    @Test
    @DisplayName("파라미터 맵을 올바르게 처리해야 함")
    void builder_ShouldHandleParametersMap() {
        // given
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("searchKeyword", "hamalog");
        parameters.put("pageSize", 10);
        parameters.put("isActive", true);

        // when
        ApiEvent event = ApiEvent.builder()
                .parameters(parameters)
                .build();

        // then
        assertThat(event.getParameters()).isNotNull();
        assertThat(event.getParameters()).hasSize(3);
        assertThat(event.getParameters().get("searchKeyword")).isEqualTo("hamalog");
        assertThat(event.getParameters().get("pageSize")).isEqualTo(10);
        assertThat(event.getParameters().get("isActive")).isEqualTo(true);
    }

    @Test
    @DisplayName("null 파라미터를 처리할 수 있어야 함")
    void builder_ShouldHandleNullParameters() {
        // given
        // when
        ApiEvent event = ApiEvent.builder()
                .parameters(null)
                .userId(null)
                .build();

        // then
        assertThat(event.getParameters()).isNull();
        assertThat(event.getUserId()).isNull();
    }

    @Test
    @DisplayName("대용량 요청/응답 크기를 처리할 수 있어야 함")
    void builder_ShouldHandleLargeRequestResponseSizes() {
        // given
        Long largeSize = Long.MAX_VALUE;

        // when
        ApiEvent event = ApiEvent.builder()
                .requestSize(largeSize)
                .responseSize(largeSize)
                .build();

        // then
        assertThat(event.getRequestSize()).isEqualTo(largeSize);
        assertThat(event.getResponseSize()).isEqualTo(largeSize);
    }

    @Test
    @DisplayName("긴 실행 시간을 처리할 수 있어야 함")
    void builder_ShouldHandleLongDuration() {
        // given
        long longDuration = 30000L; // 30 seconds

        // when
        ApiEvent event = ApiEvent.builder()
                .durationMs(longDuration)
                .build();

        // then
        assertThat(event.getDurationMs()).isEqualTo(longDuration);
    }

    @Test
    @DisplayName("다양한 HTTP 상태 코드를 처리할 수 있어야 함")
    void builder_ShouldHandleVariousStatusCodes() {
        // given & when
        ApiEvent successEvent = ApiEvent.builder().statusCode(200).build();
        ApiEvent errorEvent = ApiEvent.builder().statusCode(500).build();
        ApiEvent notFoundEvent = ApiEvent.builder().statusCode(404).build();

        // then
        assertThat(successEvent.getStatusCode()).isEqualTo(200);
        assertThat(errorEvent.getStatusCode()).isEqualTo(500);
        assertThat(notFoundEvent.getStatusCode()).isEqualTo(404);
    }
}