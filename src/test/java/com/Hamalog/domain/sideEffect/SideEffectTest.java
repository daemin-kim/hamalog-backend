package com.Hamalog.domain.sideEffect;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SideEffect Entity Tests")
class SideEffectTest {

    @Test
    @DisplayName("Should create SideEffect with all fields")
    void createSideEffect_AllFields_Success() {
        // given
        SideEffect sideEffect = new SideEffect();
        
        // when
        // Using reflection to set private fields since there are no setters
        try {
            java.lang.reflect.Field typeField = SideEffect.class.getDeclaredField("type");
            typeField.setAccessible(true);
            typeField.set(sideEffect, "Physical");
            
            java.lang.reflect.Field nameField = SideEffect.class.getDeclaredField("name");
            nameField.setAccessible(true);
            nameField.set(sideEffect, "Headache");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        // then
        assertThat(sideEffect.getType()).isEqualTo("Physical");
        assertThat(sideEffect.getName()).isEqualTo("Headache");
        assertThat(sideEffect.getSideEffectId()).isNull(); // ID is null before persistence
    }

    @Test
    @DisplayName("Should have null id when newly created")
    void createSideEffect_NewEntity_IdIsNull() {
        // given & when
        SideEffect sideEffect = new SideEffect();
        
        // then
        assertThat(sideEffect.getSideEffectId()).isNull();
    }

    @Test
    @DisplayName("Should handle null type")
    void createSideEffect_NullType_Success() {
        // given
        SideEffect sideEffect = new SideEffect();
        
        // when & then
        assertThat(sideEffect.getType()).isNull();
    }

    @Test
    @DisplayName("Should handle null name")
    void createSideEffect_NullName_Success() {
        // given
        SideEffect sideEffect = new SideEffect();
        
        // when & then
        assertThat(sideEffect.getName()).isNull();
    }

    @Test
    @DisplayName("Should handle various type values")
    void createSideEffect_VariousTypeValues_Success() {
        // given
        SideEffect sideEffect1 = new SideEffect();
        SideEffect sideEffect2 = new SideEffect();
        SideEffect sideEffect3 = new SideEffect();
        
        // when
        try {
            java.lang.reflect.Field typeField = SideEffect.class.getDeclaredField("type");
            typeField.setAccessible(true);
            typeField.set(sideEffect1, "Physical");
            typeField.set(sideEffect2, "Mental");
            typeField.set(sideEffect3, "Emotional");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        // then
        assertThat(sideEffect1.getType()).isEqualTo("Physical");
        assertThat(sideEffect2.getType()).isEqualTo("Mental");
        assertThat(sideEffect3.getType()).isEqualTo("Emotional");
    }

    @Test
    @DisplayName("Should handle various name values")
    void createSideEffect_VariousNameValues_Success() {
        // given
        SideEffect sideEffect1 = new SideEffect();
        SideEffect sideEffect2 = new SideEffect();
        SideEffect sideEffect3 = new SideEffect();
        
        // when
        try {
            java.lang.reflect.Field nameField = SideEffect.class.getDeclaredField("name");
            nameField.setAccessible(true);
            nameField.set(sideEffect1, "Headache");
            nameField.set(sideEffect2, "Nausea");
            nameField.set(sideEffect3, "Dizziness");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        // then
        assertThat(sideEffect1.getName()).isEqualTo("Headache");
        assertThat(sideEffect2.getName()).isEqualTo("Nausea");
        assertThat(sideEffect3.getName()).isEqualTo("Dizziness");
    }

    @Test
    @DisplayName("Should handle maximum length type value")
    void createSideEffect_MaxLengthType_Success() {
        // given
        SideEffect sideEffect = new SideEffect();
        String maxLengthType = "A".repeat(20); // 20 characters (max length)
        
        // when
        try {
            java.lang.reflect.Field typeField = SideEffect.class.getDeclaredField("type");
            typeField.setAccessible(true);
            typeField.set(sideEffect, maxLengthType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        // then
        assertThat(sideEffect.getType()).isEqualTo(maxLengthType);
        assertThat(sideEffect.getType().length()).isEqualTo(20);
    }

    @Test
    @DisplayName("Should handle maximum length name value")
    void createSideEffect_MaxLengthName_Success() {
        // given
        SideEffect sideEffect = new SideEffect();
        String maxLengthName = "B".repeat(20); // 20 characters (max length)
        
        // when
        try {
            java.lang.reflect.Field nameField = SideEffect.class.getDeclaredField("name");
            nameField.setAccessible(true);
            nameField.set(sideEffect, maxLengthName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        // then
        assertThat(sideEffect.getName()).isEqualTo(maxLengthName);
        assertThat(sideEffect.getName().length()).isEqualTo(20);
    }

    @Test
    @DisplayName("Should handle Korean characters in type and name")
    void createSideEffect_KoreanCharacters_Success() {
        // given
        SideEffect sideEffect = new SideEffect();
        String koreanType = "신체적";
        String koreanName = "두통";
        
        // when
        try {
            java.lang.reflect.Field typeField = SideEffect.class.getDeclaredField("type");
            typeField.setAccessible(true);
            typeField.set(sideEffect, koreanType);
            
            java.lang.reflect.Field nameField = SideEffect.class.getDeclaredField("name");
            nameField.setAccessible(true);
            nameField.set(sideEffect, koreanName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        // then
        assertThat(sideEffect.getType()).isEqualTo(koreanType);
        assertThat(sideEffect.getName()).isEqualTo(koreanName);
    }

    @Test
    @DisplayName("Should support Lombok getter methods")
    void lombokGetters_AllFields_Success() {
        // given
        SideEffect sideEffect = new SideEffect();
        
        // when & then
        assertThat(sideEffect).hasFieldOrProperty("sideEffectId");
        assertThat(sideEffect).hasFieldOrProperty("type");
        assertThat(sideEffect).hasFieldOrProperty("name");
        
        // Verify getter methods exist
        assertThat(sideEffect.getSideEffectId()).isNull();
        assertThat(sideEffect.getType()).isNull();
        assertThat(sideEffect.getName()).isNull();
    }

    @Test
    @DisplayName("Should handle empty strings for type and name")
    void createSideEffect_EmptyStrings_Success() {
        // given
        SideEffect sideEffect = new SideEffect();
        
        // when
        try {
            java.lang.reflect.Field typeField = SideEffect.class.getDeclaredField("type");
            typeField.setAccessible(true);
            typeField.set(sideEffect, "");
            
            java.lang.reflect.Field nameField = SideEffect.class.getDeclaredField("name");
            nameField.setAccessible(true);
            nameField.set(sideEffect, "");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        // then
        assertThat(sideEffect.getType()).isEmpty();
        assertThat(sideEffect.getName()).isEmpty();
    }
}