package com.Hamalog.logging.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PerformanceEvent Tests")
class PerformanceEventTest {

    @Test
    @DisplayName("빌더 패턴을 사용하여 PerformanceEvent를 생성할 수 있어야 함")
    void builder_ShouldCreatePerformanceEventWithAllFields() {
        // given
        // when
        PerformanceEvent event = PerformanceEvent.builder()
                .operation("데이터베이스 조회")
                .durationMs(150L)
                .durationNanos(150_000_000L)
                .performanceLevel("NORMAL")
                .success(true)
                .errorType(null)
                .userId("user123")
                .methodName("findMedicationSchedulesByUserId")
                .className("MedicationScheduleService")
                .memoryBefore(1024L * 1024L) // 1MB
                .memoryAfter(1536L * 1024L) // 1.5MB
                .cpuTime(120L)
                .build();

        // then
        assertThat(event.getOperation()).isEqualTo("데이터베이스 조회");
        assertThat(event.getDurationMs()).isEqualTo(150L);
        assertThat(event.getDurationNanos()).isEqualTo(150_000_000L);
        assertThat(event.getPerformanceLevel()).isEqualTo("NORMAL");
        assertThat(event.isSuccess()).isTrue();
        assertThat(event.getErrorType()).isNull();
        assertThat(event.getUserId()).isEqualTo("user123");
        assertThat(event.getMethodName()).isEqualTo("findMedicationSchedulesByUserId");
        assertThat(event.getClassName()).isEqualTo("MedicationScheduleService");
        assertThat(event.getMemoryBefore()).isEqualTo(1024L * 1024L);
        assertThat(event.getMemoryAfter()).isEqualTo(1536L * 1024L);
        assertThat(event.getCpuTime()).isEqualTo(120L);
    }

    @Test
    @DisplayName("빈 PerformanceEvent를 생성할 수 있어야 함")
    void builder_ShouldCreateEmptyPerformanceEvent() {
        // given
        // when
        PerformanceEvent event = PerformanceEvent.builder().build();

        // then
        assertThat(event.getOperation()).isNull();
        assertThat(event.getDurationMs()).isEqualTo(0L);
        assertThat(event.getDurationNanos()).isEqualTo(0L);
        assertThat(event.getPerformanceLevel()).isNull();
        assertThat(event.isSuccess()).isFalse();
        assertThat(event.getErrorType()).isNull();
        assertThat(event.getUserId()).isNull();
        assertThat(event.getMethodName()).isNull();
        assertThat(event.getClassName()).isNull();
        assertThat(event.getMemoryBefore()).isNull();
        assertThat(event.getMemoryAfter()).isNull();
        assertThat(event.getCpuTime()).isNull();
    }

    @Test
    @DisplayName("빌더 패턴은 체이닝을 지원해야 함")
    void builder_ShouldSupportMethodChaining() {
        // given
        // when
        PerformanceEvent event = PerformanceEvent.builder()
                .operation("API 호출")
                .durationMs(50L)
                .success(true)
                .build();

        // then
        assertThat(event.getOperation()).isEqualTo("API 호출");
        assertThat(event.getDurationMs()).isEqualTo(50L);
        assertThat(event.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("다양한 성능 레벨을 처리할 수 있어야 함")
    void builder_ShouldHandleVariousPerformanceLevels() {
        // given & when
        PerformanceEvent fastEvent = PerformanceEvent.builder()
                .performanceLevel("FAST")
                .build();
        
        PerformanceEvent normalEvent = PerformanceEvent.builder()
                .performanceLevel("NORMAL")
                .build();
        
        PerformanceEvent slowEvent = PerformanceEvent.builder()
                .performanceLevel("SLOW")
                .build();
        
        PerformanceEvent criticalEvent = PerformanceEvent.builder()
                .performanceLevel("CRITICAL")
                .build();

        // then
        assertThat(fastEvent.getPerformanceLevel()).isEqualTo("FAST");
        assertThat(normalEvent.getPerformanceLevel()).isEqualTo("NORMAL");
        assertThat(slowEvent.getPerformanceLevel()).isEqualTo("SLOW");
        assertThat(criticalEvent.getPerformanceLevel()).isEqualTo("CRITICAL");
    }

    @Test
    @DisplayName("성공과 실패 상태를 처리할 수 있어야 함")
    void builder_ShouldHandleSuccessAndFailureStates() {
        // given & when
        PerformanceEvent successEvent = PerformanceEvent.builder()
                .success(true)
                .build();
        
        PerformanceEvent failureEvent = PerformanceEvent.builder()
                .success(false)
                .errorType("DATABASE_CONNECTION_TIMEOUT")
                .build();

        // then
        assertThat(successEvent.isSuccess()).isTrue();
        assertThat(successEvent.getErrorType()).isNull();
        
        assertThat(failureEvent.isSuccess()).isFalse();
        assertThat(failureEvent.getErrorType()).isEqualTo("DATABASE_CONNECTION_TIMEOUT");
    }

    @Test
    @DisplayName("다양한 에러 타입을 처리할 수 있어야 함")
    void builder_ShouldHandleVariousErrorTypes() {
        // given & when
        PerformanceEvent timeoutEvent = PerformanceEvent.builder()
                .errorType("TIMEOUT")
                .build();
        
        PerformanceEvent memoryEvent = PerformanceEvent.builder()
                .errorType("OUT_OF_MEMORY")
                .build();
        
        PerformanceEvent networkEvent = PerformanceEvent.builder()
                .errorType("NETWORK_ERROR")
                .build();

        // then
        assertThat(timeoutEvent.getErrorType()).isEqualTo("TIMEOUT");
        assertThat(memoryEvent.getErrorType()).isEqualTo("OUT_OF_MEMORY");
        assertThat(networkEvent.getErrorType()).isEqualTo("NETWORK_ERROR");
    }

    @Test
    @DisplayName("긴 실행 시간을 처리할 수 있어야 함")
    void builder_ShouldHandleLongDurations() {
        // given
        long longDurationMs = 30_000L; // 30 seconds
        long longDurationNanos = 30_000_000_000L; // 30 seconds in nanos

        // when
        PerformanceEvent event = PerformanceEvent.builder()
                .durationMs(longDurationMs)
                .durationNanos(longDurationNanos)
                .build();

        // then
        assertThat(event.getDurationMs()).isEqualTo(longDurationMs);
        assertThat(event.getDurationNanos()).isEqualTo(longDurationNanos);
    }

    @Test
    @DisplayName("메모리 사용량을 올바르게 처리해야 함")
    void builder_ShouldHandleMemoryUsage() {
        // given
        Long memoryBefore = 512L * 1024L * 1024L; // 512MB
        Long memoryAfter = 1024L * 1024L * 1024L; // 1GB

        // when
        PerformanceEvent event = PerformanceEvent.builder()
                .memoryBefore(memoryBefore)
                .memoryAfter(memoryAfter)
                .build();

        // then
        assertThat(event.getMemoryBefore()).isEqualTo(memoryBefore);
        assertThat(event.getMemoryAfter()).isEqualTo(memoryAfter);
        assertThat(event.getMemoryAfter() - event.getMemoryBefore())
                .isEqualTo(512L * 1024L * 1024L); // 512MB increase
    }

    @Test
    @DisplayName("CPU 시간을 처리할 수 있어야 함")
    void builder_ShouldHandleCpuTime() {
        // given
        Long cpuTime = 1500L; // 1.5 seconds in milliseconds

        // when
        PerformanceEvent event = PerformanceEvent.builder()
                .cpuTime(cpuTime)
                .build();

        // then
        assertThat(event.getCpuTime()).isEqualTo(cpuTime);
    }

    @Test
    @DisplayName("메소드와 클래스 정보를 처리할 수 있어야 함")
    void builder_ShouldHandleMethodAndClassInformation() {
        // given
        // when
        PerformanceEvent event = PerformanceEvent.builder()
                .methodName("createMedicationSchedule")
                .className("com.Hamalog.service.medication.MedicationScheduleService")
                .build();

        // then
        assertThat(event.getMethodName()).isEqualTo("createMedicationSchedule");
        assertThat(event.getClassName()).isEqualTo("com.Hamalog.service.medication.MedicationScheduleService");
    }

    @Test
    @DisplayName("null 값을 처리할 수 있어야 함")
    void builder_ShouldHandleNullValues() {
        // given
        // when
        PerformanceEvent event = PerformanceEvent.builder()
                .operation("테스트 연산")
                .userId(null)
                .errorType(null)
                .memoryBefore(null)
                .build();

        // then
        assertThat(event.getOperation()).isEqualTo("테스트 연산");
        assertThat(event.getUserId()).isNull();
        assertThat(event.getErrorType()).isNull();
        assertThat(event.getMemoryBefore()).isNull();
    }

    @Test
    @DisplayName("실제 성능 모니터링 시나리오를 처리할 수 있어야 함")
    void builder_ShouldHandleRealPerformanceScenario() {
        // given
        // when
        PerformanceEvent event = PerformanceEvent.builder()
                .operation("복약 스케줄 목록 조회")
                .durationMs(2500L)
                .durationNanos(2_500_000_000L)
                .performanceLevel("SLOW")
                .success(true)
                .userId("patient_123")
                .methodName("getMedicationSchedules")
                .className("MedicationScheduleController")
                .memoryBefore(200L * 1024L * 1024L) // 200MB
                .memoryAfter(250L * 1024L * 1024L)  // 250MB
                .cpuTime(2300L)
                .build();

        // then
        assertThat(event.getOperation()).isEqualTo("복약 스케줄 목록 조회");
        assertThat(event.getDurationMs()).isEqualTo(2500L);
        assertThat(event.getPerformanceLevel()).isEqualTo("SLOW");
        assertThat(event.isSuccess()).isTrue();
        assertThat(event.getUserId()).isEqualTo("patient_123");
        assertThat(event.getMethodName()).isEqualTo("getMedicationSchedules");
        assertThat(event.getClassName()).isEqualTo("MedicationScheduleController");
        
        // Memory usage calculation
        long memoryUsed = event.getMemoryAfter() - event.getMemoryBefore();
        assertThat(memoryUsed).isEqualTo(50L * 1024L * 1024L); // 50MB used
        
        // Performance is slower than CPU time (indicates I/O wait)
        assertThat(event.getDurationMs()).isGreaterThan(event.getCpuTime());
    }

    @Test
    @DisplayName("극도로 빠른 성능을 처리할 수 있어야 함")
    void builder_ShouldHandleVeryFastPerformance() {
        // given
        // when
        PerformanceEvent event = PerformanceEvent.builder()
                .operation("캐시 조회")
                .durationMs(1L)
                .durationNanos(500_000L) // 0.5ms
                .performanceLevel("FAST")
                .success(true)
                .build();

        // then
        assertThat(event.getDurationMs()).isEqualTo(1L);
        assertThat(event.getDurationNanos()).isEqualTo(500_000L);
        assertThat(event.getPerformanceLevel()).isEqualTo("FAST");
        assertThat(event.isSuccess()).isTrue();
    }
}