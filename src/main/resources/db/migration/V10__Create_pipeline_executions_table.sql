-- Таблица для хранения состояния долгоживущих, динамических конвейеров.
-- Это позволяет реализовать приостановку и возобновление выполнения.
CREATE TABLE IF NOT EXISTS pipeline_executions
(
    id                  UUID PRIMARY KEY,
    session_id          UUID,
    status              VARCHAR(50)              NOT NULL,
    plan_steps          JSONB,
    accumulated_context JSONB,
    current_step_index  INTEGER                  NOT NULL DEFAULT 0,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_executions_status ON pipeline_executions (status);
CREATE INDEX IF NOT EXISTS idx_executions_session_id ON pipeline_executions (session_id);