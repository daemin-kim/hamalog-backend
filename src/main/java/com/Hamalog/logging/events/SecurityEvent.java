package com.Hamalog.logging.events;

/**
 * Security event model for structured security logging
 */
public class SecurityEvent {
    private String eventType;
    private String userId;
    private String ipAddress;
    private String userAgent;
    private String resource;
    private String action;
    private String result;
    private String riskLevel;
    private String details;

    public static SecurityEventBuilder builder() {
        return new SecurityEventBuilder();
    }

    // Getters
    public String getEventType() { return eventType; }
    public String getUserId() { return userId; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public String getResource() { return resource; }
    public String getAction() { return action; }
    public String getResult() { return result; }
    public String getRiskLevel() { return riskLevel; }
    public String getDetails() { return details; }

    // Private constructor - use builder
    private SecurityEvent() {}

    public static class SecurityEventBuilder {
        private final SecurityEvent event = new SecurityEvent();

        public SecurityEventBuilder eventType(String eventType) {
            event.eventType = eventType;
            return this;
        }

        public SecurityEventBuilder userId(String userId) {
            event.userId = userId;
            return this;
        }

        public SecurityEventBuilder ipAddress(String ipAddress) {
            event.ipAddress = ipAddress;
            return this;
        }

        public SecurityEventBuilder userAgent(String userAgent) {
            event.userAgent = userAgent;
            return this;
        }

        public SecurityEventBuilder resource(String resource) {
            event.resource = resource;
            return this;
        }

        public SecurityEventBuilder action(String action) {
            event.action = action;
            return this;
        }

        public SecurityEventBuilder result(String result) {
            event.result = result;
            return this;
        }

        public SecurityEventBuilder riskLevel(String riskLevel) {
            event.riskLevel = riskLevel;
            return this;
        }

        public SecurityEventBuilder details(String details) {
            event.details = details;
            return this;
        }

        public SecurityEvent build() {
            return event;
        }
    }
}