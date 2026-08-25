CREATE TABLE chats
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT                                 NOT NULL,
    title      VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),

    CONSTRAINT fk_chats_user
        FOREIGN KEY (user_id) REFERENCES users (id)
            ON DELETE CASCADE
);

COMMENT ON TABLE chats IS 'Чаты пользователей (история диалогов)';
COMMENT ON COLUMN chats.user_id IS 'FK на users.id';
COMMENT ON COLUMN chats.title IS 'Название чата, можно генерировать из первого сообщения';