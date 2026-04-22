package com.example.booking.service;

import com.example.booking.dto.CreateUserRequest;
import com.example.booking.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service contract for user management.
 */
public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    UserResponse getUser(UUID userId);

    Page<UserResponse> getAllUsers(Pageable pageable);

    UserResponse updateUser(UUID userId, CreateUserRequest request);

    void deleteUser(UUID userId);
}
