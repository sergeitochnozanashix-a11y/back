CREATE TABLE test_answer_audit
(
    id             BIGSERIAL PRIMARY KEY,
    answer_id      BIGINT                      NOT NULL,
    set_by_type    VARCHAR(16)                 NOT NULL,
    set_by_id      BIGINT,
    old_evaluation VARCHAR(16),
    new_evaluation VARCHAR(16)                 NOT NULL,
    notes          VARCHAR(1024),
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    CONSTRAINT fk_taa_answer FOREIGN KEY (answer_id) REFERENCES test_answers (id) ON DELETE CASCADE
);

CREATE INDEX idx_taa_answer_created ON test_answer_audit (answer_id, created_at DESC);
CREATE INDEX idx_taa_setby ON test_answer_audit (set_by_type, set_by_id);
