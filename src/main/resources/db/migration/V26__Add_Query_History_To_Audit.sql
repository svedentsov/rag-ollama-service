-- Добавляем колонку для хранения полной истории трансформации запроса.
-- Это ключевой элемент для обеспечения полной наблюдаемости RAG-конвейера.
ALTER TABLE public.rag_audit_log
ADD COLUMN IF NOT EXISTS query_formation_history JSONB;

COMMENT ON COLUMN public.rag_audit_log.query_formation_history IS 'Пошаговая история трансформации исходного запроса пользователя.';