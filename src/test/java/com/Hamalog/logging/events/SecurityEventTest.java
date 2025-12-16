package com.Hamalog.logging.events;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SecurityEvent Tests")
class SecurityEventTest {

    @Test
    @DisplayName("빌더 패턴을 사용하여 SecurityEvent를 생성할 수 있어야 함")
    void builder_ShouldCreateSecurityEventWithAllFields() {
        // given
        // when
        SecurityEvent event = SecurityEvent.builder()
                .eventType("AUTHENTICATION_FAILURE")
                .userId("user123@example.com")
                .ipAddress("192.168.1.100")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .resource("/api/auth/login")
                .action("로그인 시도")
                .result("FAILURE")
                .riskLevel("MEDIUM")
                .details("잘못된 비밀번호로 인한 로그인 실패")
                .build();

        // then
        assertThat(event.getEventType()).isEqualTo("AUTHENTICATION_FAILURE");
        assertThat(event.getUserId()).isEqualTo("user123@example.com");
        assertThat(event.getIpAddress()).isEqualTo("192.168.1.100");
        assertThat(event.getUserAgent()).isEqualTo("Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        assertThat(event.getResource()).isEqualTo("/api/auth/login");
        assertThat(event.getAction()).isEqualTo("로그인 시도");
        assertThat(event.getResult()).isEqualTo("FAILURE");
        assertThat(event.getRiskLevel()).isEqualTo("MEDIUM");
        assertThat(event.getDetails()).isEqualTo("잘못된 비밀번호로 인한 로그인 실패");
    }

    @Test
    @DisplayName("빈 SecurityEvent를 생성할 수 있어야 함")
    void builder_ShouldCreateEmptySecurityEvent() {
        // given
        // when
        SecurityEvent event = SecurityEvent.builder().build();

        // then
        assertThat(event.getEventType()).isNull();
        assertThat(event.getUserId()).isNull();
        assertThat(event.getIpAddress()).isNull();
        assertThat(event.getUserAgent()).isNull();
        assertThat(event.getResource()).isNull();
        assertThat(event.getAction()).isNull();
        assertThat(event.getResult()).isNull();
        assertThat(event.getRiskLevel()).isNull();
        assertThat(event.getDetails()).isNull();
    }

    @Test
    @DisplayName("빌더 패턴은 체이닝을 지원해야 함")
    void builder_ShouldSupportMethodChaining() {
        // given
        // when
        SecurityEvent event = SecurityEvent.builder()
                .eventType("AUTHORIZATION_SUCCESS")
                .resource("/api/medication/schedules")
                .result("SUCCESS")
                .build();

        // then
        assertThat(event.getEventType()).isEqualTo("AUTHORIZATION_SUCCESS");
        assertThat(event.getResource()).isEqualTo("/api/medication/schedules");
        assertThat(event.getResult()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("다양한 보안 이벤트 타입을 처리할 수 있어야 함")
    void builder_ShouldHandleVariousSecurityEventTypes() {
        // given & when
        SecurityEvent authEvent = SecurityEvent.builder()
                .eventType("AUTHENTICATION_SUCCESS")
                .build();
        
        SecurityEvent authzEvent = SecurityEvent.builder()
                .eventType("AUTHORIZATION_FAILURE")
                .build();
        
        SecurityEvent suspiciousEvent = SecurityEvent.builder()
                .eventType("SUSPICIOUS_ACTIVITY")
                .build();
        
        SecurityEvent bruteForceEvent = SecurityEvent.builder()
                .eventType("BRUTE_FORCE_ATTEMPT")
                .build();

        // then
        assertThat(authEvent.getEventType()).isEqualTo("AUTHENTICATION_SUCCESS");
        assertThat(authzEvent.getEventType()).isEqualTo("AUTHORIZATION_FAILURE");
        assertThat(suspiciousEvent.getEventType()).isEqualTo("SUSPICIOUS_ACTIVITY");
        assertThat(bruteForceEvent.getEventType()).isEqualTo("BRUTE_FORCE_ATTEMPT");
    }

    @Test
    @DisplayName("다양한 위험 수준을 처리할 수 있어야 함")
    void builder_ShouldHandleVariousRiskLevels() {
        // given & when
        SecurityEvent lowRiskEvent = SecurityEvent.builder()
                .riskLevel("LOW")
                .build();
        
        SecurityEvent mediumRiskEvent = SecurityEvent.builder()
                .riskLevel("MEDIUM")
                .build();
        
        SecurityEvent highRiskEvent = SecurityEvent.builder()
                .riskLevel("HIGH")
                .build();
        
        SecurityEvent criticalRiskEvent = SecurityEvent.builder()
                .riskLevel("CRITICAL")
                .build();

        // then
        assertThat(lowRiskEvent.getRiskLevel()).isEqualTo("LOW");
        assertThat(mediumRiskEvent.getRiskLevel()).isEqualTo("MEDIUM");
        assertThat(highRiskEvent.getRiskLevel()).isEqualTo("HIGH");
        assertThat(criticalRiskEvent.getRiskLevel()).isEqualTo("CRITICAL");
    }

    @Test
    @DisplayName("다양한 결과 상태를 처리할 수 있어야 함")
    void builder_ShouldHandleVariousResults() {
        // given & when
        SecurityEvent successEvent = SecurityEvent.builder().result("SUCCESS").build();
        SecurityEvent failureEvent = SecurityEvent.builder().result("FAILURE").build();
        SecurityEvent blockedEvent = SecurityEvent.builder().result("BLOCKED").build();
        SecurityEvent suspendedEvent = SecurityEvent.builder().result("SUSPENDED").build();

        // then
        assertThat(successEvent.getResult()).isEqualTo("SUCCESS");
        assertThat(failureEvent.getResult()).isEqualTo("FAILURE");
        assertThat(blockedEvent.getResult()).isEqualTo("BLOCKED");
        assertThat(suspendedEvent.getResult()).isEqualTo("SUSPENDED");
    }

    @Test
    @DisplayName("다양한 리소스를 처리할 수 있어야 함")
    void builder_ShouldHandleVariousResources() {
        // given & when
        SecurityEvent apiEvent = SecurityEvent.builder()
                .resource("/api/auth/login")
                .build();
        
        SecurityEvent medicationEvent = SecurityEvent.builder()
                .resource("/api/medication/schedules/{id}")
                .build();
        
        SecurityEvent memberEvent = SecurityEvent.builder()
                .resource("/api/members/profile")
                .build();

        // then
        assertThat(apiEvent.getResource()).isEqualTo("/api/auth/login");
        assertThat(medicationEvent.getResource()).isEqualTo("/api/medication/schedules/{id}");
        assertThat(memberEvent.getResource()).isEqualTo("/api/members/profile");
    }

    @Test
    @DisplayName("한글 액션과 상세 정보를 처리할 수 있어야 함")
    void builder_ShouldHandleKoreanActionAndDetails() {
        // given
        String koreanAction = "복약 스케줄 조회 시도";
        String koreanDetails = "인증되지 않은 사용자가 다른 사용자의 복약 정보에 접근을 시도했습니다.";

        // when
        SecurityEvent event = SecurityEvent.builder()
                .action(koreanAction)
                .details(koreanDetails)
                .build();

        // then
        assertThat(event.getAction()).isEqualTo(koreanAction);
        assertThat(event.getDetails()).isEqualTo(koreanDetails);
    }

    @Test
    @DisplayName("null 값을 처리할 수 있어야 함")
    void builder_ShouldHandleNullValues() {
        // given
        // when
        SecurityEvent event = SecurityEvent.builder()
                .eventType("AUTHENTICATION_ATTEMPT")
                .userId(null)
                .userAgent(null)
                .details(null)
                .build();

        // then
        assertThat(event.getEventType()).isEqualTo("AUTHENTICATION_ATTEMPT");
        assertThat(event.getUserId()).isNull();
        assertThat(event.getUserAgent()).isNull();
        assertThat(event.getDetails()).isNull();
    }

    @Test
    @DisplayName("의심스러운 IP 주소를 처리할 수 있어야 함")
    void builder_ShouldHandleSuspiciousIpAddresses() {
        // given
        String suspiciousIp = "203.0.113.0"; // RFC 5737 test IP

        // when
        SecurityEvent event = SecurityEvent.builder()
                .eventType("SUSPICIOUS_IP_ACCESS")
                .ipAddress(suspiciousIp)
                .riskLevel("HIGH")
                .build();

        // then
        assertThat(event.getEventType()).isEqualTo("SUSPICIOUS_IP_ACCESS");
        assertThat(event.getIpAddress()).isEqualTo(suspiciousIp);
        assertThat(event.getRiskLevel()).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("다양한 사용자 에이전트를 처리할 수 있어야 함")
    void builder_ShouldHandleVariousUserAgents() {
        // given & when
        SecurityEvent chromeEvent = SecurityEvent.builder()
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .build();
        
        SecurityEvent mobileEvent = SecurityEvent.builder()
                .userAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1")
                .build();
        
        SecurityEvent botEvent = SecurityEvent.builder()
                .userAgent("curl/7.68.0")
                .build();

        // then
        assertThat(chromeEvent.getUserAgent()).contains("Chrome");
        assertThat(mobileEvent.getUserAgent()).contains("iPhone");
        assertThat(botEvent.getUserAgent()).contains("curl");
    }

    @Test
    @DisplayName("실제 보안 이벤트 시나리오를 처리할 수 있어야 함")
    void builder_ShouldHandleRealSecurityScenario() {
        // given
        // when
        SecurityEvent event = SecurityEvent.builder()
                .eventType("UNAUTHORIZED_ACCESS_ATTEMPT")
                .userId("malicious_user@suspicious.com")
                .ipAddress("198.51.100.42")
                .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36")
                .resource("/api/medication/schedules/456")
                .action("다른 사용자 복약 정보 조회 시도")
                .result("BLOCKED")
                .riskLevel("HIGH")
                .details("사용자 ID 123의 복약 스케줄에 대해 인증되지 않은 접근을 시도했습니다. IP 주소가 블랙리스트에 등록되었습니다.")
                .build();

        // then
        assertThat(event.getEventType()).isEqualTo("UNAUTHORIZED_ACCESS_ATTEMPT");
        assertThat(event.getUserId()).isEqualTo("malicious_user@suspicious.com");
        assertThat(event.getIpAddress()).isEqualTo("198.51.100.42");
        assertThat(event.getUserAgent()).contains("Linux");
        assertThat(event.getResource()).isEqualTo("/api/medication/schedules/456");
        assertThat(event.getAction()).contains("다른 사용자");
        assertThat(event.getResult()).isEqualTo("BLOCKED");
        assertThat(event.getRiskLevel()).isEqualTo("HIGH");
        assertThat(event.getDetails()).contains("블랙리스트");
    }

    @Test
    @DisplayName("성공적인 인증 이벤트를 처리할 수 있어야 함")
    void builder_ShouldHandleSuccessfulAuthenticationEvent() {
        // given
        // when
        SecurityEvent event = SecurityEvent.builder()
                .eventType("AUTHENTICATION_SUCCESS")
                .userId("patient_789@example.com")
                .ipAddress("192.168.1.10")
                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)")
                .resource("/api/auth/login")
                .action("로그인")
                .result("SUCCESS")
                .riskLevel("LOW")
                .details("정상적인 로그인이 완료되었습니다.")
                .build();

        // then
        assertThat(event.getEventType()).isEqualTo("AUTHENTICATION_SUCCESS");
        assertThat(event.getUserId()).isEqualTo("patient_789@example.com");
        assertThat(event.getIpAddress()).isEqualTo("192.168.1.10");
        assertThat(event.getResource()).isEqualTo("/api/auth/login");
        assertThat(event.getResult()).isEqualTo("SUCCESS");
        assertThat(event.getRiskLevel()).isEqualTo("LOW");
    }

    @Test
    @DisplayName("긴 상세 정보를 처리할 수 있어야 함")
    void builder_ShouldHandleLongDetails() {
        // given
        String longDetails = "이것은 매우 상세한 보안 이벤트 로그입니다. ".repeat(50);

        // when
        SecurityEvent event = SecurityEvent.builder()
                .details(longDetails)
                .build();

        // then
        assertThat(event.getDetails()).isEqualTo(longDetails);
        assertThat(event.getDetails().length()).isGreaterThan(1000);
    }
}