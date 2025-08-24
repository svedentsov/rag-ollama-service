-- Таблица для хранения сырой обратной связи от пользователей.
CREATE TABLE IF NOT EXISTS feedback_log
(
    id          UUID PRIMARY KEY,
    request_id  VARCHAR(36)              NOT NULL,
    is_helpful  BOOLEAN                  NOT NULL,
    user_comment TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_feedback_log_request_id ON feedback_log (request_id);

-- Таблица для хранения готовых обучающих пар для дообучения моделей.
CREATE TABLE IF NOT EXISTS training_data_pairs
(
    id                UUID PRIMARY KEY,
    query_text        TEXT    NOT NULL,
    document_id       UUID    NOT NULL,
    -- 'positive' или 'negative'
    label             VARCHAR(50) NOT NULL,
    -- ID обратной связи, которая сгенерировала эту пару
    source_feedback_id UUID REFERENCES feedback_log(id),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_training_data_label ON training_data_pairs (label);