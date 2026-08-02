package com.bsplay.shared.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final int LIMIT_PER_MINUTE = 30;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public RateLimitFilter(Clock clock) {
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) return true;
        String path = request.getRequestURI();
        return !("/api/v1/rooms".equals(path) || path.matches("/api/v1/rooms/[^/]+/join"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long minute = clock.millis() / 60_000;
        String key = request.getRemoteAddr() + ':' + request.getRequestURI();
        Window window = windows.compute(key, (ignored, current) -> current == null || current.minute != minute
                ? new Window(minute, 1) : new Window(minute, current.requests + 1));
        if (window.requests > LIMIT_PER_MINUTE) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"code\":\"RATE_LIMITED\",\"message\":\"Demasiados intentos. Espera un minuto.\"}");
            return;
        }
        if (windows.size() > 10_000) windows.entrySet().removeIf(entry -> entry.getValue().minute < minute - 2);
        filterChain.doFilter(request, response);
    }

    private record Window(long minute, int requests) {}
}
