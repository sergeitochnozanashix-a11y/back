-- Профильные данные вынесены из users отдельной таблицей: сущность User
-- реализует UserDetails и загружается целиком при проверке JWT на каждом
-- запросе. Держать в ней девять необязательных полей значило бы тянуть их
-- из базы постоянно, хотя нужны они только на странице профиля.
--
-- Первичный ключ совпадает с users.id (@MapsId), поэтому отдельная
-- последовательность не нужна, а связь один-к-одному гарантирована схемой.
CREATE TABLE user_profiles
(
    user_id    BIGINT PRIMARY KEY,
    first_name VARCHAR(100),
    last_name  VARCHAR(100),
    gender     VARCHAR(10),
    phone      VARCHAR(32),
    address    VARCHAR(255),
    birth_date DATE,
    country    VARCHAR(100),
    city       VARCHAR(100),
    -- objectKey из FileUploadResponse; сам файл лежит в S3, отдаётся через
    -- GET /api/v1/file-storage/download/{avatarKey}
    avatar_key VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_user_profiles_gender CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE'))
);
