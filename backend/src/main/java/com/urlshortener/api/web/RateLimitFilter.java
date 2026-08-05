package com.urlshortener.api.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.urlshortener.api.config.AppProperties;
import com.urlshortener.api.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Per-IP fixed-window limiter on link creation, the abuse control called out in HLD §9 since
 * there's no auth to gate on for the MVP. Only guards {@code POST /api/urls} — the read/redirect
 * path stays unthrottled since it's meant to absorb public traffic.
 *
 * <p>Uses {@code request.getRemoteAddr()}, which is the direct TCP peer; behind a real load
 * balancer this would need to read {@code X-Forwarded-For} instead (noted as a limitation).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final AppProperties properties;
    private final ObjectMapper objectMapper;
    private final Cache<String, AtomicInteger> requestCounts;

    public RateLimitFilter(AppProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.requestCounts =
                Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).maximumSize(100_000).build();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (isCreateRequest(request) && isOverLimit(request.getRemoteAddr())) {
            respondRateLimited(response);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isCreateRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod()) && "/api/urls".equals(request.getRequestURI());
    }

    private boolean isOverLimit(String clientIp) {
        AtomicInteger count = requestCounts.get(clientIp, key -> new AtomicInteger());
        return count.incrementAndGet() > properties.rateLimit().requestsPerMinute();
    }

    private void respondRateLimited(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body =
                ErrorResponse.of("RATE_LIMITED", "Too many link-creation requests, try again shortly");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
