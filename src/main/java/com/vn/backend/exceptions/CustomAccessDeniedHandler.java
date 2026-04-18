package com.vn.backend.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.dto.response.common.AppErrorResponse;
import com.vn.backend.dto.response.common.AppResponse;
import com.vn.backend.dto.response.common.ResponseErrorElement;
import com.vn.backend.utils.MessageUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final MessageUtils messageUtils;
    private final ObjectMapper objectMapper;

    public CustomAccessDeniedHandler(MessageUtils messageUtils, ObjectMapper objectMapper) {
        this.messageUtils = messageUtils;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        
        // Get internationalized error message
        String errorMessage = messageUtils.getMessage(MessageConst.FORBIDDEN);
        
        // Create error response following base code pattern
        final AppResponse<Object> apiError = AppResponse.builder().build();
        List<ResponseErrorElement> errors = new ArrayList<>();
        errors.add(ResponseErrorElement.builder()
                .code(MessageConst.FORBIDDEN)
                .message(errorMessage)
                .build());
        AppErrorResponse<Object> errorResponse = new AppErrorResponse<>(apiError, errors);
        
        // Write response
        PrintWriter out = response.getWriter();
        out.print(objectMapper.writeValueAsString(errorResponse));
        out.flush();
    }
}