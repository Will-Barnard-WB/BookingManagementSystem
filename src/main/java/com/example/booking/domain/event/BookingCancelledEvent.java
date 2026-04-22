package com.example.booking.domain.event;

import com.example.booking.domain.enums.BookingStatus;
import java.time.LocalDateTime;
import java.util.UUID;

/** Published when a booking transitions (PENDING|CONFIRMED) → CANCELLED. */
public class BookingCancelledEvent {

    private final UUID bookingId;
    private final UUID userId;
    private final UUID resourceId;
    private final LocalDateTime timestamp;
    private final BookingStatus previousStatus;
    private final BookingStatus status;

    public BookingCancelledEvent(UUID bookingId, UUID userId, UUID resourceId,
                                 LocalDateTime timestamp,
                                 BookingStatus previousStatus, BookingStatus status) {
        this.bookingId      = bookingId;
        this.userId         = userId;
        this.resourceId     = resourceId;
        this.timestamp      = timestamp;
        this.previousStatus = previousStatus;
        this.status         = status;
    }

    public UUID getBookingId()             { return bookingId; }
    public UUID getUserId()                { return userId; }
    public UUID getResourceId()            { return resourceId; }
    public LocalDateTime getTimestamp()    { return timestamp; }
    public BookingStatus getPreviousStatus() { return previousStatus; }
    public BookingStatus getStatus()       { return status; }
}
