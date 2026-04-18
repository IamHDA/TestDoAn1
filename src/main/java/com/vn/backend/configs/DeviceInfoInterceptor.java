package com.vn.backend.configs;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;



@Component
public class DeviceInfoInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(DeviceInfoInterceptor.class);


    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response, Object handler)  {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        String userAgent = request.getHeader("User-Agent");
        String deviceInfo = "IP: " + ip + " | User-Agent: " + userAgent;

        DeviceInfoContext.set(deviceInfo);
        logger.info(deviceInfo);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler, Exception ex){
        DeviceInfoContext.clear();
    }
}
