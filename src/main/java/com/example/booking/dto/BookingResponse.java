package com.example.booking.dto;

import com.example.booking.domain.enums.BookingStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Outbound representation of a Booking.
 *
 * Deliberately flat — avoids nesting full User/Resource objects to keep
 * the API payload small. Use dedicated endpoints for User/Resource details.
 */
@Value
@Builder
public class BookingResponse {

    UUID id;
    UUID userId;
    String userName;
    UUID resourceId;
    String resourceName;
    LocalDateTime startTime;
    LocalDateTime endTime;
    BookingStatus status;
    LocalDateTime createdAt;
}
