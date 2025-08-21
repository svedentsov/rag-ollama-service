-- Убедимся, что используем схему public
SET search_path = public;

-- Шаг 1: Добавляем в таблицу `vector_store` новый столбец типа tsvector.
-- Этот тип данных специально предназначен для хранения предварительно
-- обработанного текста для полнотекстового поиска (лексемы, стоп-слова и т.д.).
ALTER TABLE vector_store ADD COLUMN IF NOT EXISTS content_tsv tsvector;

-- Шаг 2: Создаем функцию, которая будет вызываться триггером.
-- Эта функция берет текстовое поле 'content', преобразует его в tsvector
-- (используя конфигурацию для русского языка 'russian') и сохраняет
-- в поле 'content_tsv'.
CREATE OR REPLACE FUNCTION update_content_tsv()
RETURNS TRIGGER AS $$
BEGIN
    NEW.content_tsv := to_tsvector('russian', NEW.content);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Шаг 3: Создаем триггер, который будет автоматически вызывать нашу функцию
-- перед каждой операцией вставки (INSERT) или обновления (UPDATE) в таблице.
-- Это гарантирует, что наш поисковый индекс всегда будет актуален.
DROP TRIGGER IF EXISTS tsvectorupdate ON vector_store;
CREATE TRIGGER tsvectorupdate
BEFORE INSERT OR UPDATE ON vector_store
FOR EACH ROW EXECUTE FUNCTION update_content_tsv();

-- Шаг 4: Создаем GIN (Generalized Inverted Index) индекс на нашем tsvector-столбце.
-- GIN-индексы идеально подходят для полнотекстового поиска и обеспечивают
-- высочайшую производительность.
CREATE INDEX IF NOT EXISTS idx_content_tsv ON vector_store USING GIN(content_tsv);

-- Шаг 5: (Опционально, но рекомендуется) Единоразово заполняем
-- столбец content_tsv для уже существующих данных в таблице.
UPDATE vector_store SET content_tsv = to_tsvector('russian', content) WHERE content_tsv IS NULL;
