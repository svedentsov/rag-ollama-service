-- Эта миграция НЕ требуется для реализации с ConcurrentHashMap,
-- но она была бы необходима для распределенного rate-limiting (например, с Bucket4j + Hazelcast/Redis).
-- Этот файл как пример, но для текущей реализации он не активен.
-- Для активации потребуется добавить зависимость bucket4j-jcache и настроить JCache провайдера (e.g. Hazelcast).

-- CREATE TABLE IF NOT EXISTS rate_limit_buckets (
--     id VARCHAR(255) PRIMARY KEY,
--     bucket_state BYTEA NOT NULL
-- );

-- Заглушка, чтобы Flyway не ругался на пустой файл
SELECT 1;
