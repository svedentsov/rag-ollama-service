package com.example.ragollama.evaluation.model;

import java.util.Set;

/**
 * Представляет одну запись в "золотом" датасете для оценки RAG-системы.
 *
 * @param queryId             Уникальный идентификатор вопроса.
 * @param queryText           Текст вопроса пользователя.
 * @param expectedDocumentIds Множество ID документов, которые считаются релевантными
 *                            и должны быть найдены на этапе Retrieval.
 */
public record GoldenRecord(
        String queryId,
        String queryText,
        Set<String> expectedDocumentIds
) {
}
