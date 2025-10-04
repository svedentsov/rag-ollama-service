-- Добавляем колонку для хранения выбора активных веток в формате JSONB.
-- Это позволит сохранять состояние UI между сессиями и устройствами.
ALTER TABLE public.chat_sessions
ADD COLUMN IF NOT EXISTS active_branches JSONB;

COMMENT ON COLUMN public.chat_sessions.active_branches IS 'Хранит выбор активных веток для каждого родительского сообщения. Ключ - parent_message_id, Значение - active_child_message_id.';