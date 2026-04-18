package com.vn.backend.annotation;
import com.vn.backend.annotation.validator.EnumValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE,
        ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = EnumValidator.class)
public @interface ValidEnum {

    String message() default "INVALID_ENUM";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String fieldName();

    Class<? extends Enum<?>> enumClass();
}

