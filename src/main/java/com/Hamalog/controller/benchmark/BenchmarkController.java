package com.Hamalog.controller.benchmark;

import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.dto.benchmark.MemberBenchmarkResponse;
import com.Hamalog.dto.medication.response.MedicationScheduleListResponse;
import com.Hamalog.dto.medication.response.MedicationScheduleResponse;
import com.Hamalog.service.benchmark.BenchmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 성능 벤치마크 전용 컨트롤러
 *
 * N+1 문제의 Before/After 성능 비교를 위한 엔드포인트를 제공합니다.
 * 프로덕션 환경에서는 비활성화됩니다.
 *
 * 사용 시나리오:
 * - optimized=false: N+1 문제가 발생하는 naive 쿼리 사용
 * - optimized=true: @EntityGraph 최적화 쿼리 사용
 */
@Tag(name = "Benchmark API", description = "성능 벤치마크 전용 API (개발/테스트 환경 전용)")
@RestController
@RequestMapping("/api/v1/benchmark")
@RequiredArgsConstructor
@Slf4j
@Profile({"dev", "test", "benchmark"})  // 프로덕션에서 비활성화
public class BenchmarkController {

    private final BenchmarkService benchmarkService;

    @Operation(summary = "복약 스케줄 조회 (Before/After 비교)",
            description = "N+1 문제 개선 전후 성능을 비교할 수 있는 엔드포인트입니다.")
    @GetMapping("/medication-schedules/list/{member-id}")
    public ResponseEntity<MedicationScheduleListResponse> getMedicationSchedules(
            @Parameter(description = "회원 ID", required = true, example = "1")
            @PathVariable("member-id") Long memberId,

            @Parameter(description = "최적화 여부 (true: @EntityGraph, false: naive query)")
            @RequestParam(defaultValue = "true") boolean optimized
    ) {
        long startTime = System.nanoTime();

        List<MedicationSchedule> schedules;
        String queryType;

        if (optimized) {
            schedules = benchmarkService.getSchedulesOptimized(memberId);
            queryType = "Optimized (@EntityGraph)";
        } else {
            schedules = benchmarkService.getSchedulesNaive(memberId);
            queryType = "Naive (N+1 Problem)";
        }

        // DTO 변환 (Service에서 이미 medicationTimes 접근하여 N+1 발생/해결됨)
        List<MedicationScheduleResponse> responses = schedules.stream()
                .map(MedicationScheduleResponse::from)
                .toList();

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        log.info("[BENCHMARK] Query Type: {}, Member ID: {}, Records: {}, Duration: {}ms",
                queryType, memberId, responses.size(), durationMs);

        return ResponseEntity.ok(MedicationScheduleListResponse.fromList(responses));
    }

    @Operation(summary = "벤치마크 헬스 체크")
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Benchmark API is active");
    }

    @Operation(summary = "쿼리 실행 횟수 측정",
            description = "실제 발생하는 SQL 쿼리 수를 측정합니다.")
    @GetMapping("/query-count/{member-id}")
    public ResponseEntity<QueryCountResult> measureQueryCount(
            @PathVariable("member-id") Long memberId,
            @RequestParam(defaultValue = "true") boolean optimized
    ) {
        long startTime = System.nanoTime();

        // 쿼리 카운트 측정 (Hibernate Statistics 활용)
        int queryCount = benchmarkService.measureQueryCount(memberId, optimized);

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        return ResponseEntity.ok(new QueryCountResult(
                optimized ? "Optimized" : "Naive",
                queryCount,
                durationMs
        ));
    }

    public record QueryCountResult(
            String queryType,
            int queryCount,
            long durationMs
    ) {}

    // ============================================================
    // 캐시 벤치마크 엔드포인트
    // ============================================================

    @Operation(summary = "캐시를 통한 회원 조회",
            description = "Redis 캐시를 활용한 회원 조회 (캐시 HIT 시 빠른 응답)")
    @GetMapping("/member/cache/{member-id}")
    public ResponseEntity<MemberBenchmarkResponse> getMemberWithCache(
            @Parameter(description = "회원 ID", required = true, example = "1")
            @PathVariable("member-id") Long memberId
    ) {
        MemberBenchmarkResponse response = benchmarkService.getMemberWithCache(memberId);
        log.info("[BENCHMARK] Cache lookup - source: {}, time: {}ms",
                response.source(), response.queryTimeMs());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "DB 직접 회원 조회 (캐시 우회)",
            description = "캐시를 우회하고 DB에서 직접 조회")
    @GetMapping("/member/db/{member-id}")
    public ResponseEntity<MemberBenchmarkResponse> getMemberDirectFromDb(
            @Parameter(description = "회원 ID", required = true, example = "1")
            @PathVariable("member-id") Long memberId
    ) {
        MemberBenchmarkResponse response = benchmarkService.getMemberDirectFromDb(memberId);
        log.info("[BENCHMARK] Direct DB lookup - time: {}ms", response.queryTimeMs());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "캐시 워밍업",
            description = "캐시를 무효화하고 새로 조회하여 캐시에 저장")
    @PostMapping("/member/cache/warmup/{member-id}")
    public ResponseEntity<MemberBenchmarkResponse> warmupMemberCache(
            @Parameter(description = "회원 ID", required = true, example = "1")
            @PathVariable("member-id") Long memberId
    ) {
        MemberBenchmarkResponse response = benchmarkService.warmupMemberCache(memberId);
        log.info("[BENCHMARK] Cache warmup - source: {}, time: {}ms",
                response.source(), response.queryTimeMs());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "캐시 무효화",
            description = "특정 회원의 캐시 삭제")
    @DeleteMapping("/member/cache/{member-id}")
    public ResponseEntity<String> evictMemberCache(
            @Parameter(description = "회원 ID", required = true, example = "1")
            @PathVariable("member-id") Long memberId
    ) {
        benchmarkService.evictMemberCache(memberId);
        return ResponseEntity.ok("Cache evicted for memberId: " + memberId);
    }
}
