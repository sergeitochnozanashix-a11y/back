-- --- ENUMS ---


CREATE TYPE lesson_progress_status AS ENUM (
    'LOCKED',
    'UNLOCKED',
    'IN_PROGRESS',
    'TEST_PENDING',
    'COMPLETED',
    'FAILED'
    );

CREATE TYPE answer_score_type AS ENUM (
    'INCORRECT',
    'PARTIALLY_CORRECT',
    'CORRECT'
    );

-- --- TABLES ---

CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(255) UNIQUE NOT NULL,
                       email VARCHAR(255) UNIQUE NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       role VARCHAR(20) NOT NULL,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE modules (
                         id BIGSERIAL PRIMARY KEY,
                         title VARCHAR(255) NOT NULL,
                         description TEXT,
                         created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lessons (
                         id BIGSERIAL PRIMARY KEY,
                         module_id BIGINT NOT NULL,
                         title VARCHAR(255) NOT NULL,
                         description TEXT,
                         sequence_order INT NOT NULL DEFAULT 0,
                         created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         CONSTRAINT fk_lessons_module_id
                             FOREIGN KEY(module_id)
                                 REFERENCES modules(id)
                                 ON DELETE CASCADE,
                         CONSTRAINT uq_lessons_module_id_sequence_order
                             UNIQUE (module_id, sequence_order)
);
COMMENT ON COLUMN lessons.sequence_order IS 'For ordering within a module';

CREATE TABLE lesson_content_blocks (
                                       id BIGSERIAL PRIMARY KEY,
                                       lesson_id BIGINT NOT NULL,
                                       block_type VARCHAR(15) NOT NULL,
                                       content TEXT,
                                       file_url VARCHAR(1024),
                                       alt_text VARCHAR(255),
                                       created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       CONSTRAINT fk_lesson_content_blocks_lesson_id
                                           FOREIGN KEY(lesson_id)
                                               REFERENCES lessons(id)
                                               ON DELETE CASCADE
);
COMMENT ON COLUMN lesson_content_blocks.content IS 'For TEXT, HEADER (text), LINK (URL)';
COMMENT ON COLUMN lesson_content_blocks.file_url IS 'For IMAGE, VIDEO stored in Minio';
COMMENT ON COLUMN lesson_content_blocks.alt_text IS 'For IMAGE accessibility';


CREATE TABLE tests (
                       id BIGSERIAL PRIMARY KEY,
                       lesson_id BIGINT UNIQUE NOT NULL,
                       title VARCHAR(255) NOT NULL DEFAULT 'Test',
                       pass_threshold_percentage INT NOT NULL DEFAULT 70,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       CONSTRAINT fk_tests_lesson_id
                           FOREIGN KEY(lesson_id)
                               REFERENCES lessons(id)
                               ON DELETE CASCADE
);
COMMENT ON COLUMN tests.lesson_id IS 'One test per lesson';
COMMENT ON COLUMN tests.pass_threshold_percentage IS 'e.g., 70 for 70%';

CREATE TABLE questions (
                           id BIGSERIAL PRIMARY KEY,
                           test_id BIGINT NOT NULL,
                           text TEXT NOT NULL,
                           sequence_order INT NOT NULL DEFAULT 0,
                           max_score INT NOT NULL DEFAULT 2,
                           created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           CONSTRAINT fk_questions_test_id
                               FOREIGN KEY(test_id)
                                   REFERENCES tests(id)
                                   ON DELETE CASCADE
);
COMMENT ON COLUMN questions.text IS 'The question itself';
COMMENT ON COLUMN questions.sequence_order IS 'For ordering questions within a test';
COMMENT ON COLUMN questions.max_score IS 'Max points for this question (e.g., 2 for "CORRECT")';

CREATE INDEX idx_questions_test_id_sequence_order
    ON questions(test_id, sequence_order);

CREATE TABLE user_lesson_progress (
                                      id BIGSERIAL PRIMARY KEY,
                                      user_id BIGINT NOT NULL,
                                      lesson_id BIGINT NOT NULL,
                                      status lesson_progress_status NOT NULL DEFAULT 'LOCKED',
                                      unlocked_at TIMESTAMPTZ,
                                      started_at TIMESTAMPTZ,
                                      test_pending_at TIMESTAMPTZ,
                                      completed_at TIMESTAMPTZ,
                                      created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      CONSTRAINT fk_user_lesson_progress_user_id
                                          FOREIGN KEY(user_id)
                                              REFERENCES users(id)
                                              ON DELETE CASCADE,
                                      CONSTRAINT fk_user_lesson_progress_lesson_id
                                          FOREIGN KEY(lesson_id)
                                              REFERENCES lessons(id)
                                              ON DELETE CASCADE,
                                      CONSTRAINT uq_user_lesson_progress_user_id_lesson_id
                                          UNIQUE (user_id, lesson_id)
);
COMMENT ON COLUMN user_lesson_progress.started_at IS 'When user first views lesson content';
COMMENT ON COLUMN user_lesson_progress.test_pending_at IS 'When user finishes content and is ready for test';
COMMENT ON COLUMN user_lesson_progress.completed_at IS 'When test for the lesson is passed';
COMMENT ON COLUMN user_lesson_progress.created_at IS 'When this record was first created (usually when unlocked)';

CREATE TABLE user_test_attempts (
                                    id BIGSERIAL PRIMARY KEY,
                                    user_id BIGINT NOT NULL,
                                    test_id BIGINT NOT NULL,
                                    attempt_number INT NOT NULL DEFAULT 1,
                                    score_achieved DECIMAL(5,2),
                                    max_possible_score DECIMAL(5,2),
                                    is_passed BOOLEAN DEFAULT FALSE,
                                    attempted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    CONSTRAINT fk_user_test_attempts_user_id
                                        FOREIGN KEY(user_id)
                                            REFERENCES users(id)
                                            ON DELETE CASCADE,
                                    CONSTRAINT fk_user_test_attempts_test_id
                                        FOREIGN KEY(test_id)
                                            REFERENCES tests(id)
                                            ON DELETE CASCADE,
                                    CONSTRAINT uq_user_test_attempts_user_id_test_id_attempt_number
                                        UNIQUE (user_id, test_id, attempt_number)
);
COMMENT ON COLUMN user_test_attempts.score_achieved IS 'Actual score sum from answers';
COMMENT ON COLUMN user_test_attempts.max_possible_score IS 'Max possible score for this test based on its questions';

-- Table: user_answers
CREATE TABLE user_answers (
                              id BIGSERIAL PRIMARY KEY,
                              user_test_attempt_id BIGINT NOT NULL,
                              question_id BIGINT NOT NULL,
                              answer_text TEXT,
                              answer_audio_s3_key VARCHAR(1024),
                              score_awarded answer_score_type,
                              points_earned INT,
                              ai_feedback TEXT,
                              submitted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              CONSTRAINT fk_user_answers_user_test_attempt_id
                                  FOREIGN KEY(user_test_attempt_id)
                                      REFERENCES user_test_attempts(id)
                                      ON DELETE CASCADE,
                              CONSTRAINT fk_user_answers_question_id
                                  FOREIGN KEY(question_id)
                                      REFERENCES questions(id)
                                      ON DELETE CASCADE,
                              CONSTRAINT uq_user_answers_attempt_id_question_id
                                  UNIQUE (user_test_attempt_id, question_id)
);
COMMENT ON COLUMN user_answers.answer_text IS 'If text answer';
COMMENT ON COLUMN user_answers.answer_audio_s3_key IS 'If voice answer, path in Minio';
COMMENT ON COLUMN user_answers.score_awarded IS 'Enum representing 0, 1, or 2 points from AI';
COMMENT ON COLUMN user_answers.points_earned IS 'Actual numerical points (0, 1, 2) corresponding to score_awarded';
COMMENT ON COLUMN user_answers.ai_feedback IS 'Specific feedback from AI for this answer';
COMMENT ON INDEX uq_user_answers_attempt_id_question_id IS 'One answer per question per attempt';

-- --- TRIGGERS ---

CREATE OR REPLACE FUNCTION trigger_set_timestamp()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_timestamp_users BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER set_timestamp_modules BEFORE UPDATE ON modules FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER set_timestamp_lessons BEFORE UPDATE ON lessons FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER set_timestamp_lesson_content_blocks BEFORE UPDATE ON lesson_content_blocks FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER set_timestamp_tests BEFORE UPDATE ON tests FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER set_timestamp_questions BEFORE UPDATE ON questions FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER set_timestamp_user_lesson_progress BEFORE UPDATE ON user_lesson_progress FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER set_timestamp_user_test_attempts BEFORE UPDATE ON user_test_attempts FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
CREATE TRIGGER set_timestamp_user_answers BEFORE UPDATE ON user_answers FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();