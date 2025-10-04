package com.Hamalog.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RetryAspect Tests")
class RetryAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @InjectMocks
    private RetryAspect retryAspect;

    @Mock
    private RetryAspect.Retryable retryable;

    @BeforeEach
    void setUp() {
        // Set up default configuration values
        ReflectionTestUtils.setField(retryAspect, "defaultMaxAttempts", 3);
        ReflectionTestUtils.setField(retryAspect, "defaultDelay", 1000L);
        ReflectionTestUtils.setField(retryAspect, "maxDelay", 10000L);

        // Configure mock method signature
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn((Class) TestService.class);
        when(methodSignature.getName()).thenReturn("testMethod");

        // Clear MDC before each test
        MDC.clear();
    }

    @Test
    @DisplayName("Should execute successfully on first attempt")
    void retry_SuccessfulFirstAttempt_ShouldReturnResult() throws Throwable {
        // given
        String expectedResult = "success";
        when(joinPoint.proceed()).thenReturn(expectedResult);
        when(retryable.maxAttempts()).thenReturn(3);
        when(retryable.delay()).thenReturn(1000L);
        when(retryable.backoffMultiplier()).thenReturn(2.0);
        when(retryable.randomizeDelay()).thenReturn(false);
        lenient().when(retryable.retryOn()).thenReturn(new Class[0]);
        lenient().when(retryable.noRetryOn()).thenReturn(new Class[0]);

        // when
        Object result = retryAspect.retry(joinPoint, retryable);

        // then
        assertThat(result).isEqualTo(expectedResult);
        assertThat(MDC.get("retry.status")).isEqualTo("SUCCESS_FIRST_TRY");
        assertThat(MDC.get("retry.method")).isEqualTo("TestService.testMethod");
        verify(joinPoint, times(1)).proceed();
        verify(retryable, atLeast(1)).maxAttempts();
        verify(retryable, atLeast(1)).delay();
    }

    @Test
    @DisplayName("Should retry and succeed on second attempt")
    void retry_FailFirstSucceedSecond_ShouldRetryAndSucceed() throws Throwable {
        // given
        String expectedResult = "success";
        DataAccessException exception = new DataAccessException("Database connection failed") {};
        
        when(joinPoint.proceed())
                .thenThrow(exception)
                .thenReturn(expectedResult);
        
        when(retryable.maxAttempts()).thenReturn(3);
        when(retryable.delay()).thenReturn(100L); // Short delay for test speed
        when(retryable.backoffMultiplier()).thenReturn(1.0); // No backoff for test speed
        when(retryable.randomizeDelay()).thenReturn(false);
        when(retryable.retryOn()).thenReturn(new Class[0]);
        when(retryable.noRetryOn()).thenReturn(new Class[0]);

        // when
        Object result = retryAspect.retry(joinPoint, retryable);

        // then
        assertThat(result).isEqualTo(expectedResult);
        assertThat(MDC.get("retry.status")).isEqualTo("SUCCESS_AFTER_RETRY");
        assertThat(MDC.get("retry.currentAttempt")).isEqualTo("2");
        verify(joinPoint, times(2)).proceed();
    }

    @Test
    @DisplayName("Should exhaust all retries and throw last exception")
    void retry_AllAttemptsFail_ShouldExhaustRetriesAndThrow() throws Throwable {
        // given
        DataAccessException exception = new DataAccessException("Persistent database error") {};
        
        when(joinPoint.proceed()).thenThrow(exception);
        when(retryable.maxAttempts()).thenReturn(2);
        when(retryable.delay()).thenReturn(10L); // Very short delay for test speed
        when(retryable.backoffMultiplier()).thenReturn(1.0);
        when(retryable.randomizeDelay()).thenReturn(false);
        when(retryable.retryOn()).thenReturn(new Class[0]);
        when(retryable.noRetryOn()).thenReturn(new Class[0]);

        // when & then
        assertThatThrownBy(() -> retryAspect.retry(joinPoint, retryable))
                .isSameAs(exception);
        
        // MDC context is cleared after retry exhaustion, so we verify the core functionality
        verify(joinPoint, times(2)).proceed();
    }

    @Test
    @DisplayName("Should not retry for non-retryable exceptions by default")
    void retry_NonRetryableException_ShouldNotRetry() throws Throwable {
        // given
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");
        
        when(joinPoint.proceed()).thenThrow(exception);
        when(retryable.maxAttempts()).thenReturn(3);
        when(retryable.delay()).thenReturn(1000L);
        when(retryable.backoffMultiplier()).thenReturn(2.0);
        when(retryable.randomizeDelay()).thenReturn(false);
        when(retryable.retryOn()).thenReturn(new Class[0]);
        when(retryable.noRetryOn()).thenReturn(new Class[0]);

        // when & then
        assertThatThrownBy(() -> retryAspect.retry(joinPoint, retryable))
                .isSameAs(exception);
        
        assertThat(MDC.get("retry.status")).isEqualTo("NO_RETRY");
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    @DisplayName("Should retry specific exceptions when retryOn is configured")
    void retry_WithRetryOnConfiguration_ShouldRetrySpecificExceptions() throws Throwable {
        // given
        IllegalArgumentException exception = new IllegalArgumentException("Configured for retry");
        
        when(joinPoint.proceed()).thenThrow(exception);
        when(retryable.maxAttempts()).thenReturn(2);
        when(retryable.delay()).thenReturn(10L);
        when(retryable.backoffMultiplier()).thenReturn(1.0);
        when(retryable.randomizeDelay()).thenReturn(false);
        when(retryable.retryOn()).thenReturn(new Class[]{IllegalArgumentException.class});
        when(retryable.noRetryOn()).thenReturn(new Class[0]);

        // when & then
        assertThatThrownBy(() -> retryAspect.retry(joinPoint, retryable))
                .isSameAs(exception);
        
        // MDC context is cleared after retry exhaustion, so we verify the core functionality
        verify(joinPoint, times(2)).proceed();
    }

    @Test
    @DisplayName("Should not retry exceptions in noRetryOn configuration")
    void retry_WithNoRetryOnConfiguration_ShouldNotRetryExcludedExceptions() throws Throwable {
        // given
        DataAccessException exception = new DataAccessException("Should not retry this") {};
        
        when(joinPoint.proceed()).thenThrow(exception);
        when(retryable.maxAttempts()).thenReturn(3);
        when(retryable.delay()).thenReturn(1000L);
        when(retryable.backoffMultiplier()).thenReturn(2.0);
        when(retryable.randomizeDelay()).thenReturn(false);
        when(retryable.retryOn()).thenReturn(new Class[0]);
        when(retryable.noRetryOn()).thenReturn(new Class[]{DataAccessException.class});

        // when & then
        assertThatThrownBy(() -> retryAspect.retry(joinPoint, retryable))
                .isSameAs(exception);
        
        assertThat(MDC.get("retry.status")).isEqualTo("NO_RETRY");
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    @DisplayName("Should apply exponential backoff correctly")
    void retry_WithExponentialBackoff_ShouldCalculateDelaysCorrectly() throws Throwable {
        // given
        SocketTimeoutException exception = new SocketTimeoutException("Timeout");
        
        when(joinPoint.proceed()).thenThrow(exception);
        when(retryable.maxAttempts()).thenReturn(3);
        when(retryable.delay()).thenReturn(100L);
        when(retryable.backoffMultiplier()).thenReturn(2.0);
        when(retryable.randomizeDelay()).thenReturn(false);
        when(retryable.retryOn()).thenReturn(new Class[0]);
        when(retryable.noRetryOn()).thenReturn(new Class[0]);

        long startTime = System.currentTimeMillis();

        // when & then
        assertThatThrownBy(() -> retryAspect.retry(joinPoint, retryable))
                .isSameAs(exception);
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // Should have delays: 100ms + 200ms = 300ms minimum
        assertThat(totalTime).isGreaterThan(250L);
        // MDC context is cleared after retry exhaustion, so we verify the core functionality
        verify(joinPoint, times(3)).proceed();
    }

    @Test
    @DisplayName("Should use default values when annotation values are zero or negative")
    void retry_WithDefaultValues_ShouldUseConfiguredDefaults() throws Throwable {
        // given
        ResourceAccessException exception = new ResourceAccessException("Resource unavailable");
        
        when(joinPoint.proceed()).thenThrow(exception);
        when(retryable.maxAttempts()).thenReturn(0); // Should use default (3)
        when(retryable.delay()).thenReturn(0L); // Should use default (1000)
        when(retryable.backoffMultiplier()).thenReturn(2.0);
        when(retryable.randomizeDelay()).thenReturn(false);
        when(retryable.retryOn()).thenReturn(new Class[0]);
        when(retryable.noRetryOn()).thenReturn(new Class[0]);

        // when & then
        assertThatThrownBy(() -> retryAspect.retry(joinPoint, retryable))
                .isSameAs(exception);
        
        // MDC context is cleared after retry exhaustion, so we verify the core functionality
        // The test verifies that default maxAttempts (3) was used by checking the number of proceed() calls
        verify(joinPoint, times(3)).proceed();
    }

    @Test
    @DisplayName("Should handle timeout exceptions correctly")
    void retry_WithTimeoutExceptions_ShouldRetryCorrectly() throws Throwable {
        // given
        TimeoutException timeoutException = new TimeoutException("Operation timed out");
        ConnectException connectException = new ConnectException("Connection refused");
        SocketTimeoutException socketTimeoutException = new SocketTimeoutException("Socket timeout");
        
        when(retryable.maxAttempts()).thenReturn(2);
        when(retryable.delay()).thenReturn(10L);
        when(retryable.backoffMultiplier()).thenReturn(1.0);
        when(retryable.randomizeDelay()).thenReturn(false);
        when(retryable.retryOn()).thenReturn(new Class[0]);
        when(retryable.noRetryOn()).thenReturn(new Class[0]);

        // Test TimeoutException
        when(joinPoint.proceed()).thenThrow(timeoutException);
        assertThatThrownBy(() -> retryAspect.retry(joinPoint, retryable))
                .isSameAs(timeoutException);
        verify(joinPoint, times(2)).proceed();

        // Reset for next test
        reset(joinPoint);
        when(joinPoint.getSignature()).thenReturn(methodSignature);

        // Test ConnectException
        when(joinPoint.proceed()).thenThrow(connectException);
        assertThatThrownBy(() -> retryAspect.retry(joinPoint, retryable))
                .isSameAs(connectException);
        verify(joinPoint, times(2)).proceed();

        // Reset for next test
        reset(joinPoint);
        when(joinPoint.getSignature()).thenReturn(methodSignature);

        // Test SocketTimeoutException
        when(joinPoint.proceed()).thenThrow(socketTimeoutException);
        assertThatThrownBy(() -> retryAspect.retry(joinPoint, retryable))
                .isSameAs(socketTimeoutException);
        verify(joinPoint, times(2)).proceed();
    }

    @Test
    @DisplayName("Should handle RuntimeException with timeout message")
    void retry_WithTimeoutRuntimeException_ShouldRetry() throws Throwable {
        // given
        RuntimeException timeoutException = new RuntimeException("Connection timeout occurred");
        RuntimeException connectionException = new RuntimeException("Database connection failed");
        
        when(retryable.maxAttempts()).thenReturn(2);
        when(retryable.delay()).thenReturn(10L);
        when(retryable.backoffMultiplier()).thenReturn(1.0);
        when(retryable.randomizeDelay()).thenReturn(false);
        when(retryable.retryOn()).thenReturn(new Class[0]);
        when(retryable.noRetryOn()).thenReturn(new Class[0]);

        // Test timeout message
        when(joinPoint.proceed()).thenThrow(timeoutException);
        assertThatThrownBy(() -> retryAspect.retry(joinPoint, retryable))
                .isSameAs(timeoutException);
        verify(joinPoint, times(2)).proceed();

        // Reset for next test
        reset(joinPoint);
        when(joinPoint.getSignature()).thenReturn(methodSignature);

        // Test connection message
        when(joinPoint.proceed()).thenThrow(connectionException);
        assertThatThrownBy(() -> retryAspect.retry(joinPoint, retryable))
                .isSameAs(connectionException);
        verify(joinPoint, times(2)).proceed();
    }

    @Test
    @DisplayName("Should respect max delay limit")
    void retry_WithHighBackoffMultiplier_ShouldRespectMaxDelay() throws Throwable {
        // given
        DataAccessException exception = new DataAccessException("Database error") {};
        
        when(joinPoint.proceed()).thenThrow(exception);
        when(retryable.maxAttempts()).thenReturn(3);
        when(retryable.delay()).thenReturn(5000L); // High base delay
        when(retryable.backoffMultiplier()).thenReturn(10.0); // High multiplier
        when(retryable.randomizeDelay()).thenReturn(false);
        when(retryable.retryOn()).thenReturn(new Class[0]);
        when(retryable.noRetryOn()).thenReturn(new Class[0]);

        long startTime = System.currentTimeMillis();

        // when & then
        assertThatThrownBy(() -> retryAspect.retry(joinPoint, retryable))
                .isSameAs(exception);
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // Should be capped by maxDelay (10000ms) for both attempts
        // Total should be around 20000ms, but we test for reasonable upper bound
        assertThat(totalTime).isLessThan(25000L);
        verify(joinPoint, times(3)).proceed();
    }

    @Test
    @DisplayName("Should handle MDC context properly")
    void retry_ShouldSetAndClearMDCContext() throws Throwable {
        // given
        String expectedResult = "success";
        when(joinPoint.proceed()).thenReturn(expectedResult);
        when(retryable.maxAttempts()).thenReturn(3);
        when(retryable.delay()).thenReturn(1000L);
        when(retryable.backoffMultiplier()).thenReturn(2.0);
        when(retryable.randomizeDelay()).thenReturn(false);
        lenient().when(retryable.retryOn()).thenReturn(new Class[0]);
        lenient().when(retryable.noRetryOn()).thenReturn(new Class[0]);

        // when
        retryAspect.retry(joinPoint, retryable);

        // then
        assertThat(MDC.get("retry.id")).isNotNull();
        assertThat(MDC.get("retry.method")).isEqualTo("TestService.testMethod");
        assertThat(MDC.get("retry.maxAttempts")).isEqualTo("3");
        assertThat(MDC.get("retry.currentAttempt")).isEqualTo("1");
        assertThat(MDC.get("retry.status")).isEqualTo("SUCCESS_FIRST_TRY");
        
        // Verify mock interactions that actually occur on first attempt success
        verify(retryable, atLeast(1)).maxAttempts();
        verify(retryable, atLeast(1)).delay();
        verify(retryable, atLeast(1)).backoffMultiplier();
        verify(retryable, atLeast(1)).randomizeDelay();
        // retryOn() and noRetryOn() are not called on successful first attempt
    }

    @Test
    @DisplayName("Should handle interrupted exception properly")
    void retry_WhenInterrupted_ShouldHandleInterruptionCorrectly() throws Throwable {
        // given
        DataAccessException exception = new DataAccessException("Database error") {};
        
        when(joinPoint.proceed()).thenThrow(exception);
        when(retryable.maxAttempts()).thenReturn(3);
        when(retryable.delay()).thenReturn(10000L); // Long delay to test interruption
        when(retryable.backoffMultiplier()).thenReturn(1.0);
        when(retryable.randomizeDelay()).thenReturn(false);
        when(retryable.retryOn()).thenReturn(new Class[0]);
        when(retryable.noRetryOn()).thenReturn(new Class[0]);

        // Interrupt the current thread after a short delay
        Thread testThread = Thread.currentThread();
        Thread interruptThread = new Thread(() -> {
            try {
                Thread.sleep(100);
                testThread.interrupt();
            } catch (InterruptedException ignored) {}
        });
        interruptThread.start();

        // when & then
        assertThatThrownBy(() -> retryAspect.retry(joinPoint, retryable))
                .isSameAs(exception);
        
        // Thread should be interrupted
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        
        // Clean up
        Thread.interrupted(); // Clear interrupt status
        interruptThread.join();
    }

    // Helper class for testing
    private static class TestService {
        public String testMethod() {
            return "test";
        }
    }
}