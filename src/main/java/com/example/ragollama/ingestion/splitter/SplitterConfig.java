package com.example.ragollama.ingestion.splitter;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO-record для инкапсуляции параметров чанкинга.
 * <p>
 * Позволяет гибко настраивать процесс разделения текста для разных
 * типов документов, передавая этот объект в {@link com.example.ragollama.ingestion.TextSplitterService}.
 *
 * @param chunkSize     Целевой размер одного чанка в токенах.
 * @param chunkOverlap  Размер пересечения между чанками в токенах. Это помогает
 *                      сохранять семантический контекст на стыках чанков.
 * @param delimiters    Список строковых разделителей (регулярных выражений),
 *                      используемых для рекурсивного разделения текста.
 *                      Стратегия будет применять их последовательно, от более
 *                      крупных (например, параграфы) к более мелким (предложения).
 */
@Schema(description = "Конфигурация для процесса разделения текста на чанки")
public record SplitterConfig(
        int chunkSize,
        int chunkOverlap,
        List<String> delimiters
) {
}
