package com.vn.backend.annotation;


import com.vn.backend.annotation.validator.AllowLengthValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = AllowLengthValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE,
        ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(AllowLength.List.class)
public @interface AllowLength {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String fieldName() default "";

    int min() default 0;

    int max() default Integer.MAX_VALUE;

    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE,
            ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {

        AllowLength[] value();
    }
}
