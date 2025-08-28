package com.example.ragollama.qaagent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для описания одного архитектурного нарушения.
 *
 * @param filePath          Путь к файлу, где обнаружено нарушение.
 * @param componentType     Тип компонента (Controller, Service), определенный AI.
 * @param violatedPrinciple Нарушенный архитектурный принцип.
 * @param description       Подробное описание нарушения.
 * @param codeSnippet       Фрагмент кода, демонстрирующий нарушение.
 * @param recommendation    Предложение по исправлению.
 */
@Schema(description = "Описание одного архитектурного нарушения")
@JsonIgnoreProperties(ignoreUnknown = true)
public record ArchViolation(
        String filePath,
        String componentType,
        String violatedPrinciple,
        String description,
        String codeSnippet,
        String recommendation
) {
}
