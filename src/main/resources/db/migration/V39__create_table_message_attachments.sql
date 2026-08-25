CREATE TABLE message_attachments
(
    id                BIGSERIAL PRIMARY KEY,
    chat_message_id   BIGINT                                 NOT NULL,
    download_url      VARCHAR(2048)                          NOT NULL,
    original_filename VARCHAR(255)                           NOT NULL,
    content_type      VARCHAR(128)                           NOT NULL,
    file_size         BIGINT                                 NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created_by        VARCHAR(255),
    updated_by        VARCHAR(255),

    CONSTRAINT fk_attachments_to_message
        FOREIGN KEY (chat_message_id) REFERENCES chat_messages (id)
            ON DELETE CASCADE
);

COMMENT ON TABLE message_attachments IS 'Вложения (файлы), прикрепленные к сообщениям в чате';