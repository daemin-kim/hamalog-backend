package com.Hamalog.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireResourceOwnership {
    
    /**
     * Resource type enum for type safety
     */
    enum ResourceType {
        MEDICATION_RECORD("medication-record"),
        MEDICATION_SCHEDULE("medication-schedule"),
        MEDICATION_SCHEDULE_BY_MEMBER("medication-schedule-by-member"),
        MEMBER("member");
        
        private final String value;
        
        ResourceType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    /**
     * Ownership validation strategy
     */
    enum OwnershipStrategy {
        DIRECT,           // Direct resource ownership validation
        THROUGH_SCHEDULE, // Validate through medication schedule
        THROUGH_MEMBER    // Validate through member
    }
    
    /**
     * Parameter extraction strategy
     */
    enum ParameterSource {
        PATH_VARIABLE,    // Extract from @PathVariable
        REQUEST_PARAM,    // Extract from @RequestParam
        REQUEST_BODY      // Extract from request body field
    }
    
    /**
     * Resource type to validate ownership for
     */
    ResourceType resourceType();
    
    /**
     * Parameter name to extract resource ID from
     */
    String paramName();
    
    /**
     * Ownership validation strategy (default: DIRECT)
     */
    OwnershipStrategy strategy() default OwnershipStrategy.DIRECT;
    
    /**
     * Parameter extraction source (default: PATH_VARIABLE)
     */
    ParameterSource source() default ParameterSource.PATH_VARIABLE;
    
    /**
     * Field name in request body to extract ID from (when source = REQUEST_BODY)
     */
    String bodyField() default "";
    
    /**
     * HTTP status code to return on authorization failure
     */
    int errorStatus() default 403;
    
    /**
     * Error message for authorization failure
     */
    String errorMessage() default "접근 권한이 없습니다.";
}