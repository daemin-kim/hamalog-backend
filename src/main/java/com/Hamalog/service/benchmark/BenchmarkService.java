package com.Hamalog.service.benchmark;

import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.exception.member.MemberNotFoundException;
import com.Hamalog.repository.medication.MedicationScheduleRepository;
import com.Hamalog.repository.member.MemberRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.springframework.context.annotation.Profile;
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
@RequiredArgsConstructor
@Slf4j
@Profile({"dev", "test", "benchmark"})
public class BenchmarkService {

    private final MedicationScheduleRepository medicationScheduleRepository;
    private final MemberRepository memberRepository;
    private final EntityManager entityManager;

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
