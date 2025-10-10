-- Добавляем ограничение внешнего ключа для обеспечения целостности данных.
-- Теперь сообщение не может существовать без сессии.
DO $$
BEGIN
    -- Сначала удаляем старое ограничение, если оно есть, чтобы избежать конфликтов
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_chat_messages_session') THEN
        ALTER TABLE public.chat_messages DROP CONSTRAINT fk_chat_messages_session;
    END IF;

    -- Добавляем новое, корректное ограничение
    ALTER TABLE public.chat_messages
    ADD CONSTRAINT fk_chat_messages_session
    FOREIGN KEY (session_id) REFERENCES public.chat_sessions(session_id)
    ON DELETE CASCADE;
END;
$$;