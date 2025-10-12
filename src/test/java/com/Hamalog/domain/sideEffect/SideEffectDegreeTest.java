package com.Hamalog.domain.sideEffect;

import com.Hamalog.domain.member.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SideEffectDegree Entity Tests")
class SideEffectDegreeTest {

    @Test
    @DisplayName("Should create SideEffectDegree with all fields")
    void createSideEffectDegree_AllFields_Success() {
        // given
        Member member = Member.builder()
                .nickName("testUser")
                .loginId("test@example.com")
                .phoneNumber("01012345678")
                .name("테스트")
                .password("encodedPassword")
                .birth(java.time.LocalDate.of(1990, 1, 1))
                .build();
                
        SideEffectRecord sideEffectRecord = SideEffectRecord.builder()
                .member(member)
                .createdAt(LocalDateTime.now())
                .build();
                
        SideEffectDegree sideEffectDegree = new SideEffectDegree();
        
        // when
        // Using reflection to set private fields since there are no setters
        try {
            java.lang.reflect.Field sideEffectRecordField = SideEffectDegree.class.getDeclaredField("sideEffectRecord");
            sideEffectRecordField.setAccessible(true);
            sideEffectRecordField.set(sideEffectDegree, sideEffectRecord);
            
            java.lang.reflect.Field degreeField = SideEffectDegree.class.getDeclaredField("degree");
            degreeField.setAccessible(true);
            degreeField.set(sideEffectDegree, 5);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        // then
        assertThat(sideEffectDegree.getSideEffectRecord()).isEqualTo(sideEffectRecord);
        assertThat(sideEffectDegree.getDegree()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should have null id when newly created")
    void createSideEffectDegree_NewEntity_IdIsNull() {
        // given & when
        SideEffectDegree sideEffectDegree = new SideEffectDegree();
        
        // then
        assertThat(sideEffectDegree.getId()).isNull();
    }

    @Test
    @DisplayName("Should handle null sideEffectRecord")
    void createSideEffectDegree_NullSideEffectRecord_Success() {
        // given
        SideEffectDegree sideEffectDegree = new SideEffectDegree();
        
        // when & then
        assertThat(sideEffectDegree.getSideEffectRecord()).isNull();
    }

    @Test
    @DisplayName("Should handle null degree")
    void createSideEffectDegree_NullDegree_Success() {
        // given
        SideEffectDegree sideEffectDegree = new SideEffectDegree();
        
        // when & then
        assertThat(sideEffectDegree.getDegree()).isNull();
    }

    @Test
    @DisplayName("Should handle various degree values")
    void createSideEffectDegree_VariousDegreeValues_Success() {
        // given
        SideEffectDegree sideEffectDegree1 = new SideEffectDegree();
        SideEffectDegree sideEffectDegree2 = new SideEffectDegree();
        SideEffectDegree sideEffectDegree3 = new SideEffectDegree();
        
        // when
        try {
            java.lang.reflect.Field degreeField = SideEffectDegree.class.getDeclaredField("degree");
            degreeField.setAccessible(true);
            degreeField.set(sideEffectDegree1, 1);
            degreeField.set(sideEffectDegree2, 10);
            degreeField.set(sideEffectDegree3, -1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        // then
        assertThat(sideEffectDegree1.getDegree()).isEqualTo(1);
        assertThat(sideEffectDegree2.getDegree()).isEqualTo(10);
        assertThat(sideEffectDegree3.getDegree()).isEqualTo(-1);
    }

    @Test
    @DisplayName("Should maintain relationship with SideEffectRecord")
    void sideEffectRecord_Relationship_Success() {
        // given
        Member member = Member.builder()
                .nickName("relationUser")
                .loginId("relation@example.com")
                .phoneNumber("01087654321")
                .name("관계테스트")
                .password("encodedPassword")
                .birth(java.time.LocalDate.of(1985, 5, 15))
                .build();
                
        SideEffectRecord sideEffectRecord = SideEffectRecord.builder()
                .member(member)
                .createdAt(LocalDateTime.now())
                .build();
                
        SideEffectDegree sideEffectDegree = new SideEffectDegree();
        
        // when
        try {
            java.lang.reflect.Field sideEffectRecordField = SideEffectDegree.class.getDeclaredField("sideEffectRecord");
            sideEffectRecordField.setAccessible(true);
            sideEffectRecordField.set(sideEffectDegree, sideEffectRecord);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        // then
        assertThat(sideEffectDegree.getSideEffectRecord()).isNotNull();
        assertThat(sideEffectDegree.getSideEffectRecord().getMember()).isEqualTo(member);
        assertThat(sideEffectDegree.getSideEffectRecord().getMember().getNickName()).isEqualTo("relationUser");
    }

    @Test
    @DisplayName("Should support Lombok getter methods")
    void lombokGetters_AllFields_Success() {
        // given
        SideEffectDegree sideEffectDegree = new SideEffectDegree();
        
        // when & then
        assertThat(sideEffectDegree).hasFieldOrProperty("id");
        assertThat(sideEffectDegree).hasFieldOrProperty("sideEffectRecord");
        assertThat(sideEffectDegree).hasFieldOrProperty("degree");
        
        // Verify getter methods exist
        assertThat(sideEffectDegree.getId()).isNull();
        assertThat(sideEffectDegree.getSideEffectRecord()).isNull();
        assertThat(sideEffectDegree.getDegree()).isNull();
    }
}