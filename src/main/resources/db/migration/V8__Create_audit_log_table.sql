-- Создаем таблицу для хранения полного аудиторского следа RAG-взаимодействий.
CREATE TABLE IF NOT EXISTS rag_audit_log
(
    id                UUID PRIMARY KEY,
    request_id        VARCHAR(36),
    session_id        UUID,
    username          VARCHAR(255),
    original_query    TEXT                     NOT NULL,
    -- Используем JSONB для эффективного хранения и запроса списка источников
    context_documents JSONB,
    final_prompt      TEXT,
    llm_answer        TEXT,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Индексы для быстрого поиска по ключевым полям
CREATE INDEX IF NOT EXISTS idx_audit_log_request_id ON rag_audit_log (request_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_session_id ON rag_audit_log (session_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON rag_audit_log (created_at);