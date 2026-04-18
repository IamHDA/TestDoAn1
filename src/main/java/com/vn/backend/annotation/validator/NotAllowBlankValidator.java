package com.vn.backend.annotation.validator;


import com.vn.backend.annotation.NotAllowBlank;
import com.vn.backend.constants.AppConst;
import com.vn.backend.utils.MessageUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NotAllowBlankValidator implements ConstraintValidator<NotAllowBlank, Object> {

    private final MessageUtils messageUtils;
    private String messageKey;
    private String fieldName;

    public NotAllowBlankValidator(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
    }

    @Override
    public void initialize(NotAllowBlank constraintAnnotation) {
        this.messageKey = constraintAnnotation.message();
        this.fieldName = constraintAnnotation.fieldName();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            buildConstraintViolation(context);
            return false;
        }

        // Validate String
        if (value instanceof String stringValue && stringValue.trim().isEmpty()) {
            buildConstraintViolation(context);
            return false;
        }

        // Validate Boolean
        if (value instanceof Boolean) {
            return true;
        }

        // Validate other type
        String stringRepresentation = value.toString();
        if (stringRepresentation.trim().isEmpty()) {
            buildConstraintViolation(context);
            return false;
        }
        return true;
    }

    private void buildConstraintViolation(ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        String fieldDisplayName = messageUtils.getMessage(fieldName);
        String errorMessage = messageUtils.getMessage(messageKey, fieldDisplayName);
        context.buildConstraintViolationWithTemplate(
                        messageKey + AppConst.MESSAGE_SPLIT + errorMessage)
                .addConstraintViolation();
    }
}
