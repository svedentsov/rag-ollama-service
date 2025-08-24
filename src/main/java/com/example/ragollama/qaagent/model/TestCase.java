package com.example.ragollama.qaagent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * DTO для представления одного структурированного тест-кейса.
 *
 * @param title          Краткое и ясное название тест-кейса.
 * @param type           Тип теста ("Positive", "Negative", "Edge Case").
 * @param steps          Упорядоченный список шагов для воспроизведения.
 * @param expectedResult Описание ожидаемого результата после выполнения шагов.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TestCase(
        String title,
        String type,
        List<String> steps,
        String expectedResult
) {
}
