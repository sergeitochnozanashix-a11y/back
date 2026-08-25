
CREATE TABLE interviews (
                            id BIGSERIAL PRIMARY KEY,
                            title VARCHAR(255) NOT NULL,
                            description TEXT,
                            video_url VARCHAR(1024),
                            direction VARCHAR(20) NOT NULL,
                            grade VARCHAR(20) NOT NULL,
                            created_at TIMESTAMP NOT NULL,
                            updated_at TIMESTAMP NOT NULL
);