ALTER TABLE questions RENAME TO test_question;

CREATE TABLE faq_questions (
                               id BIGSERIAL PRIMARY KEY,
                               lesson_id BIGINT NOT NULL,
                               author_id BIGINT NOT NULL,
                               text TEXT NOT NULL,
                               created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (lesson_id) REFERENCES lessons(id),
    FOREIGN KEY (author_id) REFERENCES users(id)
);

ALTER TABLE faq_questions ADD COLUMN tsvector_col TSVECTOR;
-- Создаем GIN-индекс для tsvector-столбца
CREATE INDEX faq_questions_text_gin_idx ON faq_questions USING GIN (tsvector_col);
-- Создаем триггер для автоматического обновления tsvector_col при изменении 'text'
CREATE TRIGGER tsvector_faq_questions_update
    BEFORE INSERT OR UPDATE ON faq_questions
    FOR EACH ROW EXECUTE FUNCTION
    tsvector_update_trigger(tsvector_col, 'pg_catalog.english', text);


-- Таблица для ответов FAQ
CREATE TABLE faq_answers (
                             id BIGSERIAL PRIMARY KEY,
                             question_id BIGINT NOT NULL,
                             author_id BIGINT NOT NULL,
                             text TEXT NOT NULL,
                             created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

                             FOREIGN KEY (question_id) REFERENCES faq_questions(id) ON DELETE CASCADE,
                             FOREIGN KEY (author_id) REFERENCES users(id)
);

-- Добавление индекса для полнотекстового поиска по полю 'text' в faq_answers
-- Создаем tsvector-столбец
ALTER TABLE faq_answers ADD COLUMN tsvector_col TSVECTOR;
-- Создаем GIN-индекс для tsvector-столбца
CREATE INDEX faq_answers_text_gin_idx ON faq_answers USING GIN (tsvector_col);
-- Создаем триггер для автоматического обновления tsvector_col при изменении 'text'
CREATE TRIGGER tsvector_faq_answers_update
    BEFORE INSERT OR UPDATE ON faq_answers
    FOR EACH ROW EXECUTE FUNCTION
    tsvector_update_trigger(tsvector_col, 'pg_catalog.english', text);