package com.example.booking.integration;

import com.example.booking.dto.CreateResourceRequest;
import com.example.booking.dto.CreateUserRequest;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test suite for the Booking API.
 *
 * Uses Testcontainers to spin up a real PostgreSQL container, giving us
 * confidence that JPA queries, transactions, and schema creation work
 * correctly against a real database engine.
 *
 * The @DynamicPropertySource method wires the container's connection URL
 * into the Spring datasource configuration at runtime — no hard-coded ports.
 *
 * TODO: add tests for:
 *   - POST /bookings — happy path (201 Created)
 *   - POST /bookings — overlap conflict (409 Conflict)
 *   - POST /bookings — validation failure (400 Bad Request)
 *   - GET  /bookings/{id} — not found (404)
 *   - POST /bookings/{id}/cancel — happy path (200 OK)
 */
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

    // ─── Booking endpoint smoke tests ────────────────────────────────────────

    @Test
    @DisplayName("GET /bookings/{id} — returns 404 for unknown id")
    void getBooking_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/bookings/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // TODO: POST /bookings happy-path test
    // TODO: POST /bookings overlap test
    // TODO: POST /bookings/{id}/cancel test
}
