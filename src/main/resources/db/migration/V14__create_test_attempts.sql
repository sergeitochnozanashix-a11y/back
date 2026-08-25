CREATE TABLE test_attempts
(
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT                      NOT NULL,
    test_id       BIGINT                      NOT NULL,
    lesson_id     BIGINT                      NOT NULL,
    module_id     BIGINT                      NOT NULL,
    status        VARCHAR(32)                 NOT NULL DEFAULT 'SUBMITTED',
    score_percent INT,
    passed        BOOLEAN,
    evaluated_at  TIMESTAMP WITHOUT TIME ZONE,
    failed_reason VARCHAR(1024),
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    CONSTRAINT fk_ta_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_ta_test FOREIGN KEY (test_id) REFERENCES tests (id) ON DELETE CASCADE,
    CONSTRAINT fk_ta_lesson FOREIGN KEY (lesson_id) REFERENCES lessons (id) ON DELETE RESTRICT,
    CONSTRAINT fk_ta_module FOREIGN KEY (module_id) REFERENCES modules (id) ON DELETE RESTRICT,
    CONSTRAINT chk_ta_score_percent CHECK (score_percent IS NULL OR (score_percent >= 0 AND score_percent <= 100))
);

CREATE INDEX idx_ta_user_test_created ON test_attempts (user_id, test_id, created_at DESC);
CREATE INDEX idx_ta_status ON test_attempts (status);
CREATE INDEX idx_ta_lesson_user ON test_attempts (lesson_id, user_id);
CREATE INDEX idx_ta_module_user ON test_attempts (module_id, user_id);

CREATE OR REPLACE FUNCTION trg_set_updated_at_ta() RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at = (CURRENT_TIMESTAMP AT TIME ZONE 'UTC');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_updated_at_ta
    BEFORE UPDATE
    ON test_attempts
    FOR EACH ROW
EXECUTE FUNCTION trg_set_updated_at_ta();
