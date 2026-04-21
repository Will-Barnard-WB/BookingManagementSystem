package com.example.booking.service;

import com.example.booking.dto.CreateUserRequest;
import com.example.booking.dto.UserResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service contract for user management.
 */
public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    UserResponse getUser(UUID userId);

    List<UserResponse> getAllUsers();

    UserResponse updateUser(UUID userId, CreateUserRequest request);

    void deleteUser(UUID userId);
}
