package com.example.ragollama.qaagent.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для структурированного ответа от агента анализа багов.
 * <p>
 * Этот record спроектирован для десериализации JSON-ответа от LLM.
 * Аннотация {@code @JsonIgnoreProperties(ignoreUnknown = true)} делает парсинг
 * более устойчивым к возможным дополнительным полям в ответе модели.
 *
 * @param improvedDescription Улучшенное, структурированное описание бага.
 * @param isDuplicate         Вердикт LLM, является ли отчет дубликатом.
 * @param duplicateCandidates Список ID потенциальных дубликатов.
 */
@Schema(description = "Структурированный результат анализа бага")
@JsonIgnoreProperties(ignoreUnknown = true)
public record BugAnalysisResponse(
        @Schema(description = "Улучшенное и структурированное описание бага")
        String improvedDescription,
        @Schema(description = "Вердикт: является ли отчет дубликатом")
        boolean isDuplicate,
        @Schema(description = "Список ID (sourceName) потенциальных дубликатов")
        List<String> duplicateCandidates
) {
}
