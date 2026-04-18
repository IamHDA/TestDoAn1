package com.vn.backend.exceptions;

import com.vn.backend.constants.AppConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.dto.response.common.AppErrorResponse;
import com.vn.backend.dto.response.common.AppResponse;
import com.vn.backend.dto.response.common.ResponseErrorElement;
import com.vn.backend.utils.MessageUtils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final MessageUtils messageUtils;

    public GlobalExceptionHandler(MessageUtils messageUtils){this.messageUtils = messageUtils;
    }
    @ExceptionHandler(AppException.class)
    public final ResponseEntity<Object> handleCustomException(AppException ex, WebRequest request) {
        HttpHeaders headers = new HttpHeaders();
        final AppResponse<Object> apiError = AppResponse.builder().build();
        List<ResponseErrorElement> errors = new ArrayList<>();
        errors.add(ResponseErrorElement.builder().code(ex.getCode()).message(ex.getMessage()).build());
        AppErrorResponse<Object> errorResponse = new AppErrorResponse<>(apiError, errors);
        return handleExceptionInternal(ex, errorResponse, headers, ex.getHttpStatus(), request);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllException(Exception ex, WebRequest request) {
        logger.error(ex.getMessage());
        HttpHeaders headers = new HttpHeaders();
        final String errorMessage = messageUtils.getMessage(MessageConst.SERVER_UNAVAILABLE);
        final AppResponse<Object> apiError = AppResponse.builder().build();
        List<ResponseErrorElement> errors = new ArrayList<>();
        errors.add(ResponseErrorElement.builder().code(MessageConst.SERVER_UNAVAILABLE)
                .message(errorMessage).build());
        AppErrorResponse<Object> errorResponse = new AppErrorResponse<>(apiError, errors);
        return handleExceptionInternal(ex, errorResponse, headers, HttpStatus.INTERNAL_SERVER_ERROR,
                request);
    }

    @Override
    public final ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        List<ResponseErrorElement> errors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            String field = fieldError.getField();
            String code;
            String msg;
            String defaultMessage = fieldError.getDefaultMessage();
            if (StringUtils.isNotBlank(defaultMessage) && defaultMessage.contains(
                    AppConst.MESSAGE_SPLIT)) {
                code = defaultMessage.split(AppConst.MESSAGE_SPLIT)[0];
                msg = defaultMessage.split(AppConst.MESSAGE_SPLIT)[1];
            } else {
                code = MessageFormat.format(defaultMessage, fieldError.getArguments());
                msg = messageUtils.getMessage(code);
            }
            errors.add(ResponseErrorElement.builder().field(field).code(code).message(msg).build());
        }
        if (errors.isEmpty()) {
            final String errorMessage = messageUtils.getMessage(MessageConst.SERVER_UNAVAILABLE);
            errors.add(ResponseErrorElement.builder().code(MessageConst.SERVER_UNAVAILABLE)
                    .message(errorMessage).build());
            final AppResponse<Object> apiError = AppResponse.builder().build();
            AppErrorResponse<Object> errorResponse = new AppErrorResponse<>(apiError, errors);
            return handleExceptionInternal(ex, errorResponse, headers, HttpStatus.INTERNAL_SERVER_ERROR,
                    request);
        } else {
            final AppResponse<Object> apiError = AppResponse.builder().build();
            AppErrorResponse<Object> errorResponse = new AppErrorResponse<>(apiError, errors);
            return handleExceptionInternal(ex, errorResponse, headers, HttpStatus.BAD_REQUEST, request);
        }
    }
    @ExceptionHandler(ConstraintViolationException.class)
    public final ResponseEntity<Object> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {

        List<ResponseErrorElement> errors = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String field = violation.getPropertyPath().toString();
            String code;
            String msg;
            String defaultMessage = violation.getMessage();
            if (StringUtils.isNotBlank(defaultMessage) && defaultMessage.contains(
                    AppConst.MESSAGE_SPLIT)) {
                code = defaultMessage.split(AppConst.MESSAGE_SPLIT)[0];
                msg = defaultMessage.split(AppConst.MESSAGE_SPLIT)[1];
            } else {
                code = MessageFormat.format(defaultMessage, field);
                msg = messageUtils.getMessage(code);
            }
            errors.add(ResponseErrorElement.builder().field(field).code(code).message(msg).build());
        }
        HttpHeaders headers = new HttpHeaders();
        final AppResponse<Object> apiError = AppResponse.builder().build();
        AppErrorResponse<Object> errorResponse = new AppErrorResponse<>(apiError, errors);
        return handleExceptionInternal(ex, errorResponse, headers, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    public final ResponseEntity<Object> handleInsufficientAuthenticationException(
            InsufficientAuthenticationException ex, WebRequest request) {
        logger.warn("[InsufficientAuthenticationException] {}", ex.getMessage());
        HttpHeaders headers = new HttpHeaders();
        final String errorMessage = messageUtils.getMessage(MessageConst.UNAUTHORIZED);
        final AppResponse<Object> apiError = AppResponse.builder().build();
        List<ResponseErrorElement> errors = new ArrayList<>();
        errors.add(ResponseErrorElement.builder()
                .code(MessageConst.UNAUTHORIZED)
                .message(errorMessage)
                .build());
        AppErrorResponse<Object> errorResponse = new AppErrorResponse<>(apiError, errors);
        return handleExceptionInternal(ex, errorResponse, headers, HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public final ResponseEntity<Object> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        logger.warn("[AuthenticationException] {}", ex.getMessage());
        HttpHeaders headers = new HttpHeaders();
        final String errorMessage = messageUtils.getMessage(MessageConst.UNAUTHORIZED);
        final AppResponse<Object> apiError = AppResponse.builder().build();
        List<ResponseErrorElement> errors = new ArrayList<>();
        errors.add(ResponseErrorElement.builder()
                .code(MessageConst.UNAUTHORIZED)
                .message(errorMessage)
                .build());
        AppErrorResponse<Object> errorResponse = new AppErrorResponse<>(apiError, errors);
        return handleExceptionInternal(ex, errorResponse, headers, HttpStatus.UNAUTHORIZED, request);
    }

    @Override
    public final ResponseEntity<Object> handleNoResourceFoundException(
        NoResourceFoundException ex, HttpHeaders headers,
        HttpStatusCode status, WebRequest request) {
        List<ResponseErrorElement> errors = new ArrayList<>();
        errors.add(
            ResponseErrorElement.builder().code(MessageConst.CONNECTION_FAILED)
                .message(messageUtils.getMessage(MessageConst.CONNECTION_FAILED))
                .build());
        AppErrorResponse<Object> errorResponse = new AppErrorResponse<>(AppResponse.builder().build(),
            errors);
        return handleExceptionInternal(ex, errorResponse, headers, HttpStatus.NOT_FOUND,
            request);
    }


//
//    @ExceptionHandler(InvalidDataException.class)
//    @ResponseStatus(HttpStatus.BAD_REQUEST)
//    public ResponseData<ErrorResponse> handleInvalidDataException(InvalidDataException ex, WebRequest request, HttpServletRequest requests) {
//        logger.warn("[InvalidDataException] {}", ex.getMessage());
//        ErrorResponse errorResponse = ErrorResponse.builder()
//                .timestamp(new Date())
//                .path(request.getDescription(false))
//                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
//                .message(ex.getMessage())
//                .build();
//
//        return ResponseData.badRequest(errorResponse);
//    }
//
//    @ExceptionHandler(Exception.class)
//    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
//    public ResponseData<ErrorResponse> handleAllException(Exception ex, WebRequest request,HttpServletRequest requests) {
//        logger.error("[Exception] {}", ex.getMessage());
//        ErrorResponse errorResponse = ErrorResponse.builder()
//                .timestamp(new Date())
//                .path(request.getDescription(false))
//                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
//                .message("Hệ thống đang bận, vui lòng thử lại sau.")
//                .build();
//
//        return ResponseData.error( errorResponse);
//    }
//
//    @ResponseStatus(HttpStatus.NOT_FOUND)
//    @ExceptionHandler(NotFoundException.class)
//    public ResponseData<ErrorResponse> handleNotFoundException(NotFoundException ex, WebRequest request,HttpServletRequest requests) {
//        logger.warn("[NotFoundException] {}", ex.getMessage());
//
//        ErrorResponse errorResponse = ErrorResponse.builder()
//                .timestamp(new Date())
//                .path(request.getDescription(false))
//                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
//                .message(ex.getMessage())
//                .build();
//        return ResponseData.notFound(errorResponse);
//
//    }
//
//    @ResponseStatus(HttpStatus.NOT_FOUND)
//    @ExceptionHandler(NullPointerException.class)
//    public ResponseData<ErrorResponse> handleNullPointerException(NullPointerException ex, WebRequest request,HttpServletRequest requests) {
//        logger.warn("[NullPointerException] {}", ex.getMessage());
//
//        ErrorResponse errorResponse = ErrorResponse.builder()
//                .timestamp(new Date())
//                .path(request.getDescription(false))
//                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
//                .message(ex.getMessage())
//                .build();
//        return ResponseData.notFound(errorResponse);
//    }
//
//    @ExceptionHandler(AccessDeniedException.class)
//    @ResponseStatus(HttpStatus.FORBIDDEN)
//    public ResponseData<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request,HttpServletRequest requests) {
//        logger.warn("[AccessDeniedException] {}", ex.getMessage());
//
//        ErrorResponse errorResponse = ErrorResponse.builder()
//                .timestamp(new Date())
//                .path(request.getDescription(false))
//                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
//                .message(ex.getMessage())
//                .build();
//        return ResponseData.notFound(errorResponse);
//    }
//
//    @ExceptionHandler({ExpiredJwtException.class, SignatureException.class, MalformedJwtException.class, UnsupportedJwtException.class})
//    @ResponseStatus(HttpStatus.UNAUTHORIZED)
//    public ResponseData<ErrorResponse> handleJwtException(Exception ex,  HttpServletRequest request,HttpServletRequest requests) {
//        logger.warn("[JwtException] {}", ex.getMessage());
//        ErrorResponse errorResponse = ErrorResponse.builder()
//                .timestamp(new Date())
//                .path(request.getRequestURI())
//                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
//                .message("Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.")
//                .build();
//        return ResponseData.unauthorized(errorResponse);
//    }
//
//    @ExceptionHandler({
//            ConstraintViolationException.class,
//            MissingServletRequestParameterException.class,
//            MethodArgumentNotValidException.class,
//    })
//    @ResponseStatus(HttpStatus.BAD_REQUEST)
//    public ResponseData<ErrorResponse> handleValidationException(Exception e, WebRequest request, HttpServletRequest httpRequest) {
//        String path = request.getDescription(false).replace("uri=", "");
//        String message = e.getMessage();
//        String error;
//
//        if (e instanceof MethodArgumentNotValidException) {
//            int start = message.lastIndexOf("[") + 1;
//            int end = message.lastIndexOf("]") - 1;
//            message = message.substring(start, end);
//            error = "Invalid Payload";
//            logger.warn("[MethodArgumentNotValidException] {}", message);
//        } else if (e instanceof MissingServletRequestParameterException) {
//            error = "Invalid Parameter";
//            logger.warn("[MissingServletRequestParameterException] {}", message);
//        } else if (e instanceof ConstraintViolationException) {
//            error = "Invalid Parameter";
//            message = message.substring(message.indexOf(" ") + 1);
//            logger.warn("[ConstraintViolationException] {}", message);
//        }else {
//            error = "Invalid Data";
//            logger.warn("[InvalidDataException] {}", message);
//        }
//
//        ErrorResponse errorResponse = ErrorResponse.builder()
//                .timestamp(new Date())
//                .path(path)
//                .error(error)
//                .message(message)
//                .build();
//
//        return ResponseData.badRequest( errorResponse);
//    }
//    @ExceptionHandler({ MethodArgumentTypeMismatchException.class })
//    @ResponseStatus(HttpStatus.BAD_REQUEST)
//    public ResponseData<ErrorResponse> handleMethodArgumentTypeMismatch(
//            MethodArgumentTypeMismatchException ex, WebRequest request, HttpServletRequest httpRequest) {
//        logger.warn("[MethodArgumentTypeMismatchException] {}", ex.getMessage());
//        String error = ex.getName() + " should be of type " + ex.getRequiredType().getName();
//        ErrorResponse errorResponse = ErrorResponse.builder()
//                .timestamp(new Date())
//                .path(httpRequest.getRequestURI())
//                .error(error)
//                .message(ex.getLocalizedMessage())
//                .build();
//        return ResponseData.badRequest(errorResponse);
//    }
}