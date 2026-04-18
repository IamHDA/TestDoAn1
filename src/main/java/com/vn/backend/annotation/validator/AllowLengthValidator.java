package com.vn.backend.annotation.validator;


import com.vn.backend.annotation.AllowLength;
import com.vn.backend.constants.AppConst;
import com.vn.backend.utils.MessageUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AllowLengthValidator implements ConstraintValidator<AllowLength, String> {

    private final MessageUtils messageUtils;
    private String messageKey;
    private String fieldName;
    private int min;
    private int max;

    public AllowLengthValidator(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
    }

    @Override
    public void initialize(AllowLength constraintAnnotation) {
        this.messageKey = constraintAnnotation.message();
        this.fieldName = constraintAnnotation.fieldName();
        this.min = constraintAnnotation.min();
        this.max = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        int length = value.length();
        if (length < min || length > max) {
            String fieldDisplayName = messageUtils.getMessage(fieldName);
            context.disableDefaultConstraintViolation();
            String errorMessage = messageUtils.getMessage(messageKey, fieldDisplayName);
            context.buildConstraintViolationWithTemplate(
                    messageKey + AppConst.MESSAGE_SPLIT + errorMessage).addConstraintViolation();
            return false;
        }
        return true;
    }
}

