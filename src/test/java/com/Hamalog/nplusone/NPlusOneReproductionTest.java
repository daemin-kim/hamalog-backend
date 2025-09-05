package com.Hamalog.nplusone;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
@ActiveProfiles("local")
@TestPropertySource(properties = {
    "hamalog.encryption.key=+ZFRGoRl5CElrJfikdx1TmzQ3U8OJ+J6im5OMjuvsqE="
})
@Transactional
public class NPlusOneReproductionTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MedicationScheduleRepository medicationScheduleRepository;

    @BeforeEach
    void setUp() {
        System.out.println("[DEBUG_LOG] Setting up test data...");
        
        // 테스트 사용자 생성
        Member member = Member.builder()
                .loginId("test@example.com")
                .password("password")
                .name("테스트사용자")
                .phoneNumber("01012345678")
                .nickName("테스터")
                .birth(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.now())
                .build();
        
        member = memberRepository.save(member);
        System.out.println("[DEBUG_LOG] Created member with ID: " + member.getMemberId());
        
        // 여러 개의 복약 스케줄 생성
        for (int i = 1; i <= 5; i++) {
            MedicationSchedule schedule = new MedicationSchedule(
                    member,
                    "약물 " + i,
                    "병원 " + i,
                    LocalDate.now().minusDays(i),
                    "메모 " + i,
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
    void testNPlusOneProblemInMedicationScheduleList() {
        System.out.println("[DEBUG_LOG] === N+1 문제 재현 테스트 시작 ===");
        
        // 1. MedicationSchedule 목록 조회 (1번의 쿼리)
        System.out.println("[DEBUG_LOG] Step 1: Fetching medication schedules...");
        List<MedicationSchedule> schedules = medicationScheduleRepository.findAllByMember_MemberId(1L);
        System.out.println("[DEBUG_LOG] Found " + schedules.size() + " schedules");
        
        // 2. 각 MedicationSchedule을 DTO로 변환할 때 Member 정보 접근 (N번의 추가 쿼리 발생)
        System.out.println("[DEBUG_LOG] Step 2: Converting to DTOs (N+1 problem occurs here)...");
        List<MedicationScheduleResponse> responses = schedules.stream()
                .map(schedule -> {
                    System.out.println("[DEBUG_LOG] Converting schedule ID: " + schedule.getMedicationScheduleId());
                    // 여기서 N+1 문제 발생: getMember().getMemberId() 호출 시 LAZY 로딩으로 인한 추가 쿼리
                    return MedicationScheduleResponse.from(schedule);
                })
                .toList();
        
        System.out.println("[DEBUG_LOG] Converted " + responses.size() + " responses");
        System.out.println("[DEBUG_LOG] === N+1 문제 재현 테스트 완료 ===");
        
        // 결과 확인
        responses.forEach(response -> {
            System.out.println("[DEBUG_LOG] Response - Schedule ID: " + response.medicationScheduleId() + 
                             ", Member ID: " + response.memberId());
        });
    }

    @Test
    void testNPlusOneProblemInOwnershipCheck() {
        System.out.println("[DEBUG_LOG] === 권한 확인에서 N+1 문제 재현 테스트 시작 ===");
        
        List<MedicationSchedule> schedules = medicationScheduleRepository.findAllByMember_MemberId(1L);
        
        // 각 스케줄에 대해 소유자 확인 (N번의 추가 쿼리 발생)
        schedules.forEach(schedule -> {
            System.out.println("[DEBUG_LOG] Checking ownership for schedule ID: " + schedule.getMedicationScheduleId());
            // 여기서 N+1 문제 발생: getMember().getMemberId() 호출 시 LAZY 로딩
            Long memberId = schedule.getMember().getMemberId();
            System.out.println("[DEBUG_LOG] Owner member ID: " + memberId);
        });
        
        System.out.println("[DEBUG_LOG] === 권한 확인에서 N+1 문제 재현 테스트 완료 ===");
    }
}