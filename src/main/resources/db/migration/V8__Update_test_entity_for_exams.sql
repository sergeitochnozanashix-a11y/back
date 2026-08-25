ALTER TABLE tests
    ADD COLUMN test_type VARCHAR(100) NOT NULL DEFAULT 'LESSON_TEST',
    ADD COLUMN module_id BIGINT;

-- Снимаем ограничение NOT NULL с lesson_id
ALTER TABLE tests
    ALTER COLUMN lesson_id DROP NOT NULL;

-- Добавляем внешние ключи
ALTER TABLE tests
    ADD CONSTRAINT fk_tests_module_id
        FOREIGN KEY(module_id)
            REFERENCES modules(id)
            ON DELETE CASCADE;

-- Обновляем существующие записи: для всех существующих тестов,
-- которые связаны с уроками, устанавливаем тип 'LESSON_TEST'
UPDATE tests SET test_type = 'LESSON_TEST' WHERE lesson_id IS NOT NULL;