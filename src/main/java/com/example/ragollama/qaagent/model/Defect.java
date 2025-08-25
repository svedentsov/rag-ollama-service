package com.example.ragollama.qaagent.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для представления одного дефекта внутри кластера.
 *
 * @param documentId ID документа в векторном хранилище.
 * @param sourceName Имя источника (например, JIRA-тикет).
 * @param content    Часть текста баг-репорта.
 */
@Schema(description = "Представление одного дефекта в кластере")
public record Defect(
        String documentId,
        String sourceName,
        String content
) {
}
