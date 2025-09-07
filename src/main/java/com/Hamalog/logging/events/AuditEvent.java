package com.Hamalog.logging.events;

/**
 * Audit event model for structured audit logging
 */
public class AuditEvent {
    private String operation;
    private String entityType;
    private String entityId;
    private String userId;
    private String ipAddress;
    private String userAgent;
    private String status;
    private String details;

    public static AuditEventBuilder builder() {
        return new AuditEventBuilder();
    }

    // Getters
    public String getOperation() { return operation; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getUserId() { return userId; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public String getStatus() { return status; }
    public String getDetails() { return details; }

    // Private constructor - use builder
    private AuditEvent() {}

    public static class AuditEventBuilder {
        private final AuditEvent event = new AuditEvent();

        public AuditEventBuilder operation(String operation) {
            event.operation = operation;
            return this;
        }

        public AuditEventBuilder entityType(String entityType) {
            event.entityType = entityType;
            return this;
        }

        public AuditEventBuilder entityId(String entityId) {
            event.entityId = entityId;
            return this;
        }

        public AuditEventBuilder userId(String userId) {
            event.userId = userId;
            return this;
        }

        public AuditEventBuilder ipAddress(String ipAddress) {
            event.ipAddress = ipAddress;
            return this;
        }

        public AuditEventBuilder userAgent(String userAgent) {
            event.userAgent = userAgent;
            return this;
        }

        public AuditEventBuilder status(String status) {
            event.status = status;
            return this;
        }

        public AuditEventBuilder details(String details) {
            event.details = details;
            return this;
        }

        public AuditEvent build() {
            return event;
        }
    }
}