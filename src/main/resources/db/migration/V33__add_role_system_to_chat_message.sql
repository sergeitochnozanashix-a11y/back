DO
$$
    DECLARE
        constraint_name TEXT;
    BEGIN
        SELECT conname
        INTO constraint_name
        FROM pg_constraint
        WHERE conrelid = 'chat_messages'::regclass
          AND contype = 'c'
          AND conname LIKE 'chat_messages_role_check%';

        IF constraint_name IS NOT NULL THEN
            EXECUTE 'ALTER TABLE chat_messages DROP CONSTRAINT ' || quote_ident(constraint_name);
            RAISE NOTICE 'Dropped constraint: %', constraint_name;
        ELSE
            RAISE NOTICE 'Constraint for role check not found, skipping drop.';
        END IF;
    END
$$;

ALTER TABLE chat_messages
    ADD CONSTRAINT chat_messages_role_check
        CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM'));

COMMENT ON COLUMN chat_messages.role IS 'Роль автора сообщения: USER, ASSISTANT или SYSTEM';