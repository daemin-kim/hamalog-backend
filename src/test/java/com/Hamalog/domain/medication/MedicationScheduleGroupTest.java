package com.Hamalog.domain.medication;

import com.Hamalog.domain.member.Member;
import jakarta.persistence.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MedicationScheduleGroup Entity Tests")
class MedicationScheduleGroupTest {

    @Test
    @DisplayName("Default constructor should create instance with null fields and Lombok getters should exist")
    void defaultConstructor_And_Getters() {
        // when
        MedicationScheduleGroup group = new MedicationScheduleGroup();

        // then
        assertThat(group).isNotNull();
        assertThat(group.getMedicationScheduleGroupId()).isNull();
        assertThat(group.getMember()).isNull();
        assertThat(group.getName()).isNull();
    }

    @Test
    @DisplayName("JPA annotations should be present on class and fields with correct attributes")
    void jpaAnnotations_ShouldBePresent_WithCorrectAttributes() throws Exception {
        // class-level annotation
        assertThat(MedicationScheduleGroup.class.isAnnotationPresent(Entity.class)).isTrue();

        // id field annotations
        Field idField = MedicationScheduleGroup.class.getDeclaredField("medicationScheduleGroupId");
        assertThat(idField.getAnnotation(Id.class)).isNotNull();

        GeneratedValue generatedValue = idField.getAnnotation(GeneratedValue.class);
        assertThat(generatedValue).isNotNull();
        assertThat(generatedValue.strategy()).isEqualTo(GenerationType.IDENTITY);

        Column idColumn = idField.getAnnotation(Column.class);
        assertThat(idColumn).isNotNull();
        assertThat(idColumn.name()).isEqualTo("medication_schedule_group_id");

        // member field annotations
        Field memberField = MedicationScheduleGroup.class.getDeclaredField("member");
        ManyToOne manyToOne = memberField.getAnnotation(ManyToOne.class);
        assertThat(manyToOne).isNotNull();
        assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);

        JoinColumn joinColumn = memberField.getAnnotation(JoinColumn.class);
        assertThat(joinColumn).isNotNull();
        assertThat(joinColumn.name()).isEqualTo("member_id");
        assertThat(joinColumn.nullable()).isFalse();

        // name field annotations
        Field nameField = MedicationScheduleGroup.class.getDeclaredField("name");
        Column nameColumn = nameField.getAnnotation(Column.class);
        assertThat(nameColumn).isNotNull();
        assertThat(nameColumn.length()).isEqualTo(20);
        assertThat(nameColumn.nullable()).isFalse();
    }

    @Test
    @DisplayName("Reflection can set private fields and Lombok getters should return values")
    void reflection_SetFields_And_GettersReturnValues() throws Exception {
        // given
        MedicationScheduleGroup group = new MedicationScheduleGroup();

        // set id
        Field idField = MedicationScheduleGroup.class.getDeclaredField("medicationScheduleGroupId");
        idField.setAccessible(true);
        idField.set(group, 10L);

        // create a minimal Member via builder
        Member member = Member.builder()
                .loginId("user@example.com")
                .password("encoded-password")
                .name("홍길동")
                .phoneNumber("01012345678")
                .nickName("길동")
                .birth(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.of(2023, 10, 14, 12, 0))
                .build();

        Field memberField = MedicationScheduleGroup.class.getDeclaredField("member");
        memberField.setAccessible(true);
        memberField.set(group, member);

        Field nameField = MedicationScheduleGroup.class.getDeclaredField("name");
        nameField.setAccessible(true);
        nameField.set(group, "Morning Meds");

        // then
        assertThat(group.getMedicationScheduleGroupId()).isEqualTo(10L);
        assertThat(group.getMember()).isEqualTo(member);
        assertThat(group.getName()).isEqualTo("Morning Meds");
    }

    @Test
    @DisplayName("equals should use reference equality by default and toString should contain class name")
    void equality_And_ToString() {
        MedicationScheduleGroup group1 = new MedicationScheduleGroup();
        MedicationScheduleGroup group2 = new MedicationScheduleGroup();

        assertThat(group1).isNotEqualTo(group2);
        assertThat(group1).isEqualTo(group1);
        assertThat(group1.toString()).contains("MedicationScheduleGroup");
    }
}
