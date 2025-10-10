-- Добавляем колонку для реализации оптимистичной блокировки в таблицу сообщений.
-- Это предотвратит "потерянные обновления" при одновременном редактировании.
ALTER TABLE public.chat_messages
ADD COLUMN IF NOT EXISTS version BIGINT;

-- Инициализируем значением 0 для всех существующих записей.
UPDATE public.chat_messages
SET version = 0
WHERE version IS NULL;

-- Делаем колонку обязательной для новых записей.
ALTER TABLE public.chat_messages
ALTER COLUMN version SET NOT NULL;

ALTER TABLE public.chat_messages
ALTER COLUMN version SET DEFAULT 0;