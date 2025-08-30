package com.example.ragollama.agent.testanalysis.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для структурированного результата валидации кода автотеста.
 * <p>
 * Спроектирован для десериализации JSON-ответа от LLM-агента.
 *
 * @param overallScore Общая оценка качества теста по 10-балльной шкале.
 * @param summary      Краткое резюме анализа.
 * @param positives    Список сильных сторон и хороших практик, примененных в тесте.
 * @param suggestions  Список конкретных, действенных рекомендаций по улучшению кода.
 */
@Schema(description = "Структурированный результат анализа качества автотеста")
@JsonIgnoreProperties(ignoreUnknown = true)
public record TestValidationResult(
        int overallScore,
        String summary,
        List<String> positives,
        List<String> suggestions
) {
}
