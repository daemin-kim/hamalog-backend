package com.Hamalog.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ImageValidator.class)
@Documented
public @interface ValidImage {

    String message() default "Invalid image file";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    long maxSize() default 5 * 1024 * 1024;

    String[] allowedContentTypes() default {"image/jpeg", "image/png", "image/gif", "image/webp"};
}
