package com.Hamalog.dto.benchmark;

/**
 * 캐시 벤치마크 응답 DTO
 *
 * 캐시 조회 vs DB 직접 조회 성능 비교용
 */
public record MemberBenchmarkResponse(
    Long memberId,
    String loginId,
    DataSource source,
    long queryTimeNanos,
    double queryTimeMs
) {
    public enum DataSource {
        REDIS_CACHE,
        DATABASE,
        CACHE_MISS_THEN_DB
    }

    public static MemberBenchmarkResponse fromCache(Long memberId, String loginId, long queryTimeNanos) {
        return new MemberBenchmarkResponse(
            memberId,
            loginId,
            DataSource.REDIS_CACHE,
            queryTimeNanos,
            queryTimeNanos / 1_000_000.0
        );
    }

    public static MemberBenchmarkResponse fromDatabase(Long memberId, String loginId, long queryTimeNanos) {
        return new MemberBenchmarkResponse(
            memberId,
            loginId,
            DataSource.DATABASE,
            queryTimeNanos,
            queryTimeNanos / 1_000_000.0
        );
    }

    public static MemberBenchmarkResponse fromCacheMiss(Long memberId, String loginId, long queryTimeNanos) {
        return new MemberBenchmarkResponse(
            memberId,
            loginId,
            DataSource.CACHE_MISS_THEN_DB,
            queryTimeNanos,
            queryTimeNanos / 1_000_000.0
        );
    }
}

