# Booking & Reservation Management System

  [![CI](https://github.com/Will-Barnard-WB/BookingManagementSystem/actions/workflows/ci.yml/badge.svg)](https://github.com/Will-Barnard-WB
  /BookingManagementSystem/actions/workflows/ci.yml)

  A production-quality Spring Boot 3 backend for room & resource bookings — built to demonstrate concurrency-safe design, transactional
  integrity, and clean REST API engineering.

  **Status:** complete and CI-green — 34 tests passing, including a 50-thread concurrency test.
  
  ---

  ## What it does

  Manages users, bookable resources, and bookings — and guarantees **no double-booking under concurrent load**. When many requests race for
  the same slot, exactly one wins and the rest get a clean `409 Conflict`.

  ```
  Client ──REST/JSON──► Controller ──DTO──► Service ──JPA──► PostgreSQL
                                              │
                    overlap check + SERIALIZABLE tx + idempotency + outbox
  ```
  
  ---

  ## How it prevents double-booking (the core problem)
  
  The naive "check overlap, then insert" has a TOCTOU race: two concurrent requests can both pass the overlap check before either commits.
  The solution layers four mechanisms:

  1. **Overlap detection** — two intervals overlap when `startA < endB AND startB < endA`, run as a JPQL query before any insert.
  2. **SERIALIZABLE isolation** on `createBooking()` — PostgreSQL detects the phantom read and aborts one of the conflicting transactions;
  the service catches the serialisation failure and surfaces it as `409 Conflict`.
  3. **Idempotency keys** — clients can safely retry; a repeated key returns the original result instead of creating a duplicate booking.
  4. **Transactional outbox** — audit-log rows are written in the *same* transaction via a `BEFORE_COMMIT` event listener, so the audit
  trail can never drift from the booking state.
  
  **Verified:** a 50-thread `ConcurrentBookingTest` confirms exactly **one** booking commits and the other **49 are rejected with 409** —
  passing in CI.

  > Alternatives considered: pessimistic `SELECT ... FOR UPDATE`, or a PostgreSQL exclusion constraint (`tstzrange` + GiST). SERIALIZABLE
  was chosen for the strongest correctness guarantee; the trade-off is reduced write throughput under heavy contention.

  ---

  ## Tech Stack

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

  ---

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

  ---

  ## API Reference

  ### Bookings
  | Method | Path                       | Description              | Success |
  |--------|----------------------------|--------------------------|---------|
  | POST   | `/bookings`                | Create a booking         | 201     |
  | GET    | `/bookings/{id}`           | Get booking by ID        | 200     |
  | GET    | `/users/{userId}/bookings` | List a user's bookings   | 200     |
  | POST   | `/bookings/{id}/cancel`    | Cancel a booking         | 200     |

  (Users and Resources expose standard CRUD — see source.)

  ### Error envelope
  ```json
  { "error": "RESOURCE_UNAVAILABLE", "message": "Resource already booked for the requested slot.", "timestamp": "2026-04-15T10:30:00" }
  ```
  
  ---

  ## Running Locally

  ```bash
  git clone https://github.com/Will-Barnard-WB/BookingManagementSystem
  cd BookingManagementSystem
  docker-compose up --build      # Postgres + app; API at http://localhost:8080
  ```
  
  Run tests:
  ```bash
  mvn verify                     # full suite (Testcontainers pulls Postgres automatically)
  ```

  ---

  ## Project Structure

  ```
  src/main/java/com/example/booking/
  ├── controller/    # thin REST controllers
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

  ---