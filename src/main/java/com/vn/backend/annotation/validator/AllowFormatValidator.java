package com.vn.backend.annotation.validator;


import com.vn.backend.annotation.AllowFormat;
import com.vn.backend.constants.AppConst;
import com.vn.backend.utils.MessageUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class AllowFormatValidator implements
        ConstraintValidator<AllowFormat, String> {

    private final MessageUtils messageUtils;
    private List<Pattern> patterns;
    private String messageKey;
    private String fieldName;

    public AllowFormatValidator(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
    }

    @Override
    public void initialize(AllowFormat constraintAnnotation) {
        String[] regexArray = constraintAnnotation.regex();
        this.patterns = Arrays.stream(regexArray)
                .map(Pattern::compile)
                .toList();
        this.messageKey = constraintAnnotation.message();
        this.fieldName = constraintAnnotation.fieldName();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true; // Let @NotNull or @NotEmpty handle null/empty validation
        }

        // Check if value matches ANY of the provided regex patterns
        if (patterns.stream()
                .noneMatch(pattern -> pattern.matcher(value).matches())) {
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

