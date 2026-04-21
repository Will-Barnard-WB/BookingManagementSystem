package com.example.booking.exception;

/**
 * Thrown when the requested resource slot is already occupied by another booking.
 * Maps to HTTP 409 Conflict via GlobalExceptionHandler.
 */
public class ResourceUnavailableException extends RuntimeException {

    public ResourceUnavailableException(String message) {
        super(message);
    }
}
