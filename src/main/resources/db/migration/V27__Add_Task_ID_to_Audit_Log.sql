-- Добавляем колонку для связи аудиторской записи с асинхронной задачей,
-- в рамках которой она была создана.
ALTER TABLE public.rag_audit_log
ADD COLUMN IF NOT EXISTS task_id UUID NULL;

COMMENT ON COLUMN public.rag_audit_log.task_id IS 'ID асинхронной задачи (из таблицы async_tasks), которая инициировала это RAG-взаимодействие.';

-- Добавляем ограничение внешнего ключа. ON DELETE SET NULL означает,
-- что если задача будет удалена из таблицы async_tasks,
-- то поле task_id в логе просто станет NULL, но сама запись не удалится.
-- Используем DO-блок для идемпотентности, чтобы избежать ошибок при повторном запуске.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_rag_audit_log_task') THEN
        ALTER TABLE public.rag_audit_log
        ADD CONSTRAINT fk_rag_audit_log_task
        FOREIGN KEY (task_id) REFERENCES public.async_tasks(id)
        ON DELETE SET NULL;
    END IF;
END;
$$;


-- Индекс для быстрого поиска аудиторской записи по ID задачи,
-- что критически важно для обогащения истории чата.
CREATE INDEX IF NOT EXISTS idx_audit_log_task_id ON public.rag_audit_log (task_id);