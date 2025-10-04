-- Сначала удаляем существующее ограничение внешнего ключа
ALTER TABLE public.chat_messages
DROP CONSTRAINT IF EXISTS fk_chat_messages_session;

-- Затем создаем его заново, но уже БЕЗ 'ON DELETE CASCADE'
ALTER TABLE public.chat_messages
ADD CONSTRAINT fk_chat_messages_session
FOREIGN KEY (session_id) REFERENCES public.chat_sessions(session_id);

COMMENT ON CONSTRAINT fk_chat_messages_session ON public.chat_messages IS 'Связь с сессией чата. Каскадное удаление теперь управляется JPA.';