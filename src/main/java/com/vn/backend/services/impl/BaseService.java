package com.vn.backend.services.impl;

import com.vn.backend.utils.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BaseService {

    protected final MessageUtils messageUtils;
    protected Logger log = LoggerFactory.getLogger(this.getClass());

    public BaseService(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
    }
}
