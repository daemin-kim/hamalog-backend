package com.Hamalog.domain.sideEffect;

import com.Hamalog.domain.member.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SideEffectSideEffectRecord Entity Tests")
class SideEffectSideEffectRecordTest {

    @Test
    @DisplayName("Should create SideEffectSideEffectRecord using builder with all fields")
    void createSideEffectSideEffectRecord_Builder_Success() {
        // given
        Member member = Member.builder()
                .nickName("testUser")
                .loginId("test@example.com")
                .phoneNumber("01012345678")
                .name("테스트")
                .password("encodedPassword")
                .birth(LocalDate.of(1990, 1, 1))
                .build();
                
        SideEffectRecord sideEffectRecord = SideEffectRecord.builder()
                .member(member)
                .createdAt(LocalDateTime.now())
                .build();
                
        SideEffect sideEffect = new SideEffect();
        
        Long sideEffectRecordId = 1L;
        Long sideEffectId = 2L;
        Integer degree = 5;
        
        // when
        SideEffectSideEffectRecord entity = SideEffectSideEffectRecord.builder()
                .sideEffectRecordId(sideEffectRecordId)
                .sideEffectId(sideEffectId)
                .sideEffectRecord(sideEffectRecord)
                .sideEffect(sideEffect)
                .degree(degree)
                .build();
        
        // then
        assertThat(entity.getSideEffectRecordId()).isEqualTo(sideEffectRecordId);
        assertThat(entity.getSideEffectId()).isEqualTo(sideEffectId);
        assertThat(entity.getSideEffectRecord()).isEqualTo(sideEffectRecord);
        assertThat(entity.getSideEffect()).isEqualTo(sideEffect);
        assertThat(entity.getDegree()).isEqualTo(degree);
    }

    @Test
    @DisplayName("Should create SideEffectSideEffectRecord with composite ID")
    void createSideEffectSideEffectRecord_CompositeId_Success() {
        // given
        Long sideEffectRecordId = 10L;
        Long sideEffectId = 20L;
        Integer degree = 3;
        
        // when
        SideEffectSideEffectRecord entity = SideEffectSideEffectRecord.builder()
                .sideEffectRecordId(sideEffectRecordId)
                .sideEffectId(sideEffectId)
                .degree(degree)
                .build();
        
        // then
        assertThat(entity.getSideEffectRecordId()).isEqualTo(sideEffectRecordId);
        assertThat(entity.getSideEffectId()).isEqualTo(sideEffectId);
        assertThat(entity.getDegree()).isEqualTo(degree);
    }

    @Test
    @DisplayName("Should handle relationships with SideEffectRecord and SideEffect")
    void createSideEffectSideEffectRecord_WithRelationships_Success() {
        // given
        Member member = Member.builder()
                .nickName("relationUser")
                .loginId("relation@example.com")
                .phoneNumber("01087654321")
                .name("관계테스트")
                .password("encodedPassword")
                .birth(LocalDate.of(1985, 5, 15))
                .build();
                
        SideEffectRecord sideEffectRecord = SideEffectRecord.builder()
                .member(member)
                .createdAt(LocalDateTime.now())
                .build();
                
        SideEffect sideEffect = new SideEffect();
        
        // when
        SideEffectSideEffectRecord entity = SideEffectSideEffectRecord.builder()
                .sideEffectRecordId(1L)
                .sideEffectId(1L)
                .sideEffectRecord(sideEffectRecord)
                .sideEffect(sideEffect)
                .degree(7)
                .build();
        
        // then
        assertThat(entity.getSideEffectRecord()).isNotNull();
        assertThat(entity.getSideEffect()).isNotNull();
        assertThat(entity.getSideEffectRecord().getMember()).isEqualTo(member);
        assertThat(entity.getSideEffectRecord().getMember().getNickName()).isEqualTo("relationUser");
    }

    @Test
    @DisplayName("Should handle various degree values")
    void createSideEffectSideEffectRecord_VariousDegreeValues_Success() {
        // given & when
        SideEffectSideEffectRecord entity1 = SideEffectSideEffectRecord.builder()
                .sideEffectRecordId(1L)
                .sideEffectId(1L)
                .degree(1)
                .build();
                
        SideEffectSideEffectRecord entity2 = SideEffectSideEffectRecord.builder()
                .sideEffectRecordId(2L)
                .sideEffectId(2L)
                .degree(10)
                .build();
                
        SideEffectSideEffectRecord entity3 = SideEffectSideEffectRecord.builder()
                .sideEffectRecordId(3L)
                .sideEffectId(3L)
                .degree(0)
                .build();
        
        // then
        assertThat(entity1.getDegree()).isEqualTo(1);
        assertThat(entity2.getDegree()).isEqualTo(10);
        assertThat(entity3.getDegree()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle null values in builder")
    void createSideEffectSideEffectRecord_NullValues_Success() {
        // when
        SideEffectSideEffectRecord entity = SideEffectSideEffectRecord.builder()
                .sideEffectRecordId(null)
                .sideEffectId(null)
                .sideEffectRecord(null)
                .sideEffect(null)
                .degree(null)
                .build();
        
        // then
        assertThat(entity.getSideEffectRecordId()).isNull();
        assertThat(entity.getSideEffectId()).isNull();
        assertThat(entity.getSideEffectRecord()).isNull();
        assertThat(entity.getSideEffect()).isNull();
        assertThat(entity.getDegree()).isNull();
    }

    @Test
    @DisplayName("Should create empty entity using no-args constructor")
    void createSideEffectSideEffectRecord_NoArgsConstructor_Success() {
        // when
        SideEffectSideEffectRecord entity = new SideEffectSideEffectRecord();
        
        // then
        assertThat(entity.getSideEffectRecordId()).isNull();
        assertThat(entity.getSideEffectId()).isNull();
        assertThat(entity.getSideEffectRecord()).isNull();
        assertThat(entity.getSideEffect()).isNull();
        assertThat(entity.getDegree()).isNull();
    }

    @Test
    @DisplayName("Should create entity using AllArgsConstructor")
    void createSideEffectSideEffectRecord_AllArgsConstructor_Success() {
        // given
        Member member = Member.builder()
                .nickName("allArgsUser")
                .loginId("allargs@example.com")
                .phoneNumber("01044444444")
                .name("올아그테스트")
                .password("encodedPassword")
                .birth(LocalDate.of(1992, 8, 10))
                .build();
                
        SideEffectRecord sideEffectRecord = SideEffectRecord.builder()
                .member(member)
                .createdAt(LocalDateTime.now())
                .build();
                
        SideEffect sideEffect = new SideEffect();
        
        Long sideEffectRecordId = 100L;
        Long sideEffectId = 200L;
        Integer degree = 8;
        
        // when
        SideEffectSideEffectRecord entity = new SideEffectSideEffectRecord(
                sideEffectRecordId, 
                sideEffectId, 
                sideEffectRecord, 
                sideEffect, 
                degree
        );
        
        // then
        assertThat(entity.getSideEffectRecordId()).isEqualTo(sideEffectRecordId);
        assertThat(entity.getSideEffectId()).isEqualTo(sideEffectId);
        assertThat(entity.getSideEffectRecord()).isEqualTo(sideEffectRecord);
        assertThat(entity.getSideEffect()).isEqualTo(sideEffect);
        assertThat(entity.getDegree()).isEqualTo(degree);
    }

    @Test
    @DisplayName("Should support equals and hashCode from Lombok")
    void equalsAndHashCode_LombokGenerated_Success() {
        // given
        SideEffectSideEffectRecord entity1 = SideEffectSideEffectRecord.builder()
                .sideEffectRecordId(1L)
                .sideEffectId(1L)
                .degree(5)
                .build();
                
        SideEffectSideEffectRecord entity2 = SideEffectSideEffectRecord.builder()
                .sideEffectRecordId(1L)
                .sideEffectId(1L)
                .degree(5)
                .build();
                
        SideEffectSideEffectRecord entity3 = SideEffectSideEffectRecord.builder()
                .sideEffectRecordId(2L)
                .sideEffectId(2L)
                .degree(5)
                .build();
        
        // when & then
        assertThat(entity1).isEqualTo(entity2);
        assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
        assertThat(entity1).isNotEqualTo(entity3);
        assertThat(entity1.hashCode()).isNotEqualTo(entity3.hashCode());
    }

    @Test
    @DisplayName("Should support Lombok getter methods")
    void lombokGetters_AllFields_Success() {
        // given
        SideEffectSideEffectRecord entity = SideEffectSideEffectRecord.builder()
                .sideEffectRecordId(1L)
                .sideEffectId(2L)
                .degree(6)
                .build();
        
        // when & then
        assertThat(entity).hasFieldOrProperty("sideEffectRecordId");
        assertThat(entity).hasFieldOrProperty("sideEffectId");
        assertThat(entity).hasFieldOrProperty("sideEffectRecord");
        assertThat(entity).hasFieldOrProperty("sideEffect");
        assertThat(entity).hasFieldOrProperty("degree");
        
        // Verify getter methods exist and return correct values
        assertThat(entity.getSideEffectRecordId()).isEqualTo(1L);
        assertThat(entity.getSideEffectId()).isEqualTo(2L);
        assertThat(entity.getDegree()).isEqualTo(6);
    }

    @Test
    @DisplayName("Should handle negative degree values")
    void createSideEffectSideEffectRecord_NegativeDegree_Success() {
        // when
        SideEffectSideEffectRecord entity = SideEffectSideEffectRecord.builder()
                .sideEffectRecordId(1L)
                .sideEffectId(1L)
                .degree(-5)
                .build();
        
        // then
        assertThat(entity.getDegree()).isEqualTo(-5);
    }
}