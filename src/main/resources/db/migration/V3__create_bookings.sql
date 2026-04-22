CREATE TABLE bookings (
    id          UUID         NOT NULL,
    user_id     UUID         NOT NULL,
    resource_id UUID         NOT NULL,
    start_time  TIMESTAMP(6) NOT NULL,
    end_time    TIMESTAMP(6) NOT NULL,
    status      VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_bookings          PRIMARY KEY (id),
    CONSTRAINT fk_bookings_user     FOREIGN KEY (user_id)     REFERENCES users(id),
    CONSTRAINT fk_bookings_resource FOREIGN KEY (resource_id) REFERENCES resources(id)
);

-- Speeds up the overlap-detection query scoped to a resource
CREATE INDEX idx_bookings_resource_time ON bookings (resource_id, start_time, end_time);
