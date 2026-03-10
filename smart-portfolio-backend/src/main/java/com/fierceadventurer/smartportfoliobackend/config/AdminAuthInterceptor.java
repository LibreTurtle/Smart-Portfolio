package com.fierceadventurer.smartportfoliobackend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {
    @Value("${portfolio.admin-secret}")
    private String adminSecret;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception{
        // cors preflight request is allowed
        if(request.getMethod().equalsIgnoreCase("OPTIONS")){
            return true;
        }

        String providedSecret = request.getHeader("X-Admin-Secret");

        if(providedSecret != null && providedSecret.equals(adminSecret)){
            return true;
        }
        else {
            log.warn("Unauthorized access attempt to admin route from IP: {}", request.getRemoteAddr());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("401 Unauthorized: Invalid or missing Admin Secret");
            return false; // Block the request
        }
    }
}
