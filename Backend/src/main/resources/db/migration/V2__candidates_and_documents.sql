-- Candidate status as text (portable, simple)
CREATE TABLE IF NOT EXISTS candidates (
                                          id              BIGSERIAL PRIMARY KEY,
                                          status          VARCHAR(32) NOT NULL,
    upload_date     TIMESTAMP NOT NULL
    );

CREATE TABLE IF NOT EXISTS documents (
                                         id                BIGSERIAL PRIMARY KEY,
                                         candidate_id       BIGINT NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,

    upload_date        TIMESTAMP NOT NULL,
    original_filename  TEXT NOT NULL,
    file_size          BIGINT NOT NULL,
    content_type       VARCHAR(255) NOT NULL,

    storage_path       TEXT NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_candidates_status ON candidates(status);
CREATE INDEX IF NOT EXISTS idx_documents_candidate_id ON documents(candidate_id);
