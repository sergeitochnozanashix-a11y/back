-- Шаг 1: Добавляем колонку как NULLABLE
ALTER TABLE modules
    ADD COLUMN sequence_order INTEGER;

-- Шаг 2: Разрешаем дубликаты через нумерацию
WITH numbered_modules AS (
    SELECT
        id,
        ROW_NUMBER() OVER (PARTITION BY course_id ORDER BY created_at, id) as new_order
    FROM modules
    WHERE sequence_order IS NULL
)
UPDATE modules m
SET sequence_order = nm.new_order
FROM numbered_modules nm
WHERE m.id = nm.id;

-- Шаг 3: Устанавливаем значение по умолчанию
ALTER TABLE modules
    ALTER COLUMN sequence_order SET DEFAULT 0;

-- Шаг 4: Делаем колонку NOT NULL
ALTER TABLE modules
    ALTER COLUMN sequence_order SET NOT NULL;
