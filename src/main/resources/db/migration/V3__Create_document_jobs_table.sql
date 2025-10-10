-- V3__Create_document_jobs_table.sql

CREATE TABLE IF NOT EXISTS document_jobs
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_name   VARCHAR(255)             NOT NULL,
    status        VARCHAR(50)              NOT NULL,
    text_content  TEXT                     NOT NULL,
    error_message TEXT,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_document_jobs_status ON document_jobs (status);