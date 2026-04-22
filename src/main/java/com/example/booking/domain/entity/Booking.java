package com.example.booking.domain.entity;

import com.example.booking.domain.enums.BookingStatus;
import com.example.booking.exception.IllegalStateTransitionException;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Core aggregate root of the domain.
 *
 * A Booking ties a User to a Resource for a specific time window.
 * Status lifecycle:  PENDING -> CONFIRMED -> CANCELLED
 *                                          ^
 *                              PENDING can also be cancelled
 *
 * Concurrency note: the (resource, startTime, endTime, status) tuple must be
 * protected against double-booking at the service layer using either
 * SERIALIZABLE transaction isolation or a database-level unique/partial index.
 */
@Entity
@Table(name = "bookings",
        indexes = {
                // Speeds up overlap detection queries scoped to a resource
                @Index(name = "idx_bookings_resource_time",
                        columnList = "resource_id, start_time, end_time")
        })
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = BookingStatus.PENDING;
        }
    }

    // --- Constructors ---

    protected Booking() {}

    public Booking(User user, Resource resource, LocalDateTime startTime, LocalDateTime endTime) {
        this.user = user;
        this.resource = resource;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = BookingStatus.PENDING;
    }

    // --- Domain behaviour ---

    /**
     * Confirms a PENDING booking.
     * Only PENDING → CONFIRMED is allowed; any other transition throws.
     */
    public void confirm() {
        if (this.status != BookingStatus.PENDING) {
            throw new IllegalStateTransitionException(
                    "Cannot confirm a booking with status: " + this.status
                            + ". Only PENDING bookings may be confirmed.");
        }
        this.status = BookingStatus.CONFIRMED;
    }

    /**
     * Cancels the booking. PENDING or CONFIRMED → CANCELLED is allowed.
     * Re-cancelling an already-CANCELLED booking throws.
     */
    public void cancel() {
        if (this.status == BookingStatus.CANCELLED) {
            throw new IllegalStateTransitionException("Booking is already cancelled.");
        }
        this.status = BookingStatus.CANCELLED;
    }

    // --- Getters ---

    public UUID getId()     { return id; }

    public Long getVersion() { return version; }

    public User getUser() { return user; }

    public Resource getResource() { return resource; }

    public LocalDateTime getStartTime() { return startTime; }

    public LocalDateTime getEndTime() { return endTime; }

    public BookingStatus getStatus() { return status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
