package com.example.ragollama.qaagent.model;

/**
 * DTO для представления одного сгенерированного тестового файла.
 *
 * @param fileName Имя файла (например, "ApiAdminUsersAuthTest.java").
 * @param content  Полное строковое содержимое файла (Java-код).
 */
public record GeneratedTestFile(
        String fileName,
        String content
) {
}
