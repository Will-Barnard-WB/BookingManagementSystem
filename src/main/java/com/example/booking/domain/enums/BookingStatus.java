package com.example.booking.domain.enums;

/**
 * Lifecycle states for a Booking.
 *
 * PENDING   – created but not yet confirmed (e.g. awaiting payment or admin approval)
 * CONFIRMED – active, resource slot is reserved
 * CANCELLED – released; slot is available again for new bookings
 */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
