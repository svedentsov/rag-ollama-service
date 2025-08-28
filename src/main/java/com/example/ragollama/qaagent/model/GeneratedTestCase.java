package com.example.ragollama.qaagent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для представления одного сгенерированного тест-кейса.
 *
 * @param testName Имя тестового метода в BDD-стиле.
 * @param testType Тип теста ("Positive", "Negative", "EdgeCase").
 * @param testCode Полный Java-код тестового метода.
 */
@Schema(description = "Один сгенерированный тест-кейс")
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedTestCase(
        String testName,
        String testType,
        String testCode
) {
}
