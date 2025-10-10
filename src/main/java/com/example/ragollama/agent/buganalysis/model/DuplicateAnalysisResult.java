package com.example.ragollama.agent.buganalysis.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для инкапсуляции результата анализа на дубликаты.
 * <p>
 * Этот record является внутренним контрактом, который используется парсером
 * для передачи результата агенту.
 *
 * @param isDuplicate         Вердикт LLM, является ли отчет дубликатом.
 * @param duplicateCandidates Список ID потенциальных дубликатов.
 */
@Schema(description = "Результат анализа на дубликаты")
@JsonIgnoreProperties(ignoreUnknown = true)
public record DuplicateAnalysisResult(
        @Schema(description = "Вердикт: является ли отчет дубликатом")
        boolean isDuplicate,
        @Schema(description = "Список ID (sourceName) потенциальных дубликатов")
        List<String> duplicateCandidates
) {
}
