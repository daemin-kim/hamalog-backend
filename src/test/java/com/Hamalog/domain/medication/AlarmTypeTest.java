package com.Hamalog.domain.medication;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AlarmType Enum Tests")
class AlarmTypeTest {

    @Test
    @DisplayName("Should have correct enum values")
    void enumValues_CheckValues_Success() {
        // when
        AlarmType[] values = AlarmType.values();
        
        // then
        assertThat(values).hasSize(2);
        assertThat(values).containsExactly(AlarmType.SOUND, AlarmType.VIBE);
    }

    @Test
    @DisplayName("Should return correct enum for valueOf with SOUND")
    void valueOf_ValidSound_Success() {
        // when
        AlarmType result = AlarmType.valueOf("SOUND");
        
        // then
        assertThat(result).isEqualTo(AlarmType.SOUND);
    }

    @Test
    @DisplayName("Should return correct enum for valueOf with VIBE")
    void valueOf_ValidVibe_Success() {
        // when
        AlarmType result = AlarmType.valueOf("VIBE");
        
        // then
        assertThat(result).isEqualTo(AlarmType.VIBE);
    }

    @Test
    @DisplayName("Should throw exception for valueOf with invalid value")
    void valueOf_InvalidValue_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> AlarmType.valueOf("INVALID"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw exception for valueOf with null")
    void valueOf_NullValue_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> AlarmType.valueOf(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should have correct string representation")
    void toString_CheckStringRepresentation_Success() {
        // when & then
        assertThat(AlarmType.SOUND.toString()).isEqualTo("SOUND");
        assertThat(AlarmType.VIBE.toString()).isEqualTo("VIBE");
    }

    @Test
    @DisplayName("Should support ordinal values")
    void ordinal_CheckOrdinalValues_Success() {
        // when & then
        assertThat(AlarmType.SOUND.ordinal()).isEqualTo(0);
        assertThat(AlarmType.VIBE.ordinal()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should support name method")
    void name_CheckNameMethod_Success() {
        // when & then
        assertThat(AlarmType.SOUND.name()).isEqualTo("SOUND");
        assertThat(AlarmType.VIBE.name()).isEqualTo("VIBE");
    }
}