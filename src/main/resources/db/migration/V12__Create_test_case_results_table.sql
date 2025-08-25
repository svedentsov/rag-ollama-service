-- Таблица для хранения гранулярных результатов по каждому тест-кейсу.
-- Это позволит выполнять глубокий исторический анализ стабильности.
CREATE TABLE IF NOT EXISTS test_case_run_results
(
    id                UUID PRIMARY KEY,
    test_run_id       UUID REFERENCES test_run_metrics(id) ON DELETE CASCADE,
    class_name        VARCHAR(512) NOT NULL,
    test_name         VARCHAR(512) NOT NULL,
    status            VARCHAR(50)  NOT NULL,
    failure_details   TEXT,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_test_case_name ON test_case_run_results (class_name, test_name);