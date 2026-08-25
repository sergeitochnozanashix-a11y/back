ALTER TABLE chat_messages
    ADD COLUMN voice_transcript TEXT;

COMMENT ON COLUMN chat_messages.voice_transcript IS 'Расшифрованный текст из прикрепленного аудиофайла';