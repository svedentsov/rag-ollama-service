package com.example.ragollama.indexing;

import java.util.Map;

/**
 * Унифицированный DTO для запроса на индексацию данных из любого источника.
 *
 * @param documentId  Уникальный идентификатор документа в источнике.
 * @param sourceName  Человекочитаемое имя источника (например, имя файла, ID тикета).
 * @param textContent Полный текст для индексации.
 * @param metadata    Карта с дополнительными метаданными для фильтрации.
 */
public record IndexingRequest(
        String documentId,
        String sourceName,
        String textContent,
        Map<String, Object> metadata
) {
}
