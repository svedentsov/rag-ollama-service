-- Добавляем колонку для связи сообщения с асинхронной задачей,
-- которая его сгенерировала. Это необходимо для надежной системы фидбэка.
ALTER TABLE public.chat_messages
ADD COLUMN IF NOT EXISTS task_id UUID NULL;

-- Добавляем ограничение внешнего ключа. ON DELETE SET NULL означает,
-- что если задача будет удалена из таблицы async_tasks,
-- то поле task_id в сообщении просто станет NULL, но само сообщение не удалится.
ALTER TABLE public.chat_messages
ADD CONSTRAINT fk_chat_messages_task
FOREIGN KEY (task_id) REFERENCES public.async_tasks(id)
ON DELETE SET NULL;

-- Индекс для возможного поиска всех сообщений, сгенерированных одной задачей.
CREATE INDEX IF NOT EXISTS idx_chat_messages_task_id ON public.chat_messages (task_id);