package com.example.ragollama.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * DTO для ответа на успешную постановку задачи в очередь.
 * Содержит только идентификатор созданной задачи, который клиент может
 * использовать для отслеживания ее статуса.
 *
 * @param jobId Уникальный идентификатор, присвоенный задаче на обработку.
 */
@Schema(description = "DTO ответа на постановку задачи в очередь")
public record JobSubmissionResponse(
        @Schema(description = "Уникальный ID, присвоенный задаче для отслеживания",
                example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
        UUID jobId
) {
}
