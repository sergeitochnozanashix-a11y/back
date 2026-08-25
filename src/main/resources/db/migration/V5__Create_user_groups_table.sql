-- V5__Create_User_Groups_Tables.sql

-- Create the user_groups table
CREATE TABLE user_groups (
                             id BIGSERIAL PRIMARY KEY,
                             name VARCHAR(255) NOT NULL UNIQUE,
                             description TEXT,
                             owner_id BIGINT NOT NULL,
                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
);


-- Create the user_group_memberships join table
CREATE TABLE user_group_memberships (
                                        user_id BIGINT NOT NULL,
                                        group_id BIGINT NOT NULL,
                                        joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        PRIMARY KEY (user_id, group_id), -- Composite primary key
                                        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                                        FOREIGN KEY (group_id) REFERENCES user_groups (id) ON DELETE CASCADE
);

-- Add index to user_id for faster lookups of user's groups
CREATE INDEX idx_user_group_memberships_user_id ON user_group_memberships (user_id);

-- Add index to group_id for faster lookups of group's members
CREATE INDEX idx_user_group_memberships_group_id ON user_group_memberships (group_id);