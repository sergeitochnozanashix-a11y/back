ALTER TABLE chat_messages
    ADD COLUMN audio_file_url VARCHAR(256);

COMMENT ON COLUMN chat_messages.audio_file_url IS 'URL for the attached audio file';
