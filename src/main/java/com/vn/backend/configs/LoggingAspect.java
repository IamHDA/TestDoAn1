package com.vn.backend.configs;

import com.vn.backend.utils.TraceIdUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);
    
    /**
     * Log execution for Controller methods
     */
    @Around("execution(* com.vn.backend.controllers..*(..))")
    public Object logControllerExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint);
    }
    
    /**
     * Log execution for Service methods
     */
    @Around("execution(* com.vn.backend.services..*(..))")
    public Object logServiceExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint);
    }
    
    private Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        // Generate trace ID if not exists
        String traceId = TraceIdUtils.getCurrentTraceId();
        if (traceId == null) {
            traceId = TraceIdUtils.generateTraceId();
        }
        
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = signature.getMethod().getName();
        String fullMethodName = className + "." + methodName;
        
        // Log method entry [IN]
        logger.info("{} [IN] {}", TraceIdUtils.getFormattedTraceId(), fullMethodName);
        
        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable exception = null;
        
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            exception = t;
            throw t;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (exception != null) {
                // Log exception [ERR]
                logger.error("{} [ERR] {} - Exception: {} - Execution time: {}ms", 
                    TraceIdUtils.getFormattedTraceId(), 
                    fullMethodName, 
                    exception.getMessage(), 
                    executionTime);
            } else {
                // Log method exit [OUT]
                logger.info("{} [OUT] {} - Execution time: {}ms", 
                    TraceIdUtils.getFormattedTraceId(), 
                    fullMethodName, 
                    executionTime);
            }
        }
    }
}
