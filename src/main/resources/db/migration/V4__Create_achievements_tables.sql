-- V4__Create_Achievements_Tables.sql

-- Create the achievements table
CREATE TABLE achievements (
                              id BIGSERIAL PRIMARY KEY,
                              title VARCHAR(255) NOT NULL UNIQUE,
                              description TEXT,
                              icon_url VARCHAR(255),
                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create the user_achievements join table
CREATE TABLE user_achievements (
                                   user_id BIGINT NOT NULL,
                                   achievement_id BIGINT NOT NULL,
                                   awarded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   PRIMARY KEY (user_id, achievement_id),
                                   FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                                   FOREIGN KEY (achievement_id) REFERENCES achievements (id) ON DELETE CASCADE
);

-- Add index to user_id for faster lookups of user's achievements
CREATE INDEX idx_user_achievements_user_id ON user_achievements (user_id);

-- Add index to achievement_id for faster lookups of achievement's users
CREATE INDEX idx_user_achievements_achievement_id ON user_achievements (achievement_id);