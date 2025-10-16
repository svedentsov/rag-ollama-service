-- V28__Create_File_Metadata_Table.sql

-- Таблица для хранения метаданных о загруженных пользователями файлах.
CREATE TABLE IF NOT EXISTS file_metadata
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_name   VARCHAR(255)             NOT NULL,
    file_name   VARCHAR(255)             NOT NULL,
    file_path   VARCHAR(1024)            NOT NULL,
    mime_type   VARCHAR(100)             NOT NULL,
    file_size   BIGINT                   NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Индекс для быстрого поиска всех файлов конкретного пользователя.
CREATE INDEX IF NOT EXISTS idx_file_metadata_user_name ON file_metadata (user_name);

-- Уникальный индекс, чтобы один пользователь не мог загрузить два файла с одинаковым именем.
CREATE UNIQUE INDEX IF NOT EXISTS idx_file_metadata_user_name_file_name ON file_metadata (user_name, file_name);