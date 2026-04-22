-- Transactional outbox: every booking state change writes an audit row in the
-- same transaction via a BEFORE_COMMIT @TransactionalEventListener.
-- If the booking transaction rolls back, the audit row is also rolled back.
CREATE TABLE booking_audit_log (
    id              UUID         NOT NULL,
    booking_id      UUID         NOT NULL,
    user_id         UUID         NOT NULL,
    resource_id     UUID         NOT NULL,
    previous_status VARCHAR(255),
    new_status      VARCHAR(255) NOT NULL,
    changed_at      TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_booking_audit_log PRIMARY KEY (id)
);

CREATE INDEX idx_audit_log_booking_id ON booking_audit_log (booking_id);
