-- Шаг 1: Создаем таблицу для задач по обработке документов.
CREATE TABLE IF NOT EXISTS document_jobs
(
    id            UUID PRIMARY KEY,
    source_name   VARCHAR(255)             NOT NULL,
    status        VARCHAR(50)              NOT NULL,
    text_content  TEXT                     NOT NULL,
    error_message TEXT,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Шаг 2: Создаем индекс по полю 'status' для быстрого поиска задач в очереди.
-- Индекс создается отдельно от таблицы, что является стандартным синтаксисом SQL.
CREATE INDEX IF NOT EXISTS idx_document_jobs_status ON document_jobs (status);
