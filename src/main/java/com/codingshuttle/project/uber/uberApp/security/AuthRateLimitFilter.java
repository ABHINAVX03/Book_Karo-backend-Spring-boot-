package com.codingshuttle.project.uber.uberApp.security;

import com.codingshuttle.project.uber.uberApp.configs.AppSecurityProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(1)
@RequiredArgsConstructor
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final AppSecurityProperties appSecurityProperties;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!request.getServletPath().startsWith("/auth/")) {
            return true;
        }
        return !"POST".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getServletPath();
        String key = request.getRemoteAddr() + ":" + path;
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> newBucket(path));
        if (!bucket.tryConsume(1)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":{\"message\":\"Too many authentication requests. Please slow down.\"}}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private Bucket newBucket(String path) {
        if (path.startsWith("/auth/send-otp")) {
            return Bucket.builder()
                    .addLimit(Bandwidth.classic(appSecurityProperties.getOtpMaxRequestsPerMinute(), Refill.intervally(
                            appSecurityProperties.getOtpMaxRequestsPerMinute(), Duration.ofMinutes(1))))
                    .build();
        }
        if (path.startsWith("/auth/verify-otp")) {
            return Bucket.builder()
                    .addLimit(Bandwidth.classic(appSecurityProperties.getOtpVerifyMaxRequestsPerMinute(), Refill.intervally(
                            appSecurityProperties.getOtpVerifyMaxRequestsPerMinute(), Duration.ofMinutes(1))))
                    .build();
        }
        if (path.startsWith("/auth/refresh")) {
            return Bucket.builder()
                    .addLimit(Bandwidth.classic(appSecurityProperties.getRefreshMaxAttemptsPerMinute(), Refill.intervally(
                            appSecurityProperties.getRefreshMaxAttemptsPerMinute(), Duration.ofMinutes(1))))
                    .build();
        }
        return Bucket.builder()
                .addLimit(Bandwidth.classic(appSecurityProperties.getLoginMaxAttemptsPerMinute(), Refill.intervally(
                        appSecurityProperties.getLoginMaxAttemptsPerMinute(), Duration.ofMinutes(1))))
                .build();
    }
}
