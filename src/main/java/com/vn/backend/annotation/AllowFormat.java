package com.vn.backend.annotation;

import com.vn.backend.annotation.validator.AllowFormatValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = AllowFormatValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE,
        ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowFormat {

    String message() default "Invalid input format. Expected format: {regex}";

    String[] regex();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String fieldName() default "";
}

