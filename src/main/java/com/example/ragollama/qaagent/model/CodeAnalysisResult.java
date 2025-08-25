package com.example.ragollama.qaagent.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для хранения полного результата структурного анализа одного Java-файла.
 * <p>
 * Этот объект агрегирует всю информацию, извлеченную {@link com.example.ragollama.qaagent.impl.CodeParserAgent},
 * и служит структурированным "продуктом", который могут потреблять другие,
 * более высокоуровневые агенты.
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
