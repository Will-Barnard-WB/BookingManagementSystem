package com.example.booking.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Outbound representation of a Resource.
 */
@Value
@Builder
public class ResourceResponse {

    UUID id;
    String name;
    String description;
    int capacity;
    LocalDateTime createdAt;
}
