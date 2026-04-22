# Booking & Reservation Management System

[![CI](https://github.com/Will-Barnard-WB/BookingSystem/actions/workflows/ci.yml/badge.svg)](https://github.com/Will-Barnard-WB/BookingSystem/actions/workflows/ci.yml)

A production-quality Spring Boot 3 monolithic backend service for managing room and resource bookings.

Built to demonstrate clean REST API design, relational data modelling, transactional integrity, and concurrency safety.

---

## Tech Stack

| Layer       | Technology                                |
|-------------|-------------------------------------------|
| Language    | Java 17                                   |
| Framework   | Spring Boot 3.2                           |
| Web         | Spring Web (Jackson JSON)                 |
| Persistence | Spring Data JPA + Hibernate 6             |
| Database    | PostgreSQL 16                             |
| Validation  | Jakarta Bean Validation 3                 |
| Testing     | JUnit 5 + Mockito + Testcontainers        |
| Containers  | Docker + Docker Compose                   |
| Build       | Maven 3.9                                 |

---

## Architecture

```
┌──────────────────────────────────────────────────────┐
│                     HTTP Client                      │
└───────────────────────┬──────────────────────────────┘
                        │ REST (JSON)
┌───────────────────────▼──────────────────────────────┐
│                   Controller Layer                   │
│   BookingController  UserController  ResourceController│
└───────────────────────┬──────────────────────────────┘
                        │ DTOs
┌───────────────────────▼──────────────────────────────┐
│                    Service Layer                     │
│   BookingServiceImpl  UserServiceImpl  ResourceServiceImpl│
│                                                      │
│   Business rules:                                    │
│   • Overlap detection (startA < endB ∧ startB < endA)│
│   • Double-booking prevention (SERIALIZABLE tx)      │
│   • Status transition guards (PENDING→CONFIRMED→CANCELLED)│
└──────────┬─────────────────────────┬─────────────────┘
           │ JPA                     │ JPA
┌──────────▼──────────┐  ┌──────────▼─────────────────┐
│  BookingRepository  │  │  UserRepository             │
│  (overlap @Query)   │  │  ResourceRepository         │
└──────────┬──────────┘  └──────────┬─────────────────┘
           │                        │
┌──────────▼────────────────────────▼─────────────────┐
│                    PostgreSQL                        │
│   users   resources   bookings                      │
│                       └── idx_bookings_resource_time│
└──────────────────────────────────────────────────────┘
```

---

## Domain Model

### Entities

**User**
- `id` UUID, `name`, `email` (unique), `createdAt`

**Resource** (a room, seat, or any bookable asset)
- `id` UUID, `name`, `description`, `capacity`, `createdAt`

**Booking**
- `id` UUID, `user` (FK), `resource` (FK), `startTime`, `endTime`, `status`, `createdAt`

### Booking Status Lifecycle

```
PENDING ──► CONFIRMED ──► CANCELLED
   │                          ▲
   └──────────────────────────┘
```

---

## API Reference

### Bookings

| Method | Path                       | Description                          | Success |
|--------|----------------------------|--------------------------------------|---------|
| POST   | `/bookings`                | Create a booking                     | 201     |
| GET    | `/bookings/{id}`           | Get booking by ID                    | 200     |
| GET    | `/users/{userId}/bookings` | List all bookings for a user         | 200     |
| POST   | `/bookings/{id}/cancel`    | Cancel a booking                     | 200     |

### Users

| Method | Path          | Description        | Success |
|--------|---------------|--------------------|---------|
| POST   | `/users`      | Register a user    | 201     |
| GET    | `/users`      | List all users     | 200     |
| GET    | `/users/{id}` | Get user by ID     | 200     |
| PUT    | `/users/{id}` | Update user        | 200     |
| DELETE | `/users/{id}` | Delete user        | 204     |

### Resources

| Method | Path               | Description            | Success |
|--------|--------------------|------------------------|---------|
| POST   | `/resources`       | Register a resource    | 201     |
| GET    | `/resources`       | List all resources     | 200     |
| GET    | `/resources/{id}`  | Get resource by ID     | 200     |
| PUT    | `/resources/{id}`  | Update resource        | 200     |
| DELETE | `/resources/{id}`  | Delete resource        | 204     |

### Error Response Schema

All 4xx/5xx responses follow this envelope:

```json
{
  "error":     "RESOURCE_UNAVAILABLE",
  "message":   "Resource 'Conference Room A' is already booked for the requested slot.",
  "timestamp": "2026-04-15T10:30:00"
}
```

---

## Key Engineering Concepts

### Overlap Detection

Two booking intervals overlap when:

```
startA < endB  AND  startB < endA
```

Implemented as a JPQL query in `BookingRepository.findOverlappingBookings()`. The service calls this before saving any new booking.

### Concurrency Control

The naive read-check-then-write pattern has a TOCTOU race condition: two concurrent requests can both pass the overlap check before either has committed.

**Solution used here**: `SERIALIZABLE` transaction isolation on `createBooking()`. The database detects phantom reads and rolls back one of the conflicting transactions. The service layer catches the serialisation failure and surfaces it as a `ResourceUnavailableException` (409 Conflict).

**Alternative approaches**:
- `SELECT ... FOR UPDATE` on overlapping rows (pessimistic lock)
- A partial unique index on `(resource_id, tstzrange(start_time, end_time))` using PostgreSQL's exclusion constraints

### Transaction Management

- `BookingServiceImpl` is annotated `@Transactional(readOnly = true)` at the class level.
- Write methods override with `@Transactional` (defaults to READ_COMMITTED).
- `createBooking` overrides with `@Transactional(isolation = Isolation.SERIALIZABLE)`.
- `open-in-view` is explicitly set to `false` to keep transaction boundaries at the service layer.

---

## Running Locally

### Prerequisites

- Docker & Docker Compose
- JDK 17+ (for local dev without Docker)
- Maven 3.9+

### With Docker Compose (recommended)

```bash
# Clone the repo
git clone <repo-url>
cd booking-system

# Build and start Postgres + the application
docker-compose up --build

# The API is now available at http://localhost:8080
```

### Without Docker (local Maven)

```bash
# Requires a local PostgreSQL instance with the credentials in application.yml

# Run the app
mvn spring-boot:run

# Or build and run the jar
mvn package -DskipTests
java -jar target/booking-*.jar
```

### Running Tests

```bash
# Unit tests only (fast, no Docker required)
mvn test -Dtest="BookingServiceTest,BookingValidatorTest"

# Full test suite (Testcontainers pulls a Postgres image automatically)
mvn verify
```

### Smoke Test

```bash
# Create a user
curl -s -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com"}' | jq .

# Create a resource
curl -s -X POST http://localhost:8080/resources \
  -H "Content-Type: application/json" \
  -d '{"name":"Conference Room A","description":"10-person room","capacity":10}' | jq .

# Create a booking (replace UUIDs with the ids returned above)
curl -s -X POST http://localhost:8080/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "userId":     "<user-id>",
    "resourceId": "<resource-id>",
    "startTime":  "2026-06-01T10:00:00",
    "endTime":    "2026-06-01T11:00:00"
  }' | jq .
```

---

## Project Structure

```
src/
├── main/java/com/example/booking/
│   ├── BookingApplication.java
│   ├── controller/          # REST controllers (thin — delegate to service)
│   ├── service/             # Service interfaces
│   │   └── impl/            # Business logic implementations
│   ├── repository/          # Spring Data JPA repositories
│   ├── domain/
│   │   ├── entity/          # JPA entities (no Lombok to avoid proxy issues)
│   │   └── enums/           # BookingStatus
│   ├── dto/                 # Request/Response objects (Lombok @Value/@Builder)
│   ├── mapper/              # Entity → DTO mappers
│   ├── exception/           # Custom exceptions + GlobalExceptionHandler
│   └── validation/          # Custom constraint annotation + validator
└── test/java/com/example/booking/
    ├── unit/                # Mockito unit tests (no Spring context)
    ├── integration/         # @SpringBootTest + Testcontainers
    └── concurrency/         # Race condition test scaffold
```

---

## TODO / Next Steps

- [ ] Implement `createBooking()` overlap check and persistence in `BookingServiceImpl`
- [ ] Implement `cancelBooking()` domain state transition
- [ ] Add Flyway migrations (replace `ddl-auto: update`)
- [ ] Add Spring Security (JWT-based auth)
- [ ] Add pagination to list endpoints (`Page<T>`)
- [ ] Complete concurrency test in `ConcurrentBookingTest`
- [ ] Add Actuator health/metrics endpoints
