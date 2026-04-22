CREATE TABLE users (
    id          UUID         NOT NULL,
    name        VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_users    PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);
