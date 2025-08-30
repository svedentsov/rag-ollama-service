package com.example.ragollama.agent.knowledgegraph.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для представления одного узла в графе знаний.
 *
 * @param entityId   Уникальный идентификатор сущности (например, путь к файлу, хэш коммита, ID тикета).
 * @param type       Тип узла ("CodeFile", "Commit", "Requirement").
 * @param properties Карта с дополнительными свойствами узла.
 */
@Schema(description = "Представление одного узла в графе знаний")
public record GraphNode(
        String entityId,
        String type,
        java.util.Map<String, Object> properties
) {
}
