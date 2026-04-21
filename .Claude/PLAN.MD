Claude Code plan — paste each phase as a separate prompt:
Prompt 1:

Implement the core booking logic in BookingServiceImpl. createBooking() should check for overlapping bookings using the existing findOverlappingBookings() JPQL query, then persist with @Transactional(isolation = Isolation.SERIALIZABLE). Throw ResourceUnavailableException (409) on overlap. Implement cancelBooking() and confirmBooking() with status transition guards — only allow PENDING→CONFIRMED, PENDING→CANCELLED, CONFIRMED→CANCELLED. Reject invalid transitions with an IllegalStateTransitionException (400). Write unit tests for every transition including the rejected ones.

Prompt 2:

Replace ddl-auto: update with Flyway. Create versioned migrations in src/main/resources/db/migration/: V1__create_users.sql, V2__create_resources.sql, V3__create_bookings.sql. The bookings migration should include an index on (resource_id, start_time, end_time) for the overlap query. Add the Flyway Spring Boot starter to pom.xml. Make sure the existing Testcontainers integration tests still pass with Flyway managing the schema.

Prompt 3:

Add a @Version field to the Booking entity for optimistic locking. Update createBooking() to catch OptimisticLockException and rethrow as ResourceUnavailableException (409). Keep the SERIALIZABLE isolation as a second line of defence. Add a Flyway migration V4__add_booking_version.sql for the version column.

Prompt 4:

Add idempotency key support. Create an idempotency_keys table (Flyway migration V5__create_idempotency_keys.sql) with columns: key (string, unique), response_body (text), status_code (int), created_at (timestamp). Create a Spring HandlerInterceptor that checks for an Idempotency-Key header on POST requests to /bookings. If the key exists in the table, return the cached response. If not, let the request proceed and store the response after completion. If no header is provided, proceed normally without idempotency. Write integration tests covering: duplicate key returns same response, missing header works normally, different keys create different bookings.

Prompt 5:

Add domain events using Spring's ApplicationEventPublisher. Create event classes BookingCreatedEvent, BookingConfirmedEvent, BookingCancelledEvent in a domain/event package — each containing the booking ID, user ID, resource ID, timestamp, and status. Publish from the service methods after the state change. Create two @TransactionalEventListener listeners: one that writes to a booking_audit_log table (Flyway migration V6__create_audit_log.sql) recording every state transition with previous and new status, and one that logs a simulated notification (just SLF4J info log). This is the transactional outbox pattern — the audit row is written in the same transaction as the booking change. Write tests verifying that audit rows are created for each transition and that a failed transaction creates no audit row.

Prompt 6:

Add observability. First, add Spring Boot Actuator with /actuator/health and /actuator/metrics exposed. Then add custom Micrometer metrics: a counter bookings.created incremented on successful creation, a counter bookings.conflicts incremented on every 409, and a timer bookings.create.latency wrapping the createBooking() method. Second, add structured logging with MDC: create a servlet filter that generates a correlationId UUID (or reads it from an X-Correlation-ID request header), puts it in MDC, and clears it after the request. Update the logging pattern in application.yml to include correlationId. Add userId and resourceId to MDC within booking service methods.

Prompt 7:

Add pagination to all list endpoints. Update UserController, ResourceController, and the user-bookings endpoint to accept Pageable parameters and return Page<T>. Update the corresponding repository and service methods. Write tests verifying pagination parameters work correctly.

Prompt 8:

Complete the concurrency test in ConcurrentBookingTest. Use Testcontainers for a real Postgres instance. Create one user and one resource, then use an ExecutorService with a CountDownLatch to fire 50 simultaneous booking requests for the exact same time slot. Assert that exactly 1 booking is created successfully and the other 49 receive 409 status codes. Also verify only 1 audit log row exists for the creation. This is the most important test in the project — make sure it's deterministic and doesn't flake.

Prompt 9:

Add a GitHub Actions CI workflow in .github/workflows/ci.yml. It should run mvn verify on every push and PR to main, using a Java 17 setup. Testcontainers will handle the database — no need for a service container. Add a build status badge to the README.