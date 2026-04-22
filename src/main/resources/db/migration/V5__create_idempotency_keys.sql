-- Idempotency key store for POST /bookings.
-- Allows clients to safely retry requests without risk of duplicate bookings.
CREATE TABLE idempotency_keys (
    key          VARCHAR(255) NOT NULL,
    response_body TEXT,
    status_code  INTEGER      NOT NULL,
    created_at   TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_idempotency_keys PRIMARY KEY (key)
);
