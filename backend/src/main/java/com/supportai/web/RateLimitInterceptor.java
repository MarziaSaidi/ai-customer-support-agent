package com.supportai.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory, per-client fixed-window rate limiter. Applied to the public (unauthenticated)
 * chat widget endpoints so an anonymous caller cannot flood the AI backend and run up API costs.
 *
 * <p>Deliberately dependency-free (no Redis). For a single instance this is enough; a multi-instance
 * deployment would move the counters into a shared store.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    /** Guards against unbounded growth from many distinct client keys. */
    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final int maxRequests;
    private final long windowMs;
    private final Map<String, Window> counters = new ConcurrentHashMap<>();

    public RateLimitInterceptor(
            @Value("${app.rate-limit.widget.max-requests:30}") int maxRequests,
            @Value("${app.rate-limit.widget.window-ms:60000}") long windowMs
    ) {
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        long now = System.currentTimeMillis();
        Window window = counters.compute(clientKey(request), (key, existing) -> {
            if (existing == null || now - existing.start >= windowMs) {
                return new Window(now);
            }
            existing.count++;
            return existing;
        });

        if (counters.size() > MAX_TRACKED_CLIENTS) {
            counters.entrySet().removeIf(entry -> now - entry.getValue().start >= windowMs);
        }

        if (window.count > maxRequests) {
            long retryAfterSeconds = Math.max(1, (windowMs - (now - window.start)) / 1000);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"status\":429,\"message\":\"Too many requests. Please slow down and try again shortly.\"}");
            return false;
        }
        return true;
    }

    /**
     * Prefer the forwarded client IP (set by the platform's proxy/load balancer) and fall back to
     * the socket address for local/direct requests.
     */
    private String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static final class Window {
        private final long start;
        private int count;

        private Window(long start) {
            this.start = start;
            this.count = 1;
        }
    }
}
