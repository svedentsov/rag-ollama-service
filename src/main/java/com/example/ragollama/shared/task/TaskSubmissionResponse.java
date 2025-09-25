package com.example.ragollama.shared.task;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * DTO для ответа на успешный запуск асинхронной задачи.
 *
 * @param taskId Уникальный идентификатор, присвоенный задаче для отслеживания.
 */
@Schema(description = "DTO ответа на успешный запуск асинхронной задачи")
public record TaskSubmissionResponse(
        @Schema(description = "Уникальный ID, присвоенный задаче для отслеживания",
                example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
        UUID taskId
) {
}
