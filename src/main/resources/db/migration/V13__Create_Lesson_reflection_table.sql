CREATE TABLE lesson_reflection (
                                   id BIGSERIAL PRIMARY KEY,
                                   user_id BIGINT NOT NULL REFERENCES users(id),
                                   course_id BIGINT NOT NULL REFERENCES courses(id),
                                   module_id BIGINT NOT NULL REFERENCES modules(id),
                                   lesson_id BIGINT NOT NULL REFERENCES lessons(id),
                                   file_url VARCHAR(1024) NOT NULL,
                                   transcript_text TEXT,
                                   duration_sec INT,
                                   status VARCHAR(50) NOT NULL DEFAULT 'UPLOADED',
                                   attempt_no INT NOT NULL DEFAULT 1,
                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   failed_reason VARCHAR(255),
                                   CONSTRAINT uk_lr_user_lesson_attempt UNIQUE (user_id, lesson_id, attempt_no)
);

CREATE INDEX idx_lr_user_lesson_created ON lesson_reflection (user_id, lesson_id, created_at DESC);
CREATE INDEX idx_lr_course_user ON lesson_reflection (course_id, user_id);
CREATE INDEX idx_lr_status ON lesson_reflection (status);