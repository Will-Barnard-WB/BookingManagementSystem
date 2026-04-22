package com.example.booking.domain.event;

import com.example.booking.domain.enums.BookingStatus;
import java.time.LocalDateTime;
import java.util.UUID;

/** Published when a new booking is successfully persisted (status = PENDING). */
public class BookingCreatedEvent {

    private final UUID bookingId;
    private final UUID userId;
    private final UUID resourceId;
    private final LocalDateTime timestamp;
    private final BookingStatus status;

    public BookingCreatedEvent(UUID bookingId, UUID userId, UUID resourceId,
                               LocalDateTime timestamp, BookingStatus status) {
        this.bookingId  = bookingId;
        this.userId     = userId;
        this.resourceId = resourceId;
        this.timestamp  = timestamp;
        this.status     = status;
    }

    public UUID getBookingId()       { return bookingId; }
    public UUID getUserId()          { return userId; }
    public UUID getResourceId()      { return resourceId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public BookingStatus getStatus() { return status; }
}
