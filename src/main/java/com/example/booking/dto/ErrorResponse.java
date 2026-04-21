package com.example.booking.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * Standard error envelope returned by GlobalExceptionHandler for all 4xx/5xx responses.
 *
 * Example JSON:
 * {
 *   "error":     "RESOURCE_UNAVAILABLE",
 *   "message":   "Resource 'Conference Room A' is already booked for the requested slot.",
 *   "timestamp": "2026-04-15T10:30:00"
 * }
 */
@Value
@Builder
public class ErrorResponse {

    String error;
    String message;
    LocalDateTime timestamp;
}
