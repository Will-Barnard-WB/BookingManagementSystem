package com.example.booking.web;

import com.example.booking.domain.entity.IdempotencyKey;
import com.example.booking.repository.IdempotencyKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Servlet filter that provides idempotency for POST /bookings.
 *
 * If an Idempotency-Key header is present the filter:
 *   1. Checks the DB for a stored result — returns it immediately if found.
 *   2. Otherwise wraps the response in a ContentCachingResponseWrapper,
 *      lets the request proceed, and stores the result before returning.
 *
 * Requests without the header pass through untouched.
 */
@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    public IdempotencyFilter(IdempotencyKeyRepository idempotencyKeyRepository) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !"/bookings".equals(request.getRequestURI())
                || request.getHeader("Idempotency-Key") == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String key = request.getHeader("Idempotency-Key");

        // Return cached response if key already exists.
        Optional<IdempotencyKey> existing = idempotencyKeyRepository.findById(key);
        if (existing.isPresent()) {
            IdempotencyKey cached = existing.get();
            response.setStatus(cached.getStatusCode());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(cached.getResponseBody());
            return;
        }

        // Wrap response to capture the body written by the handler.
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        chain.doFilter(request, wrappedResponse);

        // Persist the result so future duplicate requests get the same response.
        String body = new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8);
        try {
            idempotencyKeyRepository.save(
                    new IdempotencyKey(key, body, wrappedResponse.getStatus(), LocalDateTime.now()));
        } catch (DataIntegrityViolationException ignored) {
            // Race condition: a concurrent request saved the same key first — harmless.
        }

        // Relay the buffered body to the actual client.
        wrappedResponse.copyBodyToResponse();
    }
}
