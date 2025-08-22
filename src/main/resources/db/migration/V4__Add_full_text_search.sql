DO $$
BEGIN
    -- === ШАГ 1: Проверка и создание кастомного словаря ===
    -- Проверяем наличие словаря в системном каталоге pg_ts_dict
    IF NOT EXISTS (SELECT 1 FROM pg_ts_dict WHERE dictname = 'russian_nostop_dict') THEN
        CREATE TEXT SEARCH DICTIONARY russian_nostop_dict (
            TEMPLATE = snowball,
            Language = russian
        );
    END IF;

    -- === ШАГ 2: Проверка и создание кастомной FTS-конфигурации ===
    -- Проверяем наличие конфигурации в системном каталоге pg_ts_config
    IF NOT EXISTS (SELECT 1 FROM pg_ts_config WHERE cfgname = 'russian_nostop') THEN
        CREATE TEXT SEARCH CONFIGURATION russian_nostop (COPY = russian);
    END IF;
END;
$$;

-- === ШАГ 3: Привязка словаря к типам токенов в конфигурации ===
-- Эта операция идемпотентна, повторное выполнение не вызовет ошибки.
ALTER TEXT SEARCH CONFIGURATION russian_nostop
    ALTER MAPPING FOR hword, hword_part, word
    WITH russian_nostop_dict;

-- === ШАГ 4: Добавление tsvector-колонки в таблицу ===
ALTER TABLE vector_store ADD COLUMN IF NOT EXISTS content_tsv tsvector;

-- === ШАГ 5: Создание функции-триггера с использованием новой конфигурации ===
-- CREATE OR REPLACE FUNCTION является идемпотентной операцией.
CREATE OR REPLACE FUNCTION update_content_tsv()
RETURNS TRIGGER AS $$
BEGIN
    -- Используем полное имя конфигурации со схемой для надежности
    NEW.content_tsv := to_tsvector('public.russian_nostop', NEW.content);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- === ШАГ 6: Создание триггера ===
-- Сначала удаляем, потом создаем — это делает операцию идемпотентной.
DROP TRIGGER IF EXISTS tsvectorupdate ON vector_store;
CREATE TRIGGER tsvectorupdate
BEFORE INSERT OR UPDATE ON vector_store
FOR EACH ROW EXECUTE FUNCTION update_content_tsv();

-- === ШАГ 7: Создание GIN-индекса ===
CREATE INDEX IF NOT EXISTS idx_content_tsv ON vector_store USING GIN(content_tsv);

-- === ШАГ 8: Единоразовое обновление существующих данных ===
-- Перестраиваем tsvector для всех строк, где он еще не был создан,
-- используя новую конфигурацию.
UPDATE vector_store SET content_tsv = to_tsvector('public.russian_nostop', content) WHERE content_tsv IS NULL;