BEGIN;

-- 1) Сносим старое ограничение, если оно есть
ALTER TABLE test_answers
    DROP CONSTRAINT IF EXISTS chk_tans_text_only;

-- 2) Ставим новый согласованный CHECK
--    Допускаются три валидных состояния:
--      A) input_type IS NULL  => все поля ввода NULL (неотвечено)
--      B) input_type = 'TEXT' => text_answer NOT NULL, voice_* NULL
--      C) input_type = 'VOICE'=> voice_file_url NOT NULL, text_answer NULL (transcript опционален)
DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1
                       FROM pg_constraint
                       WHERE conrelid = 'test_answers'::regclass
                         AND conname = 'chk_test_answers_input_coherence') THEN
            ALTER TABLE test_answers
                ADD CONSTRAINT chk_test_answers_input_coherence CHECK (
                    (
                        input_type IS NULL
                            AND text_answer IS NULL
                            AND voice_file_url IS NULL
                            AND voice_transcript IS NULL
                        )
                        OR
                    (
                        input_type = 'TEXT'
                            AND text_answer IS NOT NULL
                            AND voice_file_url IS NULL
                            AND voice_transcript IS NULL
                        )
                        OR
                    (
                        input_type = 'VOICE'
                            AND text_answer IS NULL
                            AND voice_file_url IS NOT NULL
                        )
                    );
        END IF;
    END
$$;

COMMIT;
