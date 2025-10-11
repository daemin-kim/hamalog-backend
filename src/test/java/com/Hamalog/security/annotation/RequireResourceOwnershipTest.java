package com.Hamalog.security.annotation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RequireResourceOwnership Tests")
class RequireResourceOwnershipTest {

    @Test
    @DisplayName("어노테이션이 올바른 타겟과 보존 정책을 가져야 함")
    void annotation_ShouldHaveCorrectTargetAndRetention() {
        // given
        Class<RequireResourceOwnership> annotationClass = RequireResourceOwnership.class;

        // when
        Target target = annotationClass.getAnnotation(Target.class);
        Retention retention = annotationClass.getAnnotation(Retention.class);

        // then
        assertThat(target.value()).containsExactly(ElementType.METHOD);
        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
    }

    @Test
    @DisplayName("ResourceType 열거형이 올바른 값들을 가져야 함")
    void resourceType_ShouldHaveCorrectValues() {
        // when
        RequireResourceOwnership.ResourceType[] resourceTypes = RequireResourceOwnership.ResourceType.values();

        // then
        assertThat(resourceTypes).hasSize(4);
        assertThat(resourceTypes).containsExactly(
            RequireResourceOwnership.ResourceType.MEDICATION_RECORD,
            RequireResourceOwnership.ResourceType.MEDICATION_SCHEDULE,
            RequireResourceOwnership.ResourceType.MEDICATION_SCHEDULE_BY_MEMBER,
            RequireResourceOwnership.ResourceType.MEMBER
        );
    }

    @Test
    @DisplayName("ResourceType의 getValue 메소드가 올바른 값을 반환해야 함")
    void resourceType_getValue_ShouldReturnCorrectValues() {
        // when & then
        assertThat(RequireResourceOwnership.ResourceType.MEDICATION_RECORD.getValue())
            .isEqualTo("medication-record");
        assertThat(RequireResourceOwnership.ResourceType.MEDICATION_SCHEDULE.getValue())
            .isEqualTo("medication-schedule");
        assertThat(RequireResourceOwnership.ResourceType.MEDICATION_SCHEDULE_BY_MEMBER.getValue())
            .isEqualTo("medication-schedule-by-member");
        assertThat(RequireResourceOwnership.ResourceType.MEMBER.getValue())
            .isEqualTo("member");
    }

    @Test
    @DisplayName("OwnershipStrategy 열거형이 올바른 값들을 가져야 함")
    void ownershipStrategy_ShouldHaveCorrectValues() {
        // when
        RequireResourceOwnership.OwnershipStrategy[] strategies = RequireResourceOwnership.OwnershipStrategy.values();

        // then
        assertThat(strategies).hasSize(3);
        assertThat(strategies).containsExactly(
            RequireResourceOwnership.OwnershipStrategy.DIRECT,
            RequireResourceOwnership.OwnershipStrategy.THROUGH_SCHEDULE,
            RequireResourceOwnership.OwnershipStrategy.THROUGH_MEMBER
        );
    }

    @Test
    @DisplayName("ParameterSource 열거형이 올바른 값들을 가져야 함")
    void parameterSource_ShouldHaveCorrectValues() {
        // when
        RequireResourceOwnership.ParameterSource[] sources = RequireResourceOwnership.ParameterSource.values();

        // then
        assertThat(sources).hasSize(3);
        assertThat(sources).containsExactly(
            RequireResourceOwnership.ParameterSource.PATH_VARIABLE,
            RequireResourceOwnership.ParameterSource.REQUEST_PARAM,
            RequireResourceOwnership.ParameterSource.REQUEST_BODY
        );
    }

    @Test
    @DisplayName("어노테이션의 기본값들이 올바르게 설정되어야 함")
    void annotation_ShouldHaveCorrectDefaultValues() throws Exception {
        // given
        Method strategyMethod = RequireResourceOwnership.class.getDeclaredMethod("strategy");
        Method sourceMethod = RequireResourceOwnership.class.getDeclaredMethod("source");
        Method bodyFieldMethod = RequireResourceOwnership.class.getDeclaredMethod("bodyField");
        Method errorStatusMethod = RequireResourceOwnership.class.getDeclaredMethod("errorStatus");
        Method errorMessageMethod = RequireResourceOwnership.class.getDeclaredMethod("errorMessage");

        // when & then
        assertThat(strategyMethod.getDefaultValue())
            .isEqualTo(RequireResourceOwnership.OwnershipStrategy.DIRECT);
        assertThat(sourceMethod.getDefaultValue())
            .isEqualTo(RequireResourceOwnership.ParameterSource.PATH_VARIABLE);
        assertThat(bodyFieldMethod.getDefaultValue()).isEqualTo("");
        assertThat(errorStatusMethod.getDefaultValue()).isEqualTo(403);
        assertThat(errorMessageMethod.getDefaultValue()).isEqualTo("접근 권한이 없습니다.");
    }

    // Test annotation on mock method
    @RequireResourceOwnership(
        resourceType = RequireResourceOwnership.ResourceType.MEDICATION_SCHEDULE,
        paramName = "scheduleId",
        strategy = RequireResourceOwnership.OwnershipStrategy.DIRECT,
        source = RequireResourceOwnership.ParameterSource.PATH_VARIABLE
    )
    public void mockMethodWithAnnotation(Long scheduleId) {
        // Mock method for testing annotation usage
    }

    @Test
    @DisplayName("메소드에 적용된 어노테이션이 올바른 값들을 가져야 함")
    void annotation_OnMethod_ShouldHaveCorrectValues() throws Exception {
        // given
        Method mockMethod = this.getClass().getDeclaredMethod("mockMethodWithAnnotation", Long.class);
        
        // when
        RequireResourceOwnership annotation = mockMethod.getAnnotation(RequireResourceOwnership.class);

        // then
        assertThat(annotation).isNotNull();
        assertThat(annotation.resourceType()).isEqualTo(RequireResourceOwnership.ResourceType.MEDICATION_SCHEDULE);
        assertThat(annotation.paramName()).isEqualTo("scheduleId");
        assertThat(annotation.strategy()).isEqualTo(RequireResourceOwnership.OwnershipStrategy.DIRECT);
        assertThat(annotation.source()).isEqualTo(RequireResourceOwnership.ParameterSource.PATH_VARIABLE);
        assertThat(annotation.bodyField()).isEqualTo(""); // default value
        assertThat(annotation.errorStatus()).isEqualTo(403); // default value
        assertThat(annotation.errorMessage()).isEqualTo("접근 권한이 없습니다."); // default value
    }

    // Test annotation with custom values
    @RequireResourceOwnership(
        resourceType = RequireResourceOwnership.ResourceType.MEDICATION_RECORD,
        paramName = "recordId",
        strategy = RequireResourceOwnership.OwnershipStrategy.THROUGH_SCHEDULE,
        source = RequireResourceOwnership.ParameterSource.REQUEST_BODY,
        bodyField = "medicationRecordId",
        errorStatus = 404,
        errorMessage = "복약 기록에 접근할 권한이 없습니다."
    )
    public void mockMethodWithCustomAnnotation() {
        // Mock method for testing custom annotation values
    }

    @Test
    @DisplayName("메소드에 적용된 커스텀 어노테이션이 올바른 값들을 가져야 함")
    void annotation_OnMethodWithCustomValues_ShouldHaveCorrectValues() throws Exception {
        // given
        Method mockMethod = this.getClass().getDeclaredMethod("mockMethodWithCustomAnnotation");
        
        // when
        RequireResourceOwnership annotation = mockMethod.getAnnotation(RequireResourceOwnership.class);

        // then
        assertThat(annotation).isNotNull();
        assertThat(annotation.resourceType()).isEqualTo(RequireResourceOwnership.ResourceType.MEDICATION_RECORD);
        assertThat(annotation.paramName()).isEqualTo("recordId");
        assertThat(annotation.strategy()).isEqualTo(RequireResourceOwnership.OwnershipStrategy.THROUGH_SCHEDULE);
        assertThat(annotation.source()).isEqualTo(RequireResourceOwnership.ParameterSource.REQUEST_BODY);
        assertThat(annotation.bodyField()).isEqualTo("medicationRecordId");
        assertThat(annotation.errorStatus()).isEqualTo(404);
        assertThat(annotation.errorMessage()).isEqualTo("복약 기록에 접근할 권한이 없습니다.");
    }

    @Test
    @DisplayName("ResourceType의 name 메소드가 올바른 이름을 반환해야 함")
    void resourceType_name_ShouldReturnCorrectName() {
        // when & then
        assertThat(RequireResourceOwnership.ResourceType.MEDICATION_RECORD.name())
            .isEqualTo("MEDICATION_RECORD");
        assertThat(RequireResourceOwnership.ResourceType.MEDICATION_SCHEDULE.name())
            .isEqualTo("MEDICATION_SCHEDULE");
        assertThat(RequireResourceOwnership.ResourceType.MEDICATION_SCHEDULE_BY_MEMBER.name())
            .isEqualTo("MEDICATION_SCHEDULE_BY_MEMBER");
        assertThat(RequireResourceOwnership.ResourceType.MEMBER.name())
            .isEqualTo("MEMBER");
    }

    @Test
    @DisplayName("OwnershipStrategy의 name 메소드가 올바른 이름을 반환해야 함")
    void ownershipStrategy_name_ShouldReturnCorrectName() {
        // when & then
        assertThat(RequireResourceOwnership.OwnershipStrategy.DIRECT.name()).isEqualTo("DIRECT");
        assertThat(RequireResourceOwnership.OwnershipStrategy.THROUGH_SCHEDULE.name()).isEqualTo("THROUGH_SCHEDULE");
        assertThat(RequireResourceOwnership.OwnershipStrategy.THROUGH_MEMBER.name()).isEqualTo("THROUGH_MEMBER");
    }

    @Test
    @DisplayName("ParameterSource의 name 메소드가 올바른 이름을 반환해야 함")
    void parameterSource_name_ShouldReturnCorrectName() {
        // when & then
        assertThat(RequireResourceOwnership.ParameterSource.PATH_VARIABLE.name()).isEqualTo("PATH_VARIABLE");
        assertThat(RequireResourceOwnership.ParameterSource.REQUEST_PARAM.name()).isEqualTo("REQUEST_PARAM");
        assertThat(RequireResourceOwnership.ParameterSource.REQUEST_BODY.name()).isEqualTo("REQUEST_BODY");
    }

    @Test
    @DisplayName("ResourceType.valueOf가 올바르게 동작해야 함")
    void resourceType_valueOf_ShouldWorkCorrectly() {
        // when & then
        assertThat(RequireResourceOwnership.ResourceType.valueOf("MEDICATION_RECORD"))
            .isEqualTo(RequireResourceOwnership.ResourceType.MEDICATION_RECORD);
        assertThat(RequireResourceOwnership.ResourceType.valueOf("MEDICATION_SCHEDULE"))
            .isEqualTo(RequireResourceOwnership.ResourceType.MEDICATION_SCHEDULE);
        assertThat(RequireResourceOwnership.ResourceType.valueOf("MEMBER"))
            .isEqualTo(RequireResourceOwnership.ResourceType.MEMBER);
    }

    @Test
    @DisplayName("열거형들이 올바른 순서를 가져야 함")
    void enums_ShouldHaveCorrectOrdinals() {
        // when & then
        assertThat(RequireResourceOwnership.ResourceType.MEDICATION_RECORD.ordinal()).isEqualTo(0);
        assertThat(RequireResourceOwnership.ResourceType.MEDICATION_SCHEDULE.ordinal()).isEqualTo(1);
        assertThat(RequireResourceOwnership.ResourceType.MEDICATION_SCHEDULE_BY_MEMBER.ordinal()).isEqualTo(2);
        assertThat(RequireResourceOwnership.ResourceType.MEMBER.ordinal()).isEqualTo(3);
        
        assertThat(RequireResourceOwnership.OwnershipStrategy.DIRECT.ordinal()).isEqualTo(0);
        assertThat(RequireResourceOwnership.OwnershipStrategy.THROUGH_SCHEDULE.ordinal()).isEqualTo(1);
        assertThat(RequireResourceOwnership.OwnershipStrategy.THROUGH_MEMBER.ordinal()).isEqualTo(2);
        
        assertThat(RequireResourceOwnership.ParameterSource.PATH_VARIABLE.ordinal()).isEqualTo(0);
        assertThat(RequireResourceOwnership.ParameterSource.REQUEST_PARAM.ordinal()).isEqualTo(1);
        assertThat(RequireResourceOwnership.ParameterSource.REQUEST_BODY.ordinal()).isEqualTo(2);
    }
}