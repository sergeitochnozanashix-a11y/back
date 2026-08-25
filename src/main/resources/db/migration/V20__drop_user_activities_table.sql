-- Удаляем индексы (если они ещё существуют)
DROP INDEX IF EXISTS idx_user_activity_user_id;
DROP INDEX IF EXISTS idx_user_activity_date;

-- Удаляем таблицу user_activities
DROP TABLE IF EXISTS user_activities CASCADE;
