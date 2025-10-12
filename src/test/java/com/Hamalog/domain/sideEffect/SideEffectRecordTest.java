package com.Hamalog.domain.sideEffect;

import com.Hamalog.domain.member.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SideEffectRecord Entity Tests")
class SideEffectRecordTest {

    @Test
    @DisplayName("Should create SideEffectRecord using builder with all fields")
    void createSideEffectRecord_Builder_Success() {
        // given
        Member member = Member.builder()
                .nickName("testUser")
                .loginId("test@example.com")
                .phoneNumber("01012345678")
                .name("테스트")
                .password("encodedPassword")
                .birth(LocalDate.of(1990, 1, 1))
                .build();
                
        LocalDateTime now = LocalDateTime.now();
        
        // when
        SideEffectRecord sideEffectRecord = SideEffectRecord.builder()
                .member(member)
                .createdAt(now)
                .build();
        
        // then
        assertThat(sideEffectRecord.getMember()).isEqualTo(member);
        assertThat(sideEffectRecord.getCreatedAt()).isEqualTo(now);
        assertThat(sideEffectRecord.getSideEffectRecordId()).isNull(); // ID is null before persistence
    }

    @Test
    @DisplayName("Should create SideEffectRecord with member relationship")
    void createSideEffectRecord_MemberRelationship_Success() {
        // given
        Member member = Member.builder()
                .nickName("relationUser")
                .loginId("relation@example.com")
                .phoneNumber("01087654321")
                .name("관계테스트")
                .password("encodedPassword")
                .birth(LocalDate.of(1985, 5, 15))
                .build();
                
        LocalDateTime createdAt = LocalDateTime.of(2023, 10, 15, 14, 30, 0);
        
        // when
        SideEffectRecord sideEffectRecord = SideEffectRecord.builder()
                .member(member)
                .createdAt(createdAt)
                .build();
        
        // then
        assertThat(sideEffectRecord.getMember()).isNotNull();
        assertThat(sideEffectRecord.getMember().getNickName()).isEqualTo("relationUser");
        assertThat(sideEffectRecord.getMember().getLoginId()).isEqualTo("relation@example.com");
        assertThat(sideEffectRecord.getMember().getName()).isEqualTo("관계테스트");
    }

    @Test
    @DisplayName("Should handle various created at timestamps")
    void createSideEffectRecord_VariousTimestamps_Success() {
        // given
        Member member = Member.builder()
                .nickName("timeUser")
                .loginId("time@example.com")
                .phoneNumber("01011111111")
                .name("시간테스트")
                .password("encodedPassword")
                .birth(LocalDate.of(2000, 12, 31))
                .build();
                
        LocalDateTime pastTime = LocalDateTime.of(2020, 1, 1, 0, 0, 0);
        LocalDateTime futureTime = LocalDateTime.of(2030, 12, 31, 23, 59, 59);
        LocalDateTime currentTime = LocalDateTime.now();
        
        // when
        SideEffectRecord pastRecord = SideEffectRecord.builder()
                .member(member)
                .createdAt(pastTime)
                .build();
                
        SideEffectRecord futureRecord = SideEffectRecord.builder()
                .member(member)
                .createdAt(futureTime)
                .build();
                
        SideEffectRecord currentRecord = SideEffectRecord.builder()
                .member(member)
                .createdAt(currentTime)
                .build();
        
        // then
        assertThat(pastRecord.getCreatedAt()).isEqualTo(pastTime);
        assertThat(futureRecord.getCreatedAt()).isEqualTo(futureTime);
        assertThat(currentRecord.getCreatedAt()).isEqualTo(currentTime);
    }

    @Test
    @DisplayName("Should create SideEffectRecord with null member using builder")
    void createSideEffectRecord_NullMember_Success() {
        // given
        LocalDateTime now = LocalDateTime.now();
        
        // when
        SideEffectRecord sideEffectRecord = SideEffectRecord.builder()
                .member(null)
                .createdAt(now)
                .build();
        
        // then
        assertThat(sideEffectRecord.getMember()).isNull();
        assertThat(sideEffectRecord.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should create SideEffectRecord with null createdAt using builder")
    void createSideEffectRecord_NullCreatedAt_Success() {
        // given
        Member member = Member.builder()
                .nickName("nullTimeUser")
                .loginId("nulltime@example.com")
                .phoneNumber("01022222222")
                .name("널시간테스트")
                .password("encodedPassword")
                .birth(LocalDate.of(1995, 6, 15))
                .build();
        
        // when
        SideEffectRecord sideEffectRecord = SideEffectRecord.builder()
                .member(member)
                .createdAt(null)
                .build();
        
        // then
        assertThat(sideEffectRecord.getMember()).isEqualTo(member);
        assertThat(sideEffectRecord.getCreatedAt()).isNull();
    }

    @Test
    @DisplayName("Should create empty SideEffectRecord using no-args constructor")
    void createSideEffectRecord_NoArgsConstructor_Success() {
        // when
        SideEffectRecord sideEffectRecord = new SideEffectRecord();
        
        // then
        assertThat(sideEffectRecord.getSideEffectRecordId()).isNull();
        assertThat(sideEffectRecord.getMember()).isNull();
        assertThat(sideEffectRecord.getCreatedAt()).isNull();
    }

    @Test
    @DisplayName("Should support Lombok getter methods")
    void lombokGetters_AllFields_Success() {
        // given
        Member member = Member.builder()
                .nickName("getterUser")
                .loginId("getter@example.com")
                .phoneNumber("01033333333")
                .name("게터테스트")
                .password("encodedPassword")
                .birth(LocalDate.of(1988, 3, 22))
                .build();
                
        LocalDateTime createdAt = LocalDateTime.now();
        
        SideEffectRecord sideEffectRecord = SideEffectRecord.builder()
                .member(member)
                .createdAt(createdAt)
                .build();
        
        // when & then
        assertThat(sideEffectRecord).hasFieldOrProperty("sideEffectRecordId");
        assertThat(sideEffectRecord).hasFieldOrProperty("member");
        assertThat(sideEffectRecord).hasFieldOrProperty("createdAt");
        
        // Verify getter methods exist and return correct values
        assertThat(sideEffectRecord.getSideEffectRecordId()).isNull();
        assertThat(sideEffectRecord.getMember()).isEqualTo(member);
        assertThat(sideEffectRecord.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("Should create SideEffectRecord using AllArgsConstructor")
    void createSideEffectRecord_AllArgsConstructor_Success() {
        // given
        Member member = Member.builder()
                .nickName("allArgsUser")
                .loginId("allargs@example.com")
                .phoneNumber("01044444444")
                .name("올아그테스트")
                .password("encodedPassword")
                .birth(LocalDate.of(1992, 8, 10))
                .build();
                
        Long id = 1L;
        LocalDateTime createdAt = LocalDateTime.now();
        
        // when
        SideEffectRecord sideEffectRecord = new SideEffectRecord(id, member, createdAt);
        
        // then
        assertThat(sideEffectRecord.getSideEffectRecordId()).isEqualTo(id);
        assertThat(sideEffectRecord.getMember()).isEqualTo(member);
        assertThat(sideEffectRecord.getCreatedAt()).isEqualTo(createdAt);
    }
}