package com.Hamalog.logging.events;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PerformanceEventTest {

    @Test
    void builderShouldCreateEventWithAllProperties() {
        // given
        String operation = "testOperation";
        long durationMs = 100L;
        long durationNanos = 100000000L;
        String performanceLevel = "NORMAL";
        boolean success = true;
        String errorType = null;
        String userId = "user123";
        String methodName = "testMethod";
        String className = "TestClass";
        Long memoryBefore = 1000L;
        Long memoryAfter = 1500L;
        Long cpuTime = 50L;

        // when
        PerformanceEvent event = PerformanceEvent.builder()
                .operation(operation)
                .durationMs(durationMs)
                .durationNanos(durationNanos)
                .performanceLevel(performanceLevel)
                .success(success)
                .errorType(errorType)
                .userId(userId)
                .methodName(methodName)
                .className(className)
                .memoryBefore(memoryBefore)
                .memoryAfter(memoryAfter)
                .cpuTime(cpuTime)
                .build();

        // then
        assertEquals(operation, event.getOperation());
        assertEquals(durationMs, event.getDurationMs());
        assertEquals(durationNanos, event.getDurationNanos());
        assertEquals(performanceLevel, event.getPerformanceLevel());
        assertEquals(success, event.isSuccess());
        assertEquals(errorType, event.getErrorType());
        assertEquals(userId, event.getUserId());
        assertEquals(methodName, event.getMethodName());
        assertEquals(className, event.getClassName());
        assertEquals(memoryBefore, event.getMemoryBefore());
        assertEquals(memoryAfter, event.getMemoryAfter());
        assertEquals(cpuTime, event.getCpuTime());
    }

    @Test
    void builderShouldCreateEventWithMinimalProperties() {
        // given
        String operation = "minimalOperation";

        // when
        PerformanceEvent event = PerformanceEvent.builder()
                .operation(operation)
                .build();

        // then
        assertEquals(operation, event.getOperation());
        assertEquals(0L, event.getDurationMs());
        assertEquals(0L, event.getDurationNanos());
        assertNull(event.getPerformanceLevel());
        assertFalse(event.isSuccess());
        assertNull(event.getErrorType());
        assertNull(event.getUserId());
        assertNull(event.getMethodName());
        assertNull(event.getClassName());
        assertNull(event.getMemoryBefore());
        assertNull(event.getMemoryAfter());
        assertNull(event.getCpuTime());
    }

    @Test
    void builderShouldAllowNullValues() {
        // when
        PerformanceEvent event = PerformanceEvent.builder()
                .operation(null)
                .errorType(null)
                .userId(null)
                .methodName(null)
                .className(null)
                .memoryBefore(null)
                .memoryAfter(null)
                .cpuTime(null)
                .build();

        // then
        assertNull(event.getOperation());
        assertNull(event.getErrorType());
        assertNull(event.getUserId());
        assertNull(event.getMethodName());
        assertNull(event.getClassName());
        assertNull(event.getMemoryBefore());
        assertNull(event.getMemoryAfter());
        assertNull(event.getCpuTime());
    }
}
