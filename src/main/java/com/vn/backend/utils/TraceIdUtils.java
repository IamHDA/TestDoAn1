package com.vn.backend.utils;

import org.slf4j.MDC;

import java.util.UUID;

public class TraceIdUtils {
    
    private static final String TRACE_ID_KEY = "traceId";
    
    public static String generateTraceId() {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(TRACE_ID_KEY, traceId);
        return traceId;
    }
    
    public static String getCurrentTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }
    
    public static void clearTraceId() {
        MDC.remove(TRACE_ID_KEY);
    }
    
    public static String getFormattedTraceId() {
        String traceId = getCurrentTraceId();
        return traceId != null ? "[" + traceId + "]" : "";
    }
}
