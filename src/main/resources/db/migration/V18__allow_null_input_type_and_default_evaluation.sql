BEGIN;

-- 1) Разрешаем отсутствие типа ввода (плейсхолдеры для пропущенных)
ALTER TABLE test_answers
    ALTER COLUMN input_type DROP NOT NULL;

-- 2) Обеспечиваем корректность оценки:
--    2.1. Проставим UNASSESSED там, где evaluation = NULL
UPDATE test_answers
SET evaluation = 'UNASSESSED'
WHERE evaluation IS NULL;

--    2.2. Задаём NOT NULL и DEFAULT на будущее
ALTER TABLE test_answers
    ALTER COLUMN evaluation SET NOT NULL,
    ALTER COLUMN evaluation SET DEFAULT 'UNASSESSED';

COMMIT;
