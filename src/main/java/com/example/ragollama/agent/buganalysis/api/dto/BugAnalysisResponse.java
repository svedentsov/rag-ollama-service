package com.example.ragollama.agent.buganalysis.api.dto;

import com.example.ragollama.agent.buganalysis.model.BugReportSummary;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для структурированного ответа от агента анализа багов.
 * <p>
 * Эта версия принимает улучшенное описание бага в виде структурированного
 * объекта {@link BugReportSummary}, что соответствует более качественному
 * выводу от LLM и обогащает контракт API.
 *
 * @param improvedDescription Улучшенное, структурированное описание бага.
 * @param isDuplicate         Вердикт LLM, является ли отчет дубликатом.
 * @param duplicateCandidates Список ID потенциальных дубликатов.
 */
@Schema(description = "Структурированный результат анализа бага")
@JsonIgnoreProperties(ignoreUnknown = true)
public record BugAnalysisResponse(
        @Schema(description = "Улучшенное и структурированное описание бага")
        BugReportSummary improvedDescription,
        @Schema(description = "Вердикт: является ли отчет дубликатом")
        boolean isDuplicate,
        @Schema(description = "Список ID (sourceName) потенциальных дубликатов")
        List<String> duplicateCandidates
) {
}
