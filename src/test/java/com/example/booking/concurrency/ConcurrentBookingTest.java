package com.example.booking.concurrency;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Concurrency test scaffold for double-booking prevention.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * GOAL
 * ──────────────────────────────────────────────────────────────────────────
 * Simulate N threads all attempting to book the same resource for the same
 * time slot simultaneously. After all threads complete, exactly ONE booking
 * should be in a CONFIRMED/PENDING state for that slot — all others should
 * have failed with ResourceUnavailableException.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * HOW TO IMPLEMENT THIS TEST
 * ──────────────────────────────────────────────────────────────────────────
 * 1. Extend BookingControllerIntegrationTest (reuse the Testcontainers setup)
 *    OR autowire BookingService directly in a @SpringBootTest.
 *
 * 2. Prepare shared test data (one user, one resource) before the test.
 *
 * 3. Use a CountDownLatch (a "starting gun") to make all threads begin
 *    their booking attempt at the same moment:
 *
 *    CountDownLatch startLatch  = new CountDownLatch(1);   // all wait here
 *    CountDownLatch doneLatch   = new CountDownLatch(THREAD_COUNT);
 *    AtomicInteger  successes   = new AtomicInteger(0);
 *    AtomicInteger  conflicts   = new AtomicInteger(0);
 *
 *    for (int i = 0; i < THREAD_COUNT; i++) {
 *        executor.submit(() -> {
 *            try {
 *                startLatch.await();                         // block until gun fires
 *                bookingService.createBooking(request);
 *                successes.incrementAndGet();
 *            } catch (ResourceUnavailableException e) {
 *                conflicts.incrementAndGet();
 *            } catch (Exception e) {
 *                // handle serialisation retry or unexpected errors
 *            } finally {
 *                doneLatch.countDown();
 *            }
 *        });
 *    }
 *
 *    startLatch.countDown();        // fire the starting gun
 *    doneLatch.await();             // wait for all threads to finish
 *
 * 4. Assert:
 *    assertThat(successes.get()).isEqualTo(1);
 *    assertThat(conflicts.get()).isEqualTo(THREAD_COUNT - 1);
 *
 * ──────────────────────────────────────────────────────────────────────────
 * IMPORTANT: Serialisable transactions may throw a PSQLException with
 * SQLState 40001 (serialization_failure) before the service layer gets a
 * chance to throw ResourceUnavailableException. The service layer should
 * catch this and either:
 *   a) Retry the transaction (exponential back-off, max N attempts), or
 *   b) Re-throw as ResourceUnavailableException.
 * ──────────────────────────────────────────────────────────────────────────
 *
 * @see com.example.booking.service.impl.BookingServiceImpl#createBooking
 * @see com.example.booking.exception.ResourceUnavailableException
 */
@Disabled("Scaffold only — implement once createBooking logic is complete")
class ConcurrentBookingTest {

    private static final int THREAD_COUNT = 10;

    @Test
    @DisplayName("Only one booking succeeds when N threads race for the same slot")
    void onlyOneBookingSucceeds_underConcurrentLoad() throws InterruptedException {
        // TODO: implement using the pattern described in the class Javadoc above.
        //
        // Step 1: Start a Testcontainers PostgreSQL instance (or reuse the IT setup)
        // Step 2: Create one User and one Resource via the service
        // Step 3: Build THREAD_COUNT identical CreateBookingRequests for the same slot
        // Step 4: Fire them all simultaneously via CountDownLatch
        // Step 5: Assert exactly 1 success and (THREAD_COUNT - 1) conflicts
    }
}
