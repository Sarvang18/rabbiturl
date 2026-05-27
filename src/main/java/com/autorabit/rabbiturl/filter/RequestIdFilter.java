package com.autorabit.rabbiturl.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Injects a unique request ID into every log line via MDC.
 * This allows tracing a single user request through all service layers.
 */
@Component
@Order(1)
@Slf4j
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String REQUEST_ID_MDC_KEY = "requestId";
    public static final String USER_EMAIL_MDC_KEY = "userEmail";
    public static final String SHORT_CODE_MDC_KEY = "shortCode";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        try {
            // Extract or generate request ID
            String requestId = request.getHeader(REQUEST_ID_HEADER);
            if (requestId == null || requestId.isBlank()) {
                requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            }

            // Put request ID into MDC for structured logging
            MDC.put(REQUEST_ID_MDC_KEY, requestId);

            // If authenticated, add user email to MDC
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                MDC.put(USER_EMAIL_MDC_KEY, authentication.getName());
            }

            // Add request ID to response header for client correlation
            response.setHeader(REQUEST_ID_HEADER, requestId);

            // Proceed with the filter chain
            filterChain.doFilter(request, response);

        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("method={} uri={} status={} duration={}ms",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration);

            // CRITICAL: always clear MDC to prevent leaking into next request on same thread
            MDC.clear();
        }
    }
}
