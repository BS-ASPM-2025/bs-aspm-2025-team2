CREATE TABLE IF NOT EXISTS flyway_sanity_check (
                                                   id BIGSERIAL PRIMARY KEY,
                                                   created_at TIMESTAMP NOT NULL DEFAULT NOW()
    );