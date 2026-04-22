package com.example.booking.service.impl;

import com.example.booking.domain.entity.Booking;
import com.example.booking.domain.entity.Resource;
import com.example.booking.domain.entity.User;
import com.example.booking.domain.enums.BookingStatus;
import com.example.booking.domain.event.BookingCancelledEvent;
import com.example.booking.domain.event.BookingConfirmedEvent;
import com.example.booking.domain.event.BookingCreatedEvent;
import com.example.booking.dto.BookingResponse;
import com.example.booking.dto.CreateBookingRequest;
import com.example.booking.exception.BookingNotFoundException;
import com.example.booking.exception.IllegalStateTransitionException;
import com.example.booking.exception.ResourceUnavailableException;
import com.example.booking.mapper.BookingMapper;
import com.example.booking.repository.BookingRepository;
import com.example.booking.repository.ResourceRepository;
import com.example.booking.repository.UserRepository;
import com.example.booking.service.BookingService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
    private final ApplicationEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;

    private final Counter bookingsCreatedCounter;
    private final Counter bookingsConflictsCounter;
    private final Timer   bookingsCreateLatencyTimer;

    public BookingServiceImpl(BookingRepository bookingRepository,
                               UserRepository userRepository,
                               ResourceRepository resourceRepository,
                               BookingMapper bookingMapper,
                               ApplicationEventPublisher eventPublisher,
                               MeterRegistry meterRegistry) {
        this.bookingRepository = bookingRepository;
        this.userRepository    = userRepository;
        this.resourceRepository = resourceRepository;
        this.bookingMapper     = bookingMapper;
        this.eventPublisher    = eventPublisher;
        this.meterRegistry     = meterRegistry;

        this.bookingsCreatedCounter = Counter.builder("bookings.created")
                .description("Bookings successfully created")
                .register(meterRegistry);
        this.bookingsConflictsCounter = Counter.builder("bookings.conflicts")
                .description("Booking conflict responses (409)")
                .register(meterRegistry);
        this.bookingsCreateLatencyTimer = Timer.builder("bookings.create.latency")
                .description("Latency of the createBooking operation")
                .register(meterRegistry);
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
        MDC.put("userId",     request.getUserId().toString());
        MDC.put("resourceId", request.getResourceId().toString());
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new BookingNotFoundException(
                            "User not found: " + request.getUserId()));

            Resource resource = resourceRepository.findById(request.getResourceId())
                    .orElseThrow(() -> new BookingNotFoundException(
                            "Resource not found: " + request.getResourceId()));

            List<Booking> conflicts = bookingRepository.findOverlappingBookings(
                    resource.getId(), request.getStartTime(), request.getEndTime());
            if (!conflicts.isEmpty()) {
                bookingsConflictsCounter.increment();
                throw new ResourceUnavailableException(
                        "Resource '" + resource.getName() + "' is already booked for the requested slot.");
            }

            Booking booking = new Booking(user, resource, request.getStartTime(), request.getEndTime());
            // saveAndFlush forces an immediate DB flush so that optimistic-lock
            // failures surface here rather than at the proxy commit boundary.
            Booking saved = bookingRepository.saveAndFlush(booking);

            eventPublisher.publishEvent(new BookingCreatedEvent(
                    saved.getId(), saved.getUser().getId(), saved.getResource().getId(),
                    LocalDateTime.now(), saved.getStatus()));

            bookingsCreatedCounter.increment();
            return bookingMapper.toResponse(saved);

        } catch (ObjectOptimisticLockingFailureException | CannotAcquireLockException e) {
            bookingsConflictsCounter.increment();
            throw new ResourceUnavailableException(
                    "Resource booking conflict due to concurrent access. Please retry.");
        } finally {
            sample.stop(bookingsCreateLatencyTimer);
            MDC.remove("userId");
            MDC.remove("resourceId");
        }
    }

    @Override
    public BookingResponse getBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Booking not found: " + bookingId));
        return bookingMapper.toResponse(booking);
    }

    @Override
    public Page<BookingResponse> getUserBookings(UUID userId, Pageable pageable) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(bookingMapper::toResponse);
    }

    @Override
    @Transactional
    public BookingResponse confirmBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Booking not found: " + bookingId));

        BookingStatus previous = booking.getStatus();
        booking.confirm(); // throws IllegalStateTransitionException if not PENDING
        Booking saved = bookingRepository.save(booking);

        eventPublisher.publishEvent(new BookingConfirmedEvent(
                saved.getId(), saved.getUser().getId(), saved.getResource().getId(),
                LocalDateTime.now(), previous, saved.getStatus()));

        return bookingMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Booking not found: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateTransitionException("Booking is already cancelled.");
        }

        BookingStatus previous = booking.getStatus();
        booking.cancel();
        Booking saved = bookingRepository.save(booking);

        eventPublisher.publishEvent(new BookingCancelledEvent(
                saved.getId(), saved.getUser().getId(), saved.getResource().getId(),
                LocalDateTime.now(), previous, saved.getStatus()));

        return bookingMapper.toResponse(saved);
    }
}
