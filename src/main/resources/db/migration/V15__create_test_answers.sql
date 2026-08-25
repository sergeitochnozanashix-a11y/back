CREATE TABLE test_answers
(
    id               BIGSERIAL PRIMARY KEY,
    attempt_id       BIGINT                      NOT NULL,
    question_id      BIGINT                      NOT NULL,
    input_type       VARCHAR(16)                 NOT NULL,
    text_answer      TEXT,
    voice_file_url   VARCHAR(1024),
    voice_transcript TEXT,
    evaluation       VARCHAR(16)                 NOT NULL DEFAULT 'UNASSESSED',
    evaluation_notes VARCHAR(1024),
    created_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    CONSTRAINT fk_tans_attempt FOREIGN KEY (attempt_id) REFERENCES test_attempts (id) ON DELETE CASCADE,
    CONSTRAINT fk_tans_question FOREIGN KEY (question_id) REFERENCES test_question (id) ON DELETE RESTRICT,
    CONSTRAINT uq_tans_attempt_question UNIQUE (attempt_id, question_id),
    CONSTRAINT chk_tans_text_only CHECK (
        (input_type = 'TEXT' AND text_answer IS NOT NULL AND voice_file_url IS NULL AND voice_transcript IS NULL)
            OR
        (input_type = 'VOICE' AND text_answer IS NULL AND voice_file_url IS NOT NULL)
        )
);

CREATE INDEX idx_tans_attempt ON test_answers (attempt_id);
CREATE INDEX idx_tans_question ON test_answers (question_id);
CREATE INDEX idx_tans_eval ON test_answers (evaluation);

CREATE OR REPLACE FUNCTION trg_set_updated_at_tans() RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at = (CURRENT_TIMESTAMP AT TIME ZONE 'UTC');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_updated_at_tans
    BEFORE UPDATE
    ON test_answers
    FOR EACH ROW
EXECUTE FUNCTION trg_set_updated_at_tans();
