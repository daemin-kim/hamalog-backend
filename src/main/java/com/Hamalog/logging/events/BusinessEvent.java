package com.Hamalog.logging.events;

import java.util.Map;

/**
 * Business event model for structured business logging
 */
public class BusinessEvent {
    private String eventType;
    private String entity;
    private String action;
    private String userId;
    private String result;
    private Map<String, Object> metadata;

    public static BusinessEventBuilder builder() {
        return new BusinessEventBuilder();
    }

    // Getters
    public String getEventType() { return eventType; }
    public String getEntity() { return entity; }
    public String getAction() { return action; }
    public String getUserId() { return userId; }
    public String getResult() { return result; }
    public Map<String, Object> getMetadata() { return metadata; }

    // Private constructor - use builder
    private BusinessEvent() {}

    public static class BusinessEventBuilder {
        private final BusinessEvent event = new BusinessEvent();

        public BusinessEventBuilder eventType(String eventType) {
            event.eventType = eventType;
            return this;
        }

        public BusinessEventBuilder entity(String entity) {
            event.entity = entity;
            return this;
        }

        public BusinessEventBuilder action(String action) {
            event.action = action;
            return this;
        }

        public BusinessEventBuilder userId(String userId) {
            event.userId = userId;
            return this;
        }

        public BusinessEventBuilder result(String result) {
            event.result = result;
            return this;
        }

        public BusinessEventBuilder metadata(Map<String, Object> metadata) {
            event.metadata = metadata;
            return this;
        }

        public BusinessEvent build() {
            return event;
        }
    }
}