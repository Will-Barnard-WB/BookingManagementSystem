package com.example.booking.service.impl;

import com.example.booking.domain.entity.User;
import com.example.booking.domain.enums.BookingStatus;
import com.example.booking.dto.CreateUserRequest;
import com.example.booking.dto.UserResponse;
import com.example.booking.exception.BookingNotFoundException;
import com.example.booking.exception.InvalidBookingException;
import com.example.booking.mapper.UserMapper;
import com.example.booking.repository.BookingRepository;
import com.example.booking.repository.UserRepository;
import com.example.booking.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository,
                           BookingRepository bookingRepository,
                           UserMapper userMapper) {
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new InvalidBookingException(
                    "Email already registered: " + request.getEmail());
        }
        User user = new User(request.getName(), request.getEmail());
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    public UserResponse getUser(UUID userId) {
        return userRepository.findById(userId)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new BookingNotFoundException("User not found: " + userId));
    }

    @Override
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID userId, CreateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BookingNotFoundException("User not found: " + userId));
        // Allow keeping the same email, but reject one already registered to someone else.
        userRepository.findByEmail(request.getEmail())
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new InvalidBookingException(
                            "Email already registered: " + request.getEmail());
                });
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new BookingNotFoundException("User not found: " + userId);
        }
        // Block deletion while the user still has bookings that aren't cancelled.
        // Callers must cancel those bookings first; we never silently drop them.
        boolean hasActiveBookings = bookingRepository
                .findByUserIdOrderByCreatedAtDesc(userId).stream()
                .anyMatch(booking -> booking.getStatus() != BookingStatus.CANCELLED);
        if (hasActiveBookings) {
            throw new InvalidBookingException(
                    "Cannot delete user with active bookings: " + userId);
        }
        userRepository.deleteById(userId);
    }
}
