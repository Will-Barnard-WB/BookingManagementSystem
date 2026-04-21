package com.example.booking.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Outbound representation of a User.
 */
@Value
@Builder
public class UserResponse {

    UUID id;
    String name;
    String email;
    LocalDateTime createdAt;
}
