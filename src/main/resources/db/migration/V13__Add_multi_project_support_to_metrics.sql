-- Добавляем колонку для идентификации проекта в основную таблицу метрик.
-- Она не может быть NULL, так как каждая запись должна принадлежать проекту.
ALTER TABLE test_run_metrics
ADD COLUMN IF NOT EXISTS project_id VARCHAR(255) NOT NULL;

-- Добавляем колонку в таблицу с гранулярными результатами.
ALTER TABLE test_case_run_results
ADD COLUMN IF NOT EXISTS project_id VARCHAR(255) NOT NULL;

-- Создаем индексы для быстрой фильтрации и агрегации по проектам.
CREATE INDEX IF NOT EXISTS idx_test_run_metrics_project_id ON test_run_metrics (project_id);
CREATE INDEX IF NOT EXISTS idx_test_case_run_results_project_id ON test_case_run_results (project_id);