package com.example.ragollama.rag.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * Представляет одну полную цитату источника, использованного в RAG-ответе.
 * <p>
 * Этот record является богатым DTO, который несет в себе всю необходимую
 * информацию для обеспечения полной трассируемости и воспроизводимости ответа.
 *
 * @param sourceName      Человекочитаемое имя источника (например, имя файла).
 * @param textSnippet     Точный текстовый фрагмент (чанк), который был использован в контексте.
 * @param metadata        Карта с метаданными источника, включая версионную информацию
 *                        (например, 'commit_sha', 'last_modified').
 * @param chunkId         Уникальный идентификатор чанка для точной ссылки.
 * @param similarityScore Оценка релевантности, присвоенная на этапе поиска/ранжирования.
 */
@Schema(description = "Структурированная цитата источника с метаданными")
public record SourceCitation(
        String sourceName,
        String textSnippet,
        Map<String, Object> metadata,
        String chunkId,
        Float similarityScore
) {
}
