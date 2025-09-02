package com.example.ragollama.optimization.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * DTO для структурированного отчета от {@link com.example.ragollama.optimization.InteractionAnalyzerAgent}.
 * <p>
 * Этот record является явным, строго типизированным контрактом, который
 * заменяет собой небезопасное использование {@code Map<String, Object>} для
 * передачи данных между агентами.
 *
 * @param frequentPairs    Статистика по наиболее часто встречающимся последовательностям агентов.
 * @param frequentFailures Статистика по агентам, которые чаще всего завершаются с ошибкой.
 */
@Schema(description = "Отчет об анализе взаимодействий между агентами")
public record InteractionAnalysisReport(
        Map<String, Long> frequentPairs,
        Map<String, Long> frequentFailures
) {
}
