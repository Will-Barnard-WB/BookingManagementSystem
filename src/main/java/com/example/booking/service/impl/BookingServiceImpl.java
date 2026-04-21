package com.example.booking.service.impl;

import com.example.booking.domain.entity.Booking;
import com.example.booking.domain.entity.Resource;
import com.example.booking.domain.entity.User;
import com.example.booking.domain.enums.BookingStatus;
import com.example.booking.dto.BookingResponse;
import com.example.booking.dto.CreateBookingRequest;
import com.example.booking.exception.BookingNotFoundException;
import com.example.booking.exception.InvalidBookingException;
import com.example.booking.exception.ResourceUnavailableException;
import com.example.booking.mapper.BookingMapper;
import com.example.booking.repository.BookingRepository;
import com.example.booking.repository.ResourceRepository;
import com.example.booking.repository.UserRepository;
import com.example.booking.service.BookingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core booking service implementation.
 *
 * TRANSACTIONAL STRATEGY
 * ──────────────────────
 * The class is annotated @Transactional at the type level so all public methods
 * participate in a transaction by default. createBooking() uses SERIALIZABLE
 * isolation to prevent phantom reads during the overlap check — see below.
 *
 * CONCURRENCY CONTROL
 * ───────────────────
 * Two concurrent requests for the same resource/slot both pass the overlap check
 * if they both read before either writes (a classic TOCTOU race). Using
 * SERIALIZABLE isolation causes the database to detect the conflict and roll back
 * one of the transactions, which the service layer retries or surfaces as an error.
 *
 * Alternative approaches (choose one):
 *   1. SERIALIZABLE isolation (implemented here as a scaffold)
 *   2. SELECT ... FOR UPDATE to pessimistically lock matching rows
 *   3. A partial unique index on (resource_id, status) where status != 'CANCELLED'
 */
@Service
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final BookingMapper bookingMapper;

    public BookingServiceImpl(BookingRepository bookingRepository,
                               UserRepository userRepository,
                               ResourceRepository resourceRepository,
                               BookingMapper bookingMapper) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.resourceRepository = resourceRepository;
        this.bookingMapper = bookingMapper;
    }

    /**
     * Creates a booking after validating user, resource, and slot availability.
     *
     * ISOLATION: SERIALIZABLE ensures that the overlap check + insert is
     * treated as an atomic unit from the database's perspective.
     *
     * OVERLAP RULE: Two intervals [startA, endA) and [startB, endB) overlap
     * when startA < endB AND startB < endA.
     */
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingResponse createBooking(CreateBookingRequest request) {

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BookingNotFoundException(
                        "User not found: " + request.getUserId()));

        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new BookingNotFoundException(
                        "Resource not found: " + request.getResourceId()));

        // TODO: run overlap detection query and throw if slot is taken
        List<Booking> conflicts = bookingRepository.findOverlappingBookings(
                resource.getId(), request.getStartTime(), request.getEndTime());
        if (!conflicts.isEmpty()) {
            throw new ResourceUnavailableException(
                    "Resource '" + resource.getName() + "' is already booked for the requested slot.");
        }

        Booking booking = new Booking(user, resource, request.getStartTime(), request.getEndTime());
        Booking saved = bookingRepository.save(booking);
        return bookingMapper.toResponse(saved);
    }

    @Override
    public BookingResponse getBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Booking not found: " + bookingId));
        return bookingMapper.toResponse(booking);
    }

    @Override
    public List<BookingResponse> getUserBookings(UUID userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(bookingMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Booking not found: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidBookingException("Booking is already cancelled");
        }

        booking.cancel();
        return bookingMapper.toResponse(bookingRepository.save(booking));
    }
}
