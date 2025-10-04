-- Добавляем ограничение внешнего ключа для обеспечения целостности данных.
-- Теперь сообщение не может существовать без сессии.
-- ON DELETE CASCADE гарантирует, что при удалении сессии все ее сообщения будут удалены автоматически на уровне БД.
-- Оборачиваем в DO-блок для идемпотентности, чтобы миграция не падала при повторном запуске.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_chat_messages_session') THEN
        ALTER TABLE public.chat_messages
        ADD CONSTRAINT fk_chat_messages_session
        FOREIGN KEY (session_id) REFERENCES public.chat_sessions(session_id);
    END IF;
END;
$$;