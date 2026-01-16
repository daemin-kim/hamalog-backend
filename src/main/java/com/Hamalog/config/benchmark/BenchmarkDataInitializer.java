package com.Hamalog.config.benchmark;

import com.Hamalog.domain.medication.AlarmType;
import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.domain.medication.MedicationTime;
import com.Hamalog.domain.member.Member;
import com.Hamalog.repository.medication.MedicationScheduleRepository;
import com.Hamalog.repository.medication.MedicationTimeRepository;
import com.Hamalog.repository.member.MemberRepository;
import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 벤치마크 테스트 데이터 초기화
 *
 * N+1 문제 vs Optimized 성능 비교를 위한 충분한 데이터를 생성합니다.
 *
 * 데이터 규모:
 *   - Member: 1명 (벤치마크 사용자)
 *   - MedicationSchedule: 1,000개
 *   - MedicationTime: 각 Schedule당 3개 = 3,000개
 *
 * N+1 문제 시 (배치패치 비활성화):
 *   - 1 (Schedule 목록) + 1,000 (각 Schedule의 Times) = 1,001 쿼리
 *
 * Optimized (@EntityGraph/JOIN FETCH) 시:
 *   - 2 쿼리 (멤버 확인 + 조인 페치)
 */
@Component
@Profile("benchmark")
public class BenchmarkDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkDataInitializer.class);

    private static final int SCHEDULE_COUNT = 1000;
    private static final String BENCHMARK_EMAIL = "benchmark@test.com";
    private static final String BENCHMARK_PASSWORD = "Benchmark1234!";

    private final MemberRepository memberRepository;
    private final MedicationScheduleRepository scheduleRepository;
    private final MedicationTimeRepository timeRepository;
    private final PasswordEncoder passwordEncoder;

    public BenchmarkDataInitializer(
            MemberRepository memberRepository,
            MedicationScheduleRepository scheduleRepository,
            MedicationTimeRepository timeRepository,
            PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.scheduleRepository = scheduleRepository;
        this.timeRepository = timeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    @Transactional
    public void initializeBenchmarkData() {
        log.info("============================================");
        log.info(" 벤치마크 테스트 데이터 초기화 시작");
        log.info("============================================");

        // 1. 벤치마크 사용자 확인/생성
        Member member = findOrCreateBenchmarkMember();

        // 2. 기존 스케줄 수 확인
        long existingCount = scheduleRepository.countByMember_MemberId(member.getMemberId());

        if (existingCount >= SCHEDULE_COUNT) {
            log.info("✅ 벤치마크 데이터 이미 존재 - 스케줄 {}개, 스킵합니다.", existingCount);
            return;
        }

        // 3. 대량 테스트 데이터 생성
        int toCreate = SCHEDULE_COUNT - (int) existingCount;
        log.info("📦 복약 스케줄 {}개 생성 시작...", toCreate);

        createBulkSchedules(member, toCreate, (int) existingCount);

        // 4. 결과 확인
        long scheduleCount = scheduleRepository.countByMember_MemberId(member.getMemberId());
        long timeCount = timeRepository.count();

        log.info("============================================");
        log.info(" 벤치마크 데이터 초기화 완료");
        log.info("   - Member: 1명");
        log.info("   - MedicationSchedule: {}개", scheduleCount);
        log.info("   - MedicationTime: ~{}개", timeCount);
        log.info("============================================");
    }

    private Member findOrCreateBenchmarkMember() {
        return memberRepository.findByLoginId(BENCHMARK_EMAIL)
                .orElseGet(() -> {
                    log.info("📝 벤치마크 사용자 생성 중...");
                    Member newMember = Member.builder()
                            .loginId(BENCHMARK_EMAIL)
                            .password(passwordEncoder.encode(BENCHMARK_PASSWORD))
                            .name("BenchmarkUser")
                            .phoneNumber("01012345678")
                            .nickName("benchmark")
                            .birth(LocalDate.of(1990, 1, 1))
                            .createdAt(LocalDateTime.now())
                            .deletionScheduled(false)
                            .build();
                    return memberRepository.save(newMember);
                });
    }

    private void createBulkSchedules(Member member, int count, int offset) {
        List<MedicationSchedule> schedules = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            int idx = offset + i + 1;

            MedicationSchedule schedule = new MedicationSchedule(
                    member,
                    "TestMed_" + idx,
                    "Hospital_" + idx,
                    LocalDate.now().minusDays(idx % 30),
                    "벤치마크 테스트 메모 #" + idx,
                    LocalDate.now(),
                    30 + (idx % 60),
                    (idx % 3) + 1,
                    idx % 2 == 0 ? AlarmType.SOUND : AlarmType.VIBE);

            schedules.add(schedule);
        }

        // 스케줄 일괄 저장
        List<MedicationSchedule> savedSchedules = scheduleRepository.saveAll(schedules);
        log.info("   ✅ 스케줄 {}개 저장 완료", savedSchedules.size());

        // 각 스케줄에 복약 시간 3개씩 추가
        List<MedicationTime> times = new ArrayList<>();
        LocalTime[] takeTimes = {
            LocalTime.of(8, 0), // 아침
            LocalTime.of(13, 0), // 점심
            LocalTime.of(19, 0) // 저녁
        };

        for (MedicationSchedule schedule : savedSchedules) {
            for (LocalTime takeTime : takeTimes) {
                MedicationTime time = new MedicationTime(schedule, takeTime);
                times.add(time);
            }
        }

        // 복약 시간 일괄 저장
        timeRepository.saveAll(times);
        log.info("   ✅ 복약 시간 {}개 저장 완료", times.size());
    }
}
