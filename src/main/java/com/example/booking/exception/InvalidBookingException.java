package com.example.booking.exception;

/**
 * Thrown when a booking operation violates a business rule:
 *   - startTime is not before endTime
 *   - cancelling an already-cancelled booking
 *   - duplicate email on user creation
 *
 * Maps to HTTP 400 Bad Request via GlobalExceptionHandler.
 */
public class InvalidBookingException extends RuntimeException {

    public InvalidBookingException(String message) {
        super(message);
    }
}
