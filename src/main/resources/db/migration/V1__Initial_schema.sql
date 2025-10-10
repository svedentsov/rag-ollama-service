-- V1__Initial_schema.sql
-- Идемпотентная миграция: сначала создаём расширение pgvector (тип vector),
-- затем создаём таблицы и индекс для векторного поиска.

-- Создаём расширение pgcrypto для генерации UUID
CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;
-- Создаём расширение pgvector (тип vector). IF NOT EXISTS безопасен.
CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA public;

-- Убедимся, что используем схему public
SET search_path = public;

-- Таблица для векторного хранилища
CREATE TABLE IF NOT EXISTS vector_store (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content TEXT,
    -- используем JSONB для лучшей производительности и индексации
    metadata JSONB,
    embedding vector(1024)
);

-- Индекс для ускорения поиска по схожести (HNSW / cosine).
CREATE INDEX IF NOT EXISTS idx_vector_store_embedding ON vector_store USING HNSW (embedding vector_cosine_ops);

-- Таблица для хранения истории чата
CREATE TABLE IF NOT EXISTS chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Индекс по session_id для ускорения выборок истории
CREATE INDEX IF NOT EXISTS idx_chat_messages_session_id ON chat_messages(session_id);