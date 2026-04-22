package com.example.booking.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Assigns a correlation ID to every inbound request and propagates it through
 * the SLF4J MDC so all log lines for the same request share the same ID.
 *
 * The ID is sourced from the X-Correlation-ID request header when present,
 * otherwise a fresh UUID is generated. The chosen ID is echoed back to the
 * client in the X-Correlation-ID response header.
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    static final String MDC_KEY  = "correlationId";
    static final String HEADER   = "X-Correlation-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {
        String correlationId = request.getHeader(HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();   // prevent MDC leaking to other requests on this thread
        }
    }
}
