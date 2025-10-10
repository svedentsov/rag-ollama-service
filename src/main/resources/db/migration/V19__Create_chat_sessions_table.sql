-- Таблица для хранения метаданных о сессиях чата
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS chat_sessions
(
    session_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_name  VARCHAR(255)             NOT NULL,
    chat_name  VARCHAR(255)             NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Индекс для быстрого поиска всех чатов конкретного пользователя
CREATE INDEX IF NOT EXISTS idx_chat_sessions_user_name ON chat_sessions (user_name);