ALTER TABLE lesson_reflection
    DROP CONSTRAINT IF EXISTS lesson_reflection_user_id_lesson_id_attempt_no_key;
DROP INDEX IF EXISTS idx_lr_course_user;

ALTER TABLE lesson_reflection
    DROP COLUMN IF EXISTS course_id,
    DROP COLUMN IF EXISTS module_id,
    DROP COLUMN IF EXISTS duration_sec,
    DROP COLUMN IF EXISTS attempt_no;

ALTER TABLE lesson_reflection
    RENAME COLUMN file_url TO audio_url;
ALTER TABLE lesson_reflection
    RENAME COLUMN transcript_text TO question_text;
ALTER TABLE lesson_reflection
    RENAME COLUMN failed_reason TO processing_error;

ALTER TABLE lesson_reflection
    ADD COLUMN ai_response_text TEXT;
ALTER TABLE lesson_reflection
    ALTER COLUMN processing_error TYPE TEXT;

TRUNCATE TABLE lesson_reflection RESTART IDENTITY;
