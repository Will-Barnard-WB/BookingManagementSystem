package com.example.booking.concurrency;

import com.example.booking.domain.entity.BookingAuditLog;
import com.example.booking.domain.enums.BookingStatus;
import com.example.booking.dto.CreateBookingRequest;
import com.example.booking.dto.CreateResourceRequest;
import com.example.booking.dto.CreateUserRequest;
import com.example.booking.exception.ResourceUnavailableException;
import com.example.booking.repository.BookingAuditLogRepository;
import com.example.booking.repository.BookingRepository;
import com.example.booking.service.BookingService;
import com.example.booking.service.ResourceService;
import com.example.booking.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.TransactionSystemException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency test: 50 threads race to book the same resource/slot simultaneously.
 *
 * Verifies that SERIALIZABLE isolation + optimistic locking ensure exactly one
 * booking is created, with no double-booking possible regardless of timing.
 *
 * Also verifies that the transactional audit log (Phase 5) records exactly
 * one creation row — not one row per thread.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ActiveProfiles("test")
class ConcurrentBookingTest {

    private static final int THREAD_COUNT = 50;

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("booking_concurrency_test")
                    .withUsername("test_user")
                    .withPassword("test_pass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private BookingService    bookingService;
    @Autowired private UserService       userService;
    @Autowired private ResourceService   resourceService;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingAuditLogRepository auditLogRepository;

    private CreateBookingRequest sharedRequest;
    private UUID userId;
    private UUID resourceId;

    @BeforeEach
    void setUp() {
        // Fresh user and resource for each test
        userId = userService.createUser(
                new CreateUserRequest("Concurrent User",
                        UUID.randomUUID() + "@concurrent.com")).getId();

        resourceId = resourceService.createResource(
                new CreateResourceRequest("Concurrent Room " + UUID.randomUUID(),
                        "Race room", 1)).getId();

        LocalDateTime start = LocalDateTime.now().plusHours(1);
        sharedRequest = new CreateBookingRequest(userId, resourceId, start, start.plusHours(1));
    }

    @Test
    @DisplayName("Only one booking succeeds when 50 threads race for the same slot")
    void onlyOneBookingSucceeds_underConcurrentLoad() throws InterruptedException {
        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger conflicts = new AtomicInteger(0);
        AtomicInteger unexpected = new AtomicInteger(0);

        CountDownLatch startGun  = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        try {
            for (int i = 0; i < THREAD_COUNT; i++) {
                executor.submit(() -> {
                    try {
                        startGun.await();                          // all threads wait here
                        bookingService.createBooking(sharedRequest);
                        successes.incrementAndGet();
                    } catch (ResourceUnavailableException e) {
                        // Expected: overlap detected or concurrency conflict
                        conflicts.incrementAndGet();
                    } catch (CannotAcquireLockException | TransactionSystemException e) {
                        // Serialization failure escaping the service layer — also a conflict
                        conflicts.incrementAndGet();
                    } catch (Exception e) {
                        unexpected.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startGun.countDown();                                   // fire the starting gun
            boolean finished = doneLatch.await(30, TimeUnit.SECONDS);

            assertThat(finished).as("All threads should finish within 30 seconds").isTrue();
        } finally {
            executor.shutdownNow();
        }

        // Exactly one thread must have created a booking
        assertThat(successes.get())
                .as("Exactly 1 booking should succeed")
                .isEqualTo(1);

        assertThat(conflicts.get())
                .as("All other threads should receive a conflict")
                .isEqualTo(THREAD_COUNT - 1);

        assertThat(unexpected.get())
                .as("No unexpected exceptions")
                .isZero();

        // DB must contain exactly one non-cancelled booking for this slot
        long activeBookings = bookingRepository
                .findByResourceIdAndStatusNot(resourceId, BookingStatus.CANCELLED)
                .stream()
                .filter(b -> b.getResource().getId().equals(resourceId))
                .count();
        assertThat(activeBookings).as("Only 1 active booking in the database").isEqualTo(1);

        // Audit log must record exactly one creation event for this resource's booking
        List<BookingAuditLog> auditRows = auditLogRepository.findAll().stream()
                .filter(row -> row.getResourceId().equals(resourceId)
                        && row.getNewStatus() == BookingStatus.PENDING)
                .toList();
        assertThat(auditRows).as("Exactly 1 audit row for the successful creation").hasSize(1);
    }
}
