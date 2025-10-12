package com.Hamalog.domain.idClass;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SideEffectSideEffectRecordId Tests")
class SideEffectSideEffectRecordIdTest {

    @Test
    @DisplayName("Should implement Serializable interface")
    void implementsSerializable_InterfaceCheck_Success() {
        // given
        SideEffectSideEffectRecordId id = new SideEffectSideEffectRecordId();
        
        // when & then
        assertThat(id).isInstanceOf(Serializable.class);
    }

    @Test
    @DisplayName("Should create SideEffectSideEffectRecordId with default constructor")
    void createId_DefaultConstructor_Success() {
        // when
        SideEffectSideEffectRecordId id = new SideEffectSideEffectRecordId();
        
        // then
        assertThat(id).isNotNull();
        assertThat(getSideEffectRecordId(id)).isNull();
        assertThat(getSideEffectId(id)).isNull();
    }

    @Test
    @DisplayName("Should set and get sideEffectRecordId using reflection")
    void sideEffectRecordId_SetAndGet_Success() {
        // given
        SideEffectSideEffectRecordId id = new SideEffectSideEffectRecordId();
        Long expectedId = 123L;
        
        // when
        setSideEffectRecordId(id, expectedId);
        
        // then
        assertThat(getSideEffectRecordId(id)).isEqualTo(expectedId);
    }

    @Test
    @DisplayName("Should set and get sideEffectId using reflection")
    void sideEffectId_SetAndGet_Success() {
        // given
        SideEffectSideEffectRecordId id = new SideEffectSideEffectRecordId();
        Long expectedId = 456L;
        
        // when
        setSideEffectId(id, expectedId);
        
        // then
        assertThat(getSideEffectId(id)).isEqualTo(expectedId);
    }

    @Test
    @DisplayName("Should handle null values for both fields")
    void nullValues_BothFields_Success() {
        // given
        SideEffectSideEffectRecordId id = new SideEffectSideEffectRecordId();
        
        // when
        setSideEffectRecordId(id, null);
        setSideEffectId(id, null);
        
        // then
        assertThat(getSideEffectRecordId(id)).isNull();
        assertThat(getSideEffectId(id)).isNull();
    }

    @Test
    @DisplayName("Should support equals functionality from Lombok")
    void equals_LombokGenerated_Success() {
        // given
        SideEffectSideEffectRecordId id1 = new SideEffectSideEffectRecordId();
        setSideEffectRecordId(id1, 1L);
        setSideEffectId(id1, 2L);
        
        SideEffectSideEffectRecordId id2 = new SideEffectSideEffectRecordId();
        setSideEffectRecordId(id2, 1L);
        setSideEffectId(id2, 2L);
        
        SideEffectSideEffectRecordId id3 = new SideEffectSideEffectRecordId();
        setSideEffectRecordId(id3, 3L);
        setSideEffectId(id3, 4L);
        
        // when & then
        assertThat(id1).isEqualTo(id2);
        assertThat(id1).isNotEqualTo(id3);
        assertThat(id2).isNotEqualTo(id3);
    }

    @Test
    @DisplayName("Should support hashCode functionality from Lombok")
    void hashCode_LombokGenerated_Success() {
        // given
        SideEffectSideEffectRecordId id1 = new SideEffectSideEffectRecordId();
        setSideEffectRecordId(id1, 1L);
        setSideEffectId(id1, 2L);
        
        SideEffectSideEffectRecordId id2 = new SideEffectSideEffectRecordId();
        setSideEffectRecordId(id2, 1L);
        setSideEffectId(id2, 2L);
        
        SideEffectSideEffectRecordId id3 = new SideEffectSideEffectRecordId();
        setSideEffectRecordId(id3, 3L);
        setSideEffectId(id3, 4L);
        
        // when & then
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        assertThat(id1.hashCode()).isNotEqualTo(id3.hashCode());
    }

    @Test
    @DisplayName("Should handle equals with null objects")
    void equals_NullObject_Success() {
        // given
        SideEffectSideEffectRecordId id = new SideEffectSideEffectRecordId();
        setSideEffectRecordId(id, 1L);
        setSideEffectId(id, 2L);
        
        // when & then
        assertThat(id).isNotEqualTo(null);
        assertThat(id.equals(null)).isFalse();
    }

    @Test
    @DisplayName("Should handle equals with different class objects")
    void equals_DifferentClass_Success() {
        // given
        SideEffectSideEffectRecordId id = new SideEffectSideEffectRecordId();
        String differentObject = "different";
        
        // when & then
        assertThat(id).isNotEqualTo(differentObject);
    }

    @Test
    @DisplayName("Should handle equals with same object reference")
    void equals_SameReference_Success() {
        // given
        SideEffectSideEffectRecordId id = new SideEffectSideEffectRecordId();
        setSideEffectRecordId(id, 1L);
        setSideEffectId(id, 2L);
        
        // when & then
        assertThat(id).isEqualTo(id);
        assertThat(id.equals(id)).isTrue();
    }

    @Test
    @DisplayName("Should be serializable and deserializable")
    void serialization_SerializeDeserialize_Success() throws Exception {
        // given
        SideEffectSideEffectRecordId originalId = new SideEffectSideEffectRecordId();
        setSideEffectRecordId(originalId, 100L);
        setSideEffectId(originalId, 200L);
        
        // when - serialize
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(originalId);
        oos.close();
        
        // when - deserialize
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        SideEffectSideEffectRecordId deserializedId = (SideEffectSideEffectRecordId) ois.readObject();
        ois.close();
        
        // then
        assertThat(deserializedId).isNotNull();
        assertThat(deserializedId).isEqualTo(originalId);
        assertThat(getSideEffectRecordId(deserializedId)).isEqualTo(100L);
        assertThat(getSideEffectId(deserializedId)).isEqualTo(200L);
    }

    @Test
    @DisplayName("Should handle various Long values")
    void variousLongValues_SetAndGet_Success() {
        // given
        SideEffectSideEffectRecordId id = new SideEffectSideEffectRecordId();
        
        // when & then
        setSideEffectRecordId(id, Long.MAX_VALUE);
        setSideEffectId(id, Long.MIN_VALUE);
        assertThat(getSideEffectRecordId(id)).isEqualTo(Long.MAX_VALUE);
        assertThat(getSideEffectId(id)).isEqualTo(Long.MIN_VALUE);
        
        setSideEffectRecordId(id, 0L);
        setSideEffectId(id, -1L);
        assertThat(getSideEffectRecordId(id)).isEqualTo(0L);
        assertThat(getSideEffectId(id)).isEqualTo(-1L);
    }

    // Helper methods using reflection
    private Long getSideEffectRecordId(SideEffectSideEffectRecordId id) {
        try {
            java.lang.reflect.Field field = SideEffectSideEffectRecordId.class.getDeclaredField("sideEffectRecordId");
            field.setAccessible(true);
            return (Long) field.get(id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setSideEffectRecordId(SideEffectSideEffectRecordId id, Long value) {
        try {
            java.lang.reflect.Field field = SideEffectSideEffectRecordId.class.getDeclaredField("sideEffectRecordId");
            field.setAccessible(true);
            field.set(id, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Long getSideEffectId(SideEffectSideEffectRecordId id) {
        try {
            java.lang.reflect.Field field = SideEffectSideEffectRecordId.class.getDeclaredField("sideEffectId");
            field.setAccessible(true);
            return (Long) field.get(id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setSideEffectId(SideEffectSideEffectRecordId id, Long value) {
        try {
            java.lang.reflect.Field field = SideEffectSideEffectRecordId.class.getDeclaredField("sideEffectId");
            field.setAccessible(true);
            field.set(id, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}