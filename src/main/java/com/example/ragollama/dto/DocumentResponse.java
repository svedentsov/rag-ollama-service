package com.example.ragollama.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для ответа после загрузки документа.
 *
 * @param documentId       Сгенерированный уникальный ID для загруженного документа.
 * @param indexedChunksCount Количество чанков, на которые был разбит и проиндексирован документ.
 */
@Schema(description = "DTO ответа после загрузки документа")
public record DocumentResponse(
        @Schema(description = "Уникальный ID, присвоенный документу", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
        String documentId,
        @Schema(description = "Количество чанков, на которые был разбит документ", example = "15")
        int indexedChunksCount
) {
}
