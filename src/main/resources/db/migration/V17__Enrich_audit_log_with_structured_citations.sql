-- Удаляем старую колонку, чтобы избежать путаницы
ALTER TABLE rag_audit_log
DROP COLUMN IF EXISTS context_documents;

-- Добавляем новую колонку для хранения полного, структурированного списка цитат
ALTER TABLE rag_audit_log
ADD COLUMN IF NOT EXISTS source_citations JSONB;

COMMENT ON COLUMN rag_audit_log.source_citations IS 'Полный список структурированных цитат (SourceCitation), включая текст, метаданные и версию, использованных для генерации ответа.';