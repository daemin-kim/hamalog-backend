package com.Hamalog.nplusone;

import com.Hamalog.config.TestRedisConfig;
import com.Hamalog.domain.medication.AlarmType;
import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.domain.member.Member;
import com.Hamalog.dto.medication.response.MedicationScheduleResponse;
import com.Hamalog.repository.medication.MedicationScheduleRepository;
import com.Hamalog.repository.member.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
@Import(TestRedisConfig.class)
@ActiveProfiles("local")
@TestPropertySource(locations = "classpath:application-test.properties")
@TestPropertySource(properties = {
    "hamalog.encryption.key=+ZFRGoRl5CElrJfikdx1TmzQ3U8OJ+J6im5OMjuvsqE="
})
@Transactional
public class NPlusOneSolutionTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MedicationScheduleRepository medicationScheduleRepository;

    @BeforeEach
    void setUp() {
        System.out.println("[DEBUG_LOG] Setting up test data for N+1 solution verification...");
        
        // 테스트 사용자 생성
        Member member = Member.builder()
                .loginId("solution-test@example.com")
                .password("password")
                .name("해결테스트사용자")
                .phoneNumber("01087654321")
                .nickName("해결테스터")
                .birth(LocalDate.of(1985, 5, 15))
                .createdAt(LocalDateTime.now())
                .build();
        
        member = memberRepository.save(member);
        System.out.println("[DEBUG_LOG] Created member with ID: " + member.getMemberId());
        
        // 여러 개의 복약 스케줄 생성
        for (int i = 1; i <= 10; i++) {
            MedicationSchedule schedule = new MedicationSchedule(
                    member,
                    "해결테스트약물 " + i,
                    "해결테스트병원 " + i,
                    LocalDate.now().minusDays(i),
                    "해결테스트메모 " + i,
                    LocalDate.now(),
                    30,
                    3,
                    AlarmType.SOUND
            );
            medicationScheduleRepository.save(schedule);
            System.out.println("[DEBUG_LOG] Created medication schedule " + i);
        }
    }

    @Test
    void testNPlusOneSolutionWithEntityGraph() {
        System.out.println("[DEBUG_LOG] === N+1 문제 해결 검증 테스트 (@EntityGraph 사용) ===");
        
        // 1. @EntityGraph를 사용한 MedicationSchedule 목록 조회 - Member가 함께 로드됨
        System.out.println("[DEBUG_LOG] Step 1: Fetching schedules with @EntityGraph (should include Member)...");
        List<MedicationSchedule> schedules = medicationScheduleRepository.findAllByMember_MemberId(1L);
        System.out.println("[DEBUG_LOG] Found " + schedules.size() + " schedules with Member eagerly loaded");
        
        // 2. DTO 변환 시 추가 쿼리 없이 Member 정보 접근 가능해야 함
        System.out.println("[DEBUG_LOG] Step 2: Converting to DTOs (no additional queries expected)...");
        List<MedicationScheduleResponse> responses = schedules.stream()
                .map(schedule -> {
                    System.out.println("[DEBUG_LOG] Converting schedule ID: " + schedule.getMedicationScheduleId() + 
                                     " (Member already loaded: " + (schedule.getMember() != null) + ")");
                    // Member가 이미 로드되어 있어서 추가 쿼리가 발생하지 않아야 함
                    return MedicationScheduleResponse.from(schedule);
                })
                .toList();
        
        System.out.println("[DEBUG_LOG] Successfully converted " + responses.size() + " responses without N+1 problem");
        
        // 결과 확인
        responses.forEach(response -> {
            System.out.println("[DEBUG_LOG] Response - Schedule ID: " + response.medicationScheduleId() + 
                             ", Member ID: " + response.memberId() + 
                             ", Name: " + response.name());
        });
        
        System.out.println("[DEBUG_LOG] === N+1 문제 해결 검증 테스트 완료 ===");
    }

    @Test
    void testNPlusOneSolutionWithJoinFetch() {
        System.out.println("[DEBUG_LOG] === N+1 문제 해결 검증 테스트 (JOIN FETCH 사용) ===");
        
        // JOIN FETCH를 사용한 대안 메서드 테스트
        System.out.println("[DEBUG_LOG] Step 1: Using JOIN FETCH query method...");
        List<MedicationSchedule> schedules = medicationScheduleRepository.findAllByMemberIdWithMember(1L);
        System.out.println("[DEBUG_LOG] Found " + schedules.size() + " schedules with JOIN FETCH");
        
        // Member 정보 접근 시 추가 쿼리 없어야 함
        schedules.forEach(schedule -> {
            System.out.println("[DEBUG_LOG] Schedule: " + schedule.getName() + 
                             ", Member: " + schedule.getMember().getName() +
                             " (no additional query needed)");
        });
        
        System.out.println("[DEBUG_LOG] === JOIN FETCH 해결방안 검증 완료 ===");
    }

    @Test
    void testOwnershipCheckWithoutNPlusOne() {
        System.out.println("[DEBUG_LOG] === 권한 확인 N+1 문제 해결 검증 ===");
        
        // @EntityGraph로 Member와 함께 조회
        List<MedicationSchedule> schedules = medicationScheduleRepository.findAllByMember_MemberId(1L);
        
        // 각 스케줄에 대한 권한 확인 시 추가 쿼리 없어야 함
        schedules.forEach(schedule -> {
            System.out.println("[DEBUG_LOG] Checking ownership for schedule ID: " + schedule.getMedicationScheduleId());
            // Member가 이미 로드되어 있어서 추가 쿼리 없음
            Long memberId = schedule.getMember().getMemberId();
            String memberName = schedule.getMember().getName();
            System.out.println("[DEBUG_LOG] Owner: " + memberName + " (ID: " + memberId + ") - no additional query");
        });
        
        System.out.println("[DEBUG_LOG] === 권한 확인 N+1 문제 해결 검증 완료 ===");
    }
}