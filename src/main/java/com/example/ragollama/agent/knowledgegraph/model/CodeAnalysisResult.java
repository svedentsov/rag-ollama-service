package com.example.ragollama.agent.knowledgegraph.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для хранения полного результата структурного анализа одного Java-файла.
 *
 * @param filePath Полный путь к проанализированному файлу.
 * @param methods  Список деталей по каждому публичному методу, найденному в файле.
 */
@Schema(description = "Результат структурного анализа Java-файла")
public record CodeAnalysisResult(
        String filePath,
        List<MethodDetails> methods
) {
}
