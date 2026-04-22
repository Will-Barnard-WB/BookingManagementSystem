package com.example.booking.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Persisted record of an idempotency key and the response that was returned
 * for the corresponding POST /bookings request.
 *
 * Keyed by the client-supplied Idempotency-Key header value.
 */
@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKey {

    @Id
    @Column(nullable = false)
    private String key;

    @Column(name = "response_body", columnDefinition = "text")
    private String responseBody;

    @Column(name = "status_code", nullable = false)
    private int statusCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected IdempotencyKey() {}

    public IdempotencyKey(String key, String responseBody, int statusCode, LocalDateTime createdAt) {
        this.key = key;
        this.responseBody = responseBody;
        this.statusCode = statusCode;
        this.createdAt = createdAt;
    }

    public String getKey()          { return key; }
    public String getResponseBody() { return responseBody; }
    public int    getStatusCode()   { return statusCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
