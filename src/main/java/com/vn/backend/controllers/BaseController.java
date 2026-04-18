package com.vn.backend.controllers;

import com.vn.backend.utils.ResponseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BaseController extends ResponseUtils {
    protected final Logger log = LoggerFactory.getLogger(getClass());
}
