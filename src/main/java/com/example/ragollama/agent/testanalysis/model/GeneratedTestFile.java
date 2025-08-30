package com.example.ragollama.agent.testanalysis.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для представления одного сгенерированного тестового файла.
 *
 * @param fileName Имя файла (например, "UserServiceTest.java").
 * @param content  Полное строковое содержимое файла (Java-код).
 */
@Schema(description = "Сгенерированный тестовый файл")
public record GeneratedTestFile(
        String fileName,
        String content
) {
}
