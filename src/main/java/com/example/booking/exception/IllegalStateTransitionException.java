package com.example.booking.exception;

/**
 * Thrown when a booking state transition is not permitted.
 * Maps to HTTP 400 Bad Request via GlobalExceptionHandler.
 *
 * Valid transitions:
 *   PENDING  → CONFIRMED
 *   PENDING  → CANCELLED
 *   CONFIRMED → CANCELLED
 *
 * All other transitions are rejected.
 */
public class IllegalStateTransitionException extends RuntimeException {

    public IllegalStateTransitionException(String message) {
        super(message);
    }
}
