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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 벤치마크 테스트 데이터 초기화
 *
 * benchmark 프로필이 활성화된 경우에만 실행됩니다.
 * 서버 시작 시 벤치마크용 테스트 계정과 대량의 복약 스케줄을 생성합니다.
 */
@Component
@Profile("benchmark")
@RequiredArgsConstructor
@Slf4j
public class BenchmarkDataInitializer {

    private static final String BENCHMARK_LOGIN_ID = "benchmark@test.com";
    private static final String BENCHMARK_PASSWORD = "Benchmark1234!";
    private static final int SCHEDULE_COUNT = 100;

    private final MemberRepository memberRepository;
    private final MedicationScheduleRepository medicationScheduleRepository;
    private final MedicationTimeRepository medicationTimeRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    @Transactional
    public void initBenchmarkData() {
        log.info("[BENCHMARK] 벤치마크 데이터 초기화 시작...");

        // 1. 벤치마크 사용자 생성 또는 조회
        Member benchmarkMember = memberRepository.findByLoginId(BENCHMARK_LOGIN_ID)
                .orElseGet(this::createBenchmarkMember);

        log.info("[BENCHMARK] 벤치마크 사용자: {} (ID: {})",
                benchmarkMember.getLoginId(), benchmarkMember.getMemberId());

        // 2. 기존 벤치마크 스케줄이 없으면 생성
        long existingScheduleCount = medicationScheduleRepository.countByMember_MemberId(benchmarkMember.getMemberId());
        if (existingScheduleCount < SCHEDULE_COUNT) {
            createBenchmarkSchedules(benchmarkMember);
        } else {
            log.info("[BENCHMARK] 기존 스케줄 {}개 존재, 생성 건너뜀", existingScheduleCount);
        }

        log.info("[BENCHMARK] 벤치마크 데이터 초기화 완료!");
        log.info("[BENCHMARK] 로그인 정보 - ID: {}, PW: {}", BENCHMARK_LOGIN_ID, BENCHMARK_PASSWORD);
    }

    private Member createBenchmarkMember() {
        log.info("[BENCHMARK] 벤치마크 사용자 생성 중...");

        Member member = Member.builder()
                .loginId(BENCHMARK_LOGIN_ID)
                .password(passwordEncoder.encode(BENCHMARK_PASSWORD))
                .name("벤치마크유저")
                .phoneNumber("01000000000")
                .nickName("benchmark")
                .birth(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.now())
                .build();

        return memberRepository.save(member);
    }

    private void createBenchmarkSchedules(Member member) {
        log.info("[BENCHMARK] 복약 스케줄 {}개 생성 중...", SCHEDULE_COUNT);

        List<MedicationSchedule> schedules = new ArrayList<>();
        List<MedicationTime> times = new ArrayList<>();

        for (int i = 1; i <= SCHEDULE_COUNT; i++) {
            int perDay = (i % 4) + 1; // 1~4회/일
            AlarmType alarmType = switch (i % 2) {
                case 0 -> AlarmType.SOUND;
                default -> AlarmType.VIBE;
            };

            MedicationSchedule schedule = new MedicationSchedule(
                    member,
                    "벤치마크약물_" + String.format("%03d", i),
                    "벤치마크병원_" + String.format("%03d", i),
                    LocalDate.now().minusDays(i),
                    "벤치마크 테스트 메모 #" + i + " - N+1 문제 재현용 데이터",
                    LocalDate.now(),
                    30,
                    perDay,
                    alarmType
            );

            schedules.add(schedule);
        }

        // 스케줄 저장
        List<MedicationSchedule> savedSchedules = medicationScheduleRepository.saveAll(schedules);

        // 복약 시간 생성
        for (MedicationSchedule schedule : savedSchedules) {
            // 08:00, 14:00, 20:00, 02:00 시간대
            LocalTime[] timesOfDay = {
                LocalTime.of(8, 0),
                LocalTime.of(14, 0),
                LocalTime.of(20, 0),
                LocalTime.of(2, 0)
            };

            for (int t = 0; t < schedule.getPerDay(); t++) {
                MedicationTime time = new MedicationTime(schedule, timesOfDay[t]);
                times.add(time);
            }
        }

        medicationTimeRepository.saveAll(times);

        log.info("[BENCHMARK] 스케줄 {}개, 복약시간 {}개 생성 완료",
                savedSchedules.size(), times.size());
    }
}

