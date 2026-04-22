CREATE TABLE resources (
    id          UUID         NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    capacity    INTEGER      NOT NULL,
    created_at  TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_resources PRIMARY KEY (id)
);
