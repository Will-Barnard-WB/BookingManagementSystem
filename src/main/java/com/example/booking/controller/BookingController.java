package com.example.booking.controller;

import com.example.booking.dto.BookingResponse;
import com.example.booking.dto.CreateBookingRequest;
import com.example.booking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for booking lifecycle operations.
 *
 * All endpoints are under /bookings or /users/{userId}/bookings.
 * Validation errors are handled centrally by GlobalExceptionHandler.
 */
@RestController
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * POST /bookings
     *
     * Create a new booking. Returns 201 Created with the booking detail.
     * Returns 409 Conflict if the requested slot is already taken.
     * Returns 400 Bad Request if validation fails.
     */
    @PostMapping("/bookings")
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody CreateBookingRequest request) {
        BookingResponse response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /bookings/{id}
     *
     * Retrieve a single booking by ID.
     * Returns 404 if the booking does not exist.
     */
    @GetMapping("/bookings/{id}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable UUID id) {
        return ResponseEntity.ok(bookingService.getBooking(id));
    }

    /**
     * GET /users/{userId}/bookings
     *
     * List all bookings belonging to a specific user, newest first.
     */
    @GetMapping("/users/{userId}/bookings")
    public ResponseEntity<List<BookingResponse>> getUserBookings(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(bookingService.getUserBookings(userId));
    }

    /**
     * POST /bookings/{id}/cancel
     *
     * Cancel a booking. Idempotent in that cancelling an already-cancelled
     * booking returns 400 rather than succeeding silently.
     */
    @PostMapping("/bookings/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable UUID id) {
        return ResponseEntity.ok(bookingService.cancelBooking(id));
    }
}
