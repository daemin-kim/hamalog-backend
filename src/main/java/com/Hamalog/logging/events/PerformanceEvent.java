package com.Hamalog.logging.events;

/**
 * Performance event model for structured performance logging
 */
public class PerformanceEvent {
    private String operation;
    private long durationMs;
    private long durationNanos;
    private String performanceLevel;
    private boolean success;
    private String errorType;
    private String userId;
    private String methodName;
    private String className;
    private Long memoryBefore;
    private Long memoryAfter;
    private Long cpuTime;

    public static PerformanceEventBuilder builder() {
        return new PerformanceEventBuilder();
    }

    // Getters
    public String getOperation() { return operation; }
    public long getDurationMs() { return durationMs; }
    public long getDurationNanos() { return durationNanos; }
    public String getPerformanceLevel() { return performanceLevel; }
    public boolean isSuccess() { return success; }
    public String getErrorType() { return errorType; }
    public String getUserId() { return userId; }
    public String getMethodName() { return methodName; }
    public String getClassName() { return className; }
    public Long getMemoryBefore() { return memoryBefore; }
    public Long getMemoryAfter() { return memoryAfter; }
    public Long getCpuTime() { return cpuTime; }

    // Private constructor - use builder
    private PerformanceEvent() {}

    public static class PerformanceEventBuilder {
        private final PerformanceEvent event = new PerformanceEvent();

        public PerformanceEventBuilder operation(String operation) {
            event.operation = operation;
            return this;
        }

        public PerformanceEventBuilder durationMs(long durationMs) {
            event.durationMs = durationMs;
            return this;
        }

        public PerformanceEventBuilder durationNanos(long durationNanos) {
            event.durationNanos = durationNanos;
            return this;
        }

        public PerformanceEventBuilder performanceLevel(String performanceLevel) {
            event.performanceLevel = performanceLevel;
            return this;
        }

        public PerformanceEventBuilder success(boolean success) {
            event.success = success;
            return this;
        }

        public PerformanceEventBuilder errorType(String errorType) {
            event.errorType = errorType;
            return this;
        }

        public PerformanceEventBuilder userId(String userId) {
            event.userId = userId;
            return this;
        }

        public PerformanceEventBuilder methodName(String methodName) {
            event.methodName = methodName;
            return this;
        }

        public PerformanceEventBuilder className(String className) {
            event.className = className;
            return this;
        }

        public PerformanceEventBuilder memoryBefore(Long memoryBefore) {
            event.memoryBefore = memoryBefore;
            return this;
        }

        public PerformanceEventBuilder memoryAfter(Long memoryAfter) {
            event.memoryAfter = memoryAfter;
            return this;
        }

        public PerformanceEventBuilder cpuTime(Long cpuTime) {
            event.cpuTime = cpuTime;
            return this;
        }

        public PerformanceEvent build() {
            return event;
        }
    }
}