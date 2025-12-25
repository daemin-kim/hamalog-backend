package com.Hamalog.security.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.withSettings;

import com.Hamalog.domain.medication.MedicationRecord;
import com.Hamalog.domain.medication.MedicationSchedule;
import com.Hamalog.domain.member.Member;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.security.annotation.RequireResourceOwnership;
import com.Hamalog.security.authorization.ResourceOwnershipValidator;
import com.Hamalog.service.diary.MoodDiaryService;
import com.Hamalog.service.medication.MedicationRecordService;
import com.Hamalog.service.medication.MedicationScheduleService;
import com.Hamalog.service.sideEffect.SideEffectService;
import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResourceOwnershipAspect Tests")
class ResourceOwnershipAspectTest {

    @Mock
    private MedicationRecordService medicationRecordService;

    @Mock
    private MedicationScheduleService medicationScheduleService;

    @Mock
    private SideEffectService sideEffectService;

    @Mock
    private MoodDiaryService moodDiaryService;

    @Mock
    private ResourceOwnershipValidator resourceOwnershipValidator;

    @Mock(lenient = true)
    private ProceedingJoinPoint joinPoint;

    @Mock(lenient = true)
    private MethodSignature methodSignature;

    @Mock(lenient = true)
    private Authentication authentication;

    @Mock(lenient = true)
    private SecurityContext securityContext;

    @InjectMocks
    private ResourceOwnershipAspect resourceOwnershipAspect;

    private Method testMethod;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        // Setup security context
        SecurityContextHolder.setContext(securityContext);
        
        // Setup MDC
        MDC.put("requestId", "test-request-123");
        
        // Setup mock method for parameter extraction tests
        testMethod = TestController.class.getMethod("testMethod", Long.class, String.class);
        
        // Setup common mocks
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(testMethod);
    }

    @Test
    @DisplayName("Should throw UNAUTHORIZED when no authentication is present")
    void checkResourceOwnership_NoAuthentication_ThrowsUnauthorized() throws Throwable {
        // given
        when(securityContext.getAuthentication()).thenReturn(null);
        RequireResourceOwnership annotation = createAnnotation(
            RequireResourceOwnership.ResourceType.MEDICATION_RECORD, 
            "id"
        );

        // when & then
        assertThatThrownBy(() -> resourceOwnershipAspect.checkResourceOwnership(joinPoint, annotation))
            .isInstanceOf(CustomException.class)
            .hasMessage(ErrorCode.UNAUTHORIZED.getMessage());
    }

    @Test
    @DisplayName("Should throw UNAUTHORIZED when authentication is not authenticated")
    void checkResourceOwnership_NotAuthenticated_ThrowsUnauthorized() throws Throwable {
        // given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);
        RequireResourceOwnership annotation = createAnnotation(
            RequireResourceOwnership.ResourceType.MEDICATION_RECORD, 
            "id"
        );

        // when & then
        assertThatThrownBy(() -> resourceOwnershipAspect.checkResourceOwnership(joinPoint, annotation))
            .isInstanceOf(CustomException.class)
            .hasMessage(ErrorCode.UNAUTHORIZED.getMessage());
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when resource ID parameter is not found")
    void checkResourceOwnership_ResourceIdNotFound_ThrowsBadRequest() throws Throwable {
        // given
        setupAuthenticatedUser("testUser");
        when(joinPoint.getArgs()).thenReturn(new Object[]{123L, "test"});
        RequireResourceOwnership annotation = createAnnotation(
            RequireResourceOwnership.ResourceType.MEDICATION_RECORD, 
            "nonExistentParam"
        );

        // when & then
        assertThatThrownBy(() -> resourceOwnershipAspect.checkResourceOwnership(joinPoint, annotation))
            .isInstanceOf(CustomException.class)
            .hasMessage(ErrorCode.BAD_REQUEST.getMessage());
    }

    @Test
    @DisplayName("Should throw FORBIDDEN when user is not owner of medication record")
    void checkResourceOwnership_NotOwnerOfRecord_ThrowsForbidden() throws Throwable {
        // given
        setupAuthenticatedUser("testUser");
        when(joinPoint.getArgs()).thenReturn(new Object[]{123L, "test"});
        RequireResourceOwnership annotation = createAnnotation(
            RequireResourceOwnership.ResourceType.MEDICATION_RECORD, 
            "id"
        );
        when(medicationRecordService.isOwnerOfRecord(123L, "testUser")).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> resourceOwnershipAspect.checkResourceOwnership(joinPoint, annotation))
            .isInstanceOf(CustomException.class)
            .hasMessage(ErrorCode.FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("Should proceed when user owns medication record")
    void checkResourceOwnership_UserOwnsRecord_Proceeds() throws Throwable {
        // given
        setupAuthenticatedUser("testUser");
        when(joinPoint.getArgs()).thenReturn(new Object[]{123L, "test"});
        RequireResourceOwnership annotation = createAnnotation(
            RequireResourceOwnership.ResourceType.MEDICATION_RECORD, 
            "id"
        );
        when(medicationRecordService.isOwnerOfRecord(123L, "testUser")).thenReturn(true);
        Object expectedResult = "success";
        when(joinPoint.proceed()).thenReturn(expectedResult);

        // when
        Object result = resourceOwnershipAspect.checkResourceOwnership(joinPoint, annotation);

        // then
        assertThat(result).isEqualTo(expectedResult);
        verify(joinPoint).proceed();
    }

    @Test
    @DisplayName("Should validate medication schedule ownership through member")
    void checkResourceOwnership_MedicationSchedule_ValidatesCorrectly() throws Throwable {
        // given
        setupAuthenticatedUser("testUser");
        when(joinPoint.getArgs()).thenReturn(new Object[]{123L, "test"});
        RequireResourceOwnership annotation = createAnnotation(
            RequireResourceOwnership.ResourceType.MEDICATION_SCHEDULE, 
            "id"
        );
        
        // Mock the complex logic: get schedule first, then check member ownership
        MedicationSchedule mockSchedule = mock(MedicationSchedule.class);
        Member mockMember = mock(Member.class);
        when(mockSchedule.getMember()).thenReturn(mockMember);
        when(mockMember.getMemberId()).thenReturn(456L);
        when(medicationScheduleService.getMedicationSchedule(123L)).thenReturn(mockSchedule);
        when(resourceOwnershipValidator.isOwnerByMemberId(456L, "testUser")).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("success");

        // when
        resourceOwnershipAspect.checkResourceOwnership(joinPoint, annotation);

        // then
        verify(medicationScheduleService).getMedicationSchedule(123L);
        verify(resourceOwnershipValidator).isOwnerByMemberId(456L, "testUser");
    }

    @Test
    @DisplayName("Should validate medication schedule by member ownership")
    void checkResourceOwnership_MedicationScheduleByMember_ValidatesCorrectly() throws Throwable {
        // given
        setupAuthenticatedUser("testUser");
        when(joinPoint.getArgs()).thenReturn(new Object[]{123L, "test"});
        RequireResourceOwnership annotation = createAnnotation(
            RequireResourceOwnership.ResourceType.MEDICATION_SCHEDULE_BY_MEMBER, 
            "id"
        );
        when(resourceOwnershipValidator.isOwnerByMemberId(123L, "testUser")).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("success");

        // when
        resourceOwnershipAspect.checkResourceOwnership(joinPoint, annotation);

        // then
        verify(resourceOwnershipValidator).isOwnerByMemberId(123L, "testUser");
    }

    @Test
    @DisplayName("Should validate member ownership")
    void checkResourceOwnership_Member_ValidatesCorrectly() throws Throwable {
        // given
        setupAuthenticatedUser("testUser");
        when(joinPoint.getArgs()).thenReturn(new Object[]{123L, "test"});
        RequireResourceOwnership annotation = createAnnotation(
            RequireResourceOwnership.ResourceType.MEMBER, 
            "id"
        );
        when(resourceOwnershipValidator.isOwnerByMemberId(123L, "testUser")).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("success");

        // when
        resourceOwnershipAspect.checkResourceOwnership(joinPoint, annotation);

        // then
        verify(resourceOwnershipValidator).isOwnerByMemberId(123L, "testUser");
    }

    @Test
    @DisplayName("Should handle THROUGH_SCHEDULE ownership strategy for medication record")
    void checkResourceOwnership_ThroughScheduleStrategy_ValidatesCorrectly() throws Throwable {
        // given
        setupAuthenticatedUser("testUser");
        when(joinPoint.getArgs()).thenReturn(new Object[]{123L, "test"});
        RequireResourceOwnership annotation = mock(RequireResourceOwnership.class);
        when(annotation.resourceType()).thenReturn(RequireResourceOwnership.ResourceType.MEDICATION_RECORD);
        when(annotation.paramName()).thenReturn("id");
        when(annotation.source()).thenReturn(RequireResourceOwnership.ParameterSource.PATH_VARIABLE);
        when(annotation.strategy()).thenReturn(RequireResourceOwnership.OwnershipStrategy.THROUGH_SCHEDULE);
        
        // Mock complex logic: get medication record -> get schedule -> get member -> check ownership
        MedicationRecord mockRecord = mock(MedicationRecord.class);
        MedicationSchedule mockSchedule = mock(MedicationSchedule.class);
        Member mockMember = mock(Member.class);
        when(mockRecord.getMedicationSchedule()).thenReturn(mockSchedule);
        when(mockSchedule.getMember()).thenReturn(mockMember);
        when(mockMember.getMemberId()).thenReturn(456L);
        when(medicationRecordService.getMedicationRecord(123L)).thenReturn(mockRecord);
        when(resourceOwnershipValidator.isOwnerByMemberId(456L, "testUser")).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("success");

        // when
        resourceOwnershipAspect.checkResourceOwnership(joinPoint, annotation);

        // then
        verify(medicationRecordService).getMedicationRecord(123L);
        verify(resourceOwnershipValidator).isOwnerByMemberId(456L, "testUser");
    }

    @Test
    @DisplayName("Should handle THROUGH_MEMBER ownership strategy for medication schedule")
    void checkResourceOwnership_ThroughMemberStrategy_ValidatesCorrectly() throws Throwable {
        // given
        setupAuthenticatedUser("testUser");
        when(joinPoint.getArgs()).thenReturn(new Object[]{123L, "test"});
        RequireResourceOwnership annotation = mock(RequireResourceOwnership.class);
        when(annotation.resourceType()).thenReturn(RequireResourceOwnership.ResourceType.MEDICATION_SCHEDULE);
        when(annotation.paramName()).thenReturn("id");
        when(annotation.source()).thenReturn(RequireResourceOwnership.ParameterSource.PATH_VARIABLE);
        when(annotation.strategy()).thenReturn(RequireResourceOwnership.OwnershipStrategy.THROUGH_MEMBER);
        
        when(resourceOwnershipValidator.isOwnerByMemberId(123L, "testUser")).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("success");

        // when
        resourceOwnershipAspect.checkResourceOwnership(joinPoint, annotation);

        // then
        verify(resourceOwnershipValidator).isOwnerByMemberId(123L, "testUser");
    }

    @Test
    @DisplayName("Should extract resource ID from REQUEST_BODY")
    void extractResourceId_RequestBody_ExtractsCorrectly() throws Throwable {
        // given
        setupAuthenticatedUser("testUser");
        TestRequestBody requestBody = new TestRequestBody();
        requestBody.medicationScheduleId = 999L;
        when(joinPoint.getArgs()).thenReturn(new Object[]{requestBody});
        RequireResourceOwnership annotation = mock(RequireResourceOwnership.class);
        when(annotation.resourceType()).thenReturn(RequireResourceOwnership.ResourceType.MEDICATION_RECORD);
        when(annotation.paramName()).thenReturn("medicationScheduleId");
        when(annotation.source()).thenReturn(RequireResourceOwnership.ParameterSource.REQUEST_BODY);
        when(annotation.bodyField()).thenReturn("medicationScheduleId");
        when(annotation.strategy()).thenReturn(RequireResourceOwnership.OwnershipStrategy.DIRECT);
        
        when(medicationRecordService.isOwnerOfRecord(999L, "testUser")).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("success");

        // when
        resourceOwnershipAspect.checkResourceOwnership(joinPoint, annotation);

        // then
        verify(medicationRecordService).isOwnerOfRecord(999L, "testUser");
    }

    @Test
    @DisplayName("Should extract resource ID from REQUEST_PARAM")
    void extractResourceId_RequestParam_ExtractsCorrectly() throws Throwable {
        // given
        setupAuthenticatedUser("testUser");
        setupMethodWithRequestParam();
        when(joinPoint.getArgs()).thenReturn(new Object[]{789L});
        RequireResourceOwnership annotation = createAnnotation(
            RequireResourceOwnership.ResourceType.MEDICATION_RECORD,
            "scheduleId",
            RequireResourceOwnership.ParameterSource.REQUEST_PARAM
        );
        when(medicationRecordService.isOwnerOfRecord(789L, "testUser")).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("success");

        // when
        resourceOwnershipAspect.checkResourceOwnership(joinPoint, annotation);

        // then
        verify(medicationRecordService).isOwnerOfRecord(789L, "testUser");
    }

    @Test
    @DisplayName("Should handle null resource ID from request body")
    void extractFromRequestBody_NullValue_ThrowsBadRequest() throws Throwable {
        // given
        setupAuthenticatedUser("testUser");
        TestRequestBody requestBody = new TestRequestBody();
        requestBody.medicationScheduleId = null;
        when(joinPoint.getArgs()).thenReturn(new Object[]{requestBody});
        RequireResourceOwnership annotation = mock(RequireResourceOwnership.class, withSettings().lenient());
        when(annotation.resourceType()).thenReturn(RequireResourceOwnership.ResourceType.MEDICATION_RECORD);
        when(annotation.paramName()).thenReturn("medicationScheduleId");
        when(annotation.source()).thenReturn(RequireResourceOwnership.ParameterSource.REQUEST_BODY);
        when(annotation.bodyField()).thenReturn("medicationScheduleId");
        when(annotation.strategy()).thenReturn(RequireResourceOwnership.OwnershipStrategy.DIRECT);

        // when & then
        assertThatThrownBy(() -> resourceOwnershipAspect.checkResourceOwnership(joinPoint, annotation))
            .isInstanceOf(CustomException.class)
            .hasMessage(ErrorCode.BAD_REQUEST.getMessage());
    }

    @Test
    @DisplayName("Should handle exception during THROUGH_SCHEDULE validation")
    void checkResourceOwnership_ThroughScheduleException_ReturnsFalse() throws Throwable {
        // given
        setupAuthenticatedUser("testUser");
        when(joinPoint.getArgs()).thenReturn(new Object[]{123L, "test"});
        RequireResourceOwnership annotation = mock(RequireResourceOwnership.class);
        when(annotation.resourceType()).thenReturn(RequireResourceOwnership.ResourceType.MEDICATION_RECORD);
        when(annotation.paramName()).thenReturn("id");
        when(annotation.source()).thenReturn(RequireResourceOwnership.ParameterSource.PATH_VARIABLE);
        when(annotation.strategy()).thenReturn(RequireResourceOwnership.OwnershipStrategy.THROUGH_SCHEDULE);
        
        when(medicationRecordService.getMedicationRecord(123L)).thenThrow(new RuntimeException("Test exception"));

        // when & then
        assertThatThrownBy(() -> resourceOwnershipAspect.checkResourceOwnership(joinPoint, annotation))
            .isInstanceOf(CustomException.class)
            .hasMessage(ErrorCode.FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("Should handle exception during medication schedule validation")
    void checkResourceOwnership_ScheduleException_ReturnsFalse() throws Throwable {
        // given
        setupAuthenticatedUser("testUser");
        when(joinPoint.getArgs()).thenReturn(new Object[]{123L, "test"});
        RequireResourceOwnership annotation = createAnnotation(
            RequireResourceOwnership.ResourceType.MEDICATION_SCHEDULE, 
            "id"
        );
        
        when(medicationScheduleService.getMedicationSchedule(123L)).thenThrow(new RuntimeException("Test exception"));

        // when & then
        assertThatThrownBy(() -> resourceOwnershipAspect.checkResourceOwnership(joinPoint, annotation))
            .isInstanceOf(CustomException.class)
            .hasMessage(ErrorCode.FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("Should convert various numeric types to Long")
    void convertToLong_VariousTypes_ConvertsCorrectly() throws Exception {
        // Test direct Long
        Long longValue = 123L;
        assertThat(invokeConvertToLong(longValue)).isEqualTo(123L);
        
        // Test Integer
        Integer intValue = 456;
        assertThat(invokeConvertToLong(intValue)).isEqualTo(456L);
        
        // Test String
        String stringValue = "789";
        assertThat(invokeConvertToLong(stringValue)).isEqualTo(789L);
        
        // Test null
        assertThat(invokeConvertToLong(null)).isNull();
    }

    @Test
    @DisplayName("Should identify primitive types correctly")
    void isPrimitiveType_VariousTypes_IdentifiesCorrectly() throws Exception {
        assertThat(invokeIsPrimitiveType(String.class)).isTrue();
        assertThat(invokeIsPrimitiveType(Long.class)).isTrue();
        assertThat(invokeIsPrimitiveType(Integer.class)).isTrue();
        assertThat(invokeIsPrimitiveType(TestRequestBody.class)).isFalse();
    }

    // Helper methods
    private void setupAuthenticatedUser(String username) {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(username);
    }

    private RequireResourceOwnership createAnnotation(RequireResourceOwnership.ResourceType resourceType, String paramName) {
        return createAnnotation(resourceType, paramName, RequireResourceOwnership.ParameterSource.PATH_VARIABLE);
    }

    private RequireResourceOwnership createAnnotation(RequireResourceOwnership.ResourceType resourceType, String paramName, RequireResourceOwnership.ParameterSource source) {
        RequireResourceOwnership annotation = mock(RequireResourceOwnership.class, withSettings().lenient());
        when(annotation.resourceType()).thenReturn(resourceType);
        when(annotation.paramName()).thenReturn(paramName);
        when(annotation.source()).thenReturn(source);
        when(annotation.strategy()).thenReturn(RequireResourceOwnership.OwnershipStrategy.DIRECT);
        when(annotation.bodyField()).thenReturn("");
        return annotation;
    }

    private void setupMethodWithRequestParam() throws NoSuchMethodException {
        Method method = TestControllerWithRequestParam.class.getMethod("testMethod", Long.class);
        when(methodSignature.getMethod()).thenReturn(method);
    }

    private Long invokeConvertToLong(Object value) throws Exception {
        Method method = ResourceOwnershipAspect.class.getDeclaredMethod("convertToLong", Object.class);
        method.setAccessible(true);
        return (Long) method.invoke(resourceOwnershipAspect, value);
    }

    private boolean invokeIsPrimitiveType(Class<?> type) throws Exception {
        Method method = ResourceOwnershipAspect.class.getDeclaredMethod("isPrimitiveType", Class.class);
        method.setAccessible(true);
        return (Boolean) method.invoke(resourceOwnershipAspect, type);
    }

    // Test classes and DTOs
    public static class TestController {
        public void testMethod(@PathVariable("id") Long id, String name) {
            // Test method for parameter extraction
        }
    }

    public static class TestControllerWithRequestParam {
        public void testMethod(@RequestParam("scheduleId") Long scheduleId) {
            // Test method for request param extraction
        }
    }

    public static class TestRequestBody {
        public Long medicationScheduleId;
        public String name;
    }
}

