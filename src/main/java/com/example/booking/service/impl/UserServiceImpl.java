package com.example.booking.service.impl;

import com.example.booking.domain.entity.User;
import com.example.booking.dto.CreateUserRequest;
import com.example.booking.dto.UserResponse;
import com.example.booking.exception.BookingNotFoundException;
import com.example.booking.exception.InvalidBookingException;
import com.example.booking.mapper.UserMapper;
import com.example.booking.repository.UserRepository;
import com.example.booking.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
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
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID userId, CreateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BookingNotFoundException("User not found: " + userId));
        // TODO: check that the new email is not taken by a different user
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
        // TODO: decide whether to cascade-cancel active bookings or block deletion
        userRepository.deleteById(userId);
    }
}
