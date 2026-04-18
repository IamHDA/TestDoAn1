package com.vn.backend.annotation.validator;


import com.vn.backend.annotation.ValidEnum;
import com.vn.backend.constants.AppConst;
import com.vn.backend.utils.MessageUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

public class EnumValidator implements ConstraintValidator<ValidEnum, String> {
    private final MessageUtils messageUtils;

    private Class<? extends Enum<?>> enumClass;
    private String fieldName;
    private String messageKey;

    public EnumValidator(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
    }

    @Override
    public void initialize(ValidEnum constraintAnnotation) {
        this.enumClass = constraintAnnotation.enumClass();
        this.fieldName = constraintAnnotation.fieldName();
        this.messageKey = constraintAnnotation.message();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(value)) {
            return true;
        }

        for (Enum<?> constant : enumClass.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(value)) {
                return true;
            }
        }

        context.disableDefaultConstraintViolation();

        String fieldDisplayName = messageUtils.getMessage(fieldName);
        String errorMessage = messageUtils.getMessage(messageKey, fieldDisplayName);

        context.buildConstraintViolationWithTemplate(
                messageKey + AppConst.MESSAGE_SPLIT + errorMessage
        ).addConstraintViolation();

        return false;
    }
}
