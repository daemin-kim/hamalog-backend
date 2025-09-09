package com.Hamalog.logging.business;

import com.Hamalog.logging.MDCUtil;
import com.Hamalog.logging.StructuredLogger;
import com.Hamalog.logging.events.BusinessEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Production-ready business intelligence logger that tracks user behavior,
 * feature usage analytics, and business metrics for operational insights
 */
@Slf4j
@Component
public class BusinessIntelligenceLogger {

    @Autowired
    private StructuredLogger structuredLogger;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    // In-memory metrics tracking
    private final ConcurrentHashMap<String, AtomicLong> dailyMetrics = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> featureUsage = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UserSessionTracker> userSessions = new ConcurrentHashMap<>();

    // Business metrics keys
    private static final String USER_REGISTRATIONS = "user_registrations";
    private static final String USER_LOGINS = "user_logins";
    private static final String MEDICATION_RECORDS_CREATED = "medication_records_created";
    private static final String MEDICATION_SCHEDULES_CREATED = "medication_schedules_created";
    private static final String SIDE_EFFECTS_REPORTED = "side_effects_reported";
    private static final String API_CALLS = "api_calls";
    private static final String ERROR_RATE = "error_rate";

    /**
     * Track user registration event
     */
    public void trackUserRegistration(String userId, String registrationMethod) {
        try {
            incrementDailyMetric(USER_REGISTRATIONS);
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("registration_method", registrationMethod);
            metadata.put("registration_date", LocalDate.now().toString());
            metadata.put("client_ip", MDCUtil.get(MDCUtil.IP_ADDRESS));
            metadata.put("user_agent", MDCUtil.get(MDCUtil.USER_AGENT));
            
            BusinessEvent event = BusinessEvent.builder()
                    .eventType("USER_REGISTRATION")
                    .entity("USER")
                    .action("REGISTER")
                    .userId(userId)
                    .result("SUCCESS")
                    .metadata(metadata)
                    .build();
            
            structuredLogger.business(event);
            
            log.info("USER_REGISTRATION: New user {} registered via {} - correlationId: {}", 
                userId, registrationMethod, MDCUtil.get(MDCUtil.CORRELATION_ID));
                
        } catch (Exception e) {
            log.error("Failed to track user registration", e);
        }
    }

    /**
     * Track user login event
     */
    public void trackUserLogin(String userId, String loginMethod, boolean isSuccessful) {
        try {
            if (isSuccessful) {
                incrementDailyMetric(USER_LOGINS);
                startUserSession(userId);
            }
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("login_method", loginMethod);
            metadata.put("login_time", LocalDateTime.now().toString());
            metadata.put("client_ip", MDCUtil.get(MDCUtil.IP_ADDRESS));
            metadata.put("user_agent", MDCUtil.get(MDCUtil.USER_AGENT));
            metadata.put("success", isSuccessful);
            
            BusinessEvent event = BusinessEvent.builder()
                    .eventType("USER_LOGIN")
                    .entity("USER")
                    .action("LOGIN")
                    .userId(userId)
                    .result(isSuccessful ? "SUCCESS" : "FAILURE")
                    .metadata(metadata)
                    .build();
            
            structuredLogger.business(event);
            
            log.info("USER_LOGIN: User {} login {} via {} - correlationId: {}", 
                userId, isSuccessful ? "successful" : "failed", loginMethod, MDCUtil.get(MDCUtil.CORRELATION_ID));
                
        } catch (Exception e) {
            log.error("Failed to track user login", e);
        }
    }

    /**
     * Track medication record creation
     */
    public void trackMedicationRecordCreated(String userId, String medicationName, String recordType) {
        try {
            incrementDailyMetric(MEDICATION_RECORDS_CREATED);
            incrementFeatureUsage("medication_tracking");
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("medication_name", medicationName);
            metadata.put("record_type", recordType);
            metadata.put("creation_time", LocalDateTime.now().toString());
            
            BusinessEvent event = BusinessEvent.builder()
                    .eventType("MEDICATION_RECORD_CREATED")
                    .entity("MEDICATION_RECORD")
                    .action("CREATE")
                    .userId(userId)
                    .result("SUCCESS")
                    .metadata(metadata)
                    .build();
            
            structuredLogger.business(event);
            
            log.info("MEDICATION_RECORD_CREATED: User {} created {} record for {} - correlationId: {}", 
                userId, recordType, medicationName, MDCUtil.get(MDCUtil.CORRELATION_ID));
                
        } catch (Exception e) {
            log.error("Failed to track medication record creation", e);
        }
    }

    /**
     * Track medication schedule creation
     */
    public void trackMedicationScheduleCreated(String userId, String medicationName, int frequency) {
        try {
            incrementDailyMetric(MEDICATION_SCHEDULES_CREATED);
            incrementFeatureUsage("schedule_management");
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("medication_name", medicationName);
            metadata.put("frequency", frequency);
            metadata.put("creation_time", LocalDateTime.now().toString());
            
            BusinessEvent event = BusinessEvent.builder()
                    .eventType("MEDICATION_SCHEDULE_CREATED")
                    .entity("MEDICATION_SCHEDULE")
                    .action("CREATE")
                    .userId(userId)
                    .result("SUCCESS")
                    .metadata(metadata)
                    .build();
            
            structuredLogger.business(event);
            
            log.info("MEDICATION_SCHEDULE_CREATED: User {} created schedule for {} ({}x daily) - correlationId: {}", 
                userId, medicationName, frequency, MDCUtil.get(MDCUtil.CORRELATION_ID));
                
        } catch (Exception e) {
            log.error("Failed to track medication schedule creation", e);
        }
    }

    /**
     * Track side effect reporting
     */
    public void trackSideEffectReported(String userId, String medicationName, String severity) {
        try {
            incrementDailyMetric(SIDE_EFFECTS_REPORTED);
            incrementFeatureUsage("side_effect_tracking");
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("medication_name", medicationName);
            metadata.put("severity", severity);
            metadata.put("report_time", LocalDateTime.now().toString());
            
            BusinessEvent event = BusinessEvent.builder()
                    .eventType("SIDE_EFFECT_REPORTED")
                    .entity("SIDE_EFFECT")
                    .action("REPORT")
                    .userId(userId)
                    .result("SUCCESS")
                    .metadata(metadata)
                    .build();
            
            structuredLogger.business(event);
            
            log.info("SIDE_EFFECT_REPORTED: User {} reported {} side effect for {} - correlationId: {}", 
                userId, severity, medicationName, MDCUtil.get(MDCUtil.CORRELATION_ID));
                
        } catch (Exception e) {
            log.error("Failed to track side effect report", e);
        }
    }

    /**
     * Track API usage
     */
    public void trackApiUsage(String endpoint, String method, int statusCode, long duration) {
        try {
            incrementDailyMetric(API_CALLS);
            incrementFeatureUsage("api_" + endpoint);
            
            if (statusCode >= 400) {
                incrementDailyMetric(ERROR_RATE);
            }
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("endpoint", endpoint);
            metadata.put("method", method);
            metadata.put("status_code", statusCode);
            metadata.put("duration_ms", duration);
            metadata.put("timestamp", LocalDateTime.now().toString());
            
            BusinessEvent event = BusinessEvent.builder()
                    .eventType("API_USAGE")
                    .entity("API_ENDPOINT")
                    .action(method)
                    .userId(MDCUtil.get(MDCUtil.USER_ID))
                    .result(statusCode >= 400 ? "ERROR" : "SUCCESS")
                    .metadata(metadata)
                    .build();
            
            structuredLogger.business(event);
            
        } catch (Exception e) {
            log.error("Failed to track API usage", e);
        }
    }

    /**
     * Track feature usage
     */
    public void trackFeatureUsage(String featureName, String userId, Map<String, Object> additionalData) {
        try {
            incrementFeatureUsage(featureName);
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("feature", featureName);
            metadata.put("usage_time", LocalDateTime.now().toString());
            if (additionalData != null) {
                metadata.putAll(additionalData);
            }
            
            BusinessEvent event = BusinessEvent.builder()
                    .eventType("FEATURE_USAGE")
                    .entity("FEATURE")
                    .action("USE")
                    .userId(userId)
                    .result("SUCCESS")
                    .metadata(metadata)
                    .build();
            
            structuredLogger.business(event);
            
            log.debug("FEATURE_USAGE: User {} used feature {} - correlationId: {}", 
                userId, featureName, MDCUtil.get(MDCUtil.CORRELATION_ID));
                
        } catch (Exception e) {
            log.error("Failed to track feature usage", e);
        }
    }

    /**
     * Log daily business metrics summary
     */
    @Scheduled(cron = "0 0 1 * * ?") // Every day at 1 AM
    public void logDailyBusinessMetrics() {
        try {
            Map<String, Object> dailyStats = new HashMap<>();
            
            for (Map.Entry<String, AtomicLong> entry : dailyMetrics.entrySet()) {
                dailyStats.put(entry.getKey(), entry.getValue().get());
            }
            
            // Calculate derived metrics
            long totalLogins = dailyMetrics.getOrDefault(USER_LOGINS, new AtomicLong(0)).get();
            long totalApiCalls = dailyMetrics.getOrDefault(API_CALLS, new AtomicLong(0)).get();
            long totalErrors = dailyMetrics.getOrDefault(ERROR_RATE, new AtomicLong(0)).get();
            
            double errorRate = totalApiCalls > 0 ? (double) totalErrors / totalApiCalls * 100 : 0;
            dailyStats.put("calculated_error_rate_percent", errorRate);
            dailyStats.put("active_users", userSessions.size());
            
            BusinessEvent event = BusinessEvent.builder()
                    .eventType("DAILY_METRICS_SUMMARY")
                    .entity("BUSINESS_METRICS")
                    .action("SUMMARY")
                    .userId("SYSTEM")
                    .result("SUCCESS")
                    .metadata(dailyStats)
                    .build();
            
            structuredLogger.business(event);
            
            log.info("DAILY_METRICS: Logins: {}, Registrations: {}, Med Records: {}, Side Effects: {}, API Calls: {}, Error Rate: {:.2f}%", 
                totalLogins, 
                dailyMetrics.getOrDefault(USER_REGISTRATIONS, new AtomicLong(0)).get(),
                dailyMetrics.getOrDefault(MEDICATION_RECORDS_CREATED, new AtomicLong(0)).get(),
                dailyMetrics.getOrDefault(SIDE_EFFECTS_REPORTED, new AtomicLong(0)).get(),
                totalApiCalls, 
                errorRate);
            
            // Reset daily metrics
            dailyMetrics.clear();
            
        } catch (Exception e) {
            log.error("Failed to log daily business metrics", e);
        }
    }

    /**
     * Log feature usage analytics
     */
    @Scheduled(cron = "0 0 2 * * ?") // Every day at 2 AM
    public void logFeatureUsageAnalytics() {
        try {
            Map<String, Object> featureStats = new HashMap<>();
            
            for (Map.Entry<String, AtomicLong> entry : featureUsage.entrySet()) {
                featureStats.put(entry.getKey(), entry.getValue().get());
            }
            
            BusinessEvent event = BusinessEvent.builder()
                    .eventType("FEATURE_USAGE_ANALYTICS")
                    .entity("FEATURE_ANALYTICS")
                    .action("SUMMARY")
                    .userId("SYSTEM")
                    .result("SUCCESS")
                    .metadata(featureStats)
                    .build();
            
            structuredLogger.business(event);
            
            log.info("FEATURE_USAGE_ANALYTICS: Top features - {}", formatTopFeatures(featureStats));
            
            // Reset feature usage metrics
            featureUsage.clear();
            
        } catch (Exception e) {
            log.error("Failed to log feature usage analytics", e);
        }
    }

    /**
     * Increment daily metric counter
     */
    private void incrementDailyMetric(String metricName) {
        dailyMetrics.computeIfAbsent(metricName, k -> new AtomicLong(0)).incrementAndGet();
        
        // Also store in Redis if available
        if (redisTemplate != null) {
            try {
                String key = "daily_metric:" + LocalDate.now() + ":" + metricName;
                redisTemplate.opsForValue().increment(key);
                redisTemplate.expire(key, 30, TimeUnit.DAYS); // Keep for 30 days
            } catch (Exception e) {
                log.debug("Failed to store metric in Redis", e);
            }
        }
    }

    /**
     * Increment feature usage counter
     */
    private void incrementFeatureUsage(String featureName) {
        featureUsage.computeIfAbsent(featureName, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * Start user session tracking
     */
    private void startUserSession(String userId) {
        userSessions.put(userId, new UserSessionTracker(LocalDateTime.now()));
    }

    /**
     * Format top features for logging
     */
    private String formatTopFeatures(Map<String, Object> featureStats) {
        return featureStats.entrySet().stream()
                .sorted((e1, e2) -> Long.compare((Long) e2.getValue(), (Long) e1.getValue()))
                .limit(5)
                .map(e -> e.getKey() + ":" + e.getValue())
                .reduce((a, b) -> a + ", " + b)
                .orElse("No feature usage");
    }

    /**
     * User session tracker for active user monitoring
     */
    private static class UserSessionTracker {
        private final LocalDateTime sessionStart;
        private LocalDateTime lastActivity;

        public UserSessionTracker(LocalDateTime sessionStart) {
            this.sessionStart = sessionStart;
            this.lastActivity = sessionStart;
        }

        public void updateLastActivity() {
            this.lastActivity = LocalDateTime.now();
        }

        public Duration getSessionDuration() {
            return Duration.between(sessionStart, lastActivity);
        }

        public boolean isExpired(Duration timeout) {
            return Duration.between(lastActivity, LocalDateTime.now()).compareTo(timeout) > 0;
        }
    }
}