package com.example.booking.dto;

import com.example.booking.validation.ValidBookingTime;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Inbound payload for POST /bookings.
 *
 * The @ValidBookingTime class-level constraint delegates to BookingTimeValidator
 * which checks that startTime < endTime and that neither is in the past.
 */
@ValidBookingTime
public class CreateBookingRequest {

    @NotNull(message = "userId is required")
    private UUID userId;

    @NotNull(message = "resourceId is required")
    private UUID resourceId;

    @NotNull(message = "startTime is required")
    @Future(message = "startTime must be in the future")
    private LocalDateTime startTime;

    @NotNull(message = "endTime is required")
    @Future(message = "endTime must be in the future")
    private LocalDateTime endTime;

    // --- Constructors ---

    public CreateBookingRequest() {}

    public CreateBookingRequest(UUID userId, UUID resourceId,
                                 LocalDateTime startTime, LocalDateTime endTime) {
        this.userId = userId;
        this.resourceId = resourceId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // --- Getters & Setters ---

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}
