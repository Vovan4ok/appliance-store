CREATE TABLE manufacturer (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(255)
);

CREATE TABLE employee (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255),
    email      VARCHAR(255),
    password   VARCHAR(255),
    department VARCHAR(255)
);

CREATE TABLE client (
    id       BIGSERIAL PRIMARY KEY,
    name     VARCHAR(255),
    email    VARCHAR(255),
    password VARCHAR(255),
    card     VARCHAR(255)
);

CREATE TABLE appliance (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255),
    category        VARCHAR(50),
    model           VARCHAR(255),
    manufacturer_id BIGINT REFERENCES manufacturer (id),
    power_type      VARCHAR(50),
    characteristic  VARCHAR(255),
    description     VARCHAR(1000),
    power           INTEGER,
    price           NUMERIC(19, 2)
);

CREATE TABLE orders (
    id          BIGSERIAL PRIMARY KEY,
    approved    BOOLEAN,
    client_id   BIGINT REFERENCES client (id),
    employee_id BIGINT REFERENCES employee (id)
);

CREATE TABLE order_row (
    id           BIGSERIAL PRIMARY KEY,
    appliance_id BIGINT REFERENCES appliance (id),
    amount       NUMERIC(19, 2),
    number       BIGINT,
    order_id     BIGINT REFERENCES orders (id)
);