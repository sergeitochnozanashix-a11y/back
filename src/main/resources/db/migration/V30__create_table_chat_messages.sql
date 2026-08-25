CREATE TABLE chat_messages
(
    id         BIGSERIAL PRIMARY KEY,
    chat_id    BIGINT                                 NOT NULL,
    role       VARCHAR(32)                            NOT NULL CHECK (role IN ('USER', 'ASSISTANT')),
    content    TEXT                                   NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),

    CONSTRAINT fk_chat_messages_chat
        FOREIGN KEY (chat_id) REFERENCES chats (id)
            ON DELETE CASCADE
);

COMMENT ON TABLE chat_messages IS 'Сообщения в рамках чатов (диалог с GPT)';
COMMENT ON COLUMN chat_messages.role IS 'Роль автора сообщения: USER или ASSISTANT';
COMMENT ON COLUMN chat_messages.content IS 'Текст сообщения';
COMMENT ON COLUMN chat_messages.chat_id IS 'FK на chats.id';