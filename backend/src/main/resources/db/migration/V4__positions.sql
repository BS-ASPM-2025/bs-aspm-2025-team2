CREATE TABLE positions (
                           id BIGSERIAL PRIMARY KEY,
                           name VARCHAR(255) NOT NULL,
                           required_skills TEXT,
                           skills_weight INTEGER NOT NULL,
                           experience_weight INTEGER NOT NULL
);

CREATE INDEX idx_positions_name ON positions (name);
