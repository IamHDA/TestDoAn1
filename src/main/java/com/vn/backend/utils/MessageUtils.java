package com.vn.backend.utils;


import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@Slf4j
public class MessageUtils {

    private static final String ERROR_LOG = "Error getMessage, reason: {}";

    private final MessageSource messageSource;

    public MessageUtils(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String getMessage(String code) {
        try {
            return messageSource.getMessage(code, new Object[]{}, LocaleContextHolder.getLocale());
        } catch (Exception e) {
            log.error(ERROR_LOG, e.getMessage());
            return getMessageLocale(code, Locale.getDefault());
        }
    }

    public String getMessage(String code, Object... args) {
        try {
            return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
        } catch (Exception e) {
            log.error(ERROR_LOG, e.getMessage());
            return getMessageLocale(code, Locale.getDefault(), args);
        }
    }

    public String getMessageLocale(String code, Locale locale, Object... args) {
        try {
            return messageSource.getMessage(code, args, locale);
        } catch (Exception e) {
            log.error(ERROR_LOG, e.getMessage());
            if (Locale.getDefault() != locale) {
                return getMessageLocale(code, Locale.getDefault(), args);
            }
            return null;
        }
    }

}
