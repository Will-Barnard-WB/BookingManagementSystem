package com.example.booking.service;

import com.example.booking.dto.BookingResponse;
import com.example.booking.dto.CreateBookingRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Primary service contract for booking operations.
 *
 * Implementations are responsible for:
 *  - enforcing business rules (overlap detection, status transitions)
 *  - maintaining transactional boundaries
 *  - converting domain entities to DTOs
 */
public interface BookingService {

    /**
     * Create a new booking for a user on a resource.
     *
     * Must validate:
     *  - user exists
     *  - resource exists
     *  - requested time slot does not overlap with existing active bookings
     *
     * @throws com.example.booking.exception.ResourceUnavailableException if overlap detected
     * @throws com.example.booking.exception.InvalidBookingException       if time range is invalid
     */
    BookingResponse createBooking(CreateBookingRequest request);

    /**
     * Retrieve a single booking by its ID.
     *
     * @throws com.example.booking.exception.BookingNotFoundException if not found
     */
    BookingResponse getBooking(UUID bookingId);

    /**
     * Retrieve bookings for a specific user, newest first, with pagination.
     */
    Page<BookingResponse> getUserBookings(UUID userId, Pageable pageable);

    /**
     * Confirm a PENDING booking (PENDING → CONFIRMED).
     *
     * @throws com.example.booking.exception.BookingNotFoundException        if not found
     * @throws com.example.booking.exception.IllegalStateTransitionException if not PENDING
     */
    BookingResponse confirmBooking(UUID bookingId);

    /**
     * Cancel an existing booking (PENDING or CONFIRMED → CANCELLED).
     *
     * @throws com.example.booking.exception.BookingNotFoundException        if not found
     * @throws com.example.booking.exception.IllegalStateTransitionException if already cancelled
     */
    BookingResponse cancelBooking(UUID bookingId);
}
