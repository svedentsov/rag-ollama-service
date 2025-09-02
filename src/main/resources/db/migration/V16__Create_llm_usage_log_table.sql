-- Таблица для хранения детальных логов использования LLM.
-- Является источником данных для FinOps-аналитики и контроля квот.
CREATE TABLE IF NOT EXISTS llm_usage_log
(
    id                 UUID PRIMARY KEY,
    username           VARCHAR(255)             NOT NULL,
    model_name         VARCHAR(255)             NOT NULL,
    prompt_tokens      BIGINT                   NOT NULL,
    completion_tokens  BIGINT                   NOT NULL,
    total_tokens       BIGINT                   NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Индексы для ускорения агрегирующих запросов при проверке квот и построении отчетов.
CREATE INDEX IF NOT EXISTS idx_llm_usage_log_username_created_at ON llm_usage_log (username, created_at);
CREATE INDEX IF NOT EXISTS idx_llm_usage_log_created_at ON llm_usage_log (created_at);