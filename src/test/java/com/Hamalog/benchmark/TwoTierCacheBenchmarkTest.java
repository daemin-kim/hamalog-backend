package com.Hamalog.benchmark;

import com.Hamalog.aop.CachingAspect;
import com.Hamalog.domain.member.Member;
import com.Hamalog.repository.member.MemberRepository;
import com.Hamalog.service.auth.MemberCacheService;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 2-Tier 캐시 성능 벤치마크 테스트
 *
 * L1 (Caffeine) vs L2 (Redis) vs DB 성능 비교
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("2-Tier 캐시 성능 벤치마크")
class TwoTierCacheBenchmarkTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired(required = false)
    private MemberCacheService memberCacheService;

    @Autowired(required = false)
    private CachingAspect cachingAspect;

    private Member testMember;
    private static final int WARMUP_ITERATIONS = 5;
    private static final int BENCHMARK_ITERATIONS = 20;

    @BeforeEach
    void setUp() {
        // 테스트용 회원 생성
        testMember = memberRepository.findByLoginId("benchmark@test.com")
                .orElseGet(() -> memberRepository.save(
                        Member.builder()
                                .loginId("benchmark@test.com")
                                .password("password123")
                                .name("BenchmarkUser")
                                .nickName("벤치마크")
                                .phoneNumber("01012345678")
                                .birth(java.time.LocalDate.of(1990, 1, 1))
                                .createdAt(java.time.LocalDateTime.now())
                                .deletionScheduled(false)
                                .build()
                ));
    }

    @Test
    @DisplayName("DB 직접 조회 성능 측정")
    void measureDbDirectAccess() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📊 DB 직접 조회 성능 측정");
        System.out.println("=".repeat(60));

        // 워밍업
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            memberRepository.findById(testMember.getMemberId());
        }

        // 측정
        long[] times = new long[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long start = System.nanoTime();
            Optional<Member> result = memberRepository.findById(testMember.getMemberId());
            times[i] = System.nanoTime() - start;
            Assertions.assertTrue(result.isPresent());
        }

        printBenchmarkResult("DB 직접 조회", times);
    }

    @Test
    @DisplayName("캐시 서비스 성능 측정 (L1+L2)")
    void measureCacheServiceAccess() {
        if (memberCacheService == null) {
            System.out.println("⚠️ MemberCacheService not available, skipping test");
            return;
        }

        System.out.println("\n" + "=".repeat(60));
        System.out.println("📊 캐시 서비스 성능 측정 (L1 Caffeine + L2 Redis)");
        System.out.println("=".repeat(60));

        // 캐시 워밍업 (캐시에 데이터 로드)
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            memberCacheService.findById(testMember.getMemberId());
        }

        // 측정 (캐시 HIT 상태)
        long[] times = new long[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long start = System.nanoTime();
            Optional<Member> result = memberCacheService.findById(testMember.getMemberId());
            times[i] = System.nanoTime() - start;
            Assertions.assertTrue(result.isPresent());
        }

        printBenchmarkResult("캐시 HIT (L1/L2)", times);
    }

    @Test
    @DisplayName("종합 성능 비교")
    void compareAllMethods() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("📊 2-Tier 캐시 종합 성능 비교");
        System.out.println("=".repeat(70));

        // 1. DB 직접 조회
        double dbAvgMs = measureMethod("DB 직접 조회", () ->
            memberRepository.findById(testMember.getMemberId())
        );

        // 2. 캐시 서비스 (L1+L2)
        double cacheAvgMs = 0;
        if (memberCacheService != null) {
            // 캐시 워밍업
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                memberCacheService.findById(testMember.getMemberId());
            }
            cacheAvgMs = measureMethod("캐시 HIT", () ->
                memberCacheService.findById(testMember.getMemberId())
            );
        }

        // 결과 출력
        System.out.println("\n" + "-".repeat(70));
        System.out.println("📈 성능 비교 결과");
        System.out.println("-".repeat(70));
        System.out.printf("│ %-20s │ %12.3f ms │ %12s │%n", "DB 직접 조회", dbAvgMs, "baseline");

        if (cacheAvgMs > 0) {
            double speedup = dbAvgMs / cacheAvgMs;
            double improvement = ((dbAvgMs - cacheAvgMs) / dbAvgMs) * 100;
            System.out.printf("│ %-20s │ %12.3f ms │ %10.1fx 빠름 │%n", "캐시 HIT", cacheAvgMs, speedup);
            System.out.println("-".repeat(70));
            System.out.printf("🚀 성능 향상: %.1f배 (%.1f%% 응답시간 단축)%n", speedup, improvement);
        }
        System.out.println("=".repeat(70) + "\n");
    }

    private double measureMethod(String name, Runnable method) {
        // 워밍업
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            method.run();
        }

        // 측정
        long[] times = new long[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long start = System.nanoTime();
            method.run();
            times[i] = System.nanoTime() - start;
        }

        double avgNanos = calculateAverage(times);
        return avgNanos / 1_000_000.0; // ms로 변환
    }

    private void printBenchmarkResult(String name, long[] times) {
        double avgNanos = calculateAverage(times);
        long minNanos = findMin(times);
        long maxNanos = findMax(times);
        double stdDev = calculateStdDev(times, avgNanos);

        System.out.println("\n📋 " + name + " 결과:");
        System.out.printf("   평균: %.3f ms%n", avgNanos / 1_000_000);
        System.out.printf("   최소: %.3f ms%n", minNanos / 1_000_000.0);
        System.out.printf("   최대: %.3f ms%n", maxNanos / 1_000_000.0);
        System.out.printf("   표준편차: %.3f ms%n", stdDev / 1_000_000);
        System.out.printf("   측정 횟수: %d회%n", times.length);
    }

    private double calculateAverage(long[] times) {
        long sum = 0;
        for (long time : times) {
            sum += time;
        }
        return (double) sum / times.length;
    }

    private long findMin(long[] times) {
        long min = Long.MAX_VALUE;
        for (long time : times) {
            if (time < min) min = time;
        }
        return min;
    }

    private long findMax(long[] times) {
        long max = Long.MIN_VALUE;
        for (long time : times) {
            if (time > max) max = time;
        }
        return max;
    }

    private double calculateStdDev(long[] times, double avg) {
        double variance = 0;
        for (long time : times) {
            variance += Math.pow(time - avg, 2);
        }
        return Math.sqrt(variance / times.length);
    }
}
