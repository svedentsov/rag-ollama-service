-- Таблица для персистентного хранения состояния асинхронных задач
CREATE TABLE IF NOT EXISTS async_tasks
(
    id            UUID PRIMARY KEY,
    session_id    UUID,
    status        VARCHAR(50)              NOT NULL,
    error_message TEXT,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Индекс для быстрого поиска активных задач по сессии
CREATE INDEX IF NOT EXISTS idx_async_tasks_session_id_status ON async_tasks (session_id, status);

-- Индекс для поиска "зависших" задач
CREATE INDEX IF NOT EXISTS idx_async_tasks_status ON async_tasks (status);