package com.Hamalog.domain.idClass;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.*;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MedicationScheduleMedicationScheduleGroupId Tests")
class MedicationScheduleMedicationScheduleGroupIdTest {

    private MedicationScheduleMedicationScheduleGroupId id1;
    private MedicationScheduleMedicationScheduleGroupId id2;
    private MedicationScheduleMedicationScheduleGroupId id3;

    @BeforeEach
    void setUp() throws Exception {
        id1 = createIdWithValues(1L, 10L);
        id2 = createIdWithValues(1L, 10L);
        id3 = createIdWithValues(2L, 20L);
    }

    private MedicationScheduleMedicationScheduleGroupId createIdWithValues(Long scheduleId, Long groupId) throws Exception {
        MedicationScheduleMedicationScheduleGroupId id = new MedicationScheduleMedicationScheduleGroupId();
        
        Field scheduleField = MedicationScheduleMedicationScheduleGroupId.class.getDeclaredField("medicationScheduleId");
        scheduleField.setAccessible(true);
        scheduleField.set(id, scheduleId);
        
        Field groupField = MedicationScheduleMedicationScheduleGroupId.class.getDeclaredField("medicationScheduleGroupId");
        groupField.setAccessible(true);
        groupField.set(id, groupId);
        
        return id;
    }

    private Long getMedicationScheduleId(MedicationScheduleMedicationScheduleGroupId id) throws Exception {
        Field field = MedicationScheduleMedicationScheduleGroupId.class.getDeclaredField("medicationScheduleId");
        field.setAccessible(true);
        return (Long) field.get(id);
    }

    private Long getMedicationScheduleGroupId(MedicationScheduleMedicationScheduleGroupId id) throws Exception {
        Field field = MedicationScheduleMedicationScheduleGroupId.class.getDeclaredField("medicationScheduleGroupId");
        field.setAccessible(true);
        return (Long) field.get(id);
    }

    @Test
    @DisplayName("Should create instance successfully with default constructor")
    void constructor_Default_ShouldCreateInstance() throws Exception {
        // given & when
        MedicationScheduleMedicationScheduleGroupId id = new MedicationScheduleMedicationScheduleGroupId();

        // then
        assertThat(id).isNotNull();
        assertThat(getMedicationScheduleId(id)).isNull();
        assertThat(getMedicationScheduleGroupId(id)).isNull();
    }

    @Test
    @DisplayName("Should return true for equals when objects have same values")
    void equals_WithSameValues_ShouldReturnTrue() {
        // given & when
        boolean result = id1.equals(id2);

        // then
        assertThat(result).isTrue();
        assertThat(id1).isEqualTo(id2);
    }

    @Test
    @DisplayName("Should return false for equals when objects have different values")
    void equals_WithDifferentValues_ShouldReturnFalse() {
        // given & when
        boolean result = id1.equals(id3);

        // then
        assertThat(result).isFalse();
        assertThat(id1).isNotEqualTo(id3);
    }

    @Test
    @DisplayName("Should return false for equals when comparing with null")
    void equals_WithNull_ShouldReturnFalse() {
        // given & when
        boolean result = id1.equals(null);

        // then
        assertThat(result).isFalse();
        assertThat(id1).isNotEqualTo(null);
    }

    @Test
    @DisplayName("Should return false for equals when comparing with different class")
    void equals_WithDifferentClass_ShouldReturnFalse() {
        // given
        String differentObject = "different";

        // when
        boolean result = id1.equals(differentObject);

        // then
        assertThat(result).isFalse();
        assertThat(id1).isNotEqualTo(differentObject);
    }

    @Test
    @DisplayName("Should return true for equals when comparing with same instance")
    void equals_WithSameInstance_ShouldReturnTrue() {
        // given & when
        boolean result = id1.equals(id1);

        // then
        assertThat(result).isTrue();
        assertThat(id1).isEqualTo(id1);
    }

    @Test
    @DisplayName("Should have same hashCode for objects with same values")
    void hashCode_WithSameValues_ShouldReturnSameHashCode() {
        // given & when
        int hashCode1 = id1.hashCode();
        int hashCode2 = id2.hashCode();

        // then
        assertThat(hashCode1).isEqualTo(hashCode2);
    }

    @Test
    @DisplayName("Should have different hashCode for objects with different values")
    void hashCode_WithDifferentValues_ShouldReturnDifferentHashCode() {
        // given & when
        int hashCode1 = id1.hashCode();
        int hashCode3 = id3.hashCode();

        // then
        assertThat(hashCode1).isNotEqualTo(hashCode3);
    }

    @Test
    @DisplayName("Should maintain hashCode consistency")
    void hashCode_MultipleInvocations_ShouldReturnSameValue() {
        // given & when
        int firstCall = id1.hashCode();
        int secondCall = id1.hashCode();
        int thirdCall = id1.hashCode();

        // then
        assertThat(firstCall).isEqualTo(secondCall).isEqualTo(thirdCall);
    }

    @Test
    @DisplayName("Should handle null values in equals correctly")
    void equals_WithNullFields_ShouldHandleCorrectly() throws Exception {
        // given
        MedicationScheduleMedicationScheduleGroupId idWithNulls1 = createIdWithValues(null, null);
        MedicationScheduleMedicationScheduleGroupId idWithNulls2 = createIdWithValues(null, null);

        // when & then
        assertThat(idWithNulls1).isEqualTo(idWithNulls2);
    }

    @Test
    @DisplayName("Should handle mixed null and non-null values in equals correctly")
    void equals_WithMixedNullValues_ShouldReturnFalse() throws Exception {
        // given
        MedicationScheduleMedicationScheduleGroupId idWithNulls = createIdWithValues(null, null);

        // when & then
        assertThat(idWithNulls).isNotEqualTo(id1);
        assertThat(id1).isNotEqualTo(idWithNulls);
    }

    @Test
    @DisplayName("Should handle null values in hashCode correctly")
    void hashCode_WithNullFields_ShouldHandleCorrectly() throws Exception {
        // given
        MedicationScheduleMedicationScheduleGroupId idWithNulls = createIdWithValues(null, null);

        // when & then
        // Should not throw exception and should return consistent value
        int hashCode = idWithNulls.hashCode();
        assertThat(hashCode).isEqualTo(idWithNulls.hashCode());
    }

    @Test
    @DisplayName("Should be serializable")
    void serialization_ShouldWorkCorrectly() throws IOException, ClassNotFoundException {
        // given
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);

        // when - serialize
        oos.writeObject(id1);
        oos.close();

        // then - deserialize
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        MedicationScheduleMedicationScheduleGroupId deserializedId = 
                (MedicationScheduleMedicationScheduleGroupId) ois.readObject();
        ois.close();

        // Verify deserialized object equals original
        assertThat(deserializedId).isEqualTo(id1);
        assertThat(deserializedId.hashCode()).isEqualTo(id1.hashCode());
    }

    @Test
    @DisplayName("Should handle edge case values correctly")
    void equals_WithEdgeCaseValues_ShouldHandleCorrectly() throws Exception {
        // given
        MedicationScheduleMedicationScheduleGroupId idWithZeros = createIdWithValues(0L, 0L);
        MedicationScheduleMedicationScheduleGroupId idWithNegatives = createIdWithValues(-1L, -1L);
        MedicationScheduleMedicationScheduleGroupId idWithMaxValues = createIdWithValues(Long.MAX_VALUE, Long.MAX_VALUE);

        // when & then
        assertThat(idWithZeros).isNotEqualTo(idWithNegatives);
        assertThat(idWithZeros).isNotEqualTo(idWithMaxValues);
        assertThat(idWithNegatives).isNotEqualTo(idWithMaxValues);

        // Verify each has consistent hashCode
        assertThat(idWithZeros.hashCode()).isEqualTo(idWithZeros.hashCode());
        assertThat(idWithNegatives.hashCode()).isEqualTo(idWithNegatives.hashCode());
        assertThat(idWithMaxValues.hashCode()).isEqualTo(idWithMaxValues.hashCode());
    }

    @Test
    @DisplayName("Should handle partial field differences correctly")
    void equals_WithPartialFieldDifferences_ShouldReturnFalse() throws Exception {
        // given
        MedicationScheduleMedicationScheduleGroupId idSameSchedule = createIdWithValues(1L, 99L);
        MedicationScheduleMedicationScheduleGroupId idSameGroup = createIdWithValues(99L, 10L);

        // when & then
        assertThat(id1).isNotEqualTo(idSameSchedule);
        assertThat(id1).isNotEqualTo(idSameGroup);
        assertThat(idSameSchedule).isNotEqualTo(idSameGroup);
    }

    @Test
    @DisplayName("Should verify field values are set correctly through reflection")
    void reflection_FieldAccess_ShouldWorkCorrectly() throws Exception {
        // given
        Long expectedScheduleId = 100L;
        Long expectedGroupId = 200L;
        MedicationScheduleMedicationScheduleGroupId id = createIdWithValues(expectedScheduleId, expectedGroupId);

        // when & then
        assertThat(getMedicationScheduleId(id)).isEqualTo(expectedScheduleId);
        assertThat(getMedicationScheduleGroupId(id)).isEqualTo(expectedGroupId);
    }
}