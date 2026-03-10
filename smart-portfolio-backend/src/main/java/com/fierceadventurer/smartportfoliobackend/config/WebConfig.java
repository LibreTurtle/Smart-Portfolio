package com.fierceadventurer.smartportfoliobackend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer{
    @Value("${portfolio.frontend-url}")
    private String frontendUrl;

    private final RateLimitInterceptor rateLimitInterceptor;
    private final AdminAuthInterceptor adminAuthInterceptor;

    public WebConfig(RateLimitInterceptor rateLimitInterceptor, AdminAuthInterceptor adminAuthInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.adminAuthInterceptor = adminAuthInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(frontendUrl)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*", "X-Admin-Secret")
                .allowCredentials(true);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry){
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/chat");

        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**");
    }
}
