-- Добавляем ограничение внешнего ключа для обеспечения целостности данных.
-- Теперь сообщение не может существовать без сессии.
-- ON DELETE CASCADE гарантирует, что при удалении сессии все ее сообщения будут удалены автоматически на уровне БД.
ALTER TABLE public.chat_messages
ADD CONSTRAINT fk_chat_messages_session
FOREIGN KEY (session_id) REFERENCES public.chat_sessions(session_id)
ON DELETE CASCADE;