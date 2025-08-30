package com.example.ragollama.shared.model.codeanalysis;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO, содержащий ключевые метрики статического анализа для одного файла.
 *
 * @param cyclomaticComplexity Цикломатическая сложность (количество линейно независимых путей).
 *                             Высокое значение указывает на сложную логику и трудности в тестировании.
 * @param nPathComplexity      NPath сложность (общее количество ациклических путей выполнения).
 *                             Очень высокое значение указывает на комбинаторный взрыв состояний.
 * @param lineCount            Общее количество строк кода в файле.
 */
@Schema(description = "Ключевые метрики статического анализа кода")
public record CodeMetrics(
        int cyclomaticComplexity,
        int nPathComplexity,
        int lineCount
) {
}
