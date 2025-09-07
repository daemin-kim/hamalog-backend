package com.Hamalog.logging.events;

import java.util.Map;

/**
 * API event model for structured API logging
 */
public class ApiEvent {
    private String httpMethod;
    private String path;
    private String controller;
    private String action;
    private String userId;
    private String ipAddress;
    private String userAgent;
    private long durationMs;
    private int statusCode;
    private Long requestSize;
    private Long responseSize;
    private Map<String, Object> parameters;

    public static ApiEventBuilder builder() {
        return new ApiEventBuilder();
    }

    // Getters
    public String getHttpMethod() { return httpMethod; }
    public String getPath() { return path; }
    public String getController() { return controller; }
    public String getAction() { return action; }
    public String getUserId() { return userId; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public long getDurationMs() { return durationMs; }
    public int getStatusCode() { return statusCode; }
    public Long getRequestSize() { return requestSize; }
    public Long getResponseSize() { return responseSize; }
    public Map<String, Object> getParameters() { return parameters; }

    // Private constructor - use builder
    private ApiEvent() {}

    public static class ApiEventBuilder {
        private final ApiEvent event = new ApiEvent();

        public ApiEventBuilder httpMethod(String httpMethod) {
            event.httpMethod = httpMethod;
            return this;
        }

        public ApiEventBuilder path(String path) {
            event.path = path;
            return this;
        }

        public ApiEventBuilder controller(String controller) {
            event.controller = controller;
            return this;
        }

        public ApiEventBuilder action(String action) {
            event.action = action;
            return this;
        }

        public ApiEventBuilder userId(String userId) {
            event.userId = userId;
            return this;
        }

        public ApiEventBuilder ipAddress(String ipAddress) {
            event.ipAddress = ipAddress;
            return this;
        }

        public ApiEventBuilder userAgent(String userAgent) {
            event.userAgent = userAgent;
            return this;
        }

        public ApiEventBuilder durationMs(long durationMs) {
            event.durationMs = durationMs;
            return this;
        }

        public ApiEventBuilder statusCode(int statusCode) {
            event.statusCode = statusCode;
            return this;
        }

        public ApiEventBuilder requestSize(Long requestSize) {
            event.requestSize = requestSize;
            return this;
        }

        public ApiEventBuilder responseSize(Long responseSize) {
            event.responseSize = responseSize;
            return this;
        }

        public ApiEventBuilder parameters(Map<String, Object> parameters) {
            event.parameters = parameters;
            return this;
        }

        public ApiEvent build() {
            return event;
        }
    }
}