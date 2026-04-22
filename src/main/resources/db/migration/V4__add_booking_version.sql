-- Optimistic locking: second line of defence alongside SERIALIZABLE isolation.
-- Hibernate increments this column on every UPDATE and rejects stale writes.
ALTER TABLE bookings ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
