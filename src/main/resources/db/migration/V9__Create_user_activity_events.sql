-- Flyway миграция для создания таблицы user_activities

-- Создаем таблицу с использованием ENUM
CREATE TABLE user_activities (
                                 id BIGSERIAL PRIMARY KEY,
                                 user_id BIGINT NOT NULL,
                                 activity_type VARCHAR(255) NOT NULL,
                                 activity_date DATE NOT NULL,
                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_user_activity_user_id ON user_activities (user_id);
CREATE INDEX idx_user_activity_date ON user_activities (activity_date);