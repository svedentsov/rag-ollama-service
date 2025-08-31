-- Добавляем колонку для хранения истории выполненных шагов в динамических конвейерах.
-- Тип JSONB является оптимальным для хранения структурированных, но гибких данных.
ALTER TABLE pipeline_executions
ADD COLUMN IF NOT EXISTS execution_history JSONB;

-- Добавляем булев флаг для отслеживания возобновления после утверждения человеком.
-- NOT NULL DEFAULT FALSE обеспечивает целостность данных для существующих и новых записей.
ALTER TABLE pipeline_executions
ADD COLUMN IF NOT EXISTS resumed_after_approval BOOLEAN NOT NULL DEFAULT FALSE;