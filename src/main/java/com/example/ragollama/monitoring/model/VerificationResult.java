package com.example.ragollama.monitoring.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * DTO для структурированного результата проверки цитирования источников.
 *
 * @param isValid          Вердикт: true, если ответ полностью основан на контексте и цитирует источники.
 * @param missingCitations Список утверждений из ответа, для которых не найден источник в контексте.
 * @param reasoning        Объяснение LLM-аудитора, почему проверка провалена.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record VerificationResult(
        boolean isValid,
        List<String> missingCitations,
        String reasoning
) {
}
