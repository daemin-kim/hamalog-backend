package com.Hamalog.logging.business;

import com.Hamalog.logging.MDCUtil;
import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.BusinessEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BusinessIntelligenceLogger Tests")
class BusinessIntelligenceLoggerTest {

    @Mock
    private StructuredLogger structuredLogger;
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private BusinessIntelligenceLogger logger;

    private ConcurrentHashMap<String, AtomicLong> dailyMetrics;
    private ConcurrentHashMap<String, AtomicLong> featureUsage;

    @BeforeEach
    void setUp() throws Exception {
        // Access private fields for testing
        Field dailyMetricsField = BusinessIntelligenceLogger.class.getDeclaredField("dailyMetrics");
        dailyMetricsField.setAccessible(true);
        dailyMetrics = (ConcurrentHashMap<String, AtomicLong>) dailyMetricsField.get(logger);
        
        Field featureUsageField = BusinessIntelligenceLogger.class.getDeclaredField("featureUsage");
        featureUsageField.setAccessible(true);
        featureUsage = (ConcurrentHashMap<String, AtomicLong>) featureUsageField.get(logger);
        
        // Clear metrics before each test
        dailyMetrics.clear();
        featureUsage.clear();
    }

    @Test
    @DisplayName("사용자 등록을 추적하고 로그해야 함")
    void trackUserRegistration_ShouldTrackAndLog() {
        // given
        String userId = "user123@example.com";
        String registrationMethod = "EMAIL";
        
        try (MockedStatic<MDCUtil> mdcUtilMock = mockStatic(MDCUtil.class)) {
            mdcUtilMock.when(() -> MDCUtil.get(MDCUtil.IP_ADDRESS)).thenReturn("192.168.1.1");
            mdcUtilMock.when(() -> MDCUtil.get(MDCUtil.USER_AGENT)).thenReturn("Mozilla/5.0");
            mdcUtilMock.when(() -> MDCUtil.get(MDCUtil.CORRELATION_ID)).thenReturn("corr-123");

            // when
            logger.trackUserRegistration(userId, registrationMethod);

            // then
            ArgumentCaptor<BusinessEvent> eventCaptor = ArgumentCaptor.forClass(BusinessEvent.class);
            verify(structuredLogger).business(eventCaptor.capture());
            
            BusinessEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.getEventType()).isEqualTo("USER_REGISTRATION");
            assertThat(capturedEvent.getEntity()).isEqualTo("USER");
            assertThat(capturedEvent.getAction()).isEqualTo("REGISTER");
            assertThat(capturedEvent.getUserId()).isEqualTo(userId);
            assertThat(capturedEvent.getResult()).isEqualTo("SUCCESS");
            assertThat(capturedEvent.getMetadata()).containsKey("registration_method");
            assertThat(capturedEvent.getMetadata().get("registration_method")).isEqualTo(registrationMethod);
            
            // Check metrics increment
            assertThat(dailyMetrics.get("user_registrations").get()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("성공적인 사용자 로그인을 추적해야 함")
    void trackUserLogin_Success_ShouldTrackAndLog() {
        // given
        String userId = "patient456@example.com";
        String loginMethod = "OAUTH2_KAKAO";
        boolean isSuccessful = true;
        
        try (MockedStatic<MDCUtil> mdcUtilMock = mockStatic(MDCUtil.class)) {
            mdcUtilMock.when(() -> MDCUtil.get(MDCUtil.IP_ADDRESS)).thenReturn("10.0.0.1");
            mdcUtilMock.when(() -> MDCUtil.get(MDCUtil.USER_AGENT)).thenReturn("Mobile App/1.0");
            mdcUtilMock.when(() -> MDCUtil.get(MDCUtil.CORRELATION_ID)).thenReturn("corr-456");

            // when
            logger.trackUserLogin(userId, loginMethod, isSuccessful);

            // then
            ArgumentCaptor<BusinessEvent> eventCaptor = ArgumentCaptor.forClass(BusinessEvent.class);
            verify(structuredLogger).business(eventCaptor.capture());
            
            BusinessEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.getEventType()).isEqualTo("USER_LOGIN");
            assertThat(capturedEvent.getEntity()).isEqualTo("USER");
            assertThat(capturedEvent.getAction()).isEqualTo("LOGIN");
            assertThat(capturedEvent.getUserId()).isEqualTo(userId);
            assertThat(capturedEvent.getResult()).isEqualTo("SUCCESS");
            assertThat(capturedEvent.getMetadata().get("login_method")).isEqualTo(loginMethod);
            assertThat(capturedEvent.getMetadata().get("success")).isEqualTo(true);
            
            // Check metrics increment
            assertThat(dailyMetrics.get("user_logins").get()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("실패한 사용자 로그인을 추적해야 함")
    void trackUserLogin_Failure_ShouldTrackWithoutIncrementingMetrics() {
        // given
        String userId = "user789@example.com";
        String loginMethod = "PASSWORD";
        boolean isSuccessful = false;
        
        try (MockedStatic<MDCUtil> mdcUtilMock = mockStatic(MDCUtil.class)) {
            mdcUtilMock.when(() -> MDCUtil.get(MDCUtil.IP_ADDRESS)).thenReturn("172.16.0.1");
            mdcUtilMock.when(() -> MDCUtil.get(MDCUtil.USER_AGENT)).thenReturn("Chrome/91.0");
            mdcUtilMock.when(() -> MDCUtil.get(MDCUtil.CORRELATION_ID)).thenReturn("corr-789");

            // when
            logger.trackUserLogin(userId, loginMethod, isSuccessful);

            // then
            ArgumentCaptor<BusinessEvent> eventCaptor = ArgumentCaptor.forClass(BusinessEvent.class);
            verify(structuredLogger).business(eventCaptor.capture());
            
            BusinessEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.getResult()).isEqualTo("FAILURE");
            assertThat(capturedEvent.getMetadata().get("success")).isEqualTo(false);
            
            // Failed logins should not increment user_logins metric
            assertThat(dailyMetrics.get("user_logins")).isNull();
        }
    }

    @Test
    @DisplayName("복약 기록 생성을 추적해야 함")
    void trackMedicationRecordCreated_ShouldTrackAndLog() {
        // given
        String userId = "patient123@example.com";
        String medicationName = "타이레놀";
        String recordType = "TAKEN";
        
        try (MockedStatic<MDCUtil> mdcUtilMock = mockStatic(MDCUtil.class)) {
            mdcUtilMock.when(() -> MDCUtil.get(MDCUtil.CORRELATION_ID)).thenReturn("corr-med-123");

            // when
            logger.trackMedicationRecordCreated(userId, medicationName, recordType);

            // then
            ArgumentCaptor<BusinessEvent> eventCaptor = ArgumentCaptor.forClass(BusinessEvent.class);
            verify(structuredLogger).business(eventCaptor.capture());
            
            BusinessEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.getEventType()).isEqualTo("MEDICATION_RECORD_CREATED");
            assertThat(capturedEvent.getEntity()).isEqualTo("MEDICATION_RECORD");
            assertThat(capturedEvent.getAction()).isEqualTo("CREATE");
            assertThat(capturedEvent.getUserId()).isEqualTo(userId);
            assertThat(capturedEvent.getResult()).isEqualTo("SUCCESS");
            assertThat(capturedEvent.getMetadata().get("medication_name")).isEqualTo(medicationName);
            assertThat(capturedEvent.getMetadata().get("record_type")).isEqualTo(recordType);
            
            // Check metrics increment
            assertThat(dailyMetrics.get("medication_records_created").get()).isEqualTo(1);
            assertThat(featureUsage.get("medication_tracking").get()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("복약 스케줄 생성을 추적해야 함")
    void trackMedicationScheduleCreated_ShouldTrackAndLog() {
        // given
        String userId = "patient456@example.com";
        String medicationName = "아스피린";
        int frequency = 3;
        
        try (MockedStatic<MDCUtil> mdcUtilMock = mockStatic(MDCUtil.class)) {
            mdcUtilMock.when(() -> MDCUtil.get(MDCUtil.CORRELATION_ID)).thenReturn("corr-sched-456");

            // when
            logger.trackMedicationScheduleCreated(userId, medicationName, frequency);

            // then
            ArgumentCaptor<BusinessEvent> eventCaptor = ArgumentCaptor.forClass(BusinessEvent.class);
            verify(structuredLogger).business(eventCaptor.capture());
            
            BusinessEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.getEventType()).isEqualTo("MEDICATION_SCHEDULE_CREATED");
            assertThat(capturedEvent.getEntity()).isEqualTo("MEDICATION_SCHEDULE");
            assertThat(capturedEvent.getAction()).isEqualTo("CREATE");
            assertThat(capturedEvent.getUserId()).isEqualTo(userId);
            assertThat(capturedEvent.getResult()).isEqualTo("SUCCESS");
            assertThat(capturedEvent.getMetadata().get("medication_name")).isEqualTo(medicationName);
            assertThat(capturedEvent.getMetadata().get("frequency")).isEqualTo(frequency);
            
            // Check metrics increment
            assertThat(dailyMetrics.get("medication_schedules_created").get()).isEqualTo(1);
            assertThat(featureUsage.get("schedule_management").get()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("부작용 보고를 추적해야 함")
    void trackSideEffectReported_ShouldTrackAndLog() {
        // given
        String userId = "patient789@example.com";
        String medicationName = "이부프로펜";
        String severity = "MODERATE";
        
        try (MockedStatic<MDCUtil> mdcUtilMock = mockStatic(MDCUtil.class)) {
            mdcUtilMock.when(() -> MDCUtil.get(MDCUtil.CORRELATION_ID)).thenReturn("corr-side-789");

            // when
            logger.trackSideEffectReported(userId, medicationName, severity);

            // then
            ArgumentCaptor<BusinessEvent> eventCaptor = ArgumentCaptor.forClass(BusinessEvent.class);
            verify(structuredLogger).business(eventCaptor.capture());
            
            BusinessEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.getEventType()).isEqualTo("SIDE_EFFECT_REPORTED");
            assertThat(capturedEvent.getEntity()).isEqualTo("SIDE_EFFECT");
            assertThat(capturedEvent.getAction()).isEqualTo("REPORT");
            assertThat(capturedEvent.getUserId()).isEqualTo(userId);
            assertThat(capturedEvent.getResult()).isEqualTo("SUCCESS");
            assertThat(capturedEvent.getMetadata().get("medication_name")).isEqualTo(medicationName);
            assertThat(capturedEvent.getMetadata().get("severity")).isEqualTo(severity);
            
            // Check metrics increment
            assertThat(dailyMetrics.get("side_effects_reported").get()).isEqualTo(1);
            assertThat(featureUsage.get("side_effect_tracking").get()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("API 사용량을 추적해야 함")
    void trackApiUsage_ShouldTrackAndLog() {
        // given
        String endpoint = "/api/medication/schedules";
        String method = "GET";
        int statusCode = 200;
        long duration = 150L;
        
        try (MockedStatic<MDCUtil> mdcUtilMock = mockStatic(MDCUtil.class)) {
            mdcUtilMock.when(() -> MDCUtil.get(MDCUtil.CORRELATION_ID)).thenReturn("corr-api-123");

            // when
            logger.trackApiUsage(endpoint, method, statusCode, duration);

            // then
            ArgumentCaptor<BusinessEvent> eventCaptor = ArgumentCaptor.forClass(BusinessEvent.class);
            verify(structuredLogger).business(eventCaptor.capture());
            
            BusinessEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.getEventType()).isEqualTo("API_USAGE");
            assertThat(capturedEvent.getEntity()).isEqualTo("API_ENDPOINT");
            assertThat(capturedEvent.getAction()).isEqualTo("GET");
            assertThat(capturedEvent.getResult()).isEqualTo("SUCCESS");
            assertThat(capturedEvent.getMetadata().get("endpoint")).isEqualTo(endpoint);
            assertThat(capturedEvent.getMetadata().get("method")).isEqualTo(method);
            assertThat(capturedEvent.getMetadata().get("status_code")).isEqualTo(statusCode);
            assertThat(capturedEvent.getMetadata().get("duration_ms")).isEqualTo(duration);
            
            // Check metrics increment
            assertThat(dailyMetrics.get("api_calls").get()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("에러 상태 코드로 API 사용량 추적 시 에러율을 증가시켜야 함")
    void trackApiUsage_ErrorStatusCode_ShouldIncrementErrorRate() {
        // given
        String endpoint = "/api/medication/schedules";
        String method = "POST";
        int statusCode = 500;
        long duration = 5000L;
        
        try (MockedStatic<MDCUtil> mdcUtilMock = mockStatic(MDCUtil.class)) {
            mdcUtilMock.when(() -> MDCUtil.get(MDCUtil.CORRELATION_ID)).thenReturn("corr-api-error");

            // when
            logger.trackApiUsage(endpoint, method, statusCode, duration);

            // then
            ArgumentCaptor<BusinessEvent> eventCaptor = ArgumentCaptor.forClass(BusinessEvent.class);
            verify(structuredLogger).business(eventCaptor.capture());
            
            BusinessEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.getResult()).isEqualTo("ERROR");
            
            // Check both API calls and error rate increment
            assertThat(dailyMetrics.get("api_calls").get()).isEqualTo(1);
            assertThat(dailyMetrics.get("error_rate").get()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("기능 사용량을 추적해야 함")
    void trackFeatureUsage_ShouldTrackAndLog() {
        // given
        String featureName = "medication_reminder";
        String userId = "user123@example.com";
        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put("reminder_type", "PUSH_NOTIFICATION");
        additionalData.put("scheduled_time", "09:00");
        
        try (MockedStatic<MDCUtil> mdcUtilMock = mockStatic(MDCUtil.class)) {
            mdcUtilMock.when(() -> MDCUtil.get(MDCUtil.CORRELATION_ID)).thenReturn("corr-feature-123");

            // when
            logger.trackFeatureUsage(featureName, userId, additionalData);

            // then
            ArgumentCaptor<BusinessEvent> eventCaptor = ArgumentCaptor.forClass(BusinessEvent.class);
            verify(structuredLogger).business(eventCaptor.capture());
            
            BusinessEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.getEventType()).isEqualTo("FEATURE_USAGE");
            assertThat(capturedEvent.getEntity()).isEqualTo("FEATURE");
            assertThat(capturedEvent.getAction()).isEqualTo("USE");
            assertThat(capturedEvent.getUserId()).isEqualTo(userId);
            assertThat(capturedEvent.getResult()).isEqualTo("SUCCESS");
            assertThat(capturedEvent.getMetadata().get("feature")).isEqualTo(featureName);
            assertThat(capturedEvent.getMetadata().get("reminder_type")).isEqualTo("PUSH_NOTIFICATION");
            assertThat(capturedEvent.getMetadata().get("scheduled_time")).isEqualTo("09:00");
            
            // Check feature usage increment
            assertThat(featureUsage.get(featureName).get()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("예외 발생 시 오류를 로그하고 계속 실행해야 함")
    void trackUserRegistration_ExceptionOccurs_ShouldLogErrorAndContinue() {
        // given
        String userId = "user@example.com";
        String registrationMethod = "EMAIL";
        
        doThrow(new RuntimeException("Structured logger error")).when(structuredLogger).business(any());
        
        try (MockedStatic<MDCUtil> mdcUtilMock = mockStatic(MDCUtil.class)) {
            mdcUtilMock.when(() -> MDCUtil.get(MDCUtil.IP_ADDRESS)).thenReturn("192.168.1.1");
            mdcUtilMock.when(() -> MDCUtil.get(MDCUtil.USER_AGENT)).thenReturn("Mozilla/5.0");
            mdcUtilMock.when(() -> MDCUtil.get(MDCUtil.CORRELATION_ID)).thenReturn("corr-123");

            // when & then - should not throw exception
            logger.trackUserRegistration(userId, registrationMethod);
            
            // Metrics should still be incremented even if logging fails
            assertThat(dailyMetrics.get("user_registrations").get()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("여러 번 호출 시 메트릭이 누적되어야 함")
    void multipleTrackingCalls_ShouldAccumulateMetrics() {
        // given
        String userId1 = "user1@example.com";
        String userId2 = "user2@example.com";
        
        try (MockedStatic<MDCUtil> mdcUtilMock = mockStatic(MDCUtil.class)) {
            mdcUtilMock.when(() -> MDCUtil.get(MDCUtil.IP_ADDRESS)).thenReturn("192.168.1.1");
            mdcUtilMock.when(() -> MDCUtil.get(MDCUtil.USER_AGENT)).thenReturn("Mozilla/5.0");
            mdcUtilMock.when(() -> MDCUtil.get(MDCUtil.CORRELATION_ID)).thenReturn("corr-multi");

            // when
            logger.trackUserRegistration(userId1, "EMAIL");
            logger.trackUserRegistration(userId2, "OAUTH");
            logger.trackUserLogin(userId1, "PASSWORD", true);
            
            // then
            assertThat(dailyMetrics.get("user_registrations").get()).isEqualTo(2);
            assertThat(dailyMetrics.get("user_logins").get()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("null 값을 안전하게 처리해야 함")
    void trackUserRegistration_NullValues_ShouldHandleSafely() {
        // given
        String userId = null;
        String registrationMethod = null;
        
        try (MockedStatic<MDCUtil> mdcUtilMock = mockStatic(MDCUtil.class)) {
            mdcUtilMock.when(() -> MDCUtil.get(MDCUtil.IP_ADDRESS)).thenReturn(null);
            mdcUtilMock.when(() -> MDCUtil.get(MDCUtil.USER_AGENT)).thenReturn(null);
            mdcUtilMock.when(() -> MDCUtil.get(MDCUtil.CORRELATION_ID)).thenReturn(null);

            // when & then - should not throw exception
            logger.trackUserRegistration(userId, registrationMethod);
            
            verify(structuredLogger).business(any(BusinessEvent.class));
            assertThat(dailyMetrics.get("user_registrations").get()).isEqualTo(1);
        }
    }
}