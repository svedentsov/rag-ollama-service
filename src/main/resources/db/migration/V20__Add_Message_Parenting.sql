-- Добавляем колонку для связи "ответ -> вопрос"
ALTER TABLE chat_messages
ADD COLUMN IF NOT EXISTS parent_id UUID NULL;

-- Добавляем ограничение внешнего ключа, которое ссылается на эту же таблицу.
-- Это позволяет создавать древовидные структуры.
-- ON DELETE CASCADE означает, что при удалении родительского сообщения (вопроса)
-- все дочерние (ответы) также будут удалены.
ALTER TABLE chat_messages
ADD CONSTRAINT fk_chat_messages_parent
FOREIGN KEY (parent_id) REFERENCES chat_messages(id)
ON DELETE CASCADE;

-- Индекс для ускорения поиска всех ответов на один вопрос.
CREATE INDEX IF NOT EXISTS idx_chat_messages_parent_id ON chat_messages (parent_id);