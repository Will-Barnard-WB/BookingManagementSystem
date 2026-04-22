package com.example.booking.integration;

import com.example.booking.domain.entity.BookingAuditLog;
import com.example.booking.domain.enums.BookingStatus;
import com.example.booking.dto.CreateBookingRequest;
import com.example.booking.dto.CreateResourceRequest;
import com.example.booking.dto.CreateUserRequest;
import com.example.booking.repository.BookingAuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class BookingControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("booking_test")
                    .withUsername("test_user")
                    .withPassword("test_pass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private BookingAuditLogRepository auditLogRepository;

    // ─── Helpers ────────────────────────────────────────────────────────────

    /** Creates a user with a unique email and returns its UUID string. */
    private String createUser() throws Exception {
        String email = UUID.randomUUID() + "@test.com";
        MvcResult result = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateUserRequest("Test User", email))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    /** Creates a resource with a unique name and returns its UUID string. */
    private String createResource() throws Exception {
        String name = "Room-" + UUID.randomUUID();
        MvcResult result = mockMvc.perform(post("/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateResourceRequest(name, "Test room", 4))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    /** Creates a booking for the given user/resource and returns its UUID string. */
    private String createBooking(String userId, String resourceId,
                                 LocalDateTime start, LocalDateTime end) throws Exception {
        CreateBookingRequest req = new CreateBookingRequest(
                UUID.fromString(userId), UUID.fromString(resourceId), start, end);
        MvcResult result = mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    // ─── User endpoint smoke tests ──────────────────────────────────────────

    @Test
    @DisplayName("POST /users — creates a user and returns 201")
    void createUser_returns201() throws Exception {
        CreateUserRequest request = new CreateUserRequest("Bob", "bob@example.com");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Bob"))
                .andExpect(jsonPath("$.email").value("bob@example.com"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    @DisplayName("POST /users — returns 400 for invalid email")
    void createUser_invalidEmail_returns400() throws Exception {
        CreateUserRequest request = new CreateUserRequest("Bob", "not-an-email");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    // ─── Resource endpoint smoke tests ──────────────────────────────────────

    @Test
    @DisplayName("POST /resources — creates a resource and returns 201")
    void createResource_returns201() throws Exception {
        CreateResourceRequest request =
                new CreateResourceRequest("Meeting Room B", "Quiet room", 4);

        mockMvc.perform(post("/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Meeting Room B"));
    }

    // ─── Booking lifecycle tests ─────────────────────────────────────────────

    @Test
    @DisplayName("GET /bookings/{id} — returns 404 for unknown id")
    void getBooking_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/bookings/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /bookings — happy path returns 201 with PENDING status")
    void createBooking_returns201() throws Exception {
        String userId = createUser();
        String resourceId = createResource();
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end   = start.plusHours(1);

        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateBookingRequest(UUID.fromString(userId),
                                        UUID.fromString(resourceId), start, end))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    @DisplayName("POST /bookings — overlapping slot returns 409")
    void createBooking_overlap_returns409() throws Exception {
        String userId = createUser();
        String resourceId = createResource();
        LocalDateTime start = LocalDateTime.now().plusHours(2);
        LocalDateTime end   = start.plusHours(1);

        createBooking(userId, resourceId, start, end);  // first succeeds

        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateBookingRequest(UUID.fromString(userId),
                                        UUID.fromString(resourceId), start, end))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("RESOURCE_UNAVAILABLE"));
    }

    @Test
    @DisplayName("POST /bookings/{id}/confirm — PENDING → CONFIRMED returns 200")
    void confirmBooking_returns200() throws Exception {
        String userId = createUser();
        String resourceId = createResource();
        LocalDateTime start = LocalDateTime.now().plusHours(3);
        String bookingId = createBooking(userId, resourceId, start, start.plusHours(1));

        mockMvc.perform(post("/bookings/" + bookingId + "/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("POST /bookings/{id}/confirm — CONFIRMED → CONFIRMED returns 400")
    void confirmBooking_alreadyConfirmed_returns400() throws Exception {
        String userId = createUser();
        String resourceId = createResource();
        LocalDateTime start = LocalDateTime.now().plusHours(4);
        String bookingId = createBooking(userId, resourceId, start, start.plusHours(1));

        mockMvc.perform(post("/bookings/" + bookingId + "/confirm")); // first confirm

        mockMvc.perform(post("/bookings/" + bookingId + "/confirm"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ILLEGAL_STATE_TRANSITION"));
    }

    @Test
    @DisplayName("POST /bookings/{id}/cancel — PENDING → CANCELLED returns 200")
    void cancelBooking_returns200() throws Exception {
        String userId = createUser();
        String resourceId = createResource();
        LocalDateTime start = LocalDateTime.now().plusHours(5);
        String bookingId = createBooking(userId, resourceId, start, start.plusHours(1));

        mockMvc.perform(post("/bookings/" + bookingId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("POST /bookings/{id}/cancel — already CANCELLED returns 400")
    void cancelBooking_alreadyCancelled_returns400() throws Exception {
        String userId = createUser();
        String resourceId = createResource();
        LocalDateTime start = LocalDateTime.now().plusHours(6);
        String bookingId = createBooking(userId, resourceId, start, start.plusHours(1));

        mockMvc.perform(post("/bookings/" + bookingId + "/cancel")); // first cancel

        mockMvc.perform(post("/bookings/" + bookingId + "/cancel"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ILLEGAL_STATE_TRANSITION"));
    }

    // ─── Idempotency tests ───────────────────────────────────────────────────

    @Test
    @DisplayName("Idempotency: duplicate key returns identical response without creating a second booking")
    void idempotency_duplicateKey_returnsCachedResponse() throws Exception {
        String userId = createUser();
        String resourceId = createResource();
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end   = start.plusHours(1);
        String idempotencyKey = UUID.randomUUID().toString();

        CreateBookingRequest req = new CreateBookingRequest(
                UUID.fromString(userId), UUID.fromString(resourceId), start, end);

        // First request — creates the booking
        MvcResult first = mockMvc.perform(post("/bookings")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        // Second request with same key — must return the cached response
        MvcResult second = mockMvc.perform(post("/bookings")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        assertThat(first.getResponse().getContentAsString())
                .isEqualTo(second.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("Idempotency: missing header proceeds normally without idempotency")
    void idempotency_missingHeader_worksNormally() throws Exception {
        String userId = createUser();
        String resourceId = createResource();
        LocalDateTime start = LocalDateTime.now().plusDays(2);

        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateBookingRequest(UUID.fromString(userId),
                                        UUID.fromString(resourceId), start, start.plusHours(1)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    // ─── Audit log tests ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Audit: creating a booking writes one PENDING row to the audit log")
    void auditLog_bookingCreated_writesRow() throws Exception {
        String userId = createUser();
        String resourceId = createResource();
        LocalDateTime start = LocalDateTime.now().plusDays(10);
        String bookingId = createBooking(userId, resourceId, start, start.plusHours(1));

        List<BookingAuditLog> rows = auditLogRepository
                .findByBookingIdOrderByChangedAtAsc(UUID.fromString(bookingId));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getPreviousStatus()).isNull();
        assertThat(rows.get(0).getNewStatus()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    @DisplayName("Audit: confirm writes a second row, cancel writes a third")
    void auditLog_fullLifecycle_writesThreeRows() throws Exception {
        String userId = createUser();
        String resourceId = createResource();
        LocalDateTime start = LocalDateTime.now().plusDays(11);
        String bookingId = createBooking(userId, resourceId, start, start.plusHours(1));

        mockMvc.perform(post("/bookings/" + bookingId + "/confirm")).andExpect(status().isOk());
        mockMvc.perform(post("/bookings/" + bookingId + "/cancel")).andExpect(status().isOk());

        List<BookingAuditLog> rows = auditLogRepository
                .findByBookingIdOrderByChangedAtAsc(UUID.fromString(bookingId));

        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).getNewStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(rows.get(1).getPreviousStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(rows.get(1).getNewStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(rows.get(2).getPreviousStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(rows.get(2).getNewStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    @DisplayName("Audit: a failed booking creation (overlap) writes no audit row")
    void auditLog_failedCreation_writesNoRow() throws Exception {
        String userId = createUser();
        String resourceId = createResource();
        LocalDateTime start = LocalDateTime.now().plusDays(12);
        String bookingId = createBooking(userId, resourceId, start, start.plusHours(1));

        // Overlap attempt — throws 409, no commit, no audit row
        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBookingRequest(
                                UUID.fromString(userId), UUID.fromString(resourceId),
                                start, start.plusHours(1)))))
                .andExpect(status().isConflict());

        // Audit log for the original booking must still be exactly 1 row
        List<BookingAuditLog> rows = auditLogRepository
                .findByBookingIdOrderByChangedAtAsc(UUID.fromString(bookingId));
        assertThat(rows).hasSize(1);
    }

    @Test
    @DisplayName("Idempotency: different keys create independent bookings on different slots")
    void idempotency_differentKeys_createSeparateBookings() throws Exception {
        String userId = createUser();
        String resourceId = createResource();
        LocalDateTime start1 = LocalDateTime.now().plusDays(3);
        LocalDateTime start2 = LocalDateTime.now().plusDays(4);

        MvcResult r1 = mockMvc.perform(post("/bookings")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateBookingRequest(UUID.fromString(userId),
                                        UUID.fromString(resourceId), start1, start1.plusHours(1)))))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult r2 = mockMvc.perform(post("/bookings")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateBookingRequest(UUID.fromString(userId),
                                        UUID.fromString(resourceId), start2, start2.plusHours(1)))))
                .andExpect(status().isCreated())
                .andReturn();

        String id1 = objectMapper.readTree(r1.getResponse().getContentAsString()).get("id").asText();
        String id2 = objectMapper.readTree(r2.getResponse().getContentAsString()).get("id").asText();
        assertThat(id1).isNotEqualTo(id2);
    }
}
