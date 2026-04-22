package com.example.booking.domain.entity;

import com.example.booking.domain.enums.BookingStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable record of a single booking state transition.
 *
 * Rows are written by {@link com.example.booking.event.BookingEventListener}
 * inside the same transaction as the booking change (BEFORE_COMMIT phase), so
 * a failed transaction leaves no orphaned audit rows.
 */
@Entity
@Table(name = "booking_audit_log",
        indexes = @Index(name = "idx_audit_log_booking_id", columnList = "booking_id"))
public class BookingAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status")
    private BookingStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private BookingStatus newStatus;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    protected BookingAuditLog() {}

    public BookingAuditLog(UUID bookingId, UUID userId, UUID resourceId,
                           BookingStatus previousStatus, BookingStatus newStatus,
                           LocalDateTime changedAt) {
        this.bookingId      = bookingId;
        this.userId         = userId;
        this.resourceId     = resourceId;
        this.previousStatus = previousStatus;
        this.newStatus      = newStatus;
        this.changedAt      = changedAt;
    }

    public UUID getId()                    { return id; }
    public UUID getBookingId()             { return bookingId; }
    public UUID getUserId()                { return userId; }
    public UUID getResourceId()            { return resourceId; }
    public BookingStatus getPreviousStatus() { return previousStatus; }
    public BookingStatus getNewStatus()    { return newStatus; }
    public LocalDateTime getChangedAt()    { return changedAt; }
}
