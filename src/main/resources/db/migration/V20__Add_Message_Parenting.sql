-- Добавляем колонку для связи "ответ -> вопрос", что позволит реализовать ветвление.
ALTER TABLE public.chat_messages
ADD COLUMN IF NOT EXISTS parent_id UUID NULL;

-- Добавляем ограничение внешнего ключа, которое ссылается на эту же таблицу.
-- Это позволяет создавать древовидные структуры.
-- ON DELETE CASCADE означает, что при удалении родительского сообщения (вопроса)
-- все дочерние (ответы) также будут удалены.
-- Оборачиваем в DO-блок для идемпотентности, чтобы миграция не падала при повторном запуске.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_chat_messages_parent') THEN
        ALTER TABLE public.chat_messages
        ADD CONSTRAINT fk_chat_messages_parent
        FOREIGN KEY (parent_id) REFERENCES public.chat_messages(id)
        ON DELETE CASCADE;
    END IF;
END;
$$;


-- Индекс для ускорения поиска всех ответов (ветвей) на один вопрос.
CREATE INDEX IF NOT EXISTS idx_chat_messages_parent_id ON public.chat_messages (parent_id);