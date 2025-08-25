package com.example.ragollama.qaagent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для структурированного результата работы Root Cause Analyzer Agent.
 * <p>
 * Спроектирован для десериализации JSON-ответа от LLM.
 *
 * @param mostLikelyCause Краткое, человекочитаемое описание наиболее вероятной причины сбоя.
 * @param confidenceScore Оценка уверенности LLM в своем вердикте (от 0.0 до 1.0).
 * @param reasoning       Пошаговое объяснение, как LLM пришла к своему выводу.
 * @param culpritFile     Путь к файлу, который с наибольшей вероятностью содержит ошибку.
 * @param recommendation  Конкретное предложение по исправлению проблемы.
 */
@Schema(description = "Структурированный результат анализа первопричины")
@JsonIgnoreProperties(ignoreUnknown = true)
public record RcaResult(
        String mostLikelyCause,
        double confidenceScore,
        String reasoning,
        String culpritFile,
        String recommendation
) {
}
