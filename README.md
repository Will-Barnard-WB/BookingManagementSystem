# Booking & Reservation Management System

[![CI](https://github.com/Will-Barnard-WB/BookingManagementSystem/actions/workflows/ci.yml/badge.svg)](https://github.com/Will-Barnard-WB/BookingManagementSystem/actions/workflows/ci.yml)

A Spring Boot 3 backend for room and resource bookings. The main thing it gets
right is concurrency: when several requests race for the same slot, only one
booking is created and the rest get a `409 Conflict`.

The full suite is 34 tests (unit, integration, and a 50-thread concurrency test)
and runs green in CI.

## What it does

It manages users, bookable resources, and bookings. The hard part is making sure
the same slot can't be booked twice when requests arrive at the same time.

```
Client ──REST/JSON──► Controller ──DTO──► Service ──JPA──► PostgreSQL
                                            │
                  overlap check + SERIALIZABLE tx + idempotency + outbox
```

## How double-booking is prevented

The obvious "check for an overlap, then insert" has a race: two requests can both
pass the overlap check before either one commits. Four things work together to
close that gap:

1. **Overlap detection** — two intervals overlap when `startA < endB AND startB < endA`,
   run as a JPQL query before any insert.
2. **SERIALIZABLE isolation** on `createBooking()` — PostgreSQL detects the conflict
   and aborts one of the racing transactions. The service catches that failure and
   returns `409 Conflict`.
3. **Idempotency keys** — a client can retry safely. A repeated key returns the
   original result instead of creating a second booking.
4. **Transactional outbox** — audit-log rows are written in the same transaction as
   the booking (via a `BEFORE_COMMIT` event listener), so the audit trail can't drift
   from the actual booking state.

`ConcurrentBookingTest` fires 50 threads at the same slot and checks that exactly one
booking commits while the other 49 are rejected with a 409.

I chose SERIALIZABLE over a pessimistic `SELECT ... FOR UPDATE` or a PostgreSQL
exclusion constraint (`tstzrange` + GiST) because it gives the strongest correctness
guarantee. The trade-off is lower write throughput under heavy contention.

## Tech stack

| Layer       | Technology                          |
|-------------|-------------------------------------|
| Language    | Java 17                             |
| Framework   | Spring Boot 3.2                     |
| Persistence | Spring Data JPA + Hibernate 6       |
| Database    | PostgreSQL 16                       |
| Migrations  | Flyway                              |
| Metrics     | Micrometer + Spring Actuator        |
| Validation  | Jakarta Bean Validation 3           |
| Testing     | JUnit 5 + Mockito + Testcontainers  |
| Build/Infra | Maven 3.9, Docker Compose           |

## Architecture

```
HTTP Client
   │  REST (JSON)
Controller Layer        BookingController · UserController · ResourceController
   │  DTOs
Service Layer           BookingServiceImpl · UserServiceImpl · ResourceServiceImpl
   │                     • overlap detection      • SERIALIZABLE create
   │                     • idempotency keys        • transactional outbox (audit)
   │                     • status guards (PENDING→CONFIRMED→CANCELLED)
   │  JPA
Repositories            BookingRepository (overlap @Query) · User · Resource
   │
PostgreSQL              users · resources · bookings · idempotency_keys · booking_audit_log
                        Flyway-managed schema (V1–V6)
```

## API reference

### Bookings
| Method | Path                       | Description              | Success |
|--------|----------------------------|--------------------------|---------|
| POST   | `/bookings`                | Create a booking         | 201     |
| GET    | `/bookings/{id}`           | Get booking by ID        | 200     |
| GET    | `/users/{userId}/bookings` | List a user's bookings   | 200     |
| POST   | `/bookings/{id}/cancel`    | Cancel a booking         | 200     |

Users and Resources expose standard CRUD — see the controllers for the full list.

### Error response
```json
{ "error": "RESOURCE_UNAVAILABLE", "message": "Resource already booked for the requested slot.", "timestamp": "2026-04-15T10:30:00" }
```

## Running locally

```bash
git clone https://github.com/Will-Barnard-WB/BookingManagementSystem
cd BookingManagementSystem
docker-compose up --build      # Postgres + app; API at http://localhost:8080
```

Run the tests:
```bash
mvn verify                     # Testcontainers pulls Postgres automatically
```

## Project structure

```
src/main/java/com/example/booking/
├── controller/    # REST controllers
├── service/impl/  # business logic (overlap, SERIALIZABLE create, status guards)
├── repository/    # Spring Data JPA (overlap @Query)
├── domain/        # entities, enums, events (outbox)
├── dto/ · mapper/ · exception/ · validation/ · web/   # incl. IdempotencyFilter
└── resources/db/migration/   # Flyway V1–V6
src/test/java/com/example/booking/
├── unit/          # Mockito unit tests
├── integration/   # @SpringBootTest + Testcontainers
└── concurrency/   # 50-thread race test (1 commits, 49 → 409)
```
