package com.geekup.ticketbooking.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RedissonClient redissonClient;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = request.getRemoteAddr();
        
        // We only rate limit POST /api/v1/bookings for Flash Sale protection
        if ("POST".equalsIgnoreCase(request.getMethod()) && request.getRequestURI().contains("/api/v1/bookings")) {
            RRateLimiter rateLimiter = redissonClient.getRateLimiter("rate_limit:ip:" + clientIp);
            
            // Allow 5 requests per 1 second per IP
            rateLimiter.trySetRate(RateType.OVERALL, 5, 1, RateIntervalUnit.SECONDS);
            
            if (!rateLimiter.tryAcquire()) {
                log.warn("Rate limit exceeded for IP: {}", clientIp);
                response.setStatus(429); // Too Many Requests
                response.getWriter().write("{\"error\": \"Too Many Requests\", \"message\": \"You are submitting requests too fast. Please slow down.\"}");
                response.setContentType("application/json");
                return false;
            }
        }
        
        return true;
    }
}
