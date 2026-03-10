package com.fierceadventurer.smartportfoliobackend.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    // in memory cache to store buckets mapped to ip address
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    //fetches existing bucket ip or create a new one on first visit
    public Bucket resolveBucket(String ip){
        return cache.computeIfAbsent(ip , this::newBucket);
    }

    private Bucket newBucket(String ip){
        // allows max 5 req every min and refill 5 tokens every 1 min
        Refill refill = Refill.intervally(5 , Duration.ofMinutes(1));
        Bandwidth limit = Bandwidth.classic(5 , refill);

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response , Object handler) throws Exception{
       // extract client ip
       // "X-forwarded-for" is curial for frontend hosted on cloud
       String ip = request.getHeader("X-Forwarded-For");
       if(ip == null || ip.isEmpty()){
           ip = request.getRemoteAddr();
       }

       Bucket bucket = resolveBucket(ip);

       //try to consume 1 token
        if(bucket.tryConsume(1)){
            return true;
        }
        else{
            log.warn("Rate limit exceeded for IP: {}", ip);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Too many requests. Please wait a minute before asking another question.");
            return false;

        }
    }
}
