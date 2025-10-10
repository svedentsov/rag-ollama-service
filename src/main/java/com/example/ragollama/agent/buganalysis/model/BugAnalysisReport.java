package com.example.ragollama.agent.buganalysis.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Публичный DTO для структурированного результата анализа бага.
 * <p>
 * Этот record является стабильным API-контрактом, который возвращается
 * клиенту после работы конвейера анализа баг-репортов.
 *
 * @param improvedDescription Улучшенное, структурированное описание бага.
 * @param isDuplicate         Вердикт, является ли отчет дубликатом.
 * @param duplicateCandidates Список ID потенциальных дубликатов.
 */
@Schema(description = "Структурированный результат анализа бага")
@JsonIgnoreProperties(ignoreUnknown = true)
public record BugAnalysisReport(
        @Schema(description = "Улучшенное и структурированное описание бага")
        BugReportSummary improvedDescription,
        @Schema(description = "Вердикт: является ли отчет дубликатом")
        boolean isDuplicate,
        @Schema(description = "Список ID (sourceName) потенциальных дубликатов")
        List<String> duplicateCandidates
) {
}
