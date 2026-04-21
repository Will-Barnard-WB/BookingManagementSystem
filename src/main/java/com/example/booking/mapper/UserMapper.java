package com.example.booking.mapper;

import com.example.booking.domain.entity.User;
import com.example.booking.dto.UserResponse;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for User entities.
 *
 * A library like MapStruct would be appropriate at scale, but a simple
 * hand-written mapper keeps the dependency footprint small and makes the
 * transformation logic immediately visible to the reader.
 */
@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
