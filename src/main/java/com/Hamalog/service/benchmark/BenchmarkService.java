package com.Hamalog.service.benchmark;

import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.domain.member.Member;
import com.Hamalog.dto.benchmark.MemberBenchmarkResponse;
import com.Hamalog.exception.member.MemberNotFoundException;
import com.Hamalog.repository.medication.MedicationScheduleRepository;
import com.Hamalog.repository.member.MemberRepository;
import com.Hamalog.service.auth.MemberCacheService;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 성능 벤치마크 서비스
 *
 * N+1 문제의 Before/After 비교를 위한 서비스입니다.
 * Hibernate Statistics를 활용하여 실제 쿼리 수를 측정합니다.
 */
@Service
@Transactional(readOnly = true)
@Slf4j
@Profile({"dev", "test", "benchmark"})
public class BenchmarkService {

    private final MedicationScheduleRepository medicationScheduleRepository;
    private final MemberRepository memberRepository;
    private final EntityManager entityManager;
    private final MemberCacheService memberCacheService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public BenchmarkService(
            MedicationScheduleRepository medicationScheduleRepository,
            MemberRepository memberRepository,
            EntityManager entityManager,
            @Autowired(required = false) MemberCacheService memberCacheService,
            @Autowired(required = false) RedisTemplate<String, Object> redisTemplate) {
        this.medicationScheduleRepository = medicationScheduleRepository;
        this.memberRepository = memberRepository;
        this.entityManager = entityManager;
        this.memberCacheService = memberCacheService;
        this.redisTemplate = redisTemplate;
    }

    // ============================================================
    // 캐시 벤치마크 메서드
    // ============================================================

    /**
     * 캐시를 통한 회원 조회 (Redis 캐시 HIT 시나리오)
     *
     * @param memberId 회원 ID
     * @return 벤치마크 응답 (소스: REDIS_CACHE 또는 CACHE_MISS_THEN_DB)
     */
    public MemberBenchmarkResponse getMemberWithCache(Long memberId) {
        if (memberCacheService == null) {
            log.warn("[BENCHMARK] MemberCacheService not available, falling back to DB");
            return getMemberDirectFromDb(memberId);
        }

        long startTime = System.nanoTime();
        Optional<Member> memberOpt = memberCacheService.findById(memberId);
        long endTime = System.nanoTime();
        long elapsedNanos = endTime - startTime;

        Member member = memberOpt.orElseThrow(MemberNotFoundException::new);

        // 캐시 HIT 여부 판단: 3ms 미만이면 캐시 HIT로 간주
        boolean cacheHit = elapsedNanos < 3_000_000; // 3ms

        log.info("[BENCHMARK] Cache lookup - memberId: {}, time: {}ns ({}ms), cacheHit: {}",
                memberId, elapsedNanos, elapsedNanos / 1_000_000.0, cacheHit);

        if (cacheHit) {
            return MemberBenchmarkResponse.fromCache(
                    member.getMemberId(),
                    member.getLoginId(),
                    elapsedNanos
            );
        } else {
            return MemberBenchmarkResponse.fromCacheMiss(
                    member.getMemberId(),
                    member.getLoginId(),
                    elapsedNanos
            );
        }
    }

    /**
     * DB 직접 조회 (캐시 우회)
     *
     * @param memberId 회원 ID
     * @return 벤치마크 응답 (소스: DATABASE)
     */
    public MemberBenchmarkResponse getMemberDirectFromDb(Long memberId) {
        long startTime = System.nanoTime();
        Optional<Member> memberOpt = memberRepository.findById(memberId);
        long endTime = System.nanoTime();
        long elapsedNanos = endTime - startTime;

        Member member = memberOpt.orElseThrow(MemberNotFoundException::new);

        log.info("[BENCHMARK] Direct DB lookup - memberId: {}, time: {}ns ({}ms)",
                memberId, elapsedNanos, elapsedNanos / 1_000_000.0);

        return MemberBenchmarkResponse.fromDatabase(
                member.getMemberId(),
                member.getLoginId(),
                elapsedNanos
        );
    }

    /**
     * 캐시 무효화
     *
     * @param memberId 회원 ID
     */
    public void evictMemberCache(Long memberId) {
        if (memberCacheService != null) {
            memberCacheService.evictByMemberId(memberId);
            log.info("[BENCHMARK] Cache evicted for memberId: {}", memberId);
        }

        // 직접 Redis 키 삭제 (확실한 무효화)
        if (redisTemplate != null) {
            String cacheKey = "member::memberId:" + memberId;
            Boolean deleted = redisTemplate.delete(cacheKey);
            log.info("[BENCHMARK] Redis key deleted: {} = {}", cacheKey, deleted);
        }
    }

    /**
     * 캐시 워밍업 (MISS → DB 조회 → 캐시 저장)
     *
     * @param memberId 회원 ID
     * @return 벤치마크 응답
     */
    public MemberBenchmarkResponse warmupMemberCache(Long memberId) {
        // 먼저 캐시 무효화
        evictMemberCache(memberId);

        // 캐시 조회하면 MISS 발생 → DB 조회 → 캐시 저장
        return getMemberWithCache(memberId);
    }

    /**
     * 최적화된 쿼리로 복약 스케줄 조회 (벤치마크용 - Member fetch 없음)
     *
     * @param memberId 회원 ID
     * @return 복약 스케줄 목록
     */
    public List<MedicationSchedule> getSchedulesOptimized(Long memberId) {
        validateMemberExists(memberId);

        log.debug("[BENCHMARK] Executing optimized query (MedicationTimes fetch) for memberId: {}", memberId);
        List<MedicationSchedule> schedules = medicationScheduleRepository.findAllByMemberIdOptimizedForBenchmark(memberId);

        // JOIN FETCH로 이미 로드됨 - N+1 발생하지 않음
        int totalTimes = 0;
        for (MedicationSchedule schedule : schedules) {
            totalTimes += schedule.getMedicationTimes().size();
        }
        log.debug("[BENCHMARK] Optimized query loaded {} schedules with {} medicationTimes", schedules.size(), totalTimes);

        return schedules;
    }

    /**
     * N+1 문제가 발생하는 naive 쿼리로 복약 스케줄 조회
     *
     * @param memberId 회원 ID
     * @return 복약 스케줄 목록
     */
    public List<MedicationSchedule> getSchedulesNaive(Long memberId) {
        validateMemberExists(memberId);

        log.debug("[BENCHMARK] Executing naive query (N+1 problem) for memberId: {}", memberId);
        List<MedicationSchedule> schedules = medicationScheduleRepository.findAllByMemberIdNaive(memberId);

        // N+1 문제 발생: 각 스케줄마다 medicationTimes 조회 쿼리 발생
        // 트랜잭션 내에서 LAZY 컬렉션 접근하여 초기화
        int totalTimes = 0;
        for (MedicationSchedule schedule : schedules) {
            totalTimes += schedule.getMedicationTimes().size();
        }
        log.debug("[BENCHMARK] Naive query loaded {} schedules with {} medicationTimes", schedules.size(), totalTimes);

        return schedules;
    }

    /**
     * 쿼리 실행 횟수 측정
     *
     * @param memberId 회원 ID
     * @param optimized 최적화 여부
     * @return 실행된 쿼리 수
     */
    public int measureQueryCount(Long memberId, boolean optimized) {
        Session session = entityManager.unwrap(Session.class);
        Statistics statistics = session.getSessionFactory().getStatistics();

        // 통계 초기화
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        long queryCountBefore = statistics.getQueryExecutionCount();

        // 쿼리 실행
        List<MedicationSchedule> schedules;
        if (optimized) {
            schedules = getSchedulesOptimized(memberId);
        } else {
            schedules = getSchedulesNaive(memberId);
        }

        // N+1 문제 발생 지점: Member 정보 접근
        schedules.forEach(schedule -> {
            // LAZY 로딩으로 인해 여기서 추가 쿼리 발생 가능
            Long ownerId = schedule.getMember().getMemberId();
            String ownerName = schedule.getMember().getName();
            log.trace("[BENCHMARK] Accessed member: {} ({})", ownerName, ownerId);
        });

        long queryCountAfter = statistics.getQueryExecutionCount();
        int totalQueries = (int) (queryCountAfter - queryCountBefore);

        log.info("[BENCHMARK] Query count - Type: {}, MemberId: {}, Schedules: {}, Queries: {}",
                optimized ? "Optimized" : "Naive",
                memberId,
                schedules.size(),
                totalQueries);

        return totalQueries;
    }

    private void validateMemberExists(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new MemberNotFoundException();
        }
    }
}
