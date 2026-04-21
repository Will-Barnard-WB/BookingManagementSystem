package com.example.booking.exception;

/**
 * Thrown when a requested Booking (or related entity) cannot be found.
 * Maps to HTTP 404 Not Found via GlobalExceptionHandler.
 */
public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(String message) {
        super(message);
    }
}
