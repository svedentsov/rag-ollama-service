-- Таблица для хранения сырых метрик по каждому тестовому прогону.
CREATE TABLE IF NOT EXISTS test_run_metrics
(
    id             UUID PRIMARY KEY,
    commit_hash    VARCHAR(40)              NOT NULL,
    branch_name    VARCHAR(255),
    total_count    INTEGER                  NOT NULL,
    passed_count   INTEGER                  NOT NULL,
    failed_count   INTEGER                  NOT NULL,
    skipped_count  INTEGER                  NOT NULL,
    duration_ms    BIGINT                   NOT NULL,
    run_timestamp  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_metrics_commit_hash ON test_run_metrics (commit_hash);
CREATE INDEX IF NOT EXISTS idx_metrics_run_timestamp ON test_run_metrics (run_timestamp);