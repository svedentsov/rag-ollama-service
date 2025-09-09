-- Таблица для хранения истории прогонов оценки RAG
CREATE TABLE IF NOT EXISTS evaluation_history
(
    id                   UUID PRIMARY KEY,
    f1_score             DOUBLE PRECISION         NOT NULL,
    recall               DOUBLE PRECISION         NOT NULL,
    precision            DOUBLE PRECISION         NOT NULL,
    mrr                  DOUBLE PRECISION         NOT NULL,
    ndcg_at_5            DOUBLE PRECISION         NOT NULL,
    total_records        INTEGER                  NOT NULL,
    triggering_source_id VARCHAR(255), -- ID документа/задачи, вызвавшей оценку
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Индекс для быстрого поиска последнего успешного прогона
CREATE INDEX IF NOT EXISTS idx_evaluation_history_created_at ON evaluation_history (created_at DESC);