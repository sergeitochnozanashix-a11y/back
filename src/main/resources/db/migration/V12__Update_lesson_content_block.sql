ALTER TABLE lesson_content_blocks
    ADD COLUMN sequence_order INTEGER NOT NULL DEFAULT 0;

ALTER TABLE lesson_content_blocks
    ADD COLUMN properties JSONB;

ALTER TABLE lesson_content_blocks
    ADD COLUMN parent_id BIGINT;

ALTER TABLE lesson_content_blocks
    ADD CONSTRAINT fk_parent_block
        FOREIGN KEY (parent_id) REFERENCES lesson_content_blocks (id)
            ON DELETE CASCADE;

UPDATE lesson_content_blocks
SET block_type = 'HEADING'
WHERE block_type = 'HEADER';